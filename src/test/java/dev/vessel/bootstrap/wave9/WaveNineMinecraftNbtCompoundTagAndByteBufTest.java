package dev.vessel.bootstrap.wave9;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 9 verifying Minecraft NBT {@link CompoundTag}, {@link ListTag},
 * {@link Tag}, and packet buffer {@link FriendlyByteBuf}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveNineMinecraftNbtCompoundTagAndByteBufTest {

    @Test
    void testMinecraftCompoundTagListTagAndFriendlyByteBuf() {
        CompoundTag root = new CompoundTag();
        assertEquals(Tag.TAG_COMPOUND, root.getId());
        assertTrue(root.isEmpty());

        root.putString("id", "vessel:core");
        root.putInt("energy", 50000);
        root.putBoolean("active", true);
        root.putDouble("efficiency", 99.5D);

        CompoundTag child = new CompoundTag();
        child.putLong("timestamp", 123456789L);
        root.put("meta", child);

        ListTag list = new ListTag();
        assertEquals(Tag.TAG_LIST, list.getId());
        list.add(child.copy());
        root.put("history", list);

        assertFalse(root.isEmpty());
        assertTrue(root.contains("id"));
        assertEquals("vessel:core", root.getString("id"));
        assertEquals(50000, root.getInt("energy"));
        assertTrue(root.getBoolean("active"));
        assertEquals(99.5D, root.getDouble("efficiency"));
        assertEquals(123456789L, root.getCompound("meta").getLong("timestamp"));
        assertEquals(1, list.size());

        CompoundTag copied = root.copy();
        assertEquals("vessel:core", copied.getString("id"));
        assertEquals(50000, copied.getInt("energy"));
        assertEquals(123456789L, copied.getCompound("meta").getLong("timestamp"));

        FriendlyByteBuf buf = new FriendlyByteBuf();
        ResourceLocation syncChannel = ResourceLocation.parse("vessel:sync");
        BlockPos targetPos = new BlockPos(12, 80, -34);

        buf.writeVarInt(42);
        buf.writeUtf("Sovereign");
        buf.writeResourceLocation(syncChannel);
        buf.writeBlockPos(targetPos);
        buf.writeBoolean(true);
        buf.writeInt(777);

        assertTrue(buf.readableBytes() > 0);
        assertEquals(42, buf.readVarInt());
        assertEquals("Sovereign", buf.readUtf());
        assertEquals(syncChannel, buf.readResourceLocation());
        assertEquals(targetPos, buf.readBlockPos());
        assertTrue(buf.readBoolean());
        assertEquals(777, buf.readInt());
        assertEquals(0, buf.readableBytes());
    }
}
