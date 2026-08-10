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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        applyLanguage();
        world = new BukkitWorld(this);
        engine = new TaleEngine(world, new HermesScheduler(this));

        listener = new HermesListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);

        startTickTask();

        copyHelpIfNeeded();

        getCommand("hermes").setExecutor(this);
        getCommand("hermes").setTabCompleter(this);

        int loaded = loadScripts();
        registerScriptCommands();
        if (engine != null) engine.serverEvent("server starts");
        getLogger().info("Loaded " + loaded + " script" + (loaded == 1 ? "" : "s") + " from "
                + scriptsFolder().toAbsolutePath());
    }

    @Override
    public void onDisable() {
        unregisterScriptCommands();
        if (tickTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        if (engine != null) {
            engine.serverEvent("server stops");
            engine.shutdown();
        }
        if (world != null) {
            world.shutdown();
            world.save();
        }
    }

    /** Reads the "language" config option and loads the matching translation pack. */
    private void applyLanguage() {
        dev.hermes.core.Lang.setOverrideFolder(getDataFolder().toPath().resolve("lang"));
        dev.hermes.core.Lang.setLanguage(getConfig().getString("language", "en"));
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
        String[] files = {
                "variables.md", "triggers.md", "commands.md", "gui.md", "loops.md",
                "scoreboards.md", "regions.md", "effects.md", "timers.md", "world.md",
        };
        try {
            Files.createDirectories(helpFolder);
            for (String file : files) {
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
        unregisterScriptCommands();
        if (tickTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        applyLanguage();
        world.shutdown();
        world = new BukkitWorld(this);
        engine = new TaleEngine(world, new HermesScheduler(this));
        startTickTask();
        int loaded = loadScripts();
        registerScriptCommands();
        getLogger().info("Reloaded " + loaded + " script(s).");
    }

    // ------------------------------------------------------------------
    // script-defined commands
    // ------------------------------------------------------------------

    /** Every Bukkit command registered from the loaded scripts. */
    private final List<Command> scriptCommands = new ArrayList<>();

    private void registerScriptCommands() {
        for (TaleEngine.RegisteredCommand rc : engine.commands()) {
            String name = rc.def.name.startsWith("/")
                    ? rc.def.name.substring(1).trim() : rc.def.name.trim();
            if (name.isEmpty() || name.contains(" ")) {
                getLogger().warning("Script command '" + rc.def.name
                        + "' can't be registered (use a single word, e.g. /quest).");
                continue;
            }
            Command cmd = new HermesCommand(name, rc);
            Bukkit.getCommandMap().register("hermes", cmd);
            scriptCommands.add(cmd);
            getLogger().info("Registered script command /" + name);
        }
    }

    private void unregisterScriptCommands() {
        for (Command cmd : scriptCommands) {
            cmd.unregister(Bukkit.getCommandMap());
        }
        scriptCommands.clear();
    }

    /** A command defined by a .her script. */
    private final class HermesCommand extends Command {
        private final TaleEngine.RegisteredCommand rc;

        HermesCommand(String name, TaleEngine.RegisteredCommand rc) {
            super(name);
            this.rc = rc;
            setDescription("A command defined by a Hermes script.");
            setUsage("/" + name + (rc.def.argNames.isEmpty() ? "" : " <" + String.join("> <", rc.def.argNames) + ">"));
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            if (rc.def.permission != null && !player.hasPermission(rc.def.permission)) {
                player.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            engine.fireCommand(rc, new BukkitWorld.BukkitPlayer(player), Arrays.asList(args));
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return Collections.emptyList();
        }
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
                if (args.length >= 2 && !args[1].equalsIgnoreCase("all")) {
                    String name = args[1];
                    Path target = scriptsFolder().resolve(name);
                    if (!Files.isRegularFile(target)) {
                        sender.sendMessage("§cNo script named '" + name + "' in " + scriptsFolder().getFileName() + "/");
                        return true;
                    }
                    unregisterScriptCommands();
                    engine.unload(name);
                    if (engine.load(target)) {
                        sender.sendMessage("§a[Hermes] §fReloaded " + name + ".");
                    } else {
                        for (VerseError err : engine.loadErrors()) {
                            sender.sendMessage("§c" + err.message + " §7(line " + err.line + ")");
                        }
                        sender.sendMessage("§c[Hermes] §f" + name + " has problems and was NOT reloaded.");
                    }
                    registerScriptCommands();
                    return true;
                }
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
            default:
                help(sender);
                return true;
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§6[Hermes] §fcommands:");
        sender.sendMessage("§7  /hermes reload §f- reload all scripts");
        sender.sendMessage("§7  /hermes reload all §f- reload all scripts");
        sender.sendMessage("§7  /hermes reload <script.her> §f- reload just that script");
        sender.sendMessage("§7  /hermes scripts §f- list loaded scripts");
        if (sender instanceof Player) {
            sender.sendMessage("§7Scripts live in §fplugins/Hermes/hermes/");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("reload", "scripts").stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            List<String> out = new ArrayList<>();
            out.add("all");
            for (Path p : findScripts(scriptsFolder())) {
                out.add(scriptsFolder().relativize(p).toString());
            }
            return out;
        }
        return List.of();
    }
}
