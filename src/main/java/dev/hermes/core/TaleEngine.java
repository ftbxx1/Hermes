package dev.hermes.core;

import dev.hermes.core.Ast.*;
import dev.hermes.core.WorldAPI.MobRef;
import dev.hermes.core.WorldAPI.PlayerRef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Hermes engine: loads scripts, registers their "when" triggers, listens
 * for events from the world, polls state triggers, and runs the interpreter.
 *
 * <p>The Paper plugin (and the demo console) call into this class; Hermes
 * calls back out through the WorldAPI.
 */
public final class TaleEngine {

    public final WorldAPI world;
    public final Scheduler scheduler;
    public final VarStore vars = new VarStore();

    /** Named places: "home" -> coordinates. */
    public final Map<String, WorldAPI.Vec3> marks = new HashMap<>();

    private static final class Handler {
        final Trigger trig;
        final List<Stmt> body;
        final Interpreter interp;
        Handler(Trigger trig, List<Stmt> body, Interpreter interp) { this.trig = trig; this.body = body; this.interp = interp; }
    }

    private static final class StateEntry {
        final Trigger trig;
        final List<Stmt> body;
        final Interpreter interp;
        final String id;
        StateEntry(Trigger trig, List<Stmt> body, Interpreter interp, String id) {
            this.trig = trig; this.body = body; this.interp = interp; this.id = id;
        }
    }

    private final Map<String, List<Handler>> handlers = new HashMap<>();
    private final List<StateEntry> stateTriggers = new ArrayList<>();
    private final Map<String, Boolean> stateEdges = new HashMap<>();
    private final List<LoadedScript> loaded = new ArrayList<>();
    private final List<VerseError> loadErrors = new ArrayList<>();
    private final Map<String, RegisteredCommand> commands = new LinkedHashMap<>();

    /** Per-script bookkeeping so a single file can be unloaded and reloaded. */
    private final Map<String, List<String>> scriptMarks = new HashMap<>();
    private final Map<String, List<String>> scriptRegions = new HashMap<>();
    private final Map<String, List<Runnable>> scriptTimers = new HashMap<>();

    /** A command defined by a script, ready to be executed. */
    public static final class RegisteredCommand {
        public final CommandDef def;
        public final Interpreter interp;
        RegisteredCommand(CommandDef def, Interpreter interp) { this.def = def; this.interp = interp; }
    }

    public static final class LoadedScript {
        public final String fileName;
        public final Script script;
        public final Interpreter interpreter;
        LoadedScript(String fileName, Script script, Interpreter interpreter) {
            this.fileName = fileName;
            this.script = script;
            this.interpreter = interpreter;
        }
    }

    public TaleEngine(WorldAPI world, Scheduler scheduler) {
        this.world = world;
        this.scheduler = scheduler;
    }

    public List<VerseError> loadErrors() {
        return loadErrors;
    }

    public List<LoadedScript> scripts() {
        return loaded;
    }

    /** Loads a script file. Returns false if the script had problems. */
    public boolean load(Path file) {
        String src;
        try {
            src = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            loadErrors.add(new VerseError(1, "I couldn't read the file " + file.getFileName() + "."));
            return false;
        }
        return loadString(src, file.getFileName().toString());
    }

    public boolean loadString(String src, String fileName) {
        Script script;
        try {
            script = Parser.parseScript(src, fileName);
        } catch (VerseError e) {
            e.sourceLine = sourceLine(src, e.line);
            loadErrors.add(e);
            return false;
        }

        try {
            Interpreter interp = new Interpreter(script, this);
            interp.setSourceFile(fileName);
            AtomicInteger idx = new AtomicInteger();
            List<Stmt> startup = new ArrayList<>();
            for (Stmt s : script.body) {
                if (s instanceof WhenBlock || s instanceof EveryBlock
                        || s instanceof ActionDef || s instanceof RegionDef || s instanceof MarkDef
                        || s instanceof CommandDef) {
                    register(s, interp, fileName + "#" + idx.getAndIncrement());
                } else {
                    startup.add(s);
                }
            }
            if (!startup.isEmpty()) {
                Interpreter.RunContext ctx = new Interpreter.RunContext(fileName, null, null);
                interp.runBlock(startup, ctx);
            }
            loaded.add(new LoadedScript(fileName, script, interp));
            world.log("Hermes loaded " + fileName + " (" + script.body.size() + " lines of rules).");
            return true;
        } catch (VerseError e) {
            e.sourceLine = sourceLine(src, e.line);
            loadErrors.add(e);
            return false;
        }
    }

