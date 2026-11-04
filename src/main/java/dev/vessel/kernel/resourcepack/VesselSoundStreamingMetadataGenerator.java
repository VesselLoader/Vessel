package dev.vessel.kernel.resourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Generator and validator for Minecraft resource pack {@code sounds.json} metadata.
 * <p>
 * Analyzes {@code .ogg} Vorbis audio streams. If an audio file exceeds 500 KB in size
 * OR 5.0 seconds in duration, automatically flags {@code "stream": true} in the sound
 * event definition to prevent the vanilla Java client from decompressing the full PCM waveform
 * into the client RAM Heap and causing lag spikes or OutOfMemory errors.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 * <!-- Changelog:
 *   1.0.0: Initial implementation of OGG Vorbis acoustic inspection and sounds.json streaming injector.
 *   1.0.1: Added Windows path normalization, event-type alias filtering, and hardened chained OGG loop termination.
 * -->
 */
public final class VesselSoundStreamingMetadataGenerator {

    /** Size threshold in bytes (500 KB): files larger than this must be streamed. */
    public static final long STREAMING_SIZE_THRESHOLD_BYTES = 500 * 1024L;

    /** Duration threshold in seconds (5.0s): audio tracks longer than this must be streamed. */
    public static final double STREAMING_DURATION_THRESHOLD_SECONDS = 5.0;

    /** Ogg page capture pattern ('OggS'). */
    private static final byte[] OGG_MAGIC = new byte[]{'O', 'g', 'g', 'S'};

    /** Vorbis header identification string ('vorbis'). */
    private static final byte[] VORBIS_MAGIC = new byte[]{'v', 'o', 'r', 'b', 'i', 's'};

    private final Gson gson;

    public VesselSoundStreamingMetadataGenerator() {
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    /**
     * Determines whether an audio file should have the {@code "stream": true} flag set
     * based on its file size and duration.
     *
     * @param fileSizeBytes file size in bytes
     * @param durationSeconds duration in seconds
     * @return true if file exceeds 500KB or 5 seconds
     */
    public boolean shouldStream(long fileSizeBytes, double durationSeconds) {
        return fileSizeBytes > STREAMING_SIZE_THRESHOLD_BYTES || durationSeconds > STREAMING_DURATION_THRESHOLD_SECONDS;
    }

    /**
     * Determines whether raw Ogg audio bytes should be streamed.
     *
     * @param oggBytes raw Ogg file content
     * @return true if stream flag is required
     */
    public boolean shouldStream(byte[] oggBytes) {
        Objects.requireNonNull(oggBytes, "oggBytes cannot be null");
        return analyzeOgg(oggBytes).shouldStream();
    }

    /**
     * Parses an Ogg Vorbis bitstream to extract sampling rate, audio channels, and total PCM duration.
     *
     * @param oggData raw Ogg Vorbis binary bytes
     * @return sound metadata container
     */
    public VesselSoundMetadata analyzeOgg(byte[] oggData) {
        Objects.requireNonNull(oggData, "oggData cannot be null");
        long size = oggData.length;
        if (size < 28) {
            // Insufficient bytes for a valid Ogg page header
            return new VesselSoundMetadata(size, 0.0, 0, 0, size > STREAMING_SIZE_THRESHOLD_BYTES);
        }

        int sampleRate = 0;
        int channels = 0;
        long maxGranulePosition = 0;

        int offset = 0;
        while (offset + 27 <= size) {
            // Verify 'OggS' capture pattern
            if (oggData[offset] != OGG_MAGIC[0] || oggData[offset + 1] != OGG_MAGIC[1]
                    || oggData[offset + 2] != OGG_MAGIC[2] || oggData[offset + 3] != OGG_MAGIC[3]) {
                break; // Corrupted or non-Ogg stream boundary
            }

            int headerType = oggData[offset + 5] & 0xFF;
            long granulePos = readLittleEndian64(oggData, offset + 6);
            if (granulePos > 0 && granulePos > maxGranulePosition) {
                maxGranulePosition = granulePos;
            }

            int numSegments = oggData[offset + 26] & 0xFF;
            if (offset + 27 + numSegments > size) {
                break; // Page header truncated
            }

            int payloadLength = 0;
            for (int s = 0; s < numSegments; s++) {
                payloadLength += (oggData[offset + 27 + s] & 0xFF);
            }

            int pageTotalLength = 27 + numSegments + payloadLength;
            if (pageTotalLength <= 0 || offset + pageTotalLength > size) {
                break; // Page corrupted or payload truncated
            }

            int payloadStart = offset + 27 + numSegments;

            // Check if this is the Beginning Of Stream (BOS) page containing the Vorbis ID header
            if ((headerType & 0x02) != 0 && payloadLength >= 30 && payloadStart + 30 <= size) {
                // Vorbis ID header packet starts with packet_type = 0x01 followed by 'vorbis'
                if (oggData[payloadStart] == 0x01 && matchesMagic(oggData, payloadStart + 1, VORBIS_MAGIC)) {
                    channels = oggData[payloadStart + 11] & 0xFF;
                    sampleRate = readLittleEndian32(oggData, payloadStart + 12);
                }
            }

            // Check if End Of Stream (EOS) page
            if ((headerType & 0x04) != 0 && granulePos > 0) {
                if (granulePos > maxGranulePosition) {
                    maxGranulePosition = granulePos;
                }
            }

            offset += pageTotalLength;
        }

        double duration = 0.0;
        if (sampleRate > 0 && maxGranulePosition > 0) {
            duration = (double) maxGranulePosition / (double) sampleRate;
        }

        boolean stream = shouldStream(size, duration);
        return new VesselSoundMetadata(size, duration, sampleRate, channels, stream);
    }

    /**
     * Inspects and updates a {@code sounds.json} document by examining referenced {@code .ogg} files
     * and injecting {@code "stream": true} wherever required.
     *
     * @param soundsJsonContent existing JSON string of sounds.json
     * @param soundFiles map of sound file paths or sound IDs to raw .ogg audio bytes
     * @return updated formatted sounds.json content
     */
    public String updateSoundsJson(String soundsJsonContent, Map<String, byte[]> soundFiles) {
        Objects.requireNonNull(soundsJsonContent, "soundsJsonContent cannot be null");
        Objects.requireNonNull(soundFiles, "soundFiles cannot be null");

        JsonElement parsed = JsonParser.parseString(soundsJsonContent);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("Invalid sounds.json root: expected JSON object");
        }

        JsonObject root = parsed.getAsJsonObject();
        updateSoundsJsonObject(root, soundFiles);
        return gson.toJson(root);
    }

