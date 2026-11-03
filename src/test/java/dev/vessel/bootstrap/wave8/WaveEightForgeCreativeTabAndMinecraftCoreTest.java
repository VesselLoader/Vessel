package dev.vessel.bootstrap.wave8;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 8 verifying Minecraft {@link BlockPos}, {@link Direction},
 * {@link Axis}, {@link TagKey}, {@link CreativeModeTab}, and Forge/NeoForge
 * {@link BuildCreativeModeTabContentsEvent} and {@link EntityAttributeCreationEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveEightForgeCreativeTabAndMinecraftCoreTest {

    @Test
    void testForgeNeoForgeCreativeModeTabEntityAttributesAndMinecraftBlockPosTagKey() {
        BlockPos origin = new BlockPos(10, 64, -20);
        assertEquals(new BlockPos(10, 65, -20), origin.above());
        assertEquals(new BlockPos(10, 63, -20), origin.below());
        assertEquals(new BlockPos(10, 64, -23), origin.relative(Direction.NORTH, 3));
        assertEquals(origin, BlockPos.of(origin.asLong()));
        assertEquals(Direction.SOUTH, Direction.NORTH.getOpposite());
        assertEquals(Direction.EAST, Direction.WEST.getOpposite());
        assertTrue(Axis.Y.isVertical());
        assertFalse(Axis.Y.isHorizontal());
        assertTrue(Axis.X.isHorizontal());

        ResourceLocation blockRegistry = ResourceLocation.withDefaultNamespace("block");
        ResourceLocation pickaxeLocation =
                ResourceLocation.withDefaultNamespace("mineable/pickaxe");
        TagKey<String> pickaxeTag = TagKey.create(blockRegistry, pickaxeLocation);
        assertTrue(pickaxeTag.isFor(blockRegistry));
        assertFalse(pickaxeTag.isFor(ResourceLocation.withDefaultNamespace("item")));
        assertEquals(pickaxeLocation, pickaxeTag.location());

        CreativeModeTab forgeTab = new CreativeModeTab("vessel:tab", "Vessel Tab");
        assertEquals("vessel:tab", forgeTab.getId());
        assertEquals("Vessel Tab", forgeTab.getTitle());

        BuildCreativeModeTabContentsEvent forgeTabEvent =
                new BuildCreativeModeTabContentsEvent(forgeTab, "vessel:tab");
        forgeTabEvent.accept("vessel:wrench");
        assertEquals(List.of("vessel:wrench"), forgeTabEvent.getEntries());

        EntityAttributeCreationEvent forgeAttrEvent = new EntityAttributeCreationEvent();
        forgeAttrEvent.put("vessel:golem", Map.of("generic.max_health", 100.0D));
        assertTrue(forgeAttrEvent.hasAttributes("vessel:golem"));
        assertEquals(
                100.0D,
                forgeAttrEvent.getAttributes("vessel:golem").get("generic.max_health"));

        CreativeModeTab neoTab = new CreativeModeTab("vessel:neo_tab", "Vessel NeoForge Tab");
        net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent neoTabEvent =
                new net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent(
                        neoTab,
                        "vessel:neo_tab");
        neoTabEvent.accept("vessel:neo_hammer");
        assertEquals(List.of("vessel:neo_hammer"), neoTabEvent.getEntries());

        net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent neoAttrEvent =
                new net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent();
        neoAttrEvent.put("vessel:neo_sentinel", Map.of("generic.attack_damage", 24.0D));
        assertTrue(neoAttrEvent.hasAttributes("vessel:neo_sentinel"));
        assertEquals(
                24.0D,
                neoAttrEvent.getAttributes("vessel:neo_sentinel").get("generic.attack_damage"));
    }
}
