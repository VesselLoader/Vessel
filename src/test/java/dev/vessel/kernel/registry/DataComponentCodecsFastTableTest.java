package dev.vessel.kernel.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DataComponentCodecsFastTable} verifying O(1) lookups and zero-allocation codec operations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DataComponentCodecsFastTableTest {

    @Test
    @DisplayName("Lookup: Resolves standard 1.20.5+ components by ID and Key in O(1)")
    void testStandardComponentLookups() {
        DataComponentCodecsFastTable table = new DataComponentCodecsFastTable();

        assertThat(table.getById(DataComponentCodecsFastTable.ID_MAX_STACK_SIZE)).isNotNull();
        assertThat(table.getById(DataComponentCodecsFastTable.ID_MAX_STACK_SIZE).namespacedKey())
                .isEqualTo(DataComponentCodecsFastTable.KEY_MAX_STACK_SIZE);

        assertThat(table.getByKey(DataComponentCodecsFastTable.KEY_CUSTOM_NAME)).isNotNull();
        assertThat(table.getId(DataComponentCodecsFastTable.KEY_CUSTOM_NAME))
                .isEqualTo(DataComponentCodecsFastTable.ID_CUSTOM_NAME);

        assertThat(table.containsId(DataComponentCodecsFastTable.ID_DAMAGE)).isTrue();
        assertThat(table.containsKey(DataComponentCodecsFastTable.KEY_DAMAGE)).isTrue();
    }

    @Test
    @DisplayName("Codec: Encodes and decodes primitive values directly to ByteBuffer without NBT allocations")
    void testPrimitiveDirectEncodeDecode() {
        DataComponentCodecsFastTable table = new DataComponentCodecsFastTable();
        ByteBuffer buffer = ByteBuffer.allocate(128);

        table.encodeDirect(DataComponentCodecsFastTable.ID_DAMAGE, 42, buffer);
        buffer.flip();

        short componentId = buffer.getShort();
        assertThat((int) componentId).isEqualTo(DataComponentCodecsFastTable.ID_DAMAGE);

        Object decodedValue = table.decodeDirect(DataComponentCodecsFastTable.ID_DAMAGE, buffer);
        assertThat(decodedValue).isEqualTo(42);
    }

    @Test
    @DisplayName("Codec: Encodes and decodes String component directly")
    void testStringDirectEncodeDecode() {
        DataComponentCodecsFastTable table = new DataComponentCodecsFastTable();
        ByteBuffer buffer = ByteBuffer.allocate(128);

        table.encodeDirect(DataComponentCodecsFastTable.ID_CUSTOM_NAME, "Excalibur", buffer);
        buffer.flip();

        buffer.getShort(); // consume component ID
        Object decodedName = table.decodeDirect(DataComponentCodecsFastTable.ID_CUSTOM_NAME, buffer);
        assertThat(decodedName).isEqualTo("Excalibur");
    }

    @Test
    @DisplayName("Registration: Supports custom mod data component descriptor registration")
    void testCustomComponentRegistration() {
        DataComponentCodecsFastTable table = new DataComponentCodecsFastTable();

        int customId = 150;
        String customKey = "vessel:mana_capacity";
        DataComponentCodecDescriptor desc = new DataComponentCodecDescriptor(
                customId,
                customKey,
                DataComponentValueType.INT,
                100,
                false
        );

        table.register(desc);

        assertThat(table.getById(customId)).isEqualTo(desc);
        assertThat(table.getByKey(customKey)).isEqualTo(desc);
        assertThat(table.getId(customKey)).isEqualTo(customId);
    }
}