    private void register(Stmt s, Interpreter interp, String id) {
        if (s instanceof WhenBlock w) {
            Trigger t = w.trigger;
            if (t.kind == Trigger.Kind.EVENT) {
                handlers.computeIfAbsent(t.event, k -> new ArrayList<>())
                        .add(new Handler(t, w.body, interp));
            } else {
                stateTriggers.add(new StateEntry(t, w.body, interp, id));
            }
        } else if (s instanceof EveryBlock ev) {
            long millis = Math.max(50, (long) (ev.seconds * 1000));
            Runnable task = () -> runScheduled(ev, interp, id);
            scheduler.runEvery(millis, task);
            scriptTimers.computeIfAbsent(interp.sourceFile(), k -> new ArrayList<>()).add(task);
        } else if (s instanceof RegionDef r) {
            double minX = Math.min(r.a.x(), r.b.x()), maxX = Math.max(r.a.x(), r.b.x());
            double minY = Math.min(r.a.y(), r.b.y()), maxY = Math.max(r.a.y(), r.b.y());
            double minZ = Math.min(r.a.z(), r.b.z()), maxZ = Math.max(r.a.z(), r.b.z());
            world.defineRegion(r.name, WorldAPI.Vec3.of(minX, minY, minZ), WorldAPI.Vec3.of(maxX, maxY, maxZ));
            scriptRegions.computeIfAbsent(interp.sourceFile(), k -> new ArrayList<>()).add(r.name);
        } else if (s instanceof MarkDef m) {
            marks.put(m.name, m.loc);
            scriptMarks.computeIfAbsent(interp.sourceFile(), k -> new ArrayList<>()).add(m.name);
        } else if (s instanceof CommandDef c) {
            commands.put(c.name.toLowerCase(), new RegisteredCommand(c, interp));
        }
    }

    /** Removes everything one script file contributed, so it can be reloaded. */
    public void unload(String fileName) {
        handlers.entrySet().removeIf(e -> {
            e.getValue().removeIf(h -> fileName.equals(h.interp.sourceFile()));
            return e.getValue().isEmpty();
        });
        stateTriggers.removeIf(e -> fileName.equals(e.interp.sourceFile()));
        commands.entrySet().removeIf(e -> fileName.equals(e.getValue().interp.sourceFile()));

        List<String> marks = scriptMarks.remove(fileName);
        if (marks != null) for (String name : marks) this.marks.remove(name);
        List<String> regions = scriptRegions.remove(fileName);
        if (regions != null) for (String name : regions) world.undefineRegion(name);
        List<Runnable> timers = scriptTimers.remove(fileName);
        if (timers != null) for (Runnable task : timers) scheduler.cancelEvery(task);

        loaded.removeIf(s -> s.fileName.equals(fileName));
        stateEdges.keySet().removeIf(k -> k.startsWith(fileName + "#"));
        loadErrors.clear();
    }

    /** Every command defined by the loaded scripts. */
    public List<RegisteredCommand> commands() {
        return new ArrayList<>(commands.values());
    }

    /** Runs a script command: binds the raw arguments as temporary variables. */
    public void fireCommand(RegisteredCommand rc, PlayerRef p, List<String> rawArgs) {
        Interpreter.RunContext ctx = new Interpreter.RunContext(rc.interp.scriptName(), p, null);
        List<String> args = new ArrayList<>(rawArgs);
        for (int i = 0; i < rc.def.argNames.size(); i++) {
            String value = i < args.size() ? args.get(i) : "";
            ctx.temps.put(rc.def.argNames.get(i), Value.text(value));
        }
        try {
            rc.interp.runBlock(rc.def.body, ctx);
        } catch (VerseError e) {
            reportError(rc.interp, e, p);
        }
    }

    private void runScheduled(EveryBlock ev, Interpreter interp, String id) {
        try {
            Interpreter.RunContext ctx = new Interpreter.RunContext(interp.scriptName(), null, null);
            interp.runBlock(ev.body, ctx);
        } catch (VerseError e) {
            reportError(interp, e, null);
        }
    }

    // ------------------------------------------------------------------
    // events from the world
    // ------------------------------------------------------------------

    public void playerEvent(String event, PlayerRef p) {
        dispatch(event, Trigger.Filter.NONE, null, p, null);
    }

    public void playerEventItem(String event, PlayerRef p, String item) {
        dispatch(event, Trigger.Filter.ITEM, item, p, null);
    }

    public void playerEventBlock(String event, PlayerRef p, String block) {
        dispatch(event, Trigger.Filter.BLOCK, block, p, null);
    }

    public void playerEventText(String event, PlayerRef p, String text) {
        dispatch(event, Trigger.Filter.TEXT, text, p, null);
    }

    public void playerEventRegion(String event, PlayerRef p, String region) {
        dispatch(event, Trigger.Filter.REGION, region, p, null);
    }

