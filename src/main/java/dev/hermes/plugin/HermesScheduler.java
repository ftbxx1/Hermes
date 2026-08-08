package dev.hermes.plugin;

import dev.hermes.core.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * The Scheduler backed by the Bukkit scheduler, so "every 10 seconds" and
 * delayed tasks in Hermes scripts work inside Minecraft.
 */
public final class HermesScheduler implements Scheduler {

    private final HermesPlugin plugin;
    private final Map<Runnable, BukkitTask> everyTasks = new HashMap<>();

    public HermesScheduler(HermesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public void runEvery(long millis, Runnable task) {
        long ticks = Math.max(1, millis / 50);
        everyTasks.put(task, Bukkit.getScheduler().runTaskTimer(plugin, task, ticks, ticks));
    }

    @Override public void cancelEvery(Runnable task) {
        BukkitTask t = everyTasks.remove(task);
        if (t != null) t.cancel();
    }

    @Override public void runLater(long millis, Runnable task) {
        long ticks = Math.max(1, millis / 50);
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }
}
