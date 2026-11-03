package dev.vessel.bootstrap.wave2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 2 verifying Bukkit/Paper {@link PluginCommand} execution and tab
 * completion, classic {@code BukkitScheduler}, and Paper/Folia regionized multithreading
 * schedulers ({@code GlobalRegionScheduler}, {@code RegionScheduler}, {@code AsyncScheduler}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveTwoBukkitFoliaSchedulerAndCommandTest {

    private final List<String> executionTrace = new ArrayList<>();

    public void recordBukkitTaskRun() {
        executionTrace.add("BUKKIT_SYNC_TASK");
    }

    public void recordGlobalRegionRun() {
        executionTrace.add("FOLIA_GLOBAL_REGION");
    }

    public void recordChunkRegionRun() {
        executionTrace.add("FOLIA_CHUNK_REGION_4_-2");
    }

    public void recordAsyncScheduledTask(ScheduledTask task) {
        if (task != null && !task.isCancelled()) {
            executionTrace.add("FOLIA_ASYNC_NOW");
        }
    }

    @Test
    void testBukkitPluginCommandAndFoliaRegionizedSchedulers() {
        SampleBukkitCommandPluginFixture plugin = new SampleBukkitCommandPluginFixture();
        plugin.setEnabled(true);
        assertTrue(plugin.isEnabled());

        PluginCommand command = plugin.getCommand("vesseltest");
        assertNotNull(command);
        assertEquals(plugin, command.getPlugin());

        ConsoleCommandSender consoleSender = new ConsoleCommandSender();
        boolean handled = command.execute(consoleSender, "vesseltest", new String[]{"a", "b"});
        assertTrue(handled);
        assertTrue(consoleSender.getSentMessages().contains("Executed:vesseltest:2"));

        List<String> completions =
                command.tabComplete(consoleSender, "vesseltest", new String[]{"a"});
        assertEquals(List.of("arg1", "arg2"), completions);

        BukkitTask syncTask = Bukkit.getScheduler().runTask(plugin, this::recordBukkitTaskRun);
        assertNotNull(syncTask);
        assertTrue(syncTask.isSync());
        assertFalse(syncTask.isCancelled());

        Bukkit.getGlobalRegionScheduler().execute(plugin, this::recordGlobalRegionRun);
        Bukkit.getRegionScheduler().execute(plugin, 4, -2, this::recordChunkRegionRun);
        ScheduledTask asyncTask =
                Bukkit.getAsyncScheduler().runNow(plugin, this::recordAsyncScheduledTask);
        assertNotNull(asyncTask);
        assertEquals(plugin, asyncTask.getOwningPlugin());

        assertEquals(
                List.of(
                        "BUKKIT_SYNC_TASK",
                        "FOLIA_GLOBAL_REGION",
                        "FOLIA_CHUNK_REGION_4_-2",
                        "FOLIA_ASYNC_NOW"),
                executionTrace);
    }
}
