package dev.vessel.kernel.datapack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZeroCopyNbtCompoundReader} verifying zero-heap allocation NBT reads.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class ZeroCopyNbtCompoundReaderTest {

    @Test
    @DisplayName("ReadPrimitives: Reads primitive int, byte, and string directly from raw NBT buffer")
    void testDirectPrimitiveReads() {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        // Tag 1: TAG_Int named "Damage" with value 15
        buffer.put(ZeroCopyNbtCompoundReader.ID_INT);
        putNamedKey(buffer, "Damage");
        buffer.putInt(15);

        // Tag 2: TAG_Byte named "Unbreakable" with value 1 (true)
        buffer.put(ZeroCopyNbtCompoundReader.ID_BYTE);
        putNamedKey(buffer, "Unbreakable");
        buffer.put((byte) 1);

        // Tag 3: TAG_String named "CustomName" with value "LegendarySword"
        buffer.put(ZeroCopyNbtCompoundReader.ID_STRING);
        putNamedKey(buffer, "CustomName");
        putStringValue(buffer, "LegendarySword");

        // TAG_End
        buffer.put(ZeroCopyNbtCompoundReader.ID_END);

        buffer.flip();

        int damage = ZeroCopyNbtCompoundReader.getInt(buffer, "Damage", 0);
        assertThat(damage).isEqualTo(15);

        boolean unbreakable = ZeroCopyNbtCompoundReader.getBoolean(buffer, "Unbreakable", false);
        assertThat(unbreakable).isTrue();

        String customName = ZeroCopyNbtCompoundReader.getString(buffer, "CustomName", "");
        assertThat(customName).isEqualTo("LegendarySword");

        assertThat(ZeroCopyNbtCompoundReader.hasKey(buffer, "Damage")).isTrue();
        assertThat(ZeroCopyNbtCompoundReader.hasKey(buffer, "NonExistentKey")).isFalse();
    }

    @Test
    @DisplayName("Slice: Resolves tag byte offset and length as zero-copy slice")
    void testFindSlice() {
        ByteBuffer buffer = ByteBuffer.allocate(128);

        buffer.put(ZeroCopyNbtCompoundReader.ID_INT);
        putNamedKey(buffer, "Count");
        buffer.putInt(64);
        buffer.put(ZeroCopyNbtCompoundReader.ID_END);

        buffer.flip();

        ZeroCopyNbtSlice slice = ZeroCopyNbtCompoundReader.findSlice(buffer, "Count");
        assertThat(slice).isNotNull();
        assertThat(slice.tagType()).isEqualTo(NbtTagType.TAG_INT);
        assertThat(slice.length()).isEqualTo(4);
    }

    private void putNamedKey(ByteBuffer buf, String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        buf.putShort((short) bytes.length);
        buf.put(bytes);
    }

    private void putStringValue(ByteBuffer buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.putShort((short) bytes.length);
        buf.put(bytes);
    }
}
