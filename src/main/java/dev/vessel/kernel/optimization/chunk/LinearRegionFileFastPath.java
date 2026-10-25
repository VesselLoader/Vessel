package dev.vessel.kernel.optimization.chunk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * High-performance off-heap direct byte buffer region file handler for Anvil and Linear region storage.
 * Eliminates I/O stuttering and GC pauses when players travel at extreme speeds (Elytra, Teleportation)
 * by streaming compressed chunk payloads through pre-allocated direct buffers.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LinearRegionFileFastPath implements Closeable {

    public static final int CHUNKS_PER_REGION_SIDE = 32;
    public static final int TOTAL_CHUNKS = CHUNKS_PER_REGION_SIDE * CHUNKS_PER_REGION_SIDE; // 1024
    public static final int SECTOR_BYTES = 4096;
    public static final int HEADER_SECTORS = 2; // Sector 0: locations, Sector 1: timestamps

    private final int regionX;
    private final int regionZ;
    private final RegionChunkLocation[] locations = new RegionChunkLocation[TOTAL_CHUNKS];
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private ByteBuffer regionBuffer; // Direct off-heap buffer simulating region file
    private int nextFreeSector = HEADER_SECTORS;
    private volatile boolean closed = false;

    /**
     * Constructs a LinearRegionFileFastPath with an initial direct off-heap buffer capacity.
     *
     * @param regionX region coordinate X
     * @param regionZ region coordinate Z
     * @param initialCapacityBytes initial direct buffer capacity (aligned to 4KB sectors)
     */
    public LinearRegionFileFastPath(int regionX, int regionZ, int initialCapacityBytes) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        int capacity = Math.max(initialCapacityBytes, HEADER_SECTORS * SECTOR_BYTES);
        // Direct off-heap byte buffer allocation
        this.regionBuffer = ByteBuffer.allocateDirect(capacity);
        for (int i = 0; i < TOTAL_CHUNKS; i++) {
            locations[i] = RegionChunkLocation.EMPTY;
        }
    }

    /**
     * Calculates the 0..1023 index for chunk coordinates inside this region.
     */
    public static int getChunkIndex(int chunkX, int chunkZ) {
        int localX = chunkX & (CHUNKS_PER_REGION_SIDE - 1);
        int localZ = chunkZ & (CHUNKS_PER_REGION_SIDE - 1);
        return localX + localZ * CHUNKS_PER_REGION_SIDE;
    }

    /**
     * Checks if a chunk is recorded in this region file.
     *
     * @param chunkX chunk coordinate X
     * @param chunkZ chunk coordinate Z
     * @return true if chunk exists and has non-zero sectors
     */
    public boolean hasChunk(int chunkX, int chunkZ) {
        rwLock.readLock().lock();
        try {
            int idx = getChunkIndex(chunkX, chunkZ);
            return !locations[idx].isEmpty();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the sector location descriptor for a chunk.
     *
     * @param chunkX chunk coordinate X
     * @param chunkZ chunk coordinate Z
     * @return location descriptor
     */
    public RegionChunkLocation getChunkLocation(int chunkX, int chunkZ) {
        rwLock.readLock().lock();
        try {
            return locations[getChunkIndex(chunkX, chunkZ)];
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Compresses and writes chunk payload directly into the off-heap region buffer.
     *
     * @param chunkX chunk coordinate X
     * @param chunkZ chunk coordinate Z
     * @param uncompressedData raw uncompressed NBT or chunk bytes
     * @param format compression format (DEFLATE, GZIP, or UNCOMPRESSED)
     * @throws IOException on write or compression failure
     */
    public void writeChunk(int chunkX, int chunkZ, byte[] uncompressedData, RegionCompressionFormat format) throws IOException {
        Objects.requireNonNull(uncompressedData, "uncompressedData cannot be null");
        Objects.requireNonNull(format, "format cannot be null");
        checkNotClosed();

        byte[] compressed = compress(uncompressedData, format);
        int totalPayloadBytes = 4 + 1 + compressed.length; // 4-byte length + 1-byte compression type + data
        int sectorsNeeded = (totalPayloadBytes + SECTOR_BYTES - 1) / SECTOR_BYTES;

        rwLock.writeLock().lock();
        try {
            int idx = getChunkIndex(chunkX, chunkZ);
            int sectorOffset = nextFreeSector;
            int requiredCapacity = (sectorOffset + sectorsNeeded) * SECTOR_BYTES;

            ensureCapacity(requiredCapacity);

            int writePos = sectorOffset * SECTOR_BYTES;
            regionBuffer.position(writePos);
            regionBuffer.putInt(compressed.length + 1); // Length includes format byte
            regionBuffer.put(format.typeId());
            regionBuffer.put(compressed);

            int now = (int) (System.currentTimeMillis() / 1000L);
            locations[idx] = new RegionChunkLocation(sectorOffset, sectorsNeeded, now);
            nextFreeSector += sectorsNeeded;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Reads and decompresses chunk payload directly from the off-heap region buffer.
     *
     * @param chunkX chunk coordinate X
     * @param chunkZ chunk coordinate Z
     * @return uncompressed chunk bytes, or null if absent
     * @throws IOException on decompression failure
     */
    public byte[] readChunk(int chunkX, int chunkZ) throws IOException {
        checkNotClosed();
        rwLock.readLock().lock();
        try {
            int idx = getChunkIndex(chunkX, chunkZ);
            RegionChunkLocation loc = locations[idx];
            if (loc.isEmpty()) {
                return null;
            }

            int readPos = loc.sectorOffset() * SECTOR_BYTES;
            regionBuffer.position(readPos);
            int length = regionBuffer.getInt();
            if (length <= 1) {
                return new byte[0];
            }
            byte formatId = regionBuffer.get();
            int dataLen = length - 1;
            byte[] compressed = new byte[dataLen];
            regionBuffer.get(compressed);

            return decompress(compressed, RegionCompressionFormat.fromId(formatId));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (regionBuffer.capacity() < minCapacity) {
            int newCapacity = Math.max(regionBuffer.capacity() * 2, minCapacity);
            ByteBuffer newBuf = ByteBuffer.allocateDirect(newCapacity);
            regionBuffer.position(0);
            newBuf.put(regionBuffer);
            regionBuffer = newBuf;
        }
    }

    private byte[] compress(byte[] data, RegionCompressionFormat format) throws IOException {
        if (format == RegionCompressionFormat.UNCOMPRESSED) {
            return data;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (format == RegionCompressionFormat.GZIP) {
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data);
            }
        } else {
            // Deflate default
            try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION))) {
                dos.write(data);
            }
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] compressed, RegionCompressionFormat format) throws IOException {
        if (format == RegionCompressionFormat.UNCOMPRESSED) {
            return compressed;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        if (format == RegionCompressionFormat.GZIP) {
            try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                return gzis.readAllBytes();
            }
        } else {
            try (InflaterInputStream iis = new InflaterInputStream(bais)) {
                return iis.readAllBytes();
            }
        }
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    @Override
    public void close() {
        rwLock.writeLock().lock();
        try {
            closed = true;
            regionBuffer = null;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("LinearRegionFileFastPath is closed");
        }
    }
}
