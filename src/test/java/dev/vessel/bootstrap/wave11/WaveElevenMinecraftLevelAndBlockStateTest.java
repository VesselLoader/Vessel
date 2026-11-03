package dev.vessel.bootstrap.wave11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.api.block.VesselBlockStatePropertySnapshot;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying Minecraft Level, BlockState, BlockEntity, ServerLevel, and
 * ServerPlayer alongside VUSA contracts.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveElevenMinecraftLevelAndBlockStateTest {

    @Test
    void testLevelBlockStateAndServerPlayer() {
        ResourceLocation overworldId = ResourceLocation.withDefaultNamespace("overworld");
        ServerLevel level = new ServerLevel(overworldId);
        assertEquals(overworldId, level.dimension());

        BlockPos pos = new BlockPos(10, 64, -20);
        BlockState stone = new BlockState("minecraft:stone", Map.of(), false, true);
        level.setBlockState(pos, stone);
        assertEquals(stone, level.getBlockState(pos));
        assertTrue(stone.isSolid());
        assertFalse(stone.isAir());

        BlockEntity entity = new BlockEntity(pos, stone);
        CompoundTag nbt = new CompoundTag();
        nbt.putString("CustomName", "Ancient Stone");
        entity.setNbtData(nbt);
        level.setBlockEntity(pos, entity);
        assertNotNull(level.getBlockEntity(pos));
        assertEquals("Ancient Stone", level.getBlockEntity(pos).getNbtData().getString("CustomName"));

        UUID pId = UUID.randomUUID();
        ServerPlayer player = new ServerPlayer(pId, "Shiroz", level, pos);
        level.addPlayer(player);
        assertEquals(player, level.getPlayerByUUID(pId));
        assertEquals(1, level.players().size());
        assertEquals("Shiroz", player.getGameProfileName());
        assertEquals(pos, player.blockPosition());

        VesselBlockStatePropertySnapshot vusaSnapshot = new VesselBlockStatePropertySnapshot(
                stone.getBlockId(),
                stone.getProperties(),
                stone.isSolid(),
                stone.isAir());
        assertEquals("minecraft:stone", vusaSnapshot.blockId());
        assertTrue(vusaSnapshot.solid());
    }
}
