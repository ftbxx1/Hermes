package dev.hermes.core;

import dev.hermes.core.Ast.*;
import dev.hermes.core.Lexer.Token;
import dev.hermes.core.Lexer.Type;

import java.util.ArrayList;
import java.util.List;

import static dev.hermes.core.WorldAPI.Vec3;

/**
 * Recursive-descent parser for the Hermes language. Turns tokens into an AST.
 *
 * <p>Hermes sentences are matched greedily word by word; when a sentence
 * doesn't fit the grammar, the parser says which word confused it and how to
 * fix the line.
 */
public final class Parser {

    /** Words Hermes understands as actions. Used to suggest fixes for typos. */
    static final String[] KNOWN_VERBS = {
        "give", "remove", "set", "add", "tell", "announce", "warn", "welcome", "show",
        "kill", "damage", "heal", "feed", "teleport", "spawn", "play", "write", "open", "close",
        "press", "pull", "power", "unpower", "put", "take", "make", "win", "fire", "clear",
        "create", "stop", "repeat", "loop", "if", "when", "every", "action", "region", "mark",
        "kick", "launch", "title", "actionbar", "lightning", "explode", "delete", "particles",
        "while", "wait", "freeze", "unfreeze", "randomly", "push", "throw", "drop",
        "return", "function",
    };

    private final List<Token> toks;
    private final String fileName;
    private int pos;
    private int blockDepth;
    /** How many action/function definitions are open (allows 'return' only inside them). */
    private int defDepth;

    public Parser(List<Token> toks, String fileName) {
        this.toks = toks;
        this.fileName = fileName;
    }