    public void playerEventMob(String event, PlayerRef p, String mobType) {
        dispatch(event, Trigger.Filter.MOB, mobType, p, null);
    }

    public void mobEvent(String event, MobRef m) {
        dispatch(event, Trigger.Filter.NONE, null, null, m);
    }

    public void mobEvent(String event, MobRef m, String mobType) {
        dispatch(event, Trigger.Filter.MOB, mobType, null, m);
    }

    public void mobEventByName(String event, MobRef m, String customName) {
        dispatch(event, Trigger.Filter.MOB_NAME, customName, null, m);
    }

    /** A custom event created by scripts themselves. */
    public void fireCustomEvent(String name) {
        dispatch("custom", Trigger.Filter.TEXT, name, null, null);
        world.log("Hermes event fired: " + name);
    }

    private void dispatch(String event, Trigger.Filter filterType, String filter, PlayerRef p, MobRef m) {
        List<Handler> hs = handlers.get(event);
        if (hs == null) return;
        boolean firstSeen = false;
        for (Handler h : hs) {
            Trigger t = h.trig;
            if (!matches(t, filterType, filter, m)) continue;
            if (t.first && p != null) {
                String seen = "hermes-seen-before";
                boolean first = !firstSeen && vars.getPlayer(p.name(), seen).isNone();
                if (!first) continue;
                firstSeen = true;
                vars.setPlayer(p.name(), seen, Value.number(1));
            }
            try {
                Interpreter.RunContext ctx = new Interpreter.RunContext(h.interp.scriptName(), p, m);
                if (allConditions(t, h.interp, ctx)) {
                    h.interp.runBlock(h.body, ctx);
                }
            } catch (VerseError e) {
                reportError(h.interp, e, p);
            } catch (RuntimeException e) {
                world.log("Hermes error: " + e);
            }
        }
    }

    private boolean matches(Trigger t, Trigger.Filter filterType, String filter, MobRef m) {
        if (t.filterType == Trigger.Filter.MOB_NAME) {
            return m != null && filter != null && t.filter.equalsIgnoreCase(world.mobCustomName(m));
        }
        if (t.filterType == Trigger.Filter.NONE) return true;
        if (t.filterType != filterType) return false;
        if (filter == null) return true;
        if (t.filter == null) return true;
        return t.filter.equals(filter);
    }

    private boolean allConditions(Trigger t, Interpreter interp, Interpreter.RunContext ctx) {
        for (Condition c : t.conditions) {
            if (!interp.evalCond(c, ctx)) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // state triggers (polled by the host every ~0.5s)
    // ------------------------------------------------------------------

    public void tick() {
        for (StateEntry e : stateTriggers) {
            try {
                if (e.trig.playerSubject) {
                    for (PlayerRef p : world.onlinePlayers()) {
                        evaluateState(e, p.name(), p);
                    }
                } else {
                    evaluateState(e, "", null);
                }
            } catch (VerseError err) {
                reportError(e.interp, err, null);
            }
        }
    }

    private void evaluateState(StateEntry e, String playerKey, PlayerRef p) {
        Interpreter.RunContext ctx = new Interpreter.RunContext(e.interp.scriptName(), p, null);
        boolean truth = allConditions(e.trig, e.interp, ctx);
        String key = e.id + "#" + playerKey;
        Boolean last = stateEdges.get(key);
        if (last == null) last = false;
        if (truth && !last) {
            e.interp.runBlock(e.body, ctx);
        }
        stateEdges.put(key, truth);
    }

    // ------------------------------------------------------------------
    // errors & cleanup
    // ------------------------------------------------------------------

    private void reportError(Interpreter interp, VerseError e, PlayerRef p) {
        world.log("Hermes error in " + interp.scriptName() + ": " + e.message
                + (e.line > 0 ? " (line " + e.line + ")" : ""));
        if (p != null) {
            world.sendMessage(p, "Â§cÂ§lHermes error: Â§7" + e.message
                    + (e.suggestion != null ? " Â§cTry: Â§7" + e.suggestion.replace('\n', ' ') : ""));
        }
    }

    public void shutdown() {
        // host worlds with schedulers should cancel tasks via their own mechanism
    }

    /** Extracts a source line for error messages. */
    private static String sourceLine(String src, int line) {
        if (line <= 0) return null;
        String[] lines = src.replace("\r", "").split("\n", -1);
        if (line <= lines.length) return lines[line - 1];
        return null;
    }

    /** Simple registry of all known events, used for /Hermes events. */
    public List<String> knownEvents() {
        return new ArrayList<>(handlers.keySet());
    }

    public Map<String, Integer> eventCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<String, List<Handler>> e : handlers.entrySet()) {
            counts.put(e.getKey(), e.getValue().size());
        }
        return counts;
    }
}
