package dev.vessel.kernel.network.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link VarIntStreamOverflowGuard}, {@link DirtyMetadataBitmaskPacker},
 * and {@link RakNetMtuFrameCoalescer}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class VarIntOverflowAndMtuTest {

    @Test
    @DisplayName("VarIntStreamOverflowGuard encodes and decodes VarInt/VarLong with strict overflow protection")
    void testVarIntEncodingDecodingAndOverflowInterception() {
        VarIntStreamOverflowGuard guard = new VarIntStreamOverflowGuard();

        int[] testInts = new int[]{0, 1, 127, 128, 255, 2097151, Integer.MAX_VALUE, -1};
        ByteBuffer buffer = ByteBuffer.allocate(64);

        for (int value : testInts) {
            buffer.clear();
            int bytesWritten = guard.writeVarInt(value, buffer);
            assertEquals(guard.computeVarIntSize(value), bytesWritten);

            buffer.flip();
            int decoded = guard.readVarInt(buffer);
            assertEquals(value, decoded);
        }

        // Test VarLong
        long[] testLongs = new long[]{0L, 1L, 127L, 128L, Long.MAX_VALUE, -1L};
        for (long value : testLongs) {
            buffer.clear();
            int bytesWritten = guard.writeVarLong(value, buffer);
            assertEquals(guard.computeVarLongSize(value), bytesWritten);

            buffer.flip();
            long decoded = guard.readVarLong(buffer);
            assertEquals(value, decoded);
        }

        // Test VarInt overflow attack: 6 bytes with MSB set (0x80)
        ByteBuffer malformedVarInt = ByteBuffer.wrap(new byte[]{
                (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80
        });
        VarIntOverflowException exInt = assertThrows(
                VarIntOverflowException.class,
                () -> guard.readVarInt(malformedVarInt)
        );
        assertEquals(VarIntStreamOverflowGuard.MAX_VARINT_BYTES, exInt.getMaxAllowedBytes());
        assertTrue(exInt.getMessage().contains("VarInt stream overflow"));

        // Test VarLong overflow attack: 11 bytes with MSB set (0x80)
        byte[] overflowVarLongBytes = new byte[12];
        for (int i = 0; i < overflowVarLongBytes.length; i++) {
            overflowVarLongBytes[i] = (byte) 0x80;
        }
        VarIntOverflowException exLong = assertThrows(
                VarIntOverflowException.class,
                () -> guard.readVarLong(ByteBuffer.wrap(overflowVarLongBytes))
        );
        assertEquals(VarIntStreamOverflowGuard.MAX_VARLONG_BYTES, exLong.getMaxAllowedBytes());

        // Test readVarIntFromArray
        buffer.clear();
        guard.writeVarInt(4242, buffer);
        buffer.flip();
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr);
        assertEquals(4242, guard.readVarIntFromArray(arr, 0));
    }

    @Test
    @DisplayName("DirtyMetadataBitmaskPacker packs only dirty entries and appends terminator")
    void testDirtyMetadataBitmaskPacking() {
        DirtyMetadataBitmaskPacker packer = new DirtyMetadataBitmaskPacker();

        long bitmask = 0L;
        bitmask = packer.markDirty(bitmask, 0);
        bitmask = packer.markDirty(bitmask, 5);
        bitmask = packer.markDirty(bitmask, 12);

        assertTrue(packer.isDirty(bitmask, 0));
        assertTrue(packer.isDirty(bitmask, 5));
        assertTrue(packer.isDirty(bitmask, 12));
        assertFalse(packer.isDirty(bitmask, 1));
        assertEquals(3, packer.countDirty(bitmask));

        bitmask = packer.clearDirty(bitmask, 5);
        assertFalse(packer.isDirty(bitmask, 5));
        assertEquals(2, packer.countDirty(bitmask));

        // Create 10 metadata entries, mark index 0 and 12 as dirty
        EntityMetadataEntry[] entries = new EntityMetadataEntry[16];
        for (int i = 0; i < 16; i++) {
            entries[i] = new EntityMetadataEntry(i, 1, new byte[]{(byte) i});
        }

        ByteBuffer dest = ByteBuffer.allocate(128);
        DirtyPackingSummary summary = packer.packDirtyEntries(bitmask, entries, dest);

        assertNotNull(summary);
        assertEquals(16, summary.totalEntriesCount());
        assertEquals(2, summary.dirtyEntriesPackedCount());
        assertTrue(summary.bytesWritten() > 0);
        assertTrue(summary.bandwidthSavingsPercent() >= 80.0);

        // Verify terminator written
        dest.flip();
        dest.position(summary.bytesWritten() - 1);
        assertEquals(DirtyMetadataBitmaskPacker.METADATA_TERMINATOR, dest.get());

        // Bounds test
        assertThrows(IndexOutOfBoundsException.class, () -> packer.markDirty(0L, 64));
    }

    @Test
    @DisplayName("RakNetMtuFrameCoalescer fragments oversized packets and reassembles out-of-order")
    void testRakNetMtuFragmentationAndReassembly() {
        int maxMtu = 600;
        RakNetMtuFrameCoalescer coalescer = new RakNetMtuFrameCoalescer(maxMtu);
        int compoundId = coalescer.nextCompoundId();

        // 1500 bytes payload -> 3 frames (600, 600, 300)
        byte[] payload = new byte[1500];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        List<RakNetMtuFrame> frames = coalescer.fragmentPayload(compoundId, payload);
        assertEquals(3, frames.size());
        assertEquals(0, frames.get(0).splitIndex());
        assertEquals(600, frames.get(0).payload().length);
        assertEquals(1, frames.get(1).splitIndex());
        assertEquals(600, frames.get(1).payload().length);
        assertEquals(2, frames.get(2).splitIndex());
        assertEquals(300, frames.get(2).payload().length);

        // Deliver out of order: frame 2, then frame 0, then frame 1
        CoalescedMtuResult res1 = coalescer.ingestFrame(frames.get(2));
        assertFalse(res1.complete());
        assertEquals(1, res1.receivedFrames());

        CoalescedMtuResult res2 = coalescer.ingestFrame(frames.get(0));
        assertFalse(res2.complete());
        assertEquals(2, res2.receivedFrames());

        CoalescedMtuResult resFinal = coalescer.ingestFrame(frames.get(1));
        assertTrue(resFinal.complete());
        assertEquals(3, resFinal.receivedFrames());
        assertEquals(3, resFinal.totalFrames());
        assertArrayEquals(payload, resFinal.coalescedPayload());

        // Completed compound cleared from pending
        assertEquals(0, coalescer.getPendingCompoundsCount());

        // Single frame optimization (splitCount = 1)
        RakNetMtuFrame singleFrame = new RakNetMtuFrame(100, 0, 1, new byte[]{42});
        CoalescedMtuResult singleResult = coalescer.ingestFrame(singleFrame);
        assertTrue(singleResult.complete());
        assertEquals(1, singleResult.coalescedPayload().length);
    }
}