    public static Script parseScript(String src, String fileName) {
        return new Parser(Lexer.tokenize(src), fileName).parse();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Token cur() { return toks.get(pos); }

    private Token peek(int k) {
        int i = pos + k;
        return i < toks.size() ? toks.get(i) : toks.get(toks.size() - 1);
    }

    private boolean at(String w) {
        Token t = cur();
        return t.type == Type.WORD && t.text.equals(w);
    }

    private boolean eat(String w) {
        if (at(w)) { pos++; return true; }
        return false;
    }

    private void expect(String w, String suggestion) {
        if (!eat(w)) {
            throw err("I expected '" + w + "' here, but found " + cur().describe() + ".",
                    suggestion);
        }
    }

    private void expect(String w) {
        if (!eat(w)) {
            throw err("I expected '" + w + "' here, but found " + cur().describe() + ".");
        }
    }

    private Token need(Type type, String what) {
        if (cur().type != type) {
            throw err("I expected " + what + " here, but found " + cur().describe() + ".");
        }
        return toks.get(pos++);
    }

    private String word(String what) {
        return need(Type.WORD, what).text;
    }

    private double number(String what) {
        return need(Type.NUMBER, what).num;
    }

    private String string(String what) {
        return need(Type.STRING, what).text;
    }

    private void newline() {
        while (cur().type == Type.NEWLINE) pos++;
    }

    private boolean atEndOfLine() {
        Type t = cur().type;
        return t == Type.NEWLINE || t == Type.DEDENT || t == Type.EOF;
    }

    /** Consumes the end of the line, complaining if extra words remain. */
    private void endStatement() {
        if (!atEndOfLine()) {
            throw err("I didn't understand everything on this line. The part '" + cur().text
                    + "' confused me.");
        }
        newline();
    }

    /** Eats "the", "a" or "an" where a noun follows. */
    private void eatNoise() {
        if (at("the") || at("a") || at("an") || at("any")) pos++;
    }

    private VerseError err(String message) {
        return err(message, null);
    }

    private VerseError err(String message, String suggestion) {
        return new VerseError(cur().line, message, suggestion, sourceLine(cur().line));
    }

    private String sourceLine(int line) {
        // The parser doesn't keep the source text; the loader fills this in.
        return null;
    }

    // ------------------------------------------------------------------
    // script
    // ------------------------------------------------------------------

    public Script parse() {
        newline();
        String name = null;
        if (at("script")) {
            pos++;
            name = string("the script name");
            endStatement();
        }
        List<Stmt> body = new ArrayList<>();
        while (cur().type != Type.EOF) {
            if (cur().type == Type.INDENT) {
                throw new VerseError(cur().line,
                        "This indented block doesn't belong to anything. Give it a 'when' header first.",
                        "when player joins\n    tell player \"Hello!\"",
                        sourceLine(cur().line));
            }
            body.add(statement());
        }
        return new Script(name, body);
    }

    // ------------------------------------------------------------------
    // statements
    // ------------------------------------------------------------------

    private Stmt statement() {
        if (atEndOfLine()) {
            // blank line inside a block
            newline();
            return statement();
        }
        if (cur().type == Type.INDENT) {
            throw new VerseError(cur().line,
                    "This indented block doesn't belong to anything. Give it a 'when' header first.",
                    null, sourceLine(cur().line));
        }
        if (cur().type != Type.WORD) {
            throw err("I don't understand this line. It should start with a word like 'give' or 'tell'.");
        }

        String w = cur().text;
        int line = cur().line;

        switch (w) {
            case "when": {
                if (blockDepth > 0) {
                    throw err("A 'when' can only appear at the top of the script, not inside another block.",
                            "when player joins\n    tell player \"Hello\"");
                }
                return whenBlock();
            }
            case "every": {
                if (blockDepth > 0) {
                    throw err("'every' can only appear at the top of the script.");
                }
                return everyBlock();
            }
            case "action": {
                if (blockDepth > 0) {
                    throw err("'action' can only appear at the top of the script.");
                }
                return actionDef();
            }
            case "function": {
                if (blockDepth > 0) {
                    throw err("'function' can only appear at the top of the script.");
                }
                return functionDef();
            }
            case "return": return returnStmt();
            case "command": {
                if (blockDepth > 0) {
                    throw err("'command' can only appear at the top of the script.");
                }
                return commandDef();
            }
            case "region": {
                if (blockDepth > 0) throw err("'region' can only appear at the top of the script.");
                return regionDef();
            }
            case "mark": {
                if (blockDepth > 0) throw err("'mark' can only appear at the top of the script.");
                return markDef();
            }
            case "gui": {
                if (blockDepth > 0) throw err("'gui' can only appear at the top of the script.");
                return guiDef();
            }
            case "create": return createStmt();
            case "if": return ifStmt();
            case "while": return whileStmt();
            case "wait": return waitStmt();
            case "repeat": return repeatStmt();
            case "loop": return loopStmt();
            case "stop": pos++; endStatement(); return new StopStmt(line);
            case "else": throw err("An 'else' must come right after an 'if' block.");
            case "fire": return fireStmt();
            case "win": {
                pos++;
                eatNoise();
                if (!eat("game")) throw err("I expected 'win the game'.");
                endStatement();
                return new WinStmt(line);
            }
            case "clear": {
                pos++;
                if (at("player") && peek(1).type == Type.POSSESSIVE) {
                    if (peek(2).type == Type.WORD && peek(2).text.equals("bossbar")) {
                        pos += 3;
                        endStatement();
                        return new ClearBossbarStmt(line);
                    }
                    pos += 2;
                    expect("inventory", "clear player's inventory");
                    endStatement();
                    return new ClearInventoryStmt(line);
                }
                if (!eat("list")) throw err("I can clear a list: 'clear list \"quests\"', or an inventory: 'clear player's inventory'.");
                String name = string("the list name");
                endStatement();
                return new ListClearStmt(line, name);
            }
            case "delete": {
                pos++;
                if (eat("world")) {
                    String name = string("the world name");
                    endStatement();
                    return new DeleteWorldStmt(line, name);
                }
                if (!eat("list")) throw err("I can delete a list: 'delete list \"quests\"', or a world: 'delete world \"arena\"'.");
                String name = string("the list name");
                endStatement();
                return new ListDeleteStmt(line, name);
            }
            default:
                return actionOrCall(line);
        }
    }

    private Stmt actionOrCall(int line) {
        String w = cur().text;
        switch (w) {
            case "give": return giveStmt();
            case "remove": return removeStmt();
            case "set": return setStmt();
            case "add": return addStmt();
            case "push":
            case "throw": return pushStmt();
            case "drop": return dropStmt();
            case "tell": {
                pos++;
                TargetRef t = targetRef();
                ValueExpr text = value("some text to say");
                endStatement();
                return new TellStmt(line, t, text);
            }
            case "announce": {
                pos++;
                ValueExpr text = value("some text to announce");
                endStatement();
                return new AnnounceStmt(line, text);
            }
            case "warn": {
                pos++;
                TargetRef t = targetRef();
                ValueExpr text = value("the warning text");
                endStatement();
                return new WarnStmt(line, t, text);
            }
            case "welcome": {
                pos++;
                TargetRef t = targetRef();
                expect("with", "welcome player with \"Hello!\"");
                ValueExpr text = value("the welcome text");
                endStatement();
                return new WelcomeStmt(line, t, text);
            }
            case "show": {
                pos++;
                VarTarget t = varTarget("some variable to show");
                endStatement();
                return new ShowStmt(line, t);
            }
            case "kill": {
                pos++;
                TargetRef t = targetRef();
                endStatement();
                return new KillStmt(line, t);
            }
            case "damage": {
                pos++;
                TargetRef t = targetRef();
                expect("by", "damage player by 10");
                double n = number("a number");
                endStatement();
                return new DamageStmt(line, t, n);
            }
            case "heal": {
                pos++;
                TargetRef t = targetRef();
                expect("by", "heal player by 5");
                double n = number("a number");
                endStatement();
                return new HealStmt(line, t, n);
            }
            case "feed": {
                pos++;
                TargetRef t = targetRef();
                double amount = 10;
                if (eat("by")) {
                    eatNoise();
                    amount = number("a number");
                }
                endStatement();
                return new FeedStmt(line, t, amount);
            }
            case "kick": {
                pos++;
                TargetRef t = targetRef();
                String reason = null;
                if (eat("because")) {
                    reason = string("the reason");
                }
                endStatement();
                return new KickStmt(line, t, reason);
            }
            case "launch": {
                pos++;
                if (at("firework")) {
                    pos++;
                    expect("at", "launch firework at player");
                    LocRef where = where();
                    endStatement();
                    return new FireworkStmt(line, where);
                }
                TargetRef t = targetRef();
                expect("by", "launch player by 5");
                double n = number("a number");
                endStatement();
                return new LaunchStmt(line, t, n);
            }
            case "title": {
                pos++;
                TargetRef t = targetRef();
                ValueExpr title = value("the title text");
                ValueExpr subtitle = null;
                if (eat("with")) {
                    eatNoise();
                    expect("subtitle", "title player \"Big\" with subtitle \"Small\"");
                    subtitle = value("the subtitle text");
                }
                endStatement();
                return new TitleStmt(line, t, title, subtitle);
            }
            case "actionbar": {
                pos++;
                TargetRef t = targetRef();
                ValueExpr text = value("the actionbar text");
                endStatement();
                return new ActionbarStmt(line, t, text);
            }
            case "lightning": {
                pos++;
                expect("at", "lightning at player");
                LocRef where = where();
                endStatement();
                return new LightningStmt(line, where);
            }
            case "explode": {
                pos++;
                expect("at", "explode at player");
                LocRef where = where();
                double power = 4;
                if (eat("with")) {
                    eatNoise();
                    expect("power", "explode at player with power 2");
                    power = number("a number");
                }
                endStatement();
                return new ExplodeStmt(line, where, power);
            }
            case "teleport": {
                pos++;
                TargetRef t = targetRef();
                if (eat("randomly")) {
                    expect("within", "teleport player randomly within 100");
                    double radius = number("a number");
                    if (radius <= 0) throw err("A random teleport needs a positive distance.");
                    endStatement();
                    return new TeleportRandomStmt(line, t, radius);
                }
                expect("to", "teleport player to 100 64 200");
                LocRef where = where();
                endStatement();
                return new TeleportStmt(line, t, where);
            }
            case "spawn": {
                pos++;
                if (eat("particles")) {
                    eatNoise();
                    String particle = null;
                    if (cur().type == Type.STRING) {
                        String raw = string("the particle name");
                        particle = Dictionary.findParticle(raw);
                        if (particle == null) {
                            throw new VerseError(line, "I don't know the particle '" + raw + "'.",
                                    null, null);
                        }
                    } else {
                        particle = greedy(Dictionary::findParticle, 2, null);
                        if (particle == null) {
                            String sug = Dictionary.suggest(cur().text, Dictionary.particleNames());
                            throw new VerseError(line, "I don't know the particle '" + cur().text + "'.",
                                    sug != null ? "Did you mean: '" + sug + "' ?" : null, null);
                        }
                    }
                    LocRef pWhere = spawnWhere();
                    int count = 30;
                    double size = 1.0;
                    while (eat("with")) {
                        if (eat("count")) {
                            count = (int) number("a number");
                            if (count < 1) throw err("You need at least one particle.");
                        } else if (eat("size")) {
                            size = number("a number");
                            if (size <= 0) throw err("A particle size must be more than zero.");
                        } else {
                            throw err("After 'with' I expect 'count' or 'size': spawn particles \"heart\" at player with count 50 size 2");
                        }
                    }
                    endStatement();
                    return new ParticleStmt(line, particle, pWhere, count, size);
                }
                int count = 1;
                if (cur().type == Type.NUMBER) {
                    count = (int) number("a number");
                    if (count < 1) throw err("You can't spawn less than one mob.");
                }
                boolean plural = cur().type == Type.WORD && cur().text.endsWith("s")
                        && !cur().text.endsWith("ss");
                eatNoise();
                String mob = mobNameOrError();
                eatNoise();
                LocRef where = spawnWhere();
                String customName = null;
                if (eat("named")) customName = string("the mob's name");
                endStatement();
                if (plural && count == 1) count = 3; // "spawn zombies" brings three
                return new SpawnMobStmt(line, mob, count, where, customName);
            }
            case "play": {
                pos++;
                expect("sound", "play sound \"level_up\" near player");
                String sound = soundNameOrError();
                LocRef where = soundWhere();
                endStatement();
                return new PlaySoundStmt(line, sound, where);
            }
            case "write": {
                pos++;
                expect("sign", "write sign at 10 64 20 with \"Line one\"");
                expect("at");
                Vec3 v = coords();
                expect("with", "write sign at 10 64 20 with \"Line one\"");
                List<String> lines = new ArrayList<>();
                lines.add(string("some sign text"));
                while (eat("and")) {
                    if (lines.size() >= 4) throw err("A sign only has 4 lines.");
                    lines.add(string("some sign text"));
                }
                endStatement();
                return new SignStmt(line, v, lines);
            }
            case "open": {
                pos++;
                if (eat("gui")) {
                    String gui = string("the gui name");
                    expect("to", "open gui \"Shop\" to player");
                    TargetRef t = targetRef();
                    endStatement();
                    return new OpenGuiStmt(line, t, gui);
                }
                if (!eat("door")) throw err("I can open a door: 'open door near player', or a gui: 'open gui \"Shop\" to player'.");
                expect("near");
                expect("player", "open door near player");
                endStatement();
                return new NearbyOpStmt(line, "open door");
            }
            case "close": {
                pos++;
                if (!eat("door")) throw err("I can close a door: 'close door near player'.");
                expect("near");
                expect("player", "close door near player");
                endStatement();
                return new NearbyOpStmt(line, "close door");
            }
            case "press": {
                pos++;
                if (!eat("button")) throw err("I can press a button: 'press button near player'.");
                expect("near");
                expect("player", "press button near player");
                endStatement();
                return new NearbyOpStmt(line, "press button");
            }
            case "pull": {
                pos++;
                if (!eat("lever")) throw err("I can pull a lever: 'pull lever near player'.");
                expect("near");
                expect("player", "pull lever near player");
                endStatement();
                return new NearbyOpStmt(line, "pull lever");
            }
            case "power": {
                pos++;
                expect("block", "power block at 10 64 20");
                expect("at");
                Vec3 v = coords();
                endStatement();
                return new PowerStmt(line, true, v);
            }
            case "unpower": {
                pos++;
                expect("block", "unpower block at 10 64 20");
                expect("at");
                Vec3 v = coords();
                endStatement();
                return new PowerStmt(line, false, v);
            }
            case "put": {
                pos++;
                if (at("player")) { // put player in team "x"
                    pos++;
                    expect("in", "put player in team \"red\"");
                    eatNoise();
                    if (!eat("team")) throw err("I expected 'put player in team \"name\"'.");
                    String team = string("the team name");
                    endStatement();
                    return new TeamStmt(line, "join", TargetRef.PLAYER, team);
                }
                // put <n> <item> in chest at X Y Z
                double n = number("a number");
                String item = itemNameOrError();
                expect("in");
                expect("chest", "put 5 diamonds in chest at 10 64 20");
                expect("at");
                Vec3 v = coords();
                endStatement();
                return new ChestStmt(line, "add", v, item, (int) n);
            }
            case "take": {
                pos++;
                if (at("from")) { // handled by removeStmt normally; safe fallback
                    throw err("To take things, write: remove 5 diamonds from player");
                }
                double n = number("a number");
                String item = itemNameOrError();
                expect("from");
                expect("chest", "take 5 diamonds from chest at 10 64 20");
                expect("at");
                Vec3 v = coords();
                endStatement();
                return new ChestStmt(line, "take", v, item, (int) n);
            }
            case "make": {
                pos++;
                TargetRef t = targetRef();
                if (eat("run")) {
                    expect("command", "make player run command \"/spawn\"");
                    String command = string("the command");
                    endStatement();
                    return new RunCommandStmt(line, t, command);
                }
                if (eat("swing")) {
                    eatNoise();
                    eat("their"); eat("hand"); eat("arm");
                    endStatement();
                    return new SwingStmt(line, t);
                }
                if (eat("look")) {
                    expect("at", "make player look at 10 64 20");
                    LocRef where = where();
                    endStatement();
                    return new LookStmt(line, t, where);
                }
                String adj = effectNameOrError("an effect like 'stronger'");
                endStatement();
                return new MakeEffectStmt(line, t, adj);
            }
            case "freeze": {
                pos++;
                TargetRef t = targetRef();
                endStatement();
                return new FreezeStmt(line, t, true);
            }
            case "unfreeze": {
                pos++;
                TargetRef t = targetRef();
                endStatement();
                return new FreezeStmt(line, t, false);
            }
            default:
                // maybe a user-defined action call
                return actionCall(line);
        }
    }

    private Stmt actionCall(int line) {
        String name = cur().text;
        pos++;
        List<ValueExpr> args = new ArrayList<>();
        eatNoise();
        while (!atEndOfLine() && !at("the") && !at("player") && !at("mob")
                && !at("a") && !at("an")) {
            args.add(value("an argument for " + name));
            eatNoise();
        }
        boolean passPlayer = false;
        if (at("the") || at("a") || at("an")) {
            pos++;
            if (eat("player")) passPlayer = true;
            else pos--; // put back the noise word
        } else if (eat("player")) {
            passPlayer = true;
        }
        if (!atEndOfLine()) {
            String closest = Dictionary.suggest(name, KNOWN_VERBS);
            throw new VerseError(line,
                    "I don't know how to '" + name + "'.",
                    closest != null ? "Did you mean: '" + closest + "' ?" : null,
                    sourceLine(line));
        }
        endStatement();
        return new ActionCallStmt(line, name, args, passPlayer);
    }

    // ------------------------------------------------------------------
    // wordy statements
    // ------------------------------------------------------------------

    private Stmt giveStmt() {
        int line = cur().line;
        pos++;
        TargetRef t;
        if (eat("all")) {
            if (!eat("players") && !eat("player")) {
                throw err("give all players 1 diamond");
            }
            t = TargetRef.ALL_PLAYERS;
        } else {
            t = targetRef();
        }

        if (cur().type == Type.NUMBER) {
            double n = number("a number");
            if ((at("xp") || at("experience"))) { pos++; endStatement(); return new GiveXpStmt(line, t, n); }
            if ((at("level") || at("levels"))) { pos++; endStatement(); return new GiveLevelsStmt(line, t, n); }
            WorldAPI.ItemSpec spec = itemSpec((int) n);
            endStatement();
            return new GiveItemStmt(line, t, spec);
        }

        if (at("permission")) {
            pos++;
            String perm = string("the permission name");
            endStatement();
            return new PermissionStmt(line, true, t, perm);
        }

        if (at("book")) {
            pos++;
            String book = string("the book name");
            endStatement();
            return new GiveBookStmt(line, t, book);
        }

        eatNoise();
        String effect = greedy(Dictionary::findEffect, 3, null);
        if (effect != null) {
            int seconds = 10;
            if (eat("for")) {
                double n = number("a number");
                seconds = (int) durationSeconds(n);
                if (seconds <= 0) throw err("An effect needs a real duration.");
            }
            endStatement();
            return new EffectStmt(line, t, effect, seconds);
        }

        WorldAPI.ItemSpec spec = itemSpec(1);
        endStatement();
        return new GiveItemStmt(line, t, spec);
    }

    private Stmt pushStmt() {
        int line = cur().line;
        pos++;
        TargetRef t = targetRef();
        String direction;
        if (at("up") || at("down") || at("forwards") || at("backwards") || at("left") || at("right")) {
            direction = cur().text;
            pos++;
        } else {
            throw err("I expected 'up', 'down', 'forwards', 'backwards', 'left' or 'right' here.",
                    "push player up by 3");
        }
        expect("by", "push player up by 3");
        double strength = number("a number");
        endStatement();
        return new PushStmt(line, t, direction, strength);
    }

    private Stmt dropStmt() {
        int line = cur().line;
        pos++;
        int count = 1;
        if (cur().type == Type.NUMBER) count = (int) number("a number");
        WorldAPI.ItemSpec spec = itemSpec(count, new String[]{"at"});
        expect("at", "drop 5 diamonds at player");
        LocRef where = where();
        endStatement();
        return new DropStmt(line, spec, where);
    }

    /** "5 diamond named \"X\" with lore \"...\" with enchant sharpness 5" after the count. */
    private WorldAPI.ItemSpec itemSpec(int count) {
        return itemSpec(count, (String[]) null);
    }

    /** Same, but stops the item spec when it meets one of the given words (e.g. "at"). */
    private WorldAPI.ItemSpec itemSpec(int count, String[] stopAt) {
        eatNoise();
        int itemStart = pos;
        String item = itemNameOrError();
        String name = null;
        List<String> lore = new ArrayList<>();
        List<WorldAPI.EnchantSpec> enchants = new ArrayList<>();
        while (true) {
            if (at("named")) {
                pos++;
                name = string("the display name");
            } else if (at("with")) {
                pos++;
                if (eat("lore")) {
                    lore.add(string("a lore line"));
                } else if (eat("enchant")) {
                    String ench = enchantNameOrError();
                    int lvl = 1;
                    if (cur().type == Type.NUMBER) lvl = (int) number("an enchant level");
                    enchants.add(new WorldAPI.EnchantSpec(ench, lvl));
                } else {
                    throw err("After 'with' I expect 'lore' or 'enchant'.");
                }
            } else {
                break;
            }
        }
        boolean stops = stopAt != null && cur().type == Type.WORD
                && java.util.Arrays.asList(stopAt).contains(cur().text);
        if (!atEndOfLine() && !stops) {
            StringBuilder full = new StringBuilder();
            for (int i = itemStart; i < toks.size() && toks.get(i).type == Type.WORD; i++) {
                if (full.length() > 0) full.append(' ');
                full.append(toks.get(i).text);
            }
            String missing = full.toString();
            String closest = Dictionary.suggest(missing, Dictionary.itemNames());
            throw new VerseError(cur().line,
                    "I don't know the item '" + missing + "'.",
                    closest != null
                            ? "Did you mean: '" + closest + "' ?"
                            : "Did you mean: add " + count + " to player's " + missing + " ?",
                    sourceLine(cur().line));
        }
        return new WorldAPI.ItemSpec(item, count, name, lore, enchants);
    }

    private String enchantNameOrError() {
        String ench = greedy(Dictionary::findEnchant, 3, null);
        if (ench == null) {
            String missing = cur().text;
            String sug = Dictionary.suggest(missing, Dictionary.enchantNames());
            throw new VerseError(cur().line, "I don't know the enchantment '" + missing + "'.",
                    sug != null ? "Did you mean: '" + sug + "' ?" : null, sourceLine(cur().line));
        }
        return ench;
    }

    private Stmt removeStmt() {
        int line = cur().line;
        pos++;

        if (cur().type == Type.STRING) {
            ValueExpr v = value("some text to remove");
            expect("from", "remove \"Defeat the dragon\" from list \"quests\"");
            if (eat("list")) {
                String list = string("the list name");
                endStatement();
                return new ListRemoveStmt(line, list, v);
            }
            throw err("I can only remove text from a list: remove \"x\" from list \"quests\"");
        }

        if (cur().type == Type.NUMBER) {
            double n = number("a number");
            if (eat("from")) {
                VarTarget t = varTarget("a variable to remove from");
                endStatement();
                return new RemoveStmt(line, t, new NumExpr(line, n));
            }
            eatNoise();
            String item = greedy(Dictionary::findItem, 4, "I don't know the item ");
            if (item == null) throw err("I don't know the item '" + cur().text + "'.");
            expect("from", "remove 5 diamonds from player");
            TargetRef t = targetRef();
            endStatement();
            return new TakeItemStmt(line, t, item, (int) n);
        }

        if (at("permission")) {
            pos++;
            String perm = string("the permission name");
            expect("from", "remove permission \"vip\" from player");
            TargetRef t = targetRef();
            endStatement();
            return new PermissionStmt(line, false, t, perm);
        }

        eatNoise();
        String effect = greedy(Dictionary::findEffect, 3, null);
        if (effect != null) {
            expect("from", "remove speed from player");
            TargetRef t = targetRef();
            endStatement();
            return new RemoveEffectStmt(line, t, effect);
        }

        throw new VerseError(line, "I don't know what to remove.",
                "remove 5 diamonds from player\nor: remove speed from player", sourceLine(line));
    }

    private Stmt setStmt() {
        int line = cur().line;
        pos++;

        if (eat("weather")) {
            if (at("in")) {
                pos++;
                expect("world", "set weather in world \"arena\" to rain");
                String world = string("the world name");
                expect("to", "set weather in world \"arena\" to rain");
                String w = weatherNameOrError();
                endStatement();
                return new SetWorldWeatherStmt(line, world, w);
            }
            expect("to", "set weather to rain");
            String w = weatherNameOrError();
            endStatement();
            return new SetWeatherStmt(line, w);
        }
        if (eat("time")) {
            if (at("in")) {
                pos++;
                expect("world", "set time in world \"arena\" to night");
                String world = string("the world name");
                expect("to", "set time in world \"arena\" to night");
                String d = daypartNameOrError();
                endStatement();
                return new SetWorldTimeStmt(line, world, d);
            }
            expect("to", "set time to noon");
            String d = daypartNameOrError();
            endStatement();
            return new SetTimeStmt(line, d);
        }
        if (eat("config")) {
            expect("value", "set config value \"prefix\" in file \"config.yml\" to \"[Hermes]\"");
            String key = string("the config key");
            expect("in", "set config value \"prefix\" in file \"config.yml\" to \"[Hermes]\"");
            expect("file", "set config value \"prefix\" in file \"config.yml\" to \"[Hermes]\"");
            String file = string("the config file name");
            expect("to", "set config value \"prefix\" in file \"config.yml\" to \"[Hermes]\"");
            ValueExpr v = value("a value");
            endStatement();
            return new ConfigSetStmt(line, file, key, v);
        }
        if (eat("slot")) {
            int slot = (int) number("a slot number");
            expect("of", "set slot 3 of gui \"Shop\" to 1 diamond");
            expect("gui", "set slot 3 of gui \"Shop\" to 1 diamond");
            String gui = string("the gui name");
            expect("to", "set slot 3 of gui \"Shop\" to 1 diamond");
            WorldAPI.ItemSpec spec = itemSpec(1);
            endStatement();
            return new SetGuiSlotStmt(line, gui, slot, spec);
        }
        if (eat("block")) {
            expect("at", "set block at 10 64 20 to diamond block");
            Vec3 v = coords();
            expect("to", "set block at 10 64 20 to diamond block");
            eatNoise();
            String b = greedy(Dictionary::findItem, 4, "I don't know the block ");
            if (b == null) throw err("I don't know the block '" + cur().text + "'.");
            endStatement();
            return new SetBlockStmt(line, v, b);
        }
        if (at("player") && peek(1).type == Type.POSSESSIVE && peek(2).type == Type.WORD) {
            String prop = peek(2).text;
            if (prop.equals("bossbar")) {
                pos += 3;
                expect("to", "set player's bossbar to \"Quest\" with progress 50");
                ValueExpr title = value("the bossbar title");
                ValueExpr progress = null;
                if (eat("with")) {
                    eatNoise();
                    expect("progress", "set player's bossbar to \"Quest\" with progress 50");
                    progress = value("the bossbar progress");
                }
                endStatement();
                return new SetBossbarStmt(line, title, progress);
            }
            if (prop.equals("respawn")) {
                pos += 3;
                expect("point", "set player's respawn point to 10 64 20");
                expect("to", "set player's respawn point to 10 64 20");
                LocRef where = where();
                endStatement();
                return new SetRespawnStmt(line, where);
            }
            if (prop.equals("speed")) {
                pos += 3;
                expect("to", "set player's speed to 0.5");
                double speed = number("a number");
                if (speed < 0 || speed > 1) throw err("Speed must be between 0 and 1.");
                endStatement();
                return new SetSpeedStmt(line, "walk", speed);
            }
            if (prop.equals("fly")) {
                pos += 3;
                expect("speed", "set player's fly speed to 0.3");
                expect("to", "set player's fly speed to 0.3");
                double speed = number("a number");
                if (speed < 0 || speed > 1) throw err("Speed must be between 0 and 1.");
                endStatement();
                return new SetSpeedStmt(line, "fly", speed);
            }
            if (prop.equals("helmet") || prop.equals("chestplate") || prop.equals("leggings")
                    || prop.equals("boots")) {
                pos += 3;
                expect("to", "set player's helmet to iron helmet");
                WorldAPI.ItemSpec spec = itemSpec(1);
                endStatement();
                return new SetEquipmentStmt(line, prop, spec);
            }
            if (prop.equals("health") || prop.equals("hunger") || prop.equals("food")
                    || prop.equals("xp") || prop.equals("experience") || prop.equals("level")) {
                pos += 3;
                expect("to", "set player's health to 10");
                ValueExpr v = value("a number");
                endStatement();
                String stat = prop.equals("food") ? "hunger" : prop.equals("experience") ? "xp" : prop;
                return new SetPlayerStatStmt(line, stat, v);
            }
        }
        if (at("player") && peek(1).type == Type.POSSESSIVE && peek(2).type == Type.WORD
                && peek(2).text.equals("gamemode")) {
            pos += 3;
            expect("to", "set player's gamemode to creative");
            eatNoise();
            String mode = greedy(Dictionary::findGamemode, 2, "I don't know the gamemode ");
            if (mode == null) {
                throw err("I don't know the gamemode '" + cur().text + "'. Try: survival, creative, adventure or spectator.");
            }
            endStatement();
            return new SetGamemodeStmt(line, mode);
        }

        VarTarget t = varTarget("somewhere to set: player's coins, world's flag, ...");
        expect("to", "set player's coins to 100");
        ValueExpr v = value("a value");
        endStatement();
        return new SetStmt(line, t, v);
    }

    private Stmt addStmt() {
        int line = cur().line;
        pos++;
        ValueExpr v = value("a number or some text");
        expect("to", "add 5 to player's coins");
        VarTarget t = varTarget("somewhere to add to");
        endStatement();
        if (t.kind.equals("list")) {
            return new ListAddStmt(line, t.name, v);
        }
        return new AddStmt(line, t, v);
    }

    private Stmt createStmt() {
        int line = cur().line;
        pos++;
        if (eat("team")) {
            String name = string("the team name");
            endStatement();
            return new TeamCreate(line, name);
        }
        if (eat("list")) {
            String name = string("the list name");
            endStatement();
            return new ListCreate(line, name);
        }
        if (eat("world")) {
            String name = string("the world name");
            endStatement();
            return new CreateWorldStmt(line, name);
        }
        if (eat("database")) {
            String name = string("the database name");
            endStatement();
            return new CreateDatabaseStmt(line, name);
        }
        if (eat("book")) {
            String name = string("the book name");
            String title = name;
            String author = "Hermes";
            List<String> pages = new ArrayList<>();
            while (true) {
                if (eat("with")) {
                    if (eat("title")) {
                        title = string("the book title");
                    } else if (eat("author")) {
                        author = string("the author name");
                    } else if (eat("page")) {
                        pages.add(string("a page of text"));
                    } else {
                        throw err("After 'with' I expect 'title', 'author' or 'page'.");
                    }
                } else {
                    break;
                }
            }
            endStatement();
            return new BookCreate(line, name, new WorldAPI.BookDef(title, author, pages));
        }
        throw err("I can create a team, a list, a world, a database or a book: 'create team \"red\"', 'create world \"arena\"', 'create database \"data\"', 'create book \"Guide\" with page \"...\"'");
    }

    private Stmt fireStmt() {        int line = cur().line;
        pos++;
        eat("custom");
        expect("event", "fire event \"boss_killed\"");
        String name = string("the event name");
        endStatement();
        return new FireEventStmt(line, name);
    }

    private Stmt regionDef() {
        int line = cur().line;
        pos++;
        String name = nameOrString("a region name");
        expect("from", "region \"Castle\" from 10 64 10 to 40 80 40");
        Vec3 a = coords();
        expect("to", "region \"Castle\" from 10 64 10 to 40 80 40");
        Vec3 b = coords();
        endStatement();
        return new RegionDef(line, name, a, b);
    }

    private Stmt markDef() {
        int line = cur().line;
        pos++;
        String name = nameOrString("a place name");
        expect("at", "mark home at 100 64 200");
        Vec3 v = coords();
        endStatement();
        return new MarkDef(line, name, v);
    }

    private Stmt guiDef() {
        int line = cur().line;
        pos++;
        String name = nameOrString("a gui name");
        expect("with", "gui \"Shop\" with 9 slots");
        int size = (int) number("the number of slots");
        eat("slots");
        if (size < 1 || size > 54) throw err("A gui can have between 1 and 54 slots.");
        List<SlotStmt> slots = new ArrayList<>();
        if (cur().type != Type.NEWLINE) {
            throw err("A gui's slots need to be listed below it.",
                    "gui \"Shop\" with 9 slots\n    slot 3 has 1 diamond");
        }
        newline();
        if (cur().type != Type.INDENT) {
            throw err("I expected an indented list of slots here.",
                    "gui \"Shop\" with 9 slots\n    slot 3 has 1 diamond");
        }
        pos++;
        blockDepth++;
        try {
            while (cur().type != Type.DEDENT && cur().type != Type.EOF) {
                if (cur().type == Type.NEWLINE) { newline(); continue; }
                if (!at("slot")) throw err("Inside a gui I only accept lines like: slot 3 has 1 diamond.");
                slots.add(slotStmt());
            }
        } finally {
            blockDepth--;
        }
        if (cur().type == Type.DEDENT) pos++;
        return new GuiDef(line, name, slots);
    }

    private SlotStmt slotStmt() {
        int line = cur().line;
        pos++;
        int slot = (int) number("a slot number");
        expect("has", "slot 3 has 1 diamond");
        int count = 1;
        if (cur().type == Type.NUMBER) count = (int) number("a number");
        WorldAPI.ItemSpec spec = itemSpec(count);
        endStatement();
        return new SlotStmt(line, slot, spec);
    }

    private Stmt actionDef() {
        int line = cur().line;
        pos++;
        String name = word("an action name");
        eatNoise();
        List<String> params = new ArrayList<>();
        while (cur().type == Type.WORD && !at("player") && !at("mob")
                && !at("the") && !at("a") && !at("an")) {
            params.add(cur().text);
            pos++;
            eatNoise();
        }
        if (at("player") || at("mob")) pos++;
        defDepth++;
        List<Stmt> body = block();
        defDepth--;
        return new ActionDef(line, name, params, body);
    }

    /** function "tax" with argument <amount> and argument <bonus> */
    private Stmt functionDef() {
        int line = cur().line;
        pos++;
        String name = nameOrString("a function name");
        List<String> params = new ArrayList<>();
        while (at("with") || at("and")) {
            pos++;
            expect("argument", "function \"tax\" with argument <amount>");
            params.add(word("an argument name"));
        }
        defDepth++;
        List<Stmt> body = block();
        defDepth--;
        return new FunctionDef(line, name, params, body);
    }

    /** "return <value>" — hands a value back from a function (or action). */
    private Stmt returnStmt() {
        int line = cur().line;
        pos++;
        if (defDepth == 0) {
            throw err("'return' can only be used inside a function or an action.",
                    "function \"tax\" with argument <amount>\n    return player's coins times 2");
        }
        ValueExpr value = value("a value to return");
        endStatement();
        return new ReturnStmt(line, value);
    }

    /** command "/pay" with argument <amount> and argument <target> permission "vip" */
    private Stmt commandDef() {
        int line = cur().line;
        pos++;
        String name = string("a command like \"/quest\"");
        List<String> args = new ArrayList<>();
        while (at("with") || at("and")) {
            pos++;
            expect("argument", "command \"/quest\" with argument <quest>");
            args.add(word("an argument name"));
        }
        String permission = null;
        if (eat("permission")) {
            permission = cur().type == Type.STRING ? string("a permission") : word("a permission");
        }
        if (!atEndOfLine()) {
            throw err("I don't understand this after the command. Try:\n"
                    + "command \"/quest\" with argument <quest>\n"
                    + "    tell player \"Quest: ${quest}\"");
        }
        List<Stmt> body = block();
        return new CommandDef(line, name, args, permission, body);
    }

    private Stmt ifStmt() {
        int line = cur().line;
        pos++;
        Condition cond = condition();
        List<Stmt> thenBody = block();
        List<Stmt> elseBody = null;
        if (eat("else")) {
            elseBody = block();
        }
        return new IfStmt(line, cond, thenBody, elseBody);
    }

    private Stmt repeatStmt() {
        int line = cur().line;
        pos++;
        double times = number("a number");
        if (!eat("times")) throw err("I expected 'times' after the number: 'repeat 5 times'.");
        List<Stmt> body = block();
        return new RepeatStmt(line, times, body);
    }

    private Stmt whileStmt() {
        int line = cur().line;
        pos++;
        Condition cond = condition();
        List<Stmt> body = block();
        return new WhileStmt(line, cond, body);
    }

    private Stmt waitStmt() {
        int line = cur().line;
        pos++;
        if (blockDepth != 1) {
            throw err("'wait' can only be used directly inside a 'when', 'every', 'action' or 'command' block.",
                    "when player joins\n    wait 2 seconds\n    tell player \"Welcome!\"");
        }
        double n = number("a number");
        double seconds = durationSeconds(n);
        endStatement();
        return new WaitStmt(line, seconds);
    }

    private Stmt loopStmt() {
        int line = cur().line;
        pos++;
        expect("over", "loop over all players as p");
        String kind;
        String listName = null;
        double from = 0, to = 0;
        if (eat("all")) {
            if (!eat("players") && !eat("player")) {
                throw err("loop over all players as p");
            }
            kind = "players";
        } else if (at("numbers")) {
            pos++;
            expect("from", "loop over numbers from 1 to 10 as i");
            from = number("a number");
            expect("to", "loop over numbers from 1 to 10 as i");
            to = number("a number");
            kind = "numbers";
        } else if (at("player") && peek(1).type == Type.POSSESSIVE
                && peek(2).type == Type.WORD && peek(2).text.equals("inventory")) {
            pos += 3;
            kind = "inventory";
        } else {
            if (at("list")) pos++;
            listName = string("the list name");
            kind = "list";
        }
        expect("as", "loop over list \"quests\" as task");
        String itemName = word("a name for each item");
        List<Stmt> body = block();
        return new LoopStmt(line, kind, listName, from, to, itemName, body);
    }

    private Stmt whenBlock() {
        int line = cur().line;
        pos++;
        Trigger trig = trigger();
        if (at("and") || at("or")) {
            Condition combined = trig.conditions.isEmpty() ? null : trig.conditions.remove(0);
            while (at("and") || at("or")) {
                boolean isAnd = at("and");
                pos++;
                Condition next = primaryCond();
                if (combined == null) { combined = next; continue; }
                if (isAnd) {
                    if (combined instanceof AndCond a) a.parts.add(next);
                    else combined = new AndCond(new ArrayList<>(List.of(combined, next)));
                } else {
                    if (combined instanceof OrCond o) o.parts.add(next);
                    else combined = new OrCond(new ArrayList<>(List.of(combined, next)));
                }
            }
            trig.conditions.add(combined);
        }
        List<Stmt> body = block();
        return new WhenBlock(line, trig, body);
    }

    private Stmt everyBlock() {
        int line = cur().line;
        pos++;
        double n = number("a number");
        double seconds = durationSeconds(n);
        List<Stmt> body = block();
        return new EveryBlock(line, seconds, body);
    }

    // ------------------------------------------------------------------
    // blocks
    // ------------------------------------------------------------------

    private List<Stmt> block() {
        if (cur().type != Type.NEWLINE) {
            throw err("This line needs its actions indented below it.",
                    "when player joins\n    tell player \"Hello!\"");
        }
        newline();
        if (cur().type != Type.INDENT) {
            throw err("I expected an indented block here.",
                    "when player joins\n    tell player \"Hello!\"");
        }
        pos++;
        blockDepth++;
        List<Stmt> body = new ArrayList<>();
        try {
            while (cur().type != Type.DEDENT && cur().type != Type.EOF) {
                body.add(statement());
            }
        } finally {
            blockDepth--;
        }
        if (cur().type == Type.DEDENT) pos++;
        return body;
    }

    // ------------------------------------------------------------------
    // nouns
    // ------------------------------------------------------------------

    private TargetRef targetRef() {
        if (at("player")) { pos++; return TargetRef.PLAYER; }
        eatNoise();
        if (at("player")) { pos++; return TargetRef.PLAYER; }
        if (at("mob")) { pos++; return TargetRef.MOB; }
        throw new VerseError(cur().line,
                "I expected the player here, but found " + cur().describe() + ".",
                "give player 5 diamonds", sourceLine(cur().line));
    }

    private String nameOrString(String what) {
        if (cur().type == Type.STRING) return string(what);
        return word(what);
    }

    /** Three numbers that make a place: 10 64 200. */
    private Vec3 coords() {
        double x = number("an X coordinate");
        double y = number("a Y coordinate");
        double z = number("a Z coordinate");
        return new Vec3(x, y, z);
    }

    private LocRef where() {
        if (cur().type == Type.NUMBER) {
            return new LocRef("coords", coords(), null);
        }
        if (at("spawn")) { pos++; return new LocRef("spawn", null, null); }
        if (at("player")) { pos++; return new LocRef("player", null, null); }
        if (at("home")) { pos++; return new LocRef("mark", null, "home"); }
        String name = nameOrString("a place name like 'home'");
        return new LocRef("mark", null, name);
    }

    private LocRef spawnWhere() {
        if (at("near")) {
            pos++;
            expect("player", "spawn zombie near player");
            return new LocRef("player", null, null);
        }
        if (at("at")) {
            pos++;
            return new LocRef("coords", coords(), null);
        }
        if (atEndOfLine()) {
            // "spawn zombies" — near the world spawn
            return new LocRef("spawn", null, null);
        }
        throw err("I expected 'near player', 'at X Y Z' or the end of the line here.",
                "spawn zombie near player\nor: spawn zombie at 100 64 200");
    }

    private LocRef soundWhere() {
        if (at("near")) {
            pos++;
            expect("player", "play sound \"level_up\" near player");
            return new LocRef("player", null, null);
        }
        if (at("at")) {
            pos++;
            return new LocRef("coords", coords(), null);
        }
        throw err("I expected 'near player' or 'at X Y Z' here.",
                "play sound \"level_up\" near player");
    }

    private double durationSeconds(double n) {
        String unit = word("a time unit: seconds, minutes, hours or ticks");
        switch (unit) {
            case "second": case "seconds": return n;
            case "minute": case "minutes": return n * 60;
            case "hour": case "hours": return n * 3600;
            case "tick": case "ticks": return n / 20.0;
            default:
                throw new VerseError(cur().line,
                        "I don't know the time unit '" + unit + "'. I know seconds, minutes, hours and ticks.",
                        "every 10 seconds", null);
        }
    }

    // ------------------------------------------------------------------
    // dictionary lookups with greedy multi-word matching
    // ------------------------------------------------------------------

    private interface Lookup { String find(String name); }

    /** Tries the longest word sequence that the dictionary knows.
     *  Native-language phrases are translated first ("espada de diamante"
     *  becomes "diamond sword"). */
    private String greedy(Lookup lookup, int maxWords, String errorPrefix) {
        for (int len = maxWords; len >= 1; len--) {
            if (pos + len > toks.size()) continue;
            StringBuilder cur = new StringBuilder();
            StringBuilder raw = new StringBuilder();
            boolean ok = true;
            for (int k = 0; k < len; k++) {
                Token t = peek(k);
                if (t.type != Type.WORD) { ok = false; break; }
                if (k > 0) { cur.append(' '); raw.append(' '); }
                cur.append(t.text);
                raw.append(t.raw);
            }
            if (!ok) continue;
            // 1. the native phrase as typed (for translation packs)
            String viaLang = Lang.translatePhrase(raw.toString());
            String canonical = viaLang != null ? lookup.find(viaLang) : null;
            // 2. the (already translated) English words
            if (canonical == null) canonical = lookup.find(cur.toString());
            if (canonical != null) {
                pos += len;
                return canonical;
            }
        }
        return null;
    }

    /** Consumes up to maxWords whose native phrase translates to a single
     *  English word (e.g. "se une" -> "joins"). Used where one keyword is
     *  expected. */
    private String greedyWords(int maxWords, String what) {
        for (int len = maxWords; len >= 1; len--) {
            if (pos + len > toks.size()) continue;
            boolean ok = true;
            for (int k = 0; k < len; k++) {
                if (peek(k).type != Type.WORD) { ok = false; break; }
            }
            if (!ok) continue;
            StringBuilder raw = new StringBuilder();
            for (int k = 0; k < len; k++) {
                if (k > 0) raw.append(' ');
                raw.append(peek(k).raw);
            }
            String english = Lang.translatePhrase(raw.toString());
            if (english != null && !english.contains(" ")) {
                pos += len;
                return english;
            }
        }
        return null;
    }

    private String itemNameOrError() {
        String item = greedy(Dictionary::findItem, 4, null);
        if (item == null) {
            String missing = cur().text;
            String sug = Dictionary.suggest(missing, Dictionary.itemNames());
            String suggestion = sug != null ? "Did you mean: '" + sug + "' ?" : null;
            throw new VerseError(cur().line, "I don't know the item '" + missing + "'.", suggestion, sourceLine(cur().line));
        }
        return item;
    }

    private String mobNameOrError() {
        String mob = greedy(Dictionary::findMob, 3, null);
        if (mob == null) {
            String missing = cur().text;
            String sug = Dictionary.suggest(missing, Dictionary.mobNames());
            String suggestion = sug != null ? "Did you mean: '" + sug + "' ?" : null;
            throw new VerseError(cur().line, "I don't know the mob '" + missing + "'.", suggestion, sourceLine(cur().line));
        }
        return mob;
    }

    private String effectNameOrError(String what) {
        String eff = greedy(Dictionary::findEffect, 3, null);
        if (eff == null) {
            String missing = cur().text;
            String sug = Dictionary.suggest(missing, Dictionary.effectNames());
            String suggestion = sug != null ? "Did you mean: '" + sug + "' ?" : null;
            throw new VerseError(cur().line, "I don't know the effect '" + missing + "'.", suggestion, sourceLine(cur().line));
        }
        return eff;
    }

    private String soundNameOrError() {
        if (cur().type == Type.STRING) {
            String raw = string("the sound name");
            String canonical = Dictionary.findSound(raw);
            if (canonical == null) {
                throw new VerseError(cur().line, "I don't know the sound '" + raw + "'.",
                        null, null);
            }
            return canonical;
        }
        String sound = greedy(Dictionary::findSound, 2, null);
        if (sound == null) {
            String missing = cur().text;
            String sug = Dictionary.suggest(missing, Dictionary.soundNames());
            throw new VerseError(cur().line, "I don't know the sound '" + missing + "'.",
                    sug != null ? "Did you mean: '" + sug + "' ?" : null, sourceLine(cur().line));
        }
        return sound;
    }

    private String weatherNameOrError() {
        String w = greedy(Dictionary::findWeather, 2, null);
        if (w == null) throw new VerseError(cur().line, "I don't know the weather '" + cur().text + "'.",
                "Try: rain, sunny or stormy", sourceLine(cur().line));
        return w;
    }

    private String daypartNameOrError() {
        String d = greedy(Dictionary::findDaypart, 2, null);
        if (d == null) throw new VerseError(cur().line, "I don't know that time of day.",
                "Try: dawn, noon, dusk or night", sourceLine(cur().line));
        return d;
    }

    // ------------------------------------------------------------------
    // values
    // ------------------------------------------------------------------

    /** "text ${value} text" becomes a TemplateExpr; parts are String or ValueExpr. */
    private ValueExpr templateExpr(Token t) {
        String raw = t.text;
        List<Object> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            int open = raw.indexOf("${", i);
            if (open < 0) {
                text.append(raw, i, raw.length());
                i = raw.length();
                break;
            }
            text.append(raw, i, open);
            int depth = 1;
            int j = open + 2;
            int close = -1;
            while (j < raw.length()) {
                char c = raw.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { close = j; break; }
                }
                j++;
            }
            if (close < 0) {
                throw new VerseError(t.line,
                        "I found '${' but no closing '}' in this text.",
                        "Tell player \"You have ${player's coins} coins\"", sourceLine(t.line));
            }
            if (text.length() > 0) { parts.add(text.toString()); text.setLength(0); }
            String inner = raw.substring(open + 2, close);
            parts.add(parseInnerValue(inner, t.line));
            i = close + 1;
        }
        if (text.length() > 0) parts.add(text.toString());
        return new TemplateExpr(t.line, parts);
    }

    /** Parses one phrase inside ${...} as its own little value. */
    private ValueExpr parseInnerValue(String src, int line) {
        if (src.trim().isEmpty()) {
            throw new VerseError(line, "I found an empty ${} in this text.",
                    "Try: ${player's coins} or ${world's greeting}", sourceLine(line));
        }
        Parser p = new Parser(Lexer.tokenize(src), fileName);
        try {
            ValueExpr v = p.value("a value inside ${...}");
            Token after = p.cur();
            boolean done = after.type == Type.EOF || after.type == Type.NEWLINE;
            if (!done) {
                throw new VerseError(line, "I don't understand '" + src.trim() + "' inside ${...}.",
                        "Try: ${player's coins} or ${world's greeting}", sourceLine(line));
            }
            return v;
        } catch (VerseError e) {
            throw new VerseError(line, "Inside ${...}: " + e.message, e.suggestion, sourceLine(line));
        }
    }

    /** A value, with optional math: "5", "5 plus 3", "2 times 4 minus 1", ... */
    private ValueExpr value(String what) {
        ValueExpr left = primaryValue(what);
        while (true) {
            if (at("plus")) {
                pos++;
                left = new BinaryExpr(left.line, "+", left, primaryValue("a number"));
            } else if (at("minus")) {
                pos++;
                left = new BinaryExpr(left.line, "-", left, primaryValue("a number"));
            } else if (at("times")) {
                pos++;
                left = new BinaryExpr(left.line, "*", left, primaryValue("a number"));
            } else if (at("divided")) {
                pos++;
                expect("by", "set player's coins to 10 divided by 2");
                left = new BinaryExpr(left.line, "/", left, primaryValue("a number"));
            } else {
                break;
            }
        }
        return left;
    }

    private ValueExpr primaryValue(String what) {
        Token t = cur();
        int line = t.line;
        switch (t.type) {
            case NUMBER: pos++; return new NumExpr(line, t.num);
            case STRING: pos++; return t.text.contains("${")
                    ? templateExpr(t) : new TextExpr(line, t.text);
            default: break;
        }
        if (at("true")) { pos++; return new TruthExpr(line, true); }
        if (at("false")) { pos++; return new TruthExpr(line, false); }
        if (at("nothing") || at("none")) { pos++; return new TextExpr(line, ""); }
        if (at("player")) {
            if (peek(1).type == Type.POSSESSIVE) {
                pos += 2;
                if (eat("score")) {
                    String obj = string("the scoreboard name");
                    return new ScoreGetExpr(line, obj);
                }
                if (at("data")) {
                    pos++;
                    String key = string("a data key");
                    return new PlayerDataGetExpr(line, key);
                }
                if (eat("held")) {
                    expect("item", "player's held item");
                    return new HeldItemExpr(line);
                }
                String name = word("a variable name");
                return possessiveExpr(line, name);
            }
            String w = peek(1).type == Type.WORD ? peek(1).text : null;
            if ("health".equals(w)) { pos += 2; return new HealthExpr(line); }
            if ("hunger".equals(w)) { pos += 2; return new HungerExpr(line); }
            if ("xp".equals(w) || "experience".equals(w)) { pos += 2; return new XpExpr(line); }
            if ("level".equals(w) || "levels".equals(w)) { pos += 2; return new LevelExpr(line); }
            throw err("I expected 'player's <name>' or 'player health' here.");
        }
        if (at("database")) {
            pos++;
            String db = string("the database name");
            expect("at", "database \"data\" at \"coins\"");
            String key = string("a database key");
            return new DatabaseGetExpr(line, db, key);
        }
        if (at("config")) {
            pos++;
            expect("value", "config value \"prefix\" in file \"config.yml\"");
            String key = string("the config key");
            expect("in", "config value \"prefix\" in file \"config.yml\"");
            expect("file", "config value \"prefix\" in file \"config.yml\"");
            String file = string("the config file name");
            return new ConfigGetExpr(line, file, key);
        }
        if (at("world") && peek(1).type == Type.POSSESSIVE) {
            VarTarget vt = varTarget(what);
            return new VarGetExpr(line, vt);
        }
        if (at("global")) {
            VarTarget vt = varTarget(what);
            return new VarGetExpr(line, vt);
        }
        if (at("temporary")) {
            VarTarget vt = varTarget(what);
            return new VarGetExpr(line, vt);
        }
        if (at("function")) {
            pos++;
            String name = nameOrString("a function name");
            List<ValueExpr> args = new ArrayList<>();
            while (at("with") || at("and")) {
                pos++;
                expect("argument", "function \"tax\" with argument 100");
                args.add(value("an argument value"));
            }
            return new FunctionCallExpr(line, name, args);
        }
        if (at("length")) {
            pos++;
            expect("of", "length of list \"quests\"");
            if (cur().type == Type.WORD && cur().text.equals("list")) pos++;
            String listName = string("the list name");
            return new LengthExpr(line, listName);
        }
        if (at("random")) {
            pos++;
            if (at("item")) {
                pos++;
                expect("from", "random item from list \"quests\"");
                if (at("list")) pos++;
                String listName = string("the list name");
                return new RandomListExpr(line, listName);
            }
            expect("number", "random number between 1 and 10");
            expect("between", "random number between 1 and 10");
            double a = number("a number");
            expect("and", "random number between 1 and 10");
            double b = number("a number");
            return new RandomExpr(line, a, b);
        }
        if (at("number") && peek(1).type == Type.WORD && peek(1).text.equals("of")
                && peek(2).type == Type.WORD && (peek(2).text.equals("players") || peek(2).text.equals("player"))) {
            pos += 3;
            return new OnlineCountExpr(line);
        }
        if (at("count")) {
            pos++;
            expect("of", "count of diamonds in player's inventory");
            eatNoise();
            String item;
            if (cur().type == Type.STRING) {
                String raw = string("the item name");
                item = Dictionary.findItem(raw);
                if (item == null) {
                    throw new VerseError(line, "I don't know the item '" + raw + "'.",
                            null, null);
                }
            } else {
                item = itemNameOrError();
            }
            expect("in", "count of diamonds in player's inventory");
            eatNoise();
            expect("player", "count of diamonds in player's inventory");
            if (cur().type == Type.POSSESSIVE) pos++;
            expect("inventory", "count of diamonds in player's inventory");
            return new CountItemExpr(line, item);
        }
        if (cur().type == Type.WORD) {
            // a plain word is a loop variable, e.g. "tell player task"
            String name = cur().text;
            pos++;
            return new VarGetExpr(line, new VarTarget("temp", name));
        }
        throw new VerseError(line, "I expected a value here: a number, some \"text\", true or false, or a variable.",
                "set player's coins to 100", sourceLine(line));
    }

    /** "player's name", "player's x"... resolve to player facts, everything else is a variable. */
    private ValueExpr possessiveExpr(int line, String name) {
        switch (name) {
            case "name": return new PlayerNameExpr(line);
            case "world": return new PlayerWorldExpr(line);
            case "x": return new PlayerCoordExpr(line, "x");
            case "y": return new PlayerCoordExpr(line, "y");
            case "z": return new PlayerCoordExpr(line, "z");
            case "gamemode": return new GamemodeExpr(line);
            case "health": return new HealthExpr(line);
            case "hunger": return new HungerExpr(line);
            case "xp": return new XpExpr(line);
            case "level": return new LevelExpr(line);
            default: return new VarGetExpr(line, new VarTarget("player", name));
        }
    }

    private VarTarget varTarget(String what) {
        if (at("player") && peek(1).type == Type.POSSESSIVE) {
            pos += 2;
            if (eat("score")) {
                String obj = string("the scoreboard name");
                return new VarTarget("score", obj);
            }
            if (at("data")) {
                pos++;
                String key = string("a data key");
                return new VarTarget("playerdata", key);
            }
            String name = word("a variable name");
            return new VarTarget("player", name);
        }
        if (at("player") && peek(1).type == Type.WORD && peek(1).text.equals("data")) {
            pos += 2;
            String key = string("a data key");
            return new VarTarget("playerdata", key);
        }
        if (at("database")) {
            pos++;
            String db = string("the database name");
            expect("at", "set database \"data\" at \"coins\" to 5");
            String key = string("a database key");
            return new VarTarget("database", db, key);
        }
        if (at("config")) {
            pos++;
            expect("value", "set config value \"prefix\" in file \"config.yml\" to \"[Hermes]\"");
            String key = string("the config key");
            expect("in", "set config value \"prefix\" in file \"config.yml\" to \"[Hermes]\"");
            expect("file", "set config value \"prefix\" in file \"config.yml\" to \"[Hermes]\"");
            String file = string("the config file name");
            return new VarTarget("config", file, key);
        }
        if (at("world") && peek(1).type == Type.POSSESSIVE) {
            pos += 2;
            String name = word("a variable name");
            return new VarTarget("world", name);
        }
        if (at("global")) {
            pos++;
            String name = nameOrString("a global name");
            return new VarTarget("world", name);
        }
        if (at("temporary")) {
            pos++;
            String name = word("a variable name");
            return new VarTarget("temp", name);
        }
        if (at("list")) {
            pos++;
            String name = string("the list name");
            return new VarTarget("list", name);
        }
        if (at("score")) {
            pos++;
            String obj = string("the scoreboard name");
            return new VarTarget("score", obj);
        }
        throw new VerseError(cur().line,
                "I expected a variable here: 'player's coins', 'world's gold', 'database \"data\" at \"coins\"' or 'temporary x'.",
                "set player's coins to 100", sourceLine(cur().line));
    }

    // ------------------------------------------------------------------
    // conditions
    // ------------------------------------------------------------------

    private Condition condition() {
        return orCond();
    }

    private Condition orCond() {
        List<Condition> parts = new ArrayList<>();
        parts.add(andCond());
        while (eat("or")) parts.add(andCond());
        return parts.size() == 1 ? parts.get(0) : new OrCond(parts);
    }

    private Condition andCond() {
        List<Condition> parts = new ArrayList<>();
        parts.add(primaryCond());
        while (eat("and")) parts.add(primaryCond());
        return parts.size() == 1 ? parts.get(0) : new AndCond(parts);
    }

    private Condition primaryCond() {
        int line = cur().line;

        if (eat("not")) return new NotCond(primaryCond());

        if (at("player")) {
            if (peek(1).type == Type.POSSESSIVE) {
                pos += 2;
                if (eat("score")) {
                    String obj = string("the scoreboard name");
                    String op = cmpOpOrNull();
                    double v = number("a number");
                    return new ScoreCond(obj, op == null ? "==" : op, v);
                }
                ValueExpr left;
                if (at("data")) {
                    pos++;
                    String key = string("a data key");
                    left = new PlayerDataGetExpr(line, key);
                } else {
                    String name = word("a variable name");
                    left = possessiveExpr(line, name);
                }
                if (eat("is") || eat("are")) { /* nothing */ }
                String op = cmpOpOrNull();
                if (op == null) {
                    ValueExpr right = value("a value to compare with");
                    return new CmpCond(left, "==", right);
                }
                ValueExpr right = value("a value");
                return new CmpCond(left, op, right);
            }
            pos++;
            String w = word("a condition");
            switch (w) {
                case "has": {
                    eatNoise();
                    if (at("permission")) {
                        pos++;
                        String perm = string("the permission name");
                        return new PermissionCond(perm);
                    }
                    double n = number("a number");
                    String name = word("something to count");
                    if (Dictionary.isItem(name)) {
                        return new HasCond(true, name, n);
                    }
                    return new HasCond(false, name, n);
                }
                case "is": {
                    if (eat("holding")) {
                        eatNoise();
                        String item = itemNameOrError();
                        return new IsHoldingCond(item);
                    }
                    if (eat("sneaking")) return new PlayerStateCond("sneaking");
                    if (eat("flying")) return new PlayerStateCond("flying");
                    if (eat("wet")) return new PlayerStateCond("wet");
                    if (eat("op")) return new PlayerStateCond("op");
                    if (eat("frozen")) return new PlayerStateCond("frozen");
                    if (eat("on")) {
                        eatNoise();
                        expect("ground", "player is on the ground");
                        return new PlayerStateCond("ground");
                    }
                    if (eat("in")) {
                        eatNoise();
                        if (at("world")) {
                            pos++;
                            String world = string("the world name");
                            return new InWorldCond(world);
                        }
                        if (at("biome")) {
                            pos++;
                            String biome = nameOrString("a biome name");
                            String canonical = Dictionary.findBiome(biome);
                            if (canonical == null) {
                                throw new VerseError(line, "I don't know the biome '" + biome + "'.",
                                        null, null);
                            }
                            return new InBiomeCond(canonical);
                        }
                        if (at("region")) pos++;
                        String gamemode = greedy(Dictionary::findGamemode, 2, null);
                        if (gamemode != null) return new InGamemodeCond(gamemode);
                        eatNoise();
                        String dim = greedy(Dictionary::findDim, 3, null);
                        if (dim != null) return new InDimensionCond(dim);
                        String region = nameOrString("a region or dimension name");
                        if (Dictionary.isDim(region)) return new InDimensionCond(Dictionary.findDim(region));
                        return new InRegionCond(region);
                    }
                    if (eat("holding")) { // "is holding X"
                        eatNoise();
                        String item = itemNameOrError();
                        return new IsHoldingCond(item);
                    }
                    throw new VerseError(line,
                            "After 'player is' I know: 'holding <item>', 'sneaking', 'flying', 'wet', 'frozen', 'op', 'on the ground', 'in the nether', 'in <region>', 'in biome <biome>', 'in creative mode'.",
                            "player is holding a torch", null);
                }
                case "health": {
                    eat("is"); eat("are");
                    String op = cmpOpOrNull();
                    if (op == null) throw new VerseError(line, "I expected 'below', 'above', 'at least'... after 'player health is'.",
                            "player health is below 5", null);
                    double v = number("a number");
                    return new CmpCond(new HealthExpr(line), op, new NumExpr(line, v));
                }
                case "hunger": {
                    eat("is"); eat("are");
                    String op = cmpOpOrNull();
                    if (op == null) throw new VerseError(line, "I expected 'below', 'above', 'at least'... after 'player hunger is'.",
                            null, null);
                    double v = number("a number");
                    return new CmpCond(new HungerExpr(line), op, new NumExpr(line, v));
                }
                case "xp": {
                    eat("is"); eat("are");
                    String op = cmpOpOrNull();
                    double v = number("a number");
                    return new CmpCond(new XpExpr(line), op == null ? "==" : op, new NumExpr(line, v));
                }
                case "level": {
                    eat("is"); eat("are");
                    String op = cmpOpOrNull();
                    double v = number("a number");
                    return new CmpCond(new LevelExpr(line), op == null ? "==" : op, new NumExpr(line, v));
                }
                default:
                    throw new VerseError(line,
                            "I don't understand '" + w + "' after 'player'. I know conditions like: has 10 diamonds, is holding a torch, health is below 5, is in the nether.",
                            "when player has 5 diamonds\n    give player a diamond sword",
                            null);
            }
        }

        if (at("world") && peek(1).type == Type.POSSESSIVE) {
            pos += 2;
            String name = word("a variable name");
            if (eat("is") || eat("are")) { /* nothing */ }
            String op = cmpOpOrNull();
            if (op == null) {
                ValueExpr right = value("a value to compare with");
                return new CmpCond(new VarGetExpr(line, new VarTarget("world", name)), "==", right);
            }
            ValueExpr right = value("a value");
            return new CmpCond(new VarGetExpr(line, new VarTarget("world", name)), op, right);
        }
        if (at("global")) {
            pos++;
            String name = nameOrString("a global name");
            if (eat("is") || eat("are")) { /* nothing */ }
            String op = cmpOpOrNull();
            if (op == null) {
                ValueExpr right = value("a value to compare with");
                return new CmpCond(new VarGetExpr(line, new VarTarget("world", name)), "==", right);
            }
            ValueExpr right = value("a value");
            return new CmpCond(new VarGetExpr(line, new VarTarget("world", name)), op, right);
        }

        if (at("it")) {
            pos++;
            expect("is", "it is nighttime");
            eatNoise();
            String weather = greedy(Dictionary::findWeather, 3, null);
            if (weather != null) return new WeatherCond(weather);
            String day = greedy(Dictionary::findDaypart, 2, null);
            if (day != null) return new TimeCond(day);
            throw new VerseError(line, "I don't know what 'it is' means here. Try: it is nighttime, it is rainy.",
                    "when it is nighttime\n    spawn zombies", null);
        }

        if (at("list")) {
            pos++;
            String name = string("the list name");
            expect("contains", "list \"quests\" contains \"Find the key\"");
            ValueExpr v = value("a value");
            return new ContainsCond(name, v);
        }

        if (at("chest")) {
            pos++;
            expect("at", "chest at 10 64 20 has 5 diamonds");
            Vec3 v = coords();
            expect("has", "chest at 10 64 20 has 5 diamonds");
            double n = number("a number");
            eatNoise();
            String item = itemNameOrError();
            return new ChestHasCond(v, item, (int) n);
        }

        if (at("chance")) {
            pos++;
            if (eat("of")) {
                double a = number("a number");
                expect("in", "chance of 1 in 4");
                double b = number("a number");
                if (b <= 0) throw err("The second number in 'chance of X in Y' must be more than zero.");
                return new ChanceCond(Math.min(100, a / b * 100));
            }
            double percent = number("a number");
            if (percent < 0 || percent > 100) throw err("A chance must be between 0 and 100.");
            return new ChanceCond(percent);
        }

        ValueExpr left = value("a value");
        boolean sawIs = eat("is") || eat("are");
        String op = cmpOpOrNull();
        if (op == null && sawIs) op = "==";
        if (op == null) {
            if (left instanceof TruthExpr) return new TruthCond(((TruthExpr) left).v);
            throw new VerseError(line,
                    "I expected a comparison here: 'is equal to', 'at least', 'below'...",
                    "if world's flag is true\n    give player a diamond", null);
        }
        ValueExpr right = value("a value");
        return new CmpCond(left, op, right);
    }

    /** Matches "equal to", "more than", "at least"... Returns null if the next words aren't a comparison. */
    private String cmpOpOrNull() {
        if (at("is") || at("are")) {
            // "is equal to 5", "is more than 5"
            if (peek(1).type == Type.WORD) {
                String nxt = peek(1).text;
                if (nxt.equals("equal")) { pos += 2; expect("to"); return "=="; }
                if (nxt.equals("more") || nxt.equals("greater")) { pos += 2; expect("than"); return ">"; }
                if (nxt.equals("less")) { pos += 2; expect("than"); return "<"; }
                if (nxt.equals("at")) {
                    if (peek(2).type == Type.WORD && peek(2).text.equals("least")) { pos += 3; return ">="; }
                    if (peek(2).type == Type.WORD && peek(2).text.equals("most")) { pos += 3; return "<="; }
                }
                if (nxt.equals("not")) {
                    if (peek(2).type == Type.WORD && peek(2).text.equals("equal")) {
                        pos += 3; expect("to"); return "!=";
                    }
                }
            }
            pos++; // consume the bare "is"/"are"
            return cmpOpOrNull();
        }
        if (eat("equal")) { expect("to"); return "=="; }
        if (eat("not")) { expect("equal"); expect("to"); return "!="; }
        if (eat("more") || eat("greater")) { expect("than"); return ">"; }
        if (eat("less")) { expect("than"); return "<"; }
        if (eat("at")) {
            if (eat("least")) return ">=";
            expect("most", "at most 5");
            return "<=";
        }
        if (eat("above") || eat("over")) return ">";
        if (eat("below") || eat("under")) return "<";
        return null;
    }

    // ------------------------------------------------------------------
    // triggers
    // ------------------------------------------------------------------

    private Trigger trigger() {
        int line = cur().line;

        if (at("server")) {
            pos++;
            if (eat("starts")) return eventTrigger("server starts", null, Trigger.Filter.NONE);
            if (eat("stops")) return eventTrigger("server stops", null, Trigger.Filter.NONE);
            throw new VerseError(line, "I expected 'server starts' or 'server stops'.",
                    "when server starts\n    announce \"The server is up!\"", null);
        }

        if (at("player")) {
            if (peek(1).type == Type.POSSESSIVE) {
                pos += 2;
                if (at("score")) {
                    pos++;
                    String obj = string("the scoreboard name");
                    if (eat("is") || eat("are")) { /* nothing */ }
                    String op = cmpOpOrNull();
                    if (op == null) throw new VerseError(line,
                            "I expected a comparison here, like: player's coins are at least 100.",
                            "when player's coins are at least 100\n    give player a diamond sword", null);
                    double v = number("a number");
                    return stateTrigger(new ScoreCond(obj, op, v), true);
                }
                String name = word("a variable name");
                if (eat("is") || eat("are")) { /* nothing */ }
                String op = cmpOpOrNull();
                if (op == null) throw new VerseError(line,
                        "I expected a comparison here, like: player's coins are at least 100.",
                        "when player's coins are at least 100\n    give player a diamond sword", null);
                double v = number("a number");
                return stateTrigger(new CmpCond(possessiveExpr(line, name),
                        op, new NumExpr(line, v)), true);
            }
            pos++;
            boolean firstJoin = false;
            if (eat("first")) firstJoin = true;
            String w = greedyWords(2, "an event");
            if (w == null) w = word("an event");
            switch (w) {
                case "joins": {
                    eatNoise(); eat("world"); eat("server"); eat("game");
                    Trigger t = eventTrigger("joins", null, Trigger.Filter.NONE);
                    if (firstJoin) t.first = true;
                    return t;
                }
                case "leaves": {
                    eatNoise(); eat("world"); eat("server"); eat("game");
                    if (firstJoin) throw new VerseError(line,
                            "'first' only works with 'when player joins'.",
                            "when player first joins\n    give player 1 diamond", null);
                    return eventTrigger("leaves", null, Trigger.Filter.NONE);
                }
                case "dies": return eventTrigger("dies", null, Trigger.Filter.NONE);
                case "respawns": return eventTrigger("respawns", null, Trigger.Filter.NONE);
                case "jumps": return eventTrigger("jumps", null, Trigger.Filter.NONE);
                case "sneaks": return eventTrigger("sneaks", null, Trigger.Filter.NONE);
                case "moves": return eventTrigger("moves", null, Trigger.Filter.NONE);
                case "attacks": return eventTrigger("attacks", null, Trigger.Filter.NONE);
                case "chats": return eventTrigger("chats", null, Trigger.Filter.NONE);
                case "fishes": return eventTrigger("fishes", null, Trigger.Filter.NONE);
                case "levels": {
                    eat("up");
                    return eventTrigger("levels up", null, Trigger.Filter.NONE);
                }
                case "clicks": {
                    eatNoise();
                    int slot = -1;
                    if (eat("slot")) {
                        slot = (int) number("a slot number");
                        eat("of");
                    }
                    expect("gui", "when player clicks slot 4 of gui \"Shop\"");
                    String gui = string("the gui name");
                    Trigger t = eventTrigger("gui click", gui, Trigger.Filter.GUI);
                    t.guiSlot = slot;
                    return t;
                }
                case "eats": {
                    if (at("any")) {
                        pos++;
                        eat("item");
                    } else {
                        eatNoise();
                        if (!atEndOfLine()) {
                            String item = itemNameOrError();
                            return eventTrigger("eats", item, Trigger.Filter.ITEM);
                        }
                    }
                    return eventTrigger("eats", null, Trigger.Filter.ITEM);
                }
                case "kills": {
                    if (at("any")) {
                        pos++;
                        eat("mob");
                    } else {
                        eatNoise();
                        if (!atEndOfLine()) {
                            String mob = mobNameOrError();
                            return eventTrigger("kills", mob, Trigger.Filter.MOB);
                        }
                    }
                    return eventTrigger("kills", null, Trigger.Filter.MOB);
                }
                case "starts": {
                    expect("sprinting", "when player starts sprinting");
                    return eventTrigger("starts sprinting", null, Trigger.Filter.NONE);
                }
                case "stops": {
                    expect("sprinting", "when player stops sprinting");
                    return eventTrigger("stops sprinting", null, Trigger.Filter.NONE);
                }
                case "takes": {
                    expect("damage", "when player takes damage");
                    return eventTrigger("takes damage", null, Trigger.Filter.NONE);
                }
                case "breaks": {
                    eatNoise();
                    if (at("block") || at("any")) {
                        pos++;
                        eat("block");
                        return eventTrigger("breaks", null, Trigger.Filter.BLOCK);
                    }
                    String block = itemNameOrError();
                    return eventTrigger("breaks", block, Trigger.Filter.BLOCK);
                }
                case "places": {
                    eatNoise();
                    if (at("block") || at("any")) {
                        pos++;
                        eat("block");
                        return eventTrigger("places", null, Trigger.Filter.BLOCK);
                    }
                    String block = itemNameOrError();
                    return eventTrigger("places", block, Trigger.Filter.BLOCK);
                }
                case "picks": {
                    expect("up", "when player picks up an item");
                    eatNoise();
                    String item = itemNameOrError();
                    return eventTrigger("picks up", item, Trigger.Filter.ITEM);
                }
                case "drops": {
                    eatNoise();
                    String item = itemNameOrError();
                    return eventTrigger("drops", item, Trigger.Filter.ITEM);
                }
                case "uses": {
                    eatNoise();
                    String item = itemNameOrError();
                    return eventTrigger("uses", item, Trigger.Filter.ITEM);
                }
                case "interacts": {
                    expect("with", "when player interacts with a block");
                    eatNoise();
                    String block = itemNameOrError();
                    return eventTrigger("interacts with", block, Trigger.Filter.BLOCK);
                }
                case "enters":
                case "reaches": {
                    eatNoise();
                    if (at("area") || at("region") || at("world")) pos++;
                    String region = nameOrString("a region name");
                    return eventTrigger("enters", region, Trigger.Filter.REGION);
                }
                case "types": {
                    String text = string("the chat message");
                    return eventTrigger("types", text, Trigger.Filter.TEXT);
                }
                case "touches": {
                    eatNoise();
                    String block = itemNameOrError();
                    return stateTrigger(new TouchCond(block), true);
                }
                case "walks": {
                    expect("on", "when player walks on lava");
                    eatNoise();
                    String block = itemNameOrError();
                    return stateTrigger(new WalkCond(block), true);
                }
                case "has": {
                    eatNoise();
                    if (at("permission")) {
                        pos++;
                        String perm = string("the permission name");
                        return stateTrigger(new PermissionCond(perm), true);
                    }
                    double n = number("a number");
                    String name = word("something to count");
                    if (Dictionary.isItem(name)) {
                        return stateTrigger(new HasCond(true, name, n), true);
                    }
                    return stateTrigger(new HasCond(false, name, n), true);
                }
                case "is": {
                    if (eat("holding")) {
                        eatNoise();
                        String item = itemNameOrError();
                        return stateTrigger(new IsHoldingCond(item), true);
                    }
                    if (eat("sneaking")) return stateTrigger(new PlayerStateCond("sneaking"), true);
                    if (eat("flying")) return stateTrigger(new PlayerStateCond("flying"), true);
                    if (eat("wet")) return stateTrigger(new PlayerStateCond("wet"), true);
                    if (eat("op")) return stateTrigger(new PlayerStateCond("op"), true);
                    if (eat("frozen")) return stateTrigger(new PlayerStateCond("frozen"), true);
                    if (eat("on")) {
                        eatNoise();
                        expect("ground", "when player is on the ground");
                        return stateTrigger(new PlayerStateCond("ground"), true);
                    }
                    if (eat("in")) {
                        eatNoise();
                        if (at("world")) {
                            pos++;
                            String world = string("the world name");
                            return stateTrigger(new InWorldCond(world), true);
                        }
                        if (at("biome")) {
                            pos++;
                            String biome = nameOrString("a biome name");
                            String canonical = Dictionary.findBiome(biome);
                            if (canonical == null) throw new VerseError(line, "I don't know the biome '" + biome + "'.");
                            return stateTrigger(new InBiomeCond(canonical), true);
                        }
                        if (at("region")) pos++;
                        String gamemode = greedy(Dictionary::findGamemode, 2, null);
                        if (gamemode != null) return stateTrigger(new InGamemodeCond(gamemode), true);
                        eatNoise();
                        String dim = greedy(Dictionary::findDim, 3, null);
                        if (dim != null) return stateTrigger(new InDimensionCond(dim), true);
                        String region = nameOrString("a region or dimension name");
                        if (Dictionary.isDim(region)) return stateTrigger(new InDimensionCond(Dictionary.findDim(region)), true);
                        return stateTrigger(new InRegionCond(region), true);
                    }
                    throw new VerseError(line,
                            "After 'player is' I know: 'holding <item>', 'sneaking', 'flying', 'wet', 'frozen', 'op', 'on the ground', 'in the nether', 'in <region>', 'in creative mode'.",
                            "when player is holding a torch\n    give player night vision", null);
                }
                case "health": {
                    eat("is"); eat("are");
                    String op = cmpOpOrNull();
                    if (op == null) throw new VerseError(line, "I expected 'below', 'at least'... after 'player health is'.",
                            "when player health is below 5\n    warn player \"You are almost dead!\"", null);
                    double v = number("a number");
                    return stateTrigger(new CmpCond(new HealthExpr(line), op, new NumExpr(line, v)), true);
                }
                case "hunger": {
                    eat("is"); eat("are");
                    String op = cmpOpOrNull();
                    if (op == null) throw new VerseError(line, "I expected a comparison after 'player hunger is'.");
                    double v = number("a number");
                    return stateTrigger(new CmpCond(new HungerExpr(line), op, new NumExpr(line, v)), true);
                }
                default:
                    throw new VerseError(line,
                            "I don't know the event '" + w + "'. I know: joins, leaves, dies, jumps, sneaks, breaks, places, chats, takes damage, enters, reaches...",
                            "when player joins\n    tell player \"Hello!\"", null);
            }
        }

        if (at("world") && peek(1).type == Type.POSSESSIVE) {
            pos += 2;
            String name = word("a variable name");
            if (eat("is") || eat("are")) { /* nothing */ }
            String op = cmpOpOrNull();
            if (op == null) {
                ValueExpr right = value("a value to compare with");
                return stateTrigger(new CmpCond(new VarGetExpr(line, new VarTarget("world", name)),
                        "==", right), false);
            }
            ValueExpr right = value("a value");
            return stateTrigger(new CmpCond(new VarGetExpr(line, new VarTarget("world", name)),
                    op, right), false);
        }
        if (at("global")) {
            pos++;
            String name = nameOrString("a global name");
            if (eat("is") || eat("are")) { /* nothing */ }
            String op = cmpOpOrNull();
            if (op == null) {
                ValueExpr right = value("a value to compare with");
                return stateTrigger(new CmpCond(new VarGetExpr(line, new VarTarget("world", name)),
                        "==", right), false);
            }
            ValueExpr right = value("a value");
            return stateTrigger(new CmpCond(new VarGetExpr(line, new VarTarget("world", name)),
                    op, right), false);
        }

        if (at("it")) {
            pos++;
            expect("is", "it is nighttime");
            eatNoise();
            String weather = greedy(Dictionary::findWeather, 3, null);
            if (weather != null) return stateTrigger(new WeatherCond(weather), false);
            String day = greedy(Dictionary::findDaypart, 2, null);
            if (day != null) return stateTrigger(new TimeCond(day), false);
            throw new VerseError(line, "I don't know what 'it is' means here. Try: it is nighttime, it is rainy.",
                    "when it is nighttime\n    spawn zombies", null);
        }

        if (at("mob")) {
            pos++;
            if (eat("named")) {
                String name = string("the mob's name");
                if (eat("dies")) return eventTrigger("mob dies", name, Trigger.Filter.MOB_NAME);
                if (eat("spawns")) return eventTrigger("mob spawns", name, Trigger.Filter.MOB_NAME);
                throw new VerseError(line, "I expected 'dies' or 'spawns' after the mob's name.",
                        "when mob named \"boss\" dies", null);
            }
            if (at("dies")) { pos++; return eventTrigger("mob dies", null, Trigger.Filter.NONE); }
            if (at("spawns")) { pos++; return eventTrigger("mob spawns", null, Trigger.Filter.NONE); }
            if (at("attacks")) { pos++; return eventTrigger("mob attacks", null, Trigger.Filter.NONE); }
            throw new VerseError(line, "I expected 'dies' or 'spawns' after 'mob'.",
                    "when mob dies\n    announce \"A mob died!\"", null);
        }

        if (at("custom") || at("event")) {
            pos++;
            eat("custom");
            expect("event", "when custom event \"boss_killed\" fires");
            String name = string("the event name");
            eat("fires");
            return eventTrigger("custom", name, Trigger.Filter.TEXT);
        }

        if (at("night")) {
            pos++;
            expect("falls", "when night falls");
            return stateTrigger(new TimeCond("night"), false);
        }
        if (at("dawn")) {
            pos++;
            expect("breaks", "when dawn breaks");
            return stateTrigger(new NotCond(new TimeCond("night")), false);
        }

        String mob = greedy(Dictionary::findMob, 3, null);
        if (mob != null) {
            if (eat("dies")) return eventTrigger("mob dies", mob, Trigger.Filter.MOB);
            if (eat("spawns")) return eventTrigger("mob spawns", mob, Trigger.Filter.MOB);
            if (eat("attacks")) return eventTrigger("mob attacks", mob, Trigger.Filter.MOB);
            throw new VerseError(line, "I expected 'dies' or 'spawns' after '" + mob + "'.",
                    "when " + mob + " dies\n    announce \"A " + mob + " died!\"", null);
        }

        throw new VerseError(line,
                "I don't understand this event. I know things like: player joins, player breaks diamond ore, it is nighttime, mob dies, custom event \"name\".",
                "when player joins\n    tell player \"Hello!\"", null);
    }

    private Trigger eventTrigger(String event, String filter, Trigger.Filter filterType) {
        return new Trigger(Trigger.Kind.EVENT, event, filter, filterType,
                new ArrayList<>(), false);
    }

    private Trigger stateTrigger(Condition cond, boolean playerSubject) {
        List<Condition> conds = new ArrayList<>();
        conds.add(cond);
        return new Trigger(Trigger.Kind.STATE, null, null, Trigger.Filter.NONE,
                conds, playerSubject);
    }
}
