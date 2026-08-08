package dev.hermes.plugin;

import dev.hermes.core.Scheduler;
import org.bukkit.Bukkit;

/**
 * The Scheduler backed by the Bukkit scheduler, so "every 10 seconds" and
 * delayed tasks in Hermes scripts work inside Minecraft.
 */
public final class HermesScheduler implements Scheduler {

    private final HermesPlugin plugin;

    public HermesScheduler(HermesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public void runEvery(long millis, Runnable task) {
        long ticks = Math.max(1, millis / 50);
        Bukkit.getScheduler().runTaskTimer(plugin, task, ticks, ticks);
    }

    @Override public void runLater(long millis, Runnable task) {
        long ticks = Math.max(1, millis / 50);
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }
}
