package dev.vessel.kernel.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.vessel.kernel.resourcepack.VesselSoundMetadata;
import dev.vessel.kernel.resourcepack.VesselSoundStreamingMetadataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for {@link VesselSoundStreamingMetadataGenerator}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VesselSoundStreamingMetadataGeneratorTest {

    private byte[] createMockOggVorbis(int sampleRate, int channels, long totalSamples, int extraPaddingBytes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // --- Page 1: BOS Page (Vorbis Identification Header) ---
        // Capture pattern
        baos.write('O'); baos.write('g'); baos.write('g'); baos.write('S');
        baos.write(0); // version
        baos.write(0x02); // header type (BOS)
        // Granule position 0 (8 bytes)
        for (int i = 0; i < 8; i++) baos.write(0);
        // Bitstream serial number
        baos.write(0x12); baos.write(0x34); baos.write(0x56); baos.write(0x78);
        // Page sequence number 0
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        // Checksum (4 bytes)
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        // Segment count
        baos.write(1);
        // Segment length 30
        baos.write(30);

        // Vorbis ID packet (30 bytes)
        baos.write(0x01); // packet type
        baos.write('v'); baos.write('o'); baos.write('r'); baos.write('b'); baos.write('i'); baos.write('s');
        // vorbis version = 0 (4 bytes)
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        // channels
        baos.write(channels & 0xFF);
        // sample rate (little endian 4 bytes)
        baos.write(sampleRate & 0xFF);
        baos.write((sampleRate >> 8) & 0xFF);
        baos.write((sampleRate >> 16) & 0xFF);
        baos.write((sampleRate >> 24) & 0xFF);
        // bitrate maximum
        for (int i = 0; i < 4; i++) baos.write(0);
        // bitrate nominal
        for (int i = 0; i < 4; i++) baos.write(0);
        // bitrate minimum
        for (int i = 0; i < 4; i++) baos.write(0);
        // blocksize & framing
        baos.write(0);
        baos.write(1);

        // --- Page 2: EOS Page with total samples in granule position ---
        baos.write('O'); baos.write('g'); baos.write('g'); baos.write('S');
        baos.write(0);
        baos.write(0x04); // EOS flag
        // Granule position (little endian 8 bytes)
        for (int i = 0; i < 8; i++) {
            baos.write((int) ((totalSamples >> (i * 8)) & 0xFF));
        }
        // Bitstream serial
        baos.write(0x12); baos.write(0x34); baos.write(0x56); baos.write(0x78);
        // Page sequence number 1
        baos.write(1); baos.write(0); baos.write(0); baos.write(0);
        // Checksum
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        // Segment count
        baos.write(1);
        baos.write(1);
        baos.write(0); // 1 byte audio packet

        // Optional extra bytes to test size threshold
        if (extraPaddingBytes > 0) {
            baos.write(new byte[extraPaddingBytes], 0, extraPaddingBytes);
        }

        return baos.toByteArray();
    }

    @Test
    @DisplayName("shouldStream correctly evaluates size and duration thresholds")
    void testStreamingThresholdLogic() {
        VesselSoundStreamingMetadataGenerator generator = new VesselSoundStreamingMetadataGenerator();

        // 400KB and 4 seconds: below both thresholds -> false
        assertThat(generator.shouldStream(400 * 1024, 4.0)).isFalse();

        // 501KB and 2 seconds: size exceeds 500KB -> true
        assertThat(generator.shouldStream(501 * 1024, 2.0)).isTrue();

        // 100KB and 6 seconds: duration exceeds 5.0s -> true
        assertThat(generator.shouldStream(100 * 1024, 6.0)).isTrue();

        // 1MB and 60 seconds: both exceeded -> true
        assertThat(generator.shouldStream(1024 * 1024, 60.0)).isTrue();

        // Exact boundary checks
        assertThat(generator.shouldStream(500 * 1024, 5.0)).isFalse();
        assertThat(generator.shouldStream(500 * 1024 + 1, 5.0)).isTrue();
        assertThat(generator.shouldStream(500 * 1024, 5.001)).isTrue();
    }

    @Test
    @DisplayName("analyzeOgg accurately extracts sampling rate, channels, and duration from Ogg Vorbis stream")
    void testOggVorbisParsing() {
        VesselSoundStreamingMetadataGenerator generator = new VesselSoundStreamingMetadataGenerator();

        // 48000 Hz, stereo (2 channels), 288,000 samples = 6.0 seconds (> 5.0s threshold)
        byte[] oggLong = createMockOggVorbis(48000, 2, 288000, 0);
        VesselSoundMetadata metaLong = generator.analyzeOgg(oggLong);

        assertThat(metaLong.sampleRate()).isEqualTo(48000);
        assertThat(metaLong.channels()).isEqualTo(2);
        assertThat(metaLong.durationSeconds()).isEqualTo(6.0);
        assertThat(metaLong.shouldStream()).isTrue();

        // 44100 Hz, mono (1 channel), 88,200 samples = 2.0 seconds (< 5.0s threshold, < 500KB)
        byte[] oggShort = createMockOggVorbis(44100, 1, 88200, 0);
        VesselSoundMetadata metaShort = generator.analyzeOgg(oggShort);

        assertThat(metaShort.sampleRate()).isEqualTo(44100);
        assertThat(metaShort.channels()).isEqualTo(1);
        assertThat(metaShort.durationSeconds()).isEqualTo(2.0);
        assertThat(metaShort.shouldStream()).isFalse();

        // Short duration but size > 500KB
        byte[] oggHeavy = createMockOggVorbis(44100, 1, 44100, 520 * 1024);
        VesselSoundMetadata metaHeavy = generator.analyzeOgg(oggHeavy);
        assertThat(metaHeavy.durationSeconds()).isEqualTo(1.0);
        assertThat(metaHeavy.shouldStream()).isTrue();
    }

    @Test
    @DisplayName("updateSoundsJson automatically injects stream: true for sounds exceeding thresholds")
    void testSoundsJsonUpdate() {
        VesselSoundStreamingMetadataGenerator generator = new VesselSoundStreamingMetadataGenerator();

        // 6-second audio track (should stream)
        byte[] longOgg = createMockOggVorbis(48000, 2, 288000, 0);
        // 1-second audio track (should not stream)
        byte[] shortOgg = createMockOggVorbis(44100, 1, 44100, 0);

        Map<String, byte[]> soundFiles = new HashMap<>();
        soundFiles.put("assets/techmod/sounds/ambient/reactor_hum.ogg", longOgg);
        soundFiles.put("assets/techmod/sounds/item/click.ogg", shortOgg);

        String initialJson = """
        {
          "techmod:reactor_ambient": {
            "category": "ambient",
            "sounds": [
              "techmod:ambient/reactor_hum"
            ]
          },
          "techmod:ui_click": {
            "category": "player",
            "sounds": [
              "techmod:item/click"
            ]
          }
        }
        """;

        String updatedJson = generator.updateSoundsJson(initialJson, soundFiles);
        JsonObject parsed = JsonParser.parseString(updatedJson).getAsJsonObject();

        // Verify reactor_ambient has "stream": true
        JsonObject reactorEvent = parsed.getAsJsonObject("techmod:reactor_ambient");
        JsonArray reactorSounds = reactorEvent.getAsJsonArray("sounds");
        assertThat(reactorSounds.size()).isEqualTo(1);
        assertThat(reactorSounds.get(0).isJsonObject()).isTrue();
        JsonObject humObj = reactorSounds.get(0).getAsJsonObject();
        assertThat(humObj.get("name").getAsString()).isEqualTo("techmod:ambient/reactor_hum");
        assertThat(humObj.get("stream").getAsBoolean()).isTrue();

        // Verify ui_click is NOT forced to stream
        JsonObject clickEvent = parsed.getAsJsonObject("techmod:ui_click");
        JsonArray clickSounds = clickEvent.getAsJsonArray("sounds");
        assertThat(clickSounds.size()).isEqualTo(1);
        // Stays string or doesn't have stream: true
        if (clickSounds.get(0).isJsonObject()) {
            assertThat(clickSounds.get(0).getAsJsonObject().has("stream")).isFalse();
        } else {
            assertThat(clickSounds.get(0).getAsString()).isEqualTo("techmod:item/click");
        }
    }

    @Test
    @DisplayName("updateSoundsJson handles Windows backslashes and skips event-type sound entries")
    void testWindowsPathsAndEventAliases() {
        VesselSoundStreamingMetadataGenerator generator = new VesselSoundStreamingMetadataGenerator();
        byte[] longOgg = createMockOggVorbis(48000, 2, 288000, 0);

        Map<String, byte[]> soundFiles = new HashMap<>();
        soundFiles.put("assets\\custommod\\sounds\\ambient\\thunder.ogg", longOgg);

        String json = """
        {
          "custommod:thunder": {
            "sounds": [
              "custommod:ambient/thunder",
              {
                "name": "minecraft:entity.lightning_bolt.thunder",
                "type": "event"
              }
            ]
          }
        }
        """;

        String updated = generator.updateSoundsJson(json, soundFiles);
        JsonObject parsed = JsonParser.parseString(updated).getAsJsonObject();
        JsonArray sounds = parsed.getAsJsonObject("custommod:thunder").getAsJsonArray("sounds");

        // First sound should be streamed
        JsonObject sound0 = sounds.get(0).getAsJsonObject();
        assertThat(sound0.get("name").getAsString()).isEqualTo("custommod:ambient/thunder");
        assertThat(sound0.get("stream").getAsBoolean()).isTrue();

        // Second sound (type: event) must NOT have stream: true injected
        JsonObject sound1 = sounds.get(1).getAsJsonObject();
        assertThat(sound1.get("type").getAsString()).isEqualTo("event");
        assertThat(sound1.has("stream")).isFalse();
    }
}
