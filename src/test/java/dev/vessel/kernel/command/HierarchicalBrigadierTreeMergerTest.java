package dev.vessel.kernel.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.command.brigadier.BrigadierCommandNode;
import dev.vessel.kernel.command.brigadier.BrigadierTreeMergeResult;
import dev.vessel.kernel.command.brigadier.CommandConflictResolutionStrategy;
import dev.vessel.kernel.command.brigadier.CommandRegistrationSource;
import dev.vessel.kernel.command.brigadier.HierarchicalBrigadierTreeMerger;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HierarchicalBrigadierTreeMerger} and Brigadier multi-loader
 * command tree harmonization under ERR-38 and ECO-015.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
class HierarchicalBrigadierTreeMergerTest {

    @Test
    void testSingleCommandRegistrationAndResolution() {
        HierarchicalBrigadierTreeMerger merger = new HierarchicalBrigadierTreeMerger();
        CommandRegistrationSource source = new CommandRegistrationSource(
                "VESSEL_CORE", "vessel", CommandRegistrationSource.PRIORITY_VESSEL_CORE);

        BrigadierCommandNode node = new BrigadierCommandNode(
                "status", "vessel", "Displays server status", "vessel.command.status",
                Set.of("info"), Collections.emptyMap(), true);

        assertTrue(merger.registerCommand(source, node));

        Optional<BrigadierCommandNode> direct = merger.resolveCommand("/status");
        assertTrue(direct.isPresent());
        assertEquals("status", direct.get().getName());
        assertEquals("vessel", direct.get().getNamespace());

        Optional<BrigadierCommandNode> qualified = merger.resolveCommand("/vessel:status");
        assertTrue(qualified.isPresent());
        assertEquals("status", qualified.get().getName());

        Optional<BrigadierCommandNode> alias = merger.resolveCommand("/info");
        assertTrue(alias.isPresent());
    }

    @Test
    void testRootCommandCollisionWithPriorityResolution() {
        HierarchicalBrigadierTreeMerger merger = new HierarchicalBrigadierTreeMerger(
                CommandConflictResolutionStrategy.PRIORITY_BASED);

        CommandRegistrationSource vanillaSource = new CommandRegistrationSource(
                "VANILLA", "minecraft", CommandRegistrationSource.PRIORITY_VANILLA);
        BrigadierCommandNode vanillaSpawn = new BrigadierCommandNode(
                "spawn", "minecraft", "Vanilla spawn command", "minecraft.command.spawn",
                Collections.emptySet(), Collections.emptyMap(), true);

        CommandRegistrationSource bukkitSource = new CommandRegistrationSource(
                "ESSENTIALS_BUKKIT", "essentials", CommandRegistrationSource.PRIORITY_BUKKIT_PLUGIN);
        BrigadierCommandNode essentialsSpawn = new BrigadierCommandNode(
                "spawn", "essentials", "EssentialsX spawn command", "essentials.spawn",
                Collections.emptySet(), Collections.emptyMap(), true);

        assertTrue(merger.registerCommand(vanillaSource, vanillaSpawn));
        assertTrue(merger.registerCommand(bukkitSource, essentialsSpawn));

        // Bukkit has higher priority (800 > 100), so root /spawn must point to EssentialsX
        Optional<BrigadierCommandNode> resolvedRoot = merger.resolveCommand("/spawn");
        assertTrue(resolvedRoot.isPresent());
        assertEquals("essentials", resolvedRoot.get().getNamespace());

        // Both namespaced variants must remain accessible without server crash
        Optional<BrigadierCommandNode> resolvedMinecraft = merger.resolveCommand("/minecraft:spawn");
        assertTrue(resolvedMinecraft.isPresent());
        assertEquals("minecraft", resolvedMinecraft.get().getNamespace());

        Optional<BrigadierCommandNode> resolvedEssentials = merger.resolveCommand("/essentials:spawn");
        assertTrue(resolvedEssentials.isPresent());
        assertEquals("essentials", resolvedEssentials.get().getNamespace());

        BrigadierTreeMergeResult metrics = merger.getMergeMetrics();
        assertEquals(1, metrics.conflictsDetected());
        assertTrue(metrics.success());
    }

    @Test
    void testConflictResolutionPreserveExisting() {
        HierarchicalBrigadierTreeMerger merger = new HierarchicalBrigadierTreeMerger(
                CommandConflictResolutionStrategy.PRESERVE_EXISTING);

        CommandRegistrationSource firstSource = new CommandRegistrationSource(
                "OLD_PLUGIN", "oldplugin", 100);
        BrigadierCommandNode oldNode = new BrigadierCommandNode(
                "tpa", "oldplugin", "Old TPA", "", Collections.emptySet(), Collections.emptyMap(), true);

        CommandRegistrationSource secondSource = new CommandRegistrationSource(
                "NEW_MOD", "newmod", 900);
        BrigadierCommandNode newNode = new BrigadierCommandNode(
                "tpa", "newmod", "New TPA", "", Collections.emptySet(), Collections.emptyMap(), true);

        merger.registerCommand(firstSource, oldNode);
        merger.registerCommand(secondSource, newNode);

        // PRESERVE_EXISTING keeps the first registered command as root
        Optional<BrigadierCommandNode> root = merger.resolveCommand("/tpa");
        assertTrue(root.isPresent());
        assertEquals("oldplugin", root.get().getNamespace());

        // Newcomer is still available namespaced
        Optional<BrigadierCommandNode> namespacedNew = merger.resolveCommand("/newmod:tpa");
        assertTrue(namespacedNew.isPresent());
        assertEquals("newmod", namespacedNew.get().getNamespace());
    }

    @Test
    void testConflictResolutionOverwrite() {
        HierarchicalBrigadierTreeMerger merger = new HierarchicalBrigadierTreeMerger(
                CommandConflictResolutionStrategy.OVERWRITE);

        CommandRegistrationSource firstSource = new CommandRegistrationSource(
                "FIRST", "first", 500);
        BrigadierCommandNode firstNode = new BrigadierCommandNode(
                "home", "first", "First home", "", Collections.emptySet(), Collections.emptyMap(), true);

        CommandRegistrationSource secondSource = new CommandRegistrationSource(
                "SECOND", "second", 100);
        BrigadierCommandNode secondNode = new BrigadierCommandNode(
                "home", "second", "Second home", "", Collections.emptySet(), Collections.emptyMap(), true);

        merger.registerCommand(firstSource, firstNode);
        merger.registerCommand(secondSource, secondNode);

        // OVERWRITE forces root to second node regardless of priority
        Optional<BrigadierCommandNode> root = merger.resolveCommand("/home");
        assertTrue(root.isPresent());
        assertEquals("second", root.get().getNamespace());

        Optional<BrigadierCommandNode> firstNamespaced = merger.resolveCommand("/first:home");
        assertTrue(firstNamespaced.isPresent());
        assertEquals("first", firstNamespaced.get().getNamespace());
    }
}