    /**
     * Updates an in-memory {@link JsonObject} representing {@code sounds.json}.
     *
     * @param root sounds.json root object
     * @param soundFiles map of sound file paths or sound IDs to raw .ogg audio bytes
     */
    public void updateSoundsJsonObject(JsonObject root, Map<String, byte[]> soundFiles) {
        Objects.requireNonNull(root, "root cannot be null");
        Objects.requireNonNull(soundFiles, "soundFiles cannot be null");

        for (Map.Entry<String, JsonElement> eventEntry : root.entrySet()) {
            if (!eventEntry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject eventObj = eventEntry.getValue().getAsJsonObject();
            if (!eventObj.has("sounds") || !eventObj.get("sounds").isJsonArray()) {
                continue;
            }

            JsonArray soundsArray = eventObj.getAsJsonArray("sounds");
            for (int i = 0; i < soundsArray.size(); i++) {
                JsonElement soundElem = soundsArray.get(i);

                if (soundElem.isJsonPrimitive() && soundElem.getAsJsonPrimitive().isString()) {
                    String soundName = soundElem.getAsString();
                    byte[] oggData = resolveSoundData(soundName, soundFiles);
                    if (oggData != null && shouldStream(oggData)) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("name", soundName);
                        obj.addProperty("stream", true);
                        soundsArray.set(i, obj);
                    }
                } else if (soundElem.isJsonObject()) {
                    JsonObject soundObj = soundElem.getAsJsonObject();
                    // Skip sound event references ("type": "event")
                    if (soundObj.has("type") && "event".equalsIgnoreCase(soundObj.get("type").getAsString())) {
                        continue;
                    }
                    if (soundObj.has("name") && soundObj.get("name").isJsonPrimitive()) {
                        String soundName = soundObj.get("name").getAsString();
                        byte[] oggData = resolveSoundData(soundName, soundFiles);
                        if (oggData != null && shouldStream(oggData)) {
                            soundObj.addProperty("stream", true);
                        }
                    }
                }
            }
        }
    }

    private byte[] resolveSoundData(String soundName, Map<String, byte[]> soundFiles) {
        if (soundName == null || soundName.isBlank() || soundFiles.isEmpty()) {
            return null;
        }

        // Direct match
        if (soundFiles.containsKey(soundName)) {
            return soundFiles.get(soundName);
        }
        if (soundFiles.containsKey(soundName + ".ogg")) {
            return soundFiles.get(soundName + ".ogg");
        }

        // Parse namespace:path or path
        String namespace = "minecraft";
        String path = soundName.trim().replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        int colonIdx = path.indexOf(':');
        if (colonIdx >= 0) {
            namespace = path.substring(0, colonIdx);
            path = path.substring(colonIdx + 1);
        }

        String[] candidates = new String[]{
                "assets/" + namespace + "/sounds/" + path + ".ogg",
                "assets/" + namespace + "/sounds/" + path,
                namespace + "/sounds/" + path + ".ogg",
                "sounds/" + path + ".ogg",
                path + ".ogg",
                path
        };

        for (String cand : candidates) {
            if (soundFiles.containsKey(cand)) {
                return soundFiles.get(cand);
            }
        }

        // Fallback for case-insensitivity or Windows backslashes
        for (Map.Entry<String, byte[]> entry : soundFiles.entrySet()) {
            String normalizedKey = entry.getKey().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
            while (normalizedKey.startsWith("/")) {
                normalizedKey = normalizedKey.substring(1);
            }
            for (String cand : candidates) {
                if (normalizedKey.equalsIgnoreCase(cand)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    private static boolean matchesMagic(byte[] data, int offset, byte[] magic) {
        if (offset + magic.length > data.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static int readLittleEndian32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private static long readLittleEndian64(byte[] data, int offset) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result |= ((long) (data[offset + i] & 0xFF)) << (i * 8);
        }
        return result;
    }
}
