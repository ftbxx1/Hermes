package dev.hermes.core;

import dev.hermes.core.Ast.*;
import dev.hermes.core.WorldAPI.MobRef;
import dev.hermes.core.WorldAPI.PlayerRef;
import dev.hermes.core.WorldAPI.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks the AST and makes the world obey. Every statement becomes calls on
 * the WorldAPI; every condition is evaluated against the current situation.
 */
public final class Interpreter {

    /** Everything a running handler knows about the moment it runs. */
    public static final class RunContext {
        public final String scriptName;
        public PlayerRef player;
        public MobRef mob;
        public final Map<String, Value> temps = new HashMap<>();
        public boolean stopped;
        public int steps;
        /** Set by a "return <value>" statement inside a function or action. */
        public Value returnValue;

        RunContext(String scriptName, PlayerRef player, MobRef mob) {
            this.scriptName = scriptName;
            this.player = player;
            this.mob = mob;
        }
    }

    private static final int MAX_STEPS = 500_000;

    private final Script script;
    private final TaleEngine engine;
    private final WorldAPI world;
    private final Scheduler scheduler;
    private final Map<String, ActionDef> actions = new HashMap<>();
    private final Map<String, FunctionDef> functions = new HashMap<>();

    private static final String[] KNOWN_VERBS = {
        "give", "remove", "set", "add", "tell", "announce", "warn", "welcome", "show",
        "kill", "damage", "heal", "feed", "teleport", "spawn", "play", "write", "open", "close",
        "press", "pull", "power", "unpower", "put", "take", "make", "win", "fire", "clear",
        "create", "stop", "repeat", "loop", "if", "kick", "launch", "title", "actionbar",
        "lightning", "explode", "delete", "particles", "while", "wait",
        "freeze", "unfreeze", "randomly", "push", "throw", "drop",
    };

    Interpreter(Script script, TaleEngine engine) {
        this.script = script;
        this.engine = engine;
        this.world = engine.world;
        this.scheduler = engine.scheduler;
        for (Stmt s : script.body) {
            if (s instanceof ActionDef a) {
                if (actions.containsKey(a.name)) {
                    throw new VerseError(a.line,
                            "There are two actions called '" + a.name + "'. Each action needs its own name.",
                            null, null);
                }
                actions.put(a.name, a);
            }
            if (s instanceof FunctionDef f) {
                if (functions.containsKey(f.name)) {
                    throw new VerseError(f.line,
                            "There are two functions called '" + f.name + "'. Each function needs its own name.",
                            null, null);
                }
                functions.put(f.name, f);
            }
        }
        validateCalls(script.body);
        validateValueCalls(script.body);
    }

