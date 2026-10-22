package dev.vessel.kernel.network.bedrock;

import dev.vessel.kernel.optimization.network.FastVarIntCodec;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * High-throughput batch packet compressor and decompressor for Minecraft Bedrock Edition RakNet packets.
 * <p>
 * Implements Bedrock Protocol batch framing (VarInt-delimited packet payloads) compressed via Zlib/Deflate.
 * Employs reusable {@link ThreadLocal} deflaters and inflaters to achieve minimal allocation latency
 * and avoid interference with Java Edition single-packet compression pipelines.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class BedrockBatchPacketCompressor {

    /** Bedrock RakNet batch packet wrapper byte ID (0xFE / 254). */
    public static final byte BEDROCK_BATCH_PACKET_ID = (byte) 0xFE;

    /** Default compression level for Bedrock packets (Level 7 represents standard balance). */
    public static final int DEFAULT_COMPRESSION_LEVEL = 7;

    /** Default maximum allowable uncompressed batch payload size (16 MB) to prevent zip-bomb attacks. */
    public static final int DEFAULT_MAX_UNCOMPRESSED_SIZE = 16 * 1024 * 1024;

    private static final int BUFFER_CHUNK_SIZE = 4096;

    private final int defaultCompressionLevel;
    private final int maxUncompressedSize;

    private final ThreadLocal<Deflater> deflaterCache;
    private final ThreadLocal<Inflater> inflaterCache;
    private final ThreadLocal<byte[]> chunkBufferCache;

    /**
     * Constructs a compressor with default parameters (level 7, 16MB max uncompressed payload).
     */
    public BedrockBatchPacketCompressor() {
        this(DEFAULT_COMPRESSION_LEVEL, DEFAULT_MAX_UNCOMPRESSED_SIZE);
    }

    /**
     * Constructs a compressor with custom parameters.
     *
     * @param defaultCompressionLevel default zlib compression level (0-9)
     * @param maxUncompressedSize maximum allowed uncompressed payload size in bytes
     */
    public BedrockBatchPacketCompressor(int defaultCompressionLevel, int maxUncompressedSize) {
        if (defaultCompressionLevel < Deflater.NO_COMPRESSION || defaultCompressionLevel > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Invalid compression level: " + defaultCompressionLevel);
        }
        if (maxUncompressedSize <= 0) {
            throw new IllegalArgumentException("maxUncompressedSize must be positive: " + maxUncompressedSize);
        }
        this.defaultCompressionLevel = defaultCompressionLevel;
        this.maxUncompressedSize = maxUncompressedSize;

        this.deflaterCache = ThreadLocal.withInitial(() -> new Deflater(defaultCompressionLevel));
        this.inflaterCache = ThreadLocal.withInitial(Inflater::new);
        this.chunkBufferCache = ThreadLocal.withInitial(() -> new byte[BUFFER_CHUNK_SIZE]);
    }

    /**
     * Compresses a raw uncompressed batch byte buffer using default compression level.
     *
     * @param rawPayload uncompressed batch byte array
     * @return compressed byte array
     */
    public byte[] compressRawBatch(byte[] rawPayload) {
        return compressRawBatch(rawPayload, defaultCompressionLevel);
    }

    /**
     * Compresses a raw uncompressed batch byte buffer using specified compression level.
     *
     * @param rawPayload uncompressed batch byte array
     * @param compressionLevel compression level (0-9)
     * @return compressed byte array
     */
    public byte[] compressRawBatch(byte[] rawPayload, int compressionLevel) {
        Objects.requireNonNull(rawPayload, "rawPayload cannot be null");
        Deflater deflater = deflaterCache.get();
        deflater.reset();
        deflater.setLevel(compressionLevel);
        deflater.setInput(rawPayload);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(64, rawPayload.length / 2));
        byte[] buffer = chunkBufferCache.get();

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }

        return baos.toByteArray();
    }

    /**
     * Decompresses a compressed Bedrock batch payload up to the default maximum size.
     *
     * @param compressedPayload compressed bytes
     * @return uncompressed byte array
     */
    public byte[] decompressRawBatch(byte[] compressedPayload) {
        return decompressRawBatch(compressedPayload, maxUncompressedSize);
    }

    /**
     * Decompresses a compressed Bedrock batch payload with explicit maximum size cap.
     *
     * @param compressedPayload compressed bytes
     * @param maxAllowedBytes safety ceiling for uncompressed output
     * @return uncompressed byte array
     */
    public byte[] decompressRawBatch(byte[] compressedPayload, int maxAllowedBytes) {
        Objects.requireNonNull(compressedPayload, "compressedPayload cannot be null");
        if (maxAllowedBytes <= 0) {
            throw new IllegalArgumentException("maxAllowedBytes must be positive: " + maxAllowedBytes);
        }
        if (compressedPayload.length == 0) {
            return new byte[0];
        }

        Inflater inflater = inflaterCache.get();
        inflater.reset();
        inflater.setInput(compressedPayload);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(maxAllowedBytes, compressedPayload.length * 2));
        byte[] buffer = chunkBufferCache.get();

        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0) {
                    if (inflater.needsInput()) {
                        break;
                    }
                    if (inflater.needsDictionary()) {
                        throw new IllegalArgumentException("Zlib preset dictionary required but not supported");
                    }
                }
                baos.write(buffer, 0, count);
                if (baos.size() > maxAllowedBytes) {
                    throw new IllegalStateException("Decompressed batch size exceeded maximum limit of " + maxAllowedBytes + " bytes");
                }
            }

            if (!inflater.finished()) {
                throw new IllegalArgumentException("Truncated or incomplete Deflate/Zlib batch packet payload");
            }

            return baos.toByteArray();
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Corrupted or invalid Deflate/Zlib batch packet payload", e);
        }
    }

    /**
     * Assembles individual packet payloads into a single Bedrock uncompressed batch format
     * where each packet is prefixed by its VarInt length.
     *
     * @param packets array of packet byte arrays
     * @return raw assembled batch byte array
     */
    public byte[] assembleRawBatch(byte[][] packets) {
        Objects.requireNonNull(packets, "packets cannot be null");
        int totalSize = 0;
        for (byte[] packet : packets) {
            Objects.requireNonNull(packet, "packet payload cannot be null");
            totalSize += FastVarIntCodec.getVarIntSize(packet.length) + packet.length;
        }

        byte[] assembled = new byte[totalSize];
        int writePos = 0;
        for (byte[] packet : packets) {
            int writtenVarInt = FastVarIntCodec.writeVarInt(packet.length, assembled, writePos);
            writePos += writtenVarInt;
            System.arraycopy(packet, 0, assembled, writePos, packet.length);
            writePos += packet.length;
        }

        return assembled;
    }

    /**
     * Unpacks an uncompressed Bedrock batch payload into discrete individual packet byte arrays.
     *
     * @param uncompressedBatch raw uncompressed batch bytes
     * @return list of individual packet payloads
     */
    public List<byte[]> unpackRawBatch(byte[] uncompressedBatch) {
        Objects.requireNonNull(uncompressedBatch, "uncompressedBatch cannot be null");
        List<byte[]> packets = new ArrayList<>();
        int readPos = 0;

        while (readPos < uncompressedBatch.length) {
            long packed = FastVarIntCodec.readVarIntPacked(uncompressedBatch, readPos);
            int packetLength = FastVarIntCodec.unpackValue(packed);
            int varIntBytes = FastVarIntCodec.unpackBytesRead(packed);
            readPos += varIntBytes;

            if (packetLength < 0 || readPos + packetLength > uncompressedBatch.length) {
                throw new IllegalStateException(
                        String.format("Invalid packet length %d at offset %d (batch length %d)", packetLength, readPos, uncompressedBatch.length)
                );
            }

            byte[] packet = new byte[packetLength];
            System.arraycopy(uncompressedBatch, readPos, packet, 0, packetLength);
            packets.add(packet);
            readPos += packetLength;
        }

        return packets;
    }

    /**
     * Full assembly and compression: prefixes each packet with VarInt length, concatenates, and deflates.
     *
     * @param packets discrete packets
     * @param compressionLevel zlib compression level
     * @return compressed batch payload
     */
    public byte[] assembleAndCompressBatch(byte[][] packets, int compressionLevel) {
        byte[] rawBatch = assembleRawBatch(packets);
        return compressRawBatch(rawBatch, compressionLevel);
    }

    /**
     * Full assembly and compression using default compression level.
     *
     * @param packets discrete packets
     * @return compressed batch payload
     */
    public byte[] assembleAndCompressBatch(byte[][] packets) {
        return assembleAndCompressBatch(packets, defaultCompressionLevel);
    }

    /**
     * Full decompression and unpacking: inflates batch payload and unpacks individual VarInt-framed packets.
     *
     * @param compressedPayload compressed batch bytes
     * @param maxAllowedBytes safety ceiling
     * @return list of individual packet payloads
     */
    public List<byte[]> decompressAndUnpackBatch(byte[] compressedPayload, int maxAllowedBytes) {
        byte[] rawBatch = decompressRawBatch(compressedPayload, maxAllowedBytes);
        return unpackRawBatch(rawBatch);
    }

    /**
     * Full decompression and unpacking using default max payload ceiling.
     *
     * @param compressedPayload compressed batch bytes
     * @return list of individual packet payloads
     */
    public List<byte[]> decompressAndUnpackBatch(byte[] compressedPayload) {
        return decompressAndUnpackBatch(compressedPayload, maxUncompressedSize);
    }

    /**
     * Checks if a raw network packet is a Bedrock RakNet batch packet.
     *
     * @param packet incoming network packet bytes
     * @return true if packet is non-null and starts with {@link #BEDROCK_BATCH_PACKET_ID}
     */
    public boolean isBatchPacket(byte[] packet) {
        return packet != null && packet.length > 0 && packet[0] == BEDROCK_BATCH_PACKET_ID;
    }

    /**
     * Wraps a compressed batch payload into a Bedrock RakNet game batch packet by prefixing 0xFE.
     *
     * @param compressedPayload compressed batch bytes
     * @return RakNet-ready batch packet
     */
    public byte[] wrapBatchPacket(byte[] compressedPayload) {
        Objects.requireNonNull(compressedPayload, "compressedPayload cannot be null");
        byte[] packet = new byte[1 + compressedPayload.length];
        packet[0] = BEDROCK_BATCH_PACKET_ID;
        System.arraycopy(compressedPayload, 0, packet, 1, compressedPayload.length);
        return packet;
    }

    /**
     * Unwraps a Bedrock RakNet game batch packet (stripping 0xFE) and decompresses its packets.
     *
     * @param rakNetPacket raw RakNet packet starting with 0xFE
     * @return list of individual unpacked packet payloads
     */
    public List<byte[]> unwrapAndDecompressBatchPacket(byte[] rakNetPacket) {
        Objects.requireNonNull(rakNetPacket, "rakNetPacket cannot be null");
        if (rakNetPacket.length == 0 || rakNetPacket[0] != BEDROCK_BATCH_PACKET_ID) {
            throw new IllegalArgumentException("Invalid Bedrock batch packet header: expected 0xFE (254)");
        }
        byte[] compressedPayload = new byte[rakNetPacket.length - 1];
        System.arraycopy(rakNetPacket, 1, compressedPayload, 0, compressedPayload.length);
        return decompressAndUnpackBatch(compressedPayload);
    }
}
