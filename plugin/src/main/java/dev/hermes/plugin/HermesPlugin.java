package dev.hermes.plugin;

import dev.hermes.core.TaleEngine;
import dev.hermes.core.VerseError;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Hermes Paper plugin: loads .her scripts from plugins/Hermes/hermes,
 * fires their triggers as players play, and applies their actions to the
 * live world.
 *
 * <p>Usage: put scripts in plugins/Hermes/hermes/ and run /hermes reload.
 */
public final class HermesPlugin extends JavaPlugin implements TabCompleter {

    private TaleEngine engine;
    private BukkitWorld world;
    private HermesListener listener;
    private int tickTaskId = -1;

    // ------------------------------------------------------------------
    // lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        world = new BukkitWorld(this);
        engine = new TaleEngine(world, new HermesScheduler(this));

        listener = new HermesListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);

        startTickTask();

        copyHelpIfNeeded();

        getCommand("hermes").setExecutor(this);
        getCommand("hermes").setTabCompleter(this);

        int loaded = loadScripts();
        getLogger().info("Loaded " + loaded + " script" + (loaded == 1 ? "" : "s") + " from "
                + scriptsFolder().toAbsolutePath());
    }

    @Override
    public void onDisable() {
        if (tickTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        if (engine != null) engine.shutdown();
        if (world != null) {
            world.shutdown();
            world.save();
        }
    }

    // ------------------------------------------------------------------
    // access for the listener
    // ------------------------------------------------------------------

    TaleEngine engine() { return engine; }

    BukkitWorld world() { return world; }

    // ------------------------------------------------------------------
    // scripts
    // ------------------------------------------------------------------

    private Path scriptsFolder() {
        return getDataFolder().toPath().resolve(getConfig().getString("scripts-folder", "hermes"));
    }

    /** Copies the bundled help files into plugins/Hermes/help/ on first run. */
    private void copyHelpIfNeeded() {
        Path helpFolder = getDataFolder().toPath().resolve("help");
        try {
            Files.createDirectories(helpFolder);
            for (String file : new String[] {"variables.md"}) {
                Path target = helpFolder.resolve(file);
                if (Files.exists(target)) continue;
                try (InputStream in = getResource("help/" + file)) {
                    if (in != null) Files.copy(in, target);
                }
                getLogger().info("Created help file " + target);
            }
        } catch (IOException e) {
            getLogger().warning("Could not write help files: " + e.getMessage());
        }
    }

    private List<Path> findScripts(Path folder) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(folder)) return out;
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".her"))
                    .sorted()
                    .forEach(out::add);
        } catch (IOException e) {
            getLogger().warning("Could not read scripts folder: " + e.getMessage());
        }
        return out;
    }

    /** Loads every .her file in the scripts folder. Returns the number loaded. */
    public int loadScripts() {
        Path folder = scriptsFolder();
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            getLogger().warning("Could not create scripts folder: " + e.getMessage());
        }
        List<Path> files = findScripts(folder);
        int loaded = 0;
        int failed = 0;
        for (Path file : files) {
            if (engine.load(file)) {
                loaded++;
            } else {
                failed++;
                for (VerseError e : engine.loadErrors()) {
                    getLogger().warning(e.pretty(file.toString()));
                }
            }
        }
        if (failed > 0) getLogger().warning(failed + " script(s) had problems. See the errors above.");
        return loaded;
    }

    /** Drops everything and reloads all scripts. */
    public void reloadAll() {
        if (tickTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        world.shutdown();
        world = new BukkitWorld(this);
        engine = new TaleEngine(world, new HermesScheduler(this));
        startTickTask();
        int loaded = loadScripts();
        getLogger().info("Reloaded " + loaded + " script(s).");
    }

    private void startTickTask() {
        int interval = Math.max(1, getConfig().getInt("state-tick-interval", 10));
        tickTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                engine.tick();
            } catch (RuntimeException e) {
                getLogger().warning("Hermes tick error: " + e.getMessage());
            }
        }, interval, interval).getTaskId();
    }

    // ------------------------------------------------------------------
    // commands
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload": {
                reloadAll();
                sender.sendMessage("§a[Hermes] §fScripts reloaded.");
                return true;
            }
            case "scripts": {
                sender.sendMessage("§a[Hermes] §fLoaded scripts:");
                for (TaleEngine.LoadedScript s : engine.scripts()) {
                    sender.sendMessage("§7  - §f" + s.fileName);
                }
                if (engine.scripts().isEmpty()) sender.sendMessage("§7  (none)");
                return true;
            }
            case "run": {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /hermes run <script.her>");
                    return true;
                }
                String name = args[1];
                Path target = scriptsFolder().resolve(name);
                if (!Files.isRegularFile(target)) {
                    sender.sendMessage("§cNo script named '" + name + "' in " + scriptsFolder().getFileName() + "/");
                    return true;
                }
                List<VerseError> before = new ArrayList<>(engine.loadErrors());
                if (engine.load(target)) {
                    sender.sendMessage("§a[Hermes] §fLoaded " + name + ".");
                } else {
                    engine.loadErrors().stream().skip(before.size()).forEach(err ->
                            sender.sendMessage("§c" + err.message + " §7(line " + err.line + ")"));
                    sender.sendMessage("§c[Hermes] §f" + name + " has problems and was not loaded.");
                }
                return true;
            }
            case "events": {
                sender.sendMessage("§a[Hermes] §fRegistered triggers:");
                Map<String, Integer> counts = engine.eventCounts();
                if (counts.isEmpty()) sender.sendMessage("§7  (no event triggers loaded)");
                counts.forEach((event, count) ->
                        sender.sendMessage("§7  - §fwhen " + event + " §7(" + count + ")"));
                return true;
            }
            case "vars": {
                sender.sendMessage("§a[Hermes] §fWorld variables: "
                        + engine.vars.playerVars().size() + " player(s), "
                        + engine.vars.worldVars().size() + " world var(s).");
                return true;
            }
            default:
                help(sender);
                return true;
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§6[Hermes] §fcommands:");
        sender.sendMessage("§7  /hermes reload §f- reload all scripts");
        sender.sendMessage("§7  /hermes scripts §f- list loaded scripts");
        sender.sendMessage("§7  /hermes run <script> §f- load one script");
        sender.sendMessage("§7  /hermes events §f- list registered triggers");
        sender.sendMessage("§7  /hermes vars §f- show engine variable counts");
        if (sender instanceof Player) {
            sender.sendMessage("§7Scripts live in §fplugins/Hermes/hermes/");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("reload", "scripts", "run", "events", "vars").stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            return findScripts(scriptsFolder()).stream()
                    .map(p -> scriptsFolder().relativize(p).toString())
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