    /** Every function call in a value must have a matching definition, so
     *  a misspelt name is caught at reload time, not mid-game. */
    private void validateValueCalls(List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof SetStmt set) checkValue(set.value);
            else if (s instanceof AddStmt a) checkValue(a.amount);
            else if (s instanceof RemoveStmt r) checkValue(r.amount);
            else if (s instanceof ListAddStmt la) checkValue(la.value);
            else if (s instanceof ListRemoveStmt lr) checkValue(lr.value);
            else if (s instanceof TellStmt t) checkValue(t.text);
            else if (s instanceof AnnounceStmt a) checkValue(a.text);
            else if (s instanceof WarnStmt w) checkValue(w.text);
            else if (s instanceof WelcomeStmt w) checkValue(w.text);
            else if (s instanceof TitleStmt t) { checkValue(t.title); if (t.subtitle != null) checkValue(t.subtitle); }
            else if (s instanceof ActionbarStmt a) checkValue(a.text);
            else if (s instanceof SetPlayerStatStmt sp) checkValue(sp.value);
            else if (s instanceof SetBossbarStmt bb) { checkValue(bb.title); if (bb.progress != null) checkValue(bb.progress); }
            else if (s instanceof ConfigSetStmt cs) checkValue(cs.value);
            else if (s instanceof ActionCallStmt ac) { for (ValueExpr v : ac.args) checkValue(v); }
            if (s instanceof IfStmt i) {
                checkCond(i.cond);
                validateValueCalls(i.thenBody);
                if (i.elseBody != null) validateValueCalls(i.elseBody);
            } else if (s instanceof WhileStmt wh) {
                checkCond(wh.cond);
                validateValueCalls(wh.body);
            } else if (s instanceof RepeatStmt rep) {
                validateValueCalls(rep.body);
            } else if (s instanceof LoopStmt l) {
                validateValueCalls(l.body);
            } else if (s instanceof WhenBlock w) {
                for (Condition c : w.trigger.conditions) checkCond(c);
                validateValueCalls(w.body);
            } else if (s instanceof EveryBlock ev) {
                validateValueCalls(ev.body);
            } else if (s instanceof ActionDef a) {
                validateValueCalls(a.body);
            } else if (s instanceof FunctionDef f) {
                validateValueCalls(f.body);
            } else if (s instanceof CommandDef c) {
                validateValueCalls(c.body);
            }
        }
    }

    private void checkCond(Condition c) {
        if (c instanceof CmpCond cmp) {
            checkValue(cmp.left);
            checkValue(cmp.right);
        } else if (c instanceof NotCond n) {
            checkCond(n.inner);
        } else if (c instanceof AndCond and) {
            for (Condition p : and.parts) checkCond(p);
        } else if (c instanceof OrCond or) {
            for (Condition p : or.parts) checkCond(p);
        }
    }

    private void checkValue(ValueExpr expr) {
        if (expr instanceof FunctionCallExpr fc) {
            if (!functions.containsKey(fc.name)) {
                String closest = Dictionary.suggest(fc.name, functions.keySet().toArray(new String[0]));
                throw new VerseError(fc.line, "There is no function called '" + fc.name + "'.",
                        closest != null
                                ? "Did you mean '" + closest + "'?\n\nTo make it, define it:\n\nfunction \"" + fc.name + "\" with argument <amount>\n    return 0"
                                : "Define it first:\n\nfunction \"" + fc.name + "\" with argument <amount>\n    return 0",
                        null);
            }
        }
        if (expr instanceof BinaryExpr b) {
            checkValue(b.left);
            checkValue(b.right);
        } else if (expr instanceof TemplateExpr t) {
            for (Object part : t.parts) {
                if (part instanceof ValueExpr ve) checkValue(ve);
            }
        }
    }

    /** Every call to an action must match a defined action. */
    private void validateCalls(List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof ActionCallStmt call) {
                if (!actions.containsKey(call.action)) {
                    String closest = Dictionary.suggest(call.action, KNOWN_VERBS);
                    throw new VerseError(s.line,
                            "I don't know the action '" + call.action + "'.",
                            closest != null
                                    ? "Did you mean the word '" + closest + "'?\n\nTo make it an action, define it:\n\naction " + call.action + " the player\n    tell the player \"Hi!\""
                                    : "Define it first:\n\naction " + call.action + " the player\n    tell the player \"Hi!\"");
                }
            } else if (s instanceof WhenBlock w) {
                validateCalls(w.body);
            } else if (s instanceof EveryBlock e) {
                validateCalls(e.body);
            } else if (s instanceof IfStmt i) {
                validateCalls(i.thenBody);
                if (i.elseBody != null) validateCalls(i.elseBody);
            } else if (s instanceof RepeatStmt r) {
                validateCalls(r.body);
            } else if (s instanceof WhileStmt wh) {
                validateCalls(wh.body);
            } else if (s instanceof LoopStmt l) {
                validateCalls(l.body);
            } else if (s instanceof ActionDef a) {
                validateCalls(a.body);
            } else if (s instanceof FunctionDef f) {
                validateCalls(f.body);
            } else if (s instanceof CommandDef c) {
                validateCalls(c.body);
            }
        }
    }

    public String scriptName() {
        return script.name != null ? script.name : "script";
    }

    /** The .her file this interpreter came from (for unloads), or null. */
    private String sourceFile = null;

    public void setSourceFile(String name) {
        this.sourceFile = name;
    }

    public String sourceFile() {
        return sourceFile;
    }

    // ------------------------------------------------------------------
    // blocks
    // ------------------------------------------------------------------

    void runBlock(List<Stmt> body, RunContext ctx) {
        runBlockFrom(body, ctx, 0);
    }

    private void runBlockFrom(List<Stmt> body, RunContext ctx, int start) {
        for (int i = start; i < body.size(); i++) {
            if (ctx.stopped) return;
            if (++ctx.steps > MAX_STEPS) {
                throw new VerseError(body.get(i).line,
                        "This script is stuck in a loop. I stopped it so the server doesn't freeze.",
                        "Use 'stop' to leave a loop early.");
            }
            Stmt s = body.get(i);
            if (s instanceof WaitStmt w) {
                long millis = Math.max(50, (long) (w.seconds * 1000));
                Map<String, Value> snapshot = new HashMap<>(ctx.temps);
                int next = i + 1;
                scheduler.runLater(millis, () -> {
                    RunContext c2 = new RunContext(ctx.scriptName, ctx.player, ctx.mob);
                    c2.temps.putAll(snapshot);
                    try {
                        runBlockFrom(body, c2, next);
                    } catch (VerseError e) {
                        reportError(e, c2.player);
                    } catch (RuntimeException e) {
                        world.log("Hermes error: " + e);
                    }
                });
                return;
            }
            run(s, ctx);
        }
    }

    /** Loops: over a list, over all players, over a range of numbers, or over an inventory. */
    private void runLoop(LoopStmt loop, RunContext ctx) {
        switch (loop.kind) {
            case "players": {
                for (PlayerRef lp : world.onlinePlayers()) {
                    if (ctx.stopped) break;
                    RunContext sub = new RunContext(ctx.scriptName, lp, ctx.mob);
                    sub.temps.put(loop.itemName, Value.text(lp.name()));
                    runBlock(loop.body, sub);
                }
                break;
            }
            case "numbers": {
                int step = loop.from <= loop.to ? 1 : -1;
                for (double i = loop.from; step > 0 ? i <= loop.to : i >= loop.to; i += step) {
                    if (ctx.stopped) break;
                    ctx.temps.put(loop.itemName, Value.number(i));
                    runBlock(loop.body, ctx);
                }
                ctx.temps.remove(loop.itemName);
                break;
            }
            case "inventory": {
                PlayerRef p = needPlayer(loop, ctx);
                for (String item : Dictionary.canonicalItems()) {
                    if (ctx.stopped) break;
                    if (world.countItem(p, item) <= 0) continue;
                    ctx.temps.put(loop.itemName, Value.text(item));
                    runBlock(loop.body, ctx);
                }
                ctx.temps.remove(loop.itemName);
                break;
            }
            default: {
                Value list = engine.vars.list(loop.listName);
                for (Value item : list.items) {
                    if (ctx.stopped) break;
                    ctx.temps.put(loop.itemName, item);
                    runBlock(loop.body, ctx);
                }
                ctx.temps.remove(loop.itemName);
                break;
            }
        }
    }

    private void run(Stmt s, RunContext ctx) {
        if (s instanceof IfStmt ifs) {
            if (evalCond(ifs.cond, ctx)) runBlock(ifs.thenBody, ctx);
            else if (ifs.elseBody != null) runBlock(ifs.elseBody, ctx);
        } else if (s instanceof RepeatStmt rep) {
            for (int i = 0; i < (int) rep.times && !ctx.stopped; i++) {
                runBlock(rep.body, ctx);
            }
        } else if (s instanceof WhileStmt wh) {
            while (!ctx.stopped && evalCond(wh.cond, ctx)) {
                runBlock(wh.body, ctx);
            }
        } else if (s instanceof LoopStmt loop) {
            runLoop(loop, ctx);
        } else if (s instanceof SetPlayerStatStmt stat) {
            PlayerRef p = needPlayer(s, ctx);
            double v = eval(stat.value, ctx).num;
            switch (stat.stat) {
                case "health": world.setHealth(p, v); break;
                case "hunger": world.setHunger(p, (int) v); break;
                case "xp": world.setXp(p, (int) v); break;
                case "level": world.setLevel(p, (int) v); break;
                default: break;
            }
        } else if (s instanceof SetBossbarStmt bb) {
            PlayerRef p = needPlayer(s, ctx);
            String title = eval(bb.title, ctx).display();
            double progress = bb.progress != null ? eval(bb.progress, ctx).num : 100;
            world.setBossbar(p, title, progress);
        } else if (s instanceof ClearBossbarStmt cb) {
            world.clearBossbar(needPlayer(s, ctx));
        } else if (s instanceof StopStmt) {
            ctx.stopped = true;
        } else if (s instanceof ReturnStmt ret) {
            ctx.returnValue = eval(ret.value, ctx);
            ctx.stopped = true;
        } else if (s instanceof SetStmt set) {
            Value v = eval(set.value, ctx);
            setVar(set.target, v, ctx);
        } else if (s instanceof AddStmt add) {
            Value delta = eval(add.amount, ctx);
            changeVar(add.target, delta.num, ctx);
        } else if (s instanceof RemoveStmt rem) {
            Value delta = eval(rem.amount, ctx);
            changeVar(rem.target, -delta.num, ctx);
        } else if (s instanceof ListCreate lc) {
            engine.vars.list(lc.name);
        } else if (s instanceof TeamCreate tc) {
            world.createTeam(tc.name);
        } else if (s instanceof ListAddStmt la) {
            engine.vars.list(la.list).items.add(eval(la.value, ctx));
        } else if (s instanceof ListRemoveStmt lr) {
            Value target = eval(lr.value, ctx);
            engine.vars.list(lr.list).items.removeIf(v -> v.equalsValue(target));
        } else if (s instanceof ListClearStmt lc) {
            engine.vars.list(lc.list).items.clear();
        } else if (s instanceof ListDeleteStmt ld) {
            engine.vars.deleteList(ld.list);
        } else if (s instanceof FeedStmt f) {
            if (f.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only feed players.", "feed player by 5");
            world.feed(needPlayer(s, ctx), f.amount);
        } else if (s instanceof ClearInventoryStmt) {
            world.clearInventory(needPlayer(s, ctx));
        } else if (s instanceof KickStmt k) {
            if (k.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only kick players.", "kick player because \"Too slow!\"");
            world.kick(needPlayer(s, ctx), k.reason);
        } else if (s instanceof LaunchStmt l) {
            if (l.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only launch players.", "launch player by 5");
            world.launch(needPlayer(s, ctx), l.amount);
        } else if (s instanceof PushStmt pu) {
            if (pu.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only push players.", "push player up by 3");
            world.push(needPlayer(s, ctx), pu.direction, pu.strength);
        } else if (s instanceof DropStmt dr) {
            world.dropItems(resolveLoc(dr.where, s, ctx), dr.spec);
        } else if (s instanceof RunCommandStmt rc) {
            if (rc.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only make players run commands.", "make player run command \"/spawn\"");
            world.runCommand(needPlayer(s, ctx), rc.command);
        } else if (s instanceof SendResourcePackStmt rp) {
            if (rp.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only send resource packs to players.", "send player resource pack \"https://example.com/pack.zip\"");
            world.sendResourcePack(needPlayer(s, ctx), rp.url);
        } else if (s instanceof SetRespawnStmt sr) {
            world.setRespawnPoint(needPlayer(s, ctx), resolveLoc(sr.where, s, ctx));
        } else if (s instanceof SetEquipmentStmt eq) {
            world.setEquipment(needPlayer(s, ctx), eq.slot, eq.spec);
        } else if (s instanceof FireworkStmt fw) {
            world.launchFirework(resolveLoc(fw.where, s, ctx));
        } else if (s instanceof SwingStmt sw) {
            if (sw.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only make players swing their hand.", "make player swing their hand");
            world.swingHand(needPlayer(s, ctx));
        } else if (s instanceof LookStmt lk) {
            if (lk.target == TargetRef.MOB) throw new VerseError(s.line,
                    "I can only make players look around.", "make player look at 10 64 20");
            world.lookAt(needPlayer(s, ctx), resolveLoc(lk.where, s, ctx));
        } else if (s instanceof SetSpeedStmt spd) {
            world.setSpeed(needPlayer(s, ctx), spd.kind, spd.speed);
        } else if (s instanceof TitleStmt title) {
            PlayerRef p = needPlayer(s, ctx);
            world.sendTitle(p, eval(title.title, ctx).display(),
                    title.subtitle != null ? eval(title.subtitle, ctx).display() : "");
        } else if (s instanceof ActionbarStmt ab) {
            world.sendActionbar(needPlayer(s, ctx), eval(ab.text, ctx).display());
        } else if (s instanceof SetGamemodeStmt gm) {
            world.setGamemode(needPlayer(s, ctx), gm.mode);
        } else if (s instanceof LightningStmt lt) {
            world.strikeLightning(resolveLoc(lt.where, s, ctx));
        } else if (s instanceof ExplodeStmt ex) {
            world.explode(resolveLoc(ex.where, s, ctx), ex.power);
        } else if (s instanceof ParticleStmt pa) {
            world.spawnParticles(pa.particle, resolveLoc(pa.where, s, ctx), pa.count, pa.size);
        } else if (s instanceof ShowStmt show) {
            Value v = getVar(show.target, ctx);
            PlayerRef p = needPlayer(s, ctx);
            world.sendMessage(p, show.target.describe() + ": " + v.display());
        } else if (s instanceof FireEventStmt fe) {
            engine.fireCustomEvent(fe.name);
        } else if (s instanceof WinStmt) {
            world.announce("The game has been won!");
            engine.fireCustomEvent("game_won");
        } else if (s instanceof KillStmt k) {
            if (k.target == TargetRef.MOB) world.killMob(needMob(s, ctx));
            else world.kill(needPlayer(s, ctx));
        } else if (s instanceof DamageStmt d) {
            if (d.target == TargetRef.MOB) world.damageMob(needMob(s, ctx), d.amount);
            else world.damage(needPlayer(s, ctx), d.amount);
        } else if (s instanceof HealStmt h) {
            if (h.target == TargetRef.MOB) world.healMob(needMob(s, ctx), h.amount);
            else world.heal(needPlayer(s, ctx), h.amount);
        } else if (s instanceof GiveItemStmt g) {
            if (g.target == TargetRef.ALL_PLAYERS) {
                for (PlayerRef each : world.onlinePlayers()) world.giveItemSpec(each, g.spec);
            } else {
                world.giveItemSpec(needPlayer(s, ctx), g.spec);
            }
        } else if (s instanceof GiveBookStmt gb) {
            WorldAPI.BookDef book = engine.book(gb.book);
            if (book == null) {
                throw new VerseError(s.line,
                        "There is no book called '" + gb.book + "'.",
                        "Create it first:\n\ncreate book \"" + gb.book + "\" with page \"...\"");
            }
            if (gb.target == TargetRef.ALL_PLAYERS) {
                for (PlayerRef each : world.onlinePlayers()) world.giveBook(each, book);
            } else {
                world.giveBook(needPlayer(s, ctx), book);
            }
        } else if (s instanceof OpenGuiStmt og) {
            engine.openGui(og.gui, needPlayer(s, ctx));
        } else if (s instanceof SetGuiSlotStmt gs) {
            engine.setGuiSlot(gs.gui, gs.slot, gs.spec);
        } else if (s instanceof BookCreate bc) {
            engine.registerBook(bc.name, bc.book, scriptName());
        } else if (s instanceof CreateWorldStmt cw) {
            world.createWorld(cw.world);
        } else if (s instanceof DeleteWorldStmt dw) {
            world.deleteWorld(dw.world);
        } else if (s instanceof SetWorldWeatherStmt sww) {
            world.setWorldWeather(sww.world, sww.weather);
        } else if (s instanceof SetWorldTimeStmt swt) {
            world.setWorldTime(swt.world, swt.daypart);
        } else if (s instanceof CreateDatabaseStmt cd) {
            engine.vars.createDatabase(cd.db);
        } else if (s instanceof ConfigSetStmt cs) {
            world.setConfigValue(cs.file, cs.key, eval(cs.value, ctx).display());
        } else if (s instanceof TakeItemStmt t) {
            if (t.target == TargetRef.ALL_PLAYERS) {
                for (PlayerRef each : world.onlinePlayers()) {
                    if (!world.takeItem(each, t.item, t.count)) {
                        world.warn(each, "You don't have " + t.count + " " + t.item + ".");
                    }
                }
            } else {
                PlayerRef p = needPlayer(s, ctx);
                if (!world.takeItem(p, t.item, t.count)) {
                    world.warn(p, "You don't have " + t.count + " " + t.item + ".");
                }
            }
        } else if (s instanceof GiveXpStmt xp) {
            if (xp.target == TargetRef.ALL_PLAYERS) {
                for (PlayerRef each : world.onlinePlayers()) world.giveXp(each, (int) xp.amount);
            } else {
                world.giveXp(needPlayer(s, ctx), (int) xp.amount);
            }
        } else if (s instanceof GiveLevelsStmt lv) {
            if (lv.target == TargetRef.ALL_PLAYERS) {
                for (PlayerRef each : world.onlinePlayers()) world.giveLevels(each, (int) lv.amount);
            } else {
                world.giveLevels(needPlayer(s, ctx), (int) lv.amount);
            }
        } else if (s instanceof EffectStmt ef) {
            if (ef.target == TargetRef.ALL_PLAYERS) {
                for (PlayerRef each : world.onlinePlayers()) world.effectOnPlayer(each, ef.effect, ef.seconds);
            } else if (ef.target == TargetRef.MOB) world.effectOnMob(needMob(s, ctx), ef.effect, ef.seconds);
            else world.effectOnPlayer(needPlayer(s, ctx), ef.effect, ef.seconds);
        } else if (s instanceof RemoveEffectStmt re) {
            if (re.target == TargetRef.MOB) world.removeEffectFromMob(needMob(s, ctx), re.effect);
            else world.removeEffectFromPlayer(needPlayer(s, ctx), re.effect);
        } else if (s instanceof MakeEffectStmt me) {
            if (me.target == TargetRef.MOB) world.effectOnMob(needMob(s, ctx), me.effect, 10);
            else world.effectOnPlayer(needPlayer(s, ctx), me.effect, 10);
        } else if (s instanceof PermissionStmt perm) {
            if (perm.target == TargetRef.ALL_PLAYERS) {
                for (PlayerRef each : world.onlinePlayers()) {
                    if (perm.grant) world.grantPermission(each, perm.perm);
                    else world.revokePermission(each, perm.perm);
                }
            } else {
                PlayerRef p = needPlayer(s, ctx);
                if (perm.grant) world.grantPermission(p, perm.perm);
                else world.revokePermission(p, perm.perm);
            }
        } else if (s instanceof TellStmt tell) {
            PlayerRef p = needPlayer(s, ctx);
            Value v = eval(tell.text, ctx);
            world.sendMessage(p, v.display());
        } else if (s instanceof AnnounceStmt ann) {
            world.announce(eval(ann.text, ctx).display());
        } else if (s instanceof WarnStmt warn) {
            world.warn(needPlayer(s, ctx), eval(warn.text, ctx).display());
        } else if (s instanceof WelcomeStmt wel) {
            world.welcome(needPlayer(s, ctx), eval(wel.text, ctx).display());
        } else if (s instanceof TeleportStmt tp) {
            if (tp.target == TargetRef.MOB) {
                world.teleportMob(needMob(s, ctx), resolveLoc(tp.where, s, ctx));
            } else {
                PlayerRef p = needPlayer(s, ctx);
                if (tp.where.kind.equals("spawn")) world.teleportToSpawn(p);
                else world.teleport(p, resolveLoc(tp.where, s, ctx));
            }
        } else if (s instanceof TeleportRandomStmt rtp) {
            if (rtp.target == TargetRef.MOB) {
                world.teleportMob(needMob(s, ctx), randomNear(world.mobLocation(needMob(s, ctx)), rtp.radius));
            } else {
                PlayerRef p = needPlayer(s, ctx);
                world.teleport(p, randomNear(world.playerLocation(p), rtp.radius));
            }
        } else if (s instanceof FreezeStmt fz) {
            if (fz.target == TargetRef.MOB) {
                throw new VerseError(s.line,
                        "I can only freeze players.", "freeze player");
            }
            world.setFrozen(needPlayer(s, ctx), fz.frozen);
        } else if (s instanceof SpawnMobStmt sm) {
            Vec3 loc = resolveLoc(sm.where, s, ctx);
            for (int i = 0; i < sm.count; i++) {
                world.spawnMob(sm.mob, loc, sm.customName);
            }
        } else if (s instanceof PlaySoundStmt ps) {
            world.playSound(ps.sound, resolveLoc(ps.where, s, ctx));
        } else if (s instanceof SetWeatherStmt sw) {
            world.setWeather(sw.weather);
        } else if (s instanceof SetTimeStmt st) {
            world.setTime(st.daypart);
        } else if (s instanceof SetBlockStmt sb) {
            world.setBlock(sb.loc, sb.block);
        } else if (s instanceof NearbyOpStmt no) {
            PlayerRef p = needPlayer(s, ctx);
            switch (no.op) {
                case "open door": world.openDoorNear(p); break;
                case "close door": world.closeDoorNear(p); break;
                case "press button": world.pressButtonNear(p); break;
                case "pull lever": world.pullLeverNear(p); break;
            }
        } else if (s instanceof SignStmt sign) {
            List<String> lines = new ArrayList<>(sign.lines);
            while (lines.size() < 4) lines.add("");
            world.writeSign(sign.loc, lines);
        } else if (s instanceof PowerStmt pw) {
            if (pw.on) world.powerBlock(pw.loc);
            else world.unpowerBlock(pw.loc);
        } else if (s instanceof ChestStmt cs) {
            if (cs.op.equals("add")) world.chestAdd(cs.loc, cs.item, cs.count);
            else world.chestTake(cs.loc, cs.item, cs.count);
        } else if (s instanceof ScoreSetStmt ss) {
            world.setScore(needPlayer(s, ctx), ss.objective, (int) ss.value);
        } else if (s instanceof ScoreAddStmt sa) {
            world.addScore(needPlayer(s, ctx), sa.objective, (int) sa.value);
        } else if (s instanceof TeamStmt ts) {
            switch (ts.op) {
                case "create": world.createTeam(ts.team); break;
                case "join": world.putInTeam(needPlayer(s, ctx), ts.team); break;
                case "leave": world.removeFromTeam(needPlayer(s, ctx), ts.team); break;
            }
        } else if (s instanceof ActionCallStmt call) {
            ActionDef def = actions.get(call.action);
            if (def == null) {
                throw new VerseError(s.line,
                        "There is no action called '" + call.action + "'.",
                        "Define it first:\n\naction " + call.action + " the player\n    tell the player \"Hi!\"");
            }
            RunContext sub = new RunContext(ctx.scriptName,
                    call.passPlayer ? ctx.player : null,
                    call.passPlayer ? ctx.mob : null);
            if (call.passPlayer) sub.temps.putAll(ctx.temps);
            for (int i = 0; i < def.params.size() && i < call.args.size(); i++) {
                sub.temps.put(def.params.get(i), eval(call.args.get(i), ctx));
            }
            runBlock(def.body, sub);
        } else {
            throw new VerseError(s.line, "I don't know how to do that yet.");
        }
    }

    // ------------------------------------------------------------------
    // targets and locations
    // ------------------------------------------------------------------

    private PlayerRef needPlayer(Stmt s, RunContext ctx) {
        if (ctx.player == null) {
            throw new VerseError(s.line,
                    "There is no player here. This action can only run when a player is involved.",
                    "Put it inside a 'when player ...' block.");
        }
        return ctx.player;
    }

    private MobRef needMob(Stmt s, RunContext ctx) {
        if (ctx.mob == null) {
            throw new VerseError(s.line,
                    "There is no mob here. This action can only run when a mob is involved.",
                    "Put it inside a 'when mob dies' block.");
        }
        return ctx.mob;
    }

    private Vec3 resolveLoc(LocRef where, Stmt s, RunContext ctx) {
        switch (where.kind) {
            case "coords": return where.vec;
            case "player": {
                PlayerRef p = needPlayer(s, ctx);
                return world.playerLocation(p);
            }
            case "spawn":
                return world.worldSpawn();
            case "mark": {
                Vec3 v = engine.marks.get(where.name);
                if (v == null) {
                    throw new VerseError(s.line,
                            "I don't know where '" + where.name + "' is.",
                            "Mark it first:\n\nmark " + where.name + " at 100 64 200");
                }
                return v;
            }
            default:
                throw new VerseError(s.line, "I need a place here: coordinates, or a marked place.");
        }
    }

    // ------------------------------------------------------------------
    // variables
    // ------------------------------------------------------------------

    private void setVar(VarTarget t, Value v, RunContext ctx) {
        switch (t.kind) {
            case "player": {
                PlayerRef p = needPlayerVar(ctx);
                engine.vars.setPlayer(p.name(), t.name, v);
                break;
            }
            case "world":
                engine.vars.setWorld(t.name, v);
                break;
            case "temp":
                ctx.temps.put(t.name, v);
                break;
            case "score": {
                PlayerRef p = needPlayerVar(ctx);
                world.setScore(p, t.name, v.isNumber() ? (int) v.num : 0);
                break;
            }
            case "database":
                engine.vars.setDatabase(t.name, t.key, v);
                break;
            case "playerdata": {
                PlayerRef p = needPlayerVar(ctx);
                engine.vars.setPlayerData(p.name(), t.name, v);
                break;
            }
            case "config":
                world.setConfigValue(t.name, t.key, v.display());
                break;
            default:
                break;
        }
    }

    private void changeVar(VarTarget t, double delta, RunContext ctx) {
        switch (t.kind) {
            case "player": {
                PlayerRef p = needPlayerVar(ctx);
                Value cur = engine.vars.getPlayer(p.name(), t.name);
                requireNumber(cur, t, ctx);
                engine.vars.setPlayer(p.name(), t.name, Value.number((cur.isNumber() ? cur.num : 0) + delta));
                break;
            }
            case "world": {
                Value cur = engine.vars.getWorld(t.name);
                requireNumber(cur, t, ctx);
                engine.vars.setWorld(t.name, Value.number((cur.isNumber() ? cur.num : 0) + delta));
                break;
            }
            case "temp": {
                Value cur = ctx.temps.getOrDefault(t.name, Value.none());
                requireNumber(cur, t, ctx);
                ctx.temps.put(t.name, Value.number((cur.isNumber() ? cur.num : 0) + delta));
                break;
            }
            case "score": {
                PlayerRef p = needPlayerVar(ctx);
                world.addScore(p, t.name, (int) delta);
                break;
            }
            case "database": {
                Value cur = engine.vars.getDatabase(t.name, t.key);
                requireNumber(cur, t, ctx);
                engine.vars.setDatabase(t.name, t.key, Value.number((cur.isNumber() ? cur.num : 0) + delta));
                break;
            }
            case "playerdata": {
                PlayerRef p = needPlayerVar(ctx);
                Value cur = engine.vars.getPlayerData(p.name(), t.name);
                requireNumber(cur, t, ctx);
                engine.vars.setPlayerData(p.name(), t.name, Value.number((cur.isNumber() ? cur.num : 0) + delta));
                break;
            }
            default:
                break;
        }
    }

    private PlayerRef needPlayerVar(RunContext ctx) {
        if (ctx.player == null) {
            throw new VerseError(0,
                    "There is no player here, so I can't change the player's variables.",
                    "Put this inside a 'when player ...' block.");
        }
        return ctx.player;
    }

    private void requireNumber(Value cur, VarTarget t, RunContext ctx) {
        if (cur.isList() || cur.isText()) {
            throw new VerseError(0, t.describe() + " is not a number, so I can't add to it.");
        }
    }

    private Value getVar(VarTarget t, RunContext ctx) {
        switch (t.kind) {
            case "player": {
                if (ctx.player == null) return Value.none();
                return engine.vars.getPlayer(ctx.player.name(), t.name);
            }
            case "world":
                return engine.vars.getWorld(t.name);
            case "temp":
                return ctx.temps.getOrDefault(t.name, Value.none());
            case "score": {
                if (ctx.player == null) return Value.number(0);
                return Value.number(world.score(ctx.player, t.name));
            }
            case "database":
                return engine.vars.getDatabase(t.name, t.key);
            case "playerdata": {
                if (ctx.player == null) return Value.none();
                return engine.vars.getPlayerData(ctx.player.name(), t.name);
            }
            case "config":
                return Value.text(world.configValue(t.name, t.key));
            default:
                return Value.none();
        }
    }

    // ------------------------------------------------------------------
    // value expressions
    // ------------------------------------------------------------------

    private Value eval(ValueExpr e, RunContext ctx) {
        if (e instanceof NumExpr n) return Value.number(n.v);
        if (e instanceof BinaryExpr b) {
            double a = numericValue(eval(b.left, ctx));
            double c = numericValue(eval(b.right, ctx));
            switch (b.op) {
                case "+": return Value.number(a + c);
                case "-": return Value.number(a - c);
                case "*": return Value.number(a * c);
                default: return c == 0 ? Value.number(0) : Value.number(a / c);
            }
        }
        if (e instanceof TextExpr t) return Value.text(t.v);
        if (e instanceof TemplateExpr tm) {
            StringBuilder sb = new StringBuilder();
            for (Object part : tm.parts) {
                sb.append(part instanceof String s ? s : eval((ValueExpr) part, ctx).display());
            }
            return Value.text(sb.toString());
        }
        if (e instanceof TruthExpr b) return Value.truth(b.v);
        if (e instanceof VarGetExpr g) return getVar(g.target, ctx);
        if (e instanceof HealthExpr) return Value.number(world.health(needPlayerForExpr(e, ctx)));
        if (e instanceof HungerExpr) return Value.number(world.hunger(needPlayerForExpr(e, ctx)));
        if (e instanceof XpExpr) return Value.number(world.xp(needPlayerForExpr(e, ctx)));
        if (e instanceof LevelExpr) return Value.number(world.level(needPlayerForExpr(e, ctx)));
        if (e instanceof ScoreGetExpr sc) {
            return Value.number(world.score(needPlayerForExpr(e, ctx), sc.objective));
        }
        if (e instanceof LengthExpr len) {
            return Value.number(engine.vars.list(len.list).items.size());
        }
        if (e instanceof DatabaseGetExpr dg) {
            return engine.vars.getDatabase(dg.db, dg.key);
        }
        if (e instanceof FunctionCallExpr fc) {
            return callFunction(fc, ctx);
        }
        if (e instanceof ConfigGetExpr cg) {
            return Value.text(world.configValue(cg.file, cg.key));
        }
        if (e instanceof PlayerDataGetExpr pd) {
            if (ctx.player == null) return Value.none();
            return engine.vars.getPlayerData(ctx.player.name(), pd.key);
        }
        if (e instanceof PlayerNameExpr) {
            return Value.text(needPlayerForExpr(e, ctx).name());
        }
        if (e instanceof PlayerWorldExpr) {
            return Value.text(world.dimensionOf(needPlayerForExpr(e, ctx)));
        }
        if (e instanceof PlayerCoordExpr c) {
            Vec3 loc = world.playerLocation(needPlayerForExpr(e, ctx));
            switch (c.axis) {
                case "x": return Value.number(loc.x());
                case "y": return Value.number(loc.y());
                default: return Value.number(loc.z());
            }
        }
        if (e instanceof GamemodeExpr) {
            return Value.text(world.gamemode(needPlayerForExpr(e, ctx)));
        }
        if (e instanceof RandomExpr r) {
            double lo = Math.min(r.a, r.b);
            double hi = Math.max(r.a, r.b);
            if (r.a == Math.floor(r.a) && r.b == Math.floor(r.b)) {
                return Value.number(Math.floor(lo + Math.random() * (hi - lo + 1)));
            }
            return Value.number(lo + Math.random() * (hi - lo));
        }
        if (e instanceof RandomListExpr rl) {
            List<Value> items = engine.vars.list(rl.list).items;
            if (items.isEmpty()) return Value.none();
            return items.get((int) (Math.random() * items.size()));
        }
        if (e instanceof OnlineCountExpr) {
            return Value.number(world.onlinePlayers().size());
        }
        if (e instanceof CountItemExpr ci) {
            return Value.number(world.countItem(needPlayerForExpr(e, ctx), ci.item));
        }
        if (e instanceof HeldItemExpr) {
            return Value.text(world.heldItem(needPlayerForExpr(e, ctx)));
        }
        if (e instanceof PingExpr) {
            return Value.number(world.ping(needPlayerForExpr(e, ctx)));
        }
        if (e instanceof IpExpr) {
            return Value.text(world.ip(needPlayerForExpr(e, ctx)));
        }
        if (e instanceof TargetExpr) {
            String t = world.targetEntity(needPlayerForExpr(e, ctx));
            return t == null ? Value.none() : Value.text(t);
        }
        if (e instanceof LastDamagerExpr) {
            String d = world.lastDamager(needPlayerForExpr(e, ctx));
            return d == null ? Value.none() : Value.text(d);
        }
        if (e instanceof StandingBlockExpr) {
            return Value.text(world.blockStandingIn(needPlayerForExpr(e, ctx)));
        }
        throw new VerseError(e.line, "I can't work out that value.");
    }

    /** Runs a user-defined function and hands back what it returned. */
    private Value callFunction(FunctionCallExpr fc, RunContext ctx) {
        FunctionDef def = functions.get(fc.name);
        if (def == null) {
            String closest = Dictionary.suggest(fc.name, functions.keySet().toArray(new String[0]));
            throw new VerseError(fc.line, "There is no function called '" + fc.name + "'.",
                    closest != null
                            ? "Did you mean '" + closest + "'?\n\nTo make it, define it:\n\nfunction \"" + fc.name + "\" with argument <amount>\n    return 0"
                            : "Define it first:\n\nfunction \"" + fc.name + "\" with argument <amount>\n    return 0");
        }
        RunContext sub = new RunContext(ctx.scriptName, ctx.player, ctx.mob);
        sub.steps = ctx.steps;
        for (int i = 0; i < def.params.size() && i < fc.args.size(); i++) {
            sub.temps.put(def.params.get(i), eval(fc.args.get(i), ctx));
        }
        runBlock(def.body, sub);
        return sub.returnValue != null ? sub.returnValue : Value.none();
    }

    private static double numericValue(Value v) {
        if (v.isNumber()) return v.num;
        if (v.isTruth()) return v.truth ? 1 : 0;
        if (v.isText()) {
            try { return Double.parseDouble(v.text.trim()); }
            catch (NumberFormatException ex) { return 0; }
        }
        return 0;
    }

    /** A random spot within radius of a location, on the same height. */
    private static Vec3 randomNear(Vec3 base, double radius) {
        double angle = Math.random() * 2 * Math.PI;
        double dist = Math.random() * radius;
        return new Vec3(base.x() + Math.cos(angle) * dist, base.y(), base.z() + Math.sin(angle) * dist);
    }

    private PlayerRef needPlayerForExpr(ValueExpr e, RunContext ctx) {
        if (ctx.player == null) {
            throw new VerseError(e.line,
                    "There is no player here, so I can't read their stats.",
                    "Run this from a 'when player ...' block.");
        }
        return ctx.player;
    }

    private void reportError(VerseError e, PlayerRef p) {
        world.log("Hermes error in " + scriptName() + ": " + e.message
                + (e.line > 0 ? " (line " + e.line + ")" : ""));
        if (p != null) {
            world.sendMessage(p, "§c§lHermes error: §7" + e.message
                    + (e.suggestion != null ? " §cTry: §7" + e.suggestion.replace('\n', ' ') : ""));
        }
    }

    // ------------------------------------------------------------------
    // conditions
    // ------------------------------------------------------------------

    boolean evalCond(Condition c, RunContext ctx) {
        if (c instanceof TruthCond t) return t.v;
        if (c instanceof ChanceCond cc) return Math.random() * 100 < cc.percent;
        if (c instanceof NotCond n) return !evalCond(n.inner, ctx);
        if (c instanceof AndCond a) {
            for (Condition p : a.parts) if (!evalCond(p, ctx)) return false;
            return true;
        }
        if (c instanceof OrCond o) {
            for (Condition p : o.parts) if (evalCond(p, ctx)) return true;
            return false;
        }
        if (c instanceof CmpCond cmp) return evalCmp(cmp, ctx);
        if (c instanceof HasCond h) {
            if (ctx.player == null) return false;
            if (h.isItem) return world.countItem(ctx.player, h.name) >= h.amount;
            Value v = engine.vars.getPlayer(ctx.player.name(), h.name);
            return v.isNumber() && v.num >= h.amount;
        }
        if (c instanceof IsHoldingCond h) {
            return ctx.player != null && world.isHolding(ctx.player, h.item);
        }
        if (c instanceof InDimensionCond d) {
            return ctx.player != null && world.dimensionOf(ctx.player).equals(d.dim);
        }
        if (c instanceof InRegionCond r) {
            return ctx.player != null && world.inRegion(r.region, world.playerLocation(ctx.player));
        }
        if (c instanceof InWorldCond wc) {
            return ctx.player != null && world.playerWorld(ctx.player).equals(wc.world);
        }
        if (c instanceof InBiomeCond b) {
            return ctx.player != null && world.biomeAt(world.playerLocation(ctx.player)).equals(b.biome);
        }
        if (c instanceof TouchCond t) {
            return ctx.player != null && world.blockAt(world.playerLocation(ctx.player)).equals(t.block);
        }
        if (c instanceof WalkCond w) {
            if (ctx.player == null) return false;
            Vec3 loc = world.playerLocation(ctx.player);
            return world.blockAt(new Vec3(loc.x(), loc.y() - 1, loc.z())).equals(w.block);
        }
        if (c instanceof TimeCond t) {
            switch (t.daypart) {
                case "night": case "nighttime": return world.isNight();
                case "day": case "daytime": return !world.isNight();
                default: return true; // other dayparts need world time; treated as true in v1 state polling
            }
        }
        if (c instanceof WeatherCond w) {
            return world.isWeather(w.weather);
        }
        if (c instanceof PermissionCond p) {
            return ctx.player != null && world.hasPermission(ctx.player, p.perm);
        }
        if (c instanceof PlayerStateCond s) {
            if (ctx.player == null) return false;
            switch (s.state) {
                case "sneaking": return world.isSneaking(ctx.player);
                case "flying": return world.isFlying(ctx.player);
                case "wet": return world.isWet(ctx.player);
                case "ground": return world.isOnGround(ctx.player);
                case "op": return world.isOp(ctx.player);
                case "frozen": return world.isFrozen(ctx.player);
                case "sprinting": return world.isSprinting(ctx.player);
                case "swimming": return world.isSwimming(ctx.player);
                case "sleeping": return world.isSleeping(ctx.player);
                case "burning": return world.isBurning(ctx.player);
                case "blocking": return world.isBlocking(ctx.player);
                default: return false;
            }
        }
        if (c instanceof InGamemodeCond gm) {
            return ctx.player != null && world.gamemode(ctx.player).equals(gm.mode);
        }
        if (c instanceof ScoreCond s) {
            if (ctx.player == null) return false;
            double v = world.score(ctx.player, s.objective);
            return compareNumbers(v, s.value, s.op);
        }
        if (c instanceof ContainsCond cc) {
            Value target = eval(cc.value, ctx);
            for (Value item : engine.vars.list(cc.list).items) {
                if (item.equalsValue(target)) return true;
            }
            return false;
        }
        if (c instanceof ChestHasCond ch) {
            return world.chestHas(ch.loc, ch.item, ch.count);
        }
        return false;
    }

    private boolean evalCmp(CmpCond cmp, RunContext ctx) {
        Value left = eval(cmp.left, ctx);
        Value right = eval(cmp.right, ctx);

        if (left.isNumber() && right.isNumber()) {
            return compareNumbers(left.num, right.num, cmp.op);
        }
        if (left.isText() && right.isText()) {
            if (cmp.op.equals("==")) return left.text.equals(right.text);
            if (cmp.op.equals("!=")) return !left.text.equals(right.text);
            return left.compareTo(right) == 0; // text ordering unsupported; treat as equality
        }
        if ((left.isTruth() || left.isNumber()) && (right.isTruth() || right.isNumber())) {
            boolean a = left.isTruth() ? left.truth : left.num != 0;
            boolean b = right.isTruth() ? right.truth : right.num != 0;
            if (cmp.op.equals("==")) return a == b;
            if (cmp.op.equals("!=")) return a != b;
            return false;
        }
        // numbers vs text: try to read the text as a number
        if (left.isNumber() && right.isText()) {
            try { return compareNumbers(left.num, Double.parseDouble(right.text.trim()), cmp.op); }
            catch (NumberFormatException ex) { return false; }
        }
        if (left.isText() && right.isNumber()) {
            try { return compareNumbers(Double.parseDouble(left.text.trim()), right.num, cmp.op); }
            catch (NumberFormatException ex) { return false; }
        }
        return false;
    }

    private static boolean compareNumbers(double a, double b, String op) {
        switch (op) {
            case ">=": return a >= b;
            case "<=": return a <= b;
            case ">": return a > b;
            case "<": return a < b;
            case "!=": return a != b;
            default: return a == b;
        }
    }
}
