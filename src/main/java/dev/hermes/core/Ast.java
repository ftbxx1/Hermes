package dev.hermes.core;

import java.util.List;

/**
 * The Hermes abstract syntax tree. One tiny class per sentence shape, so the
 * interpreter can walk the tree and "do" each statement.
 */
public final class Ast {

    private Ast() {}

    // ------------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------------

    public abstract static class Stmt {
        public final int line;
        Stmt(int line) { this.line = line; }
    }

    public static final class Script {
        public final String name;
        public final List<Stmt> body;
        public Script(String name, List<Stmt> body) { this.name = name; this.body = body; }
    }

    public static final class RegionDef extends Stmt {
        public final String name; public final WorldAPI.Vec3 a; public final WorldAPI.Vec3 b;
        public RegionDef(int line, String name, WorldAPI.Vec3 a, WorldAPI.Vec3 b) { super(line); this.name = name; this.a = a; this.b = b; }
    }

    public static final class MarkDef extends Stmt {
        public final String name; public final WorldAPI.Vec3 loc;
        public MarkDef(int line, String name, WorldAPI.Vec3 loc) { super(line); this.name = name; this.loc = loc; }
    }

    /** "gui \"Shop\" ..." followed by a block of slot lines. */
    public static final class GuiDef extends Stmt {
        public final String name; public final List<SlotStmt> slots;
        public GuiDef(int line, String name, List<SlotStmt> slots) { super(line); this.name = name; this.slots = slots; }
    }

    /** One line inside a gui definition: "slot 3 has 1 diamond named \"Star\"". */
    public static final class SlotStmt extends Stmt {
        public final int slot; public final WorldAPI.ItemSpec spec;
        public SlotStmt(int line, int slot, WorldAPI.ItemSpec spec) { super(line); this.slot = slot; this.spec = spec; }
    }

    public static final class TeamCreate extends Stmt {
        public final String name;
        public TeamCreate(int line, String name) { super(line); this.name = name; }
    }

    public static final class ListCreate extends Stmt {
        public final String name;
        public ListCreate(int line, String name) { super(line); this.name = name; }
    }

    public static final class WhenBlock extends Stmt {
        public final Trigger trigger; public final List<Stmt> body;
        public WhenBlock(int line, Trigger trigger, List<Stmt> body) { super(line); this.trigger = trigger; this.body = body; }
    }

    public static final class EveryBlock extends Stmt {
        public final double seconds; public final List<Stmt> body;
        public EveryBlock(int line, double seconds, List<Stmt> body) { super(line); this.seconds = seconds; this.body = body; }
    }

    public static final class ActionDef extends Stmt {
        public final String name; public final List<String> params; public final List<Stmt> body;
        public ActionDef(int line, String name, List<String> params, List<Stmt> body) {
            super(line); this.name = name; this.params = params; this.body = body;
        }
    }

    public static final class IfStmt extends Stmt {
        public final Condition cond; public final List<Stmt> thenBody; public final List<Stmt> elseBody;
        public IfStmt(int line, Condition cond, List<Stmt> thenBody, List<Stmt> elseBody) {
            super(line); this.cond = cond; this.thenBody = thenBody; this.elseBody = elseBody;
        }
    }

    public static final class RepeatStmt extends Stmt {
        public final double times; public final List<Stmt> body;
        public RepeatStmt(int line, double times, List<Stmt> body) { super(line); this.times = times; this.body = body; }
    }

    public static final class WhileStmt extends Stmt {
        public final Condition cond; public final List<Stmt> body;
        public WhileStmt(int line, Condition cond, List<Stmt> body) { super(line); this.cond = cond; this.body = body; }
    }

    /** Pauses a handler for a while: "wait 2 seconds". Only allowed directly
     *  inside a when/every/action/command block. */
    public static final class WaitStmt extends Stmt {
        public final double seconds;
        public WaitStmt(int line, double seconds) { super(line); this.seconds = seconds; }
    }

    public static final class LoopStmt extends Stmt {
        /** "list", "players", "numbers" or "inventory". */
        public final String kind;
        public final String listName;   // kind == "list"
        public final double from, to;   // kind == "numbers"
        public final String itemName; public final List<Stmt> body;
        public LoopStmt(int line, String kind, String listName, double from, double to,
                        String itemName, List<Stmt> body) {
            super(line); this.kind = kind; this.listName = listName;
            this.from = from; this.to = to; this.itemName = itemName; this.body = body;
        }
    }

    /** A player-facing command defined in a script: command "/quest" with argument <name>. */
    public static final class CommandDef extends Stmt {
        public final String name;          // "/quest"
        public final List<String> argNames;
        public final String permission;    // null = anyone
        public final List<Stmt> body;
        public CommandDef(int line, String name, List<String> argNames, String permission, List<Stmt> body) {
            super(line); this.name = name; this.argNames = argNames;
            this.permission = permission; this.body = body;
        }
    }

    public static final class StopStmt extends Stmt {
        public StopStmt(int line) { super(line); }
    }

    public static final class SetStmt extends Stmt {
        public final VarTarget target; public final ValueExpr value;
        public SetStmt(int line, VarTarget target, ValueExpr value) { super(line); this.target = target; this.value = value; }
    }

    public static final class AddStmt extends Stmt {
        public final VarTarget target; public final ValueExpr amount;
        public AddStmt(int line, VarTarget target, ValueExpr amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class RemoveStmt extends Stmt {
        public final VarTarget target; public final ValueExpr amount;
        public RemoveStmt(int line, VarTarget target, ValueExpr amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class ListAddStmt extends Stmt {
        public final String list; public final ValueExpr value;
        public ListAddStmt(int line, String list, ValueExpr value) { super(line); this.list = list; this.value = value; }
    }

    public static final class ListRemoveStmt extends Stmt {
        public final String list; public final ValueExpr value;
        public ListRemoveStmt(int line, String list, ValueExpr value) { super(line); this.list = list; this.value = value; }
    }

    public static final class ListClearStmt extends Stmt {
        public final String list;
        public ListClearStmt(int line, String list) { super(line); this.list = list; }
    }

    public static final class ListDeleteStmt extends Stmt {
        public final String list;
        public ListDeleteStmt(int line, String list) { super(line); this.list = list; }
    }

    public static final class ShowStmt extends Stmt {
        public final VarTarget target;
        public ShowStmt(int line, VarTarget target) { super(line); this.target = target; }
    }

    public static final class FireEventStmt extends Stmt {
        public final String name;
        public FireEventStmt(int line, String name) { super(line); this.name = name; }
    }

    public static final class WinStmt extends Stmt {
        public WinStmt(int line) { super(line); }
    }

    // ------------------------------------------------------------------
    // World actions
    // ------------------------------------------------------------------

    public enum TargetRef { PLAYER, MOB, ALL_PLAYERS }

    public static final class KillStmt extends Stmt {
        public final TargetRef target;
        public KillStmt(int line, TargetRef target) { super(line); this.target = target; }
    }

    public static final class DamageStmt extends Stmt {
        public final TargetRef target; public final double amount;
        public DamageStmt(int line, TargetRef target, double amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class HealStmt extends Stmt {
        public final TargetRef target; public final double amount;
        public HealStmt(int line, TargetRef target, double amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class GiveItemStmt extends Stmt {
        public final TargetRef target; public final WorldAPI.ItemSpec spec;
        public GiveItemStmt(int line, TargetRef target, WorldAPI.ItemSpec spec) { super(line); this.target = target; this.spec = spec; }
    }

    public static final class TakeItemStmt extends Stmt {
        public final TargetRef target; public final String item; public final int count;
        public TakeItemStmt(int line, TargetRef target, String item, int count) { super(line); this.target = target; this.item = item; this.count = count; }
    }

    public static final class GiveXpStmt extends Stmt {
        public final TargetRef target; public final double amount;
        public GiveXpStmt(int line, TargetRef target, double amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class GiveLevelsStmt extends Stmt {
        public final TargetRef target; public final double amount;
        public GiveLevelsStmt(int line, TargetRef target, double amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class EffectStmt extends Stmt {
        public final TargetRef target; public final String effect; public final int seconds;
        public EffectStmt(int line, TargetRef target, String effect, int seconds) { super(line); this.target = target; this.effect = effect; this.seconds = seconds; }
    }

    public static final class RemoveEffectStmt extends Stmt {
        public final TargetRef target; public final String effect;
        public RemoveEffectStmt(int line, TargetRef target, String effect) { super(line); this.target = target; this.effect = effect; }
    }

    public static final class MakeEffectStmt extends Stmt {
        public final TargetRef target; public final String effect;
        public MakeEffectStmt(int line, TargetRef target, String effect) { super(line); this.target = target; this.effect = effect; }
    }

    public static final class FeedStmt extends Stmt {
        public final TargetRef target; public final double amount;
        public FeedStmt(int line, TargetRef target, double amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class ClearInventoryStmt extends Stmt {
        public ClearInventoryStmt(int line) { super(line); }
    }

    public static final class KickStmt extends Stmt {
        public final TargetRef target; public final String reason;
        public KickStmt(int line, TargetRef target, String reason) { super(line); this.target = target; this.reason = reason; }
    }

    public static final class LaunchStmt extends Stmt {
        public final TargetRef target; public final double amount;
        public LaunchStmt(int line, TargetRef target, double amount) { super(line); this.target = target; this.amount = amount; }
    }

    public static final class TitleStmt extends Stmt {
        public final TargetRef target; public final ValueExpr title; public final ValueExpr subtitle;
        public TitleStmt(int line, TargetRef target, ValueExpr title, ValueExpr subtitle) {
            super(line); this.target = target; this.title = title; this.subtitle = subtitle;
        }
    }

    public static final class ActionbarStmt extends Stmt {
        public final TargetRef target; public final ValueExpr text;
        public ActionbarStmt(int line, TargetRef target, ValueExpr text) { super(line); this.target = target; this.text = text; }
    }

    public static final class SetGamemodeStmt extends Stmt {
        public final String mode;
        public SetGamemodeStmt(int line, String mode) { super(line); this.mode = mode; }
    }

    public static final class LightningStmt extends Stmt {
        public final LocRef where;
        public LightningStmt(int line, LocRef where) { super(line); this.where = where; }
    }

    public static final class ExplodeStmt extends Stmt {
        public final LocRef where; public final double power;
        public ExplodeStmt(int line, LocRef where, double power) { super(line); this.where = where; this.power = power; }
    }

    public static final class ParticleStmt extends Stmt {
        public final String particle; public final LocRef where;
        public final int count; public final double size;
        public ParticleStmt(int line, String particle, LocRef where) {
            this(line, particle, where, 30, 1.0);
        }
        public ParticleStmt(int line, String particle, LocRef where, int count, double size) {
            super(line); this.particle = particle; this.where = where; this.count = count; this.size = size;
        }
    }

    public static final class PermissionStmt extends Stmt {
        public final boolean grant; public final TargetRef target; public final String perm;
        public PermissionStmt(int line, boolean grant, TargetRef target, String perm) { super(line); this.grant = grant; this.target = target; this.perm = perm; }
    }

    public static final class TellStmt extends Stmt {
        public final TargetRef target; public final ValueExpr text;
        public TellStmt(int line, TargetRef target, ValueExpr text) { super(line); this.target = target; this.text = text; }
    }

    public static final class AnnounceStmt extends Stmt {
        public final ValueExpr text;
        public AnnounceStmt(int line, ValueExpr text) { super(line); this.text = text; }
    }

    public static final class WarnStmt extends Stmt {
        public final TargetRef target; public final ValueExpr text;
        public WarnStmt(int line, TargetRef target, ValueExpr text) { super(line); this.target = target; this.text = text; }
    }

    public static final class WelcomeStmt extends Stmt {
        public final TargetRef target; public final ValueExpr text;
        public WelcomeStmt(int line, TargetRef target, ValueExpr text) { super(line); this.target = target; this.text = text; }
    }

    public static final class LocRef {
        public final String kind; // coords | mark | spawn | player
        public final WorldAPI.Vec3 vec;
        public final String name;
        public LocRef(String kind, WorldAPI.Vec3 vec, String name) { this.kind = kind; this.vec = vec; this.name = name; }
    }

    public static final class TeleportStmt extends Stmt {
        public final TargetRef target; public final LocRef where;
        public TeleportStmt(int line, TargetRef target, LocRef where) { super(line); this.target = target; this.where = where; }
    }

    /** "teleport player randomly within 100": scatters to a random spot nearby. */
    public static final class TeleportRandomStmt extends Stmt {
        public final TargetRef target; public final double radius;
        public TeleportRandomStmt(int line, TargetRef target, double radius) { super(line); this.target = target; this.radius = radius; }
    }

    /** "freeze player" / "unfreeze player". */
    public static final class FreezeStmt extends Stmt {
        public final TargetRef target; public final boolean frozen;
        public FreezeStmt(int line, TargetRef target, boolean frozen) { super(line); this.target = target; this.frozen = frozen; }
    }

    public static final class SpawnMobStmt extends Stmt {
        public final String mob; public final int count; public final LocRef where; public final String customName;
        public SpawnMobStmt(int line, String mob, int count, LocRef where, String customName) {
            super(line); this.mob = mob; this.count = count; this.where = where; this.customName = customName;
        }
    }

    public static final class SetWeatherStmt extends Stmt {
        public final String weather;
        public SetWeatherStmt(int line, String weather) { super(line); this.weather = weather; }
    }

    public static final class PlaySoundStmt extends Stmt {
        public final String sound; public final LocRef where;
        public PlaySoundStmt(int line, String sound, LocRef where) { super(line); this.sound = sound; this.where = where; }
    }

    public static final class SetTimeStmt extends Stmt {
        public final String daypart;
        public SetTimeStmt(int line, String daypart) { super(line); this.daypart = daypart; }
    }

    public static final class SetBlockStmt extends Stmt {
        public final WorldAPI.Vec3 loc; public final String block;
        public SetBlockStmt(int line, WorldAPI.Vec3 loc, String block) { super(line); this.loc = loc; this.block = block; }
    }

    public static final class NearbyOpStmt extends Stmt {
        public final String op; // open door | close door | press button | pull lever
        public NearbyOpStmt(int line, String op) { super(line); this.op = op; }
    }

    public static final class SignStmt extends Stmt {
        public final WorldAPI.Vec3 loc; public final List<String> lines;
        public SignStmt(int line, WorldAPI.Vec3 loc, List<String> lines) { super(line); this.loc = loc; this.lines = lines; }
    }

    public static final class PowerStmt extends Stmt {
        public final boolean on; public final WorldAPI.Vec3 loc;
        public PowerStmt(int line, boolean on, WorldAPI.Vec3 loc) { super(line); this.on = on; this.loc = loc; }
    }

    public static final class ChestStmt extends Stmt {
        public final String op; // add | take
        public final WorldAPI.Vec3 loc; public final String item; public final int count;
        public ChestStmt(int line, String op, WorldAPI.Vec3 loc, String item, int count) {
            super(line); this.op = op; this.loc = loc; this.item = item; this.count = count;
        }
    }

    public static final class ScoreSetStmt extends Stmt {
        public final TargetRef target; public final String objective; public final double value;
        public ScoreSetStmt(int line, TargetRef target, String objective, double value) { super(line); this.target = target; this.objective = objective; this.value = value; }
    }

    public static final class ScoreAddStmt extends Stmt {
        public final TargetRef target; public final String objective; public final double value;
        public ScoreAddStmt(int line, TargetRef target, String objective, double value) { super(line); this.target = target; this.objective = objective; this.value = value; }
    }

    public static final class TeamStmt extends Stmt {
        public final String op; // create | join | leave
        public final TargetRef target; public final String team;
        public TeamStmt(int line, String op, TargetRef target, String team) { super(line); this.op = op; this.target = target; this.team = team; }
    }

    public static final class ActionCallStmt extends Stmt {
        public final String action; public final List<ValueExpr> args; public final boolean passPlayer;
        public ActionCallStmt(int line, String action, List<ValueExpr> args, boolean passPlayer) {
            super(line); this.action = action; this.args = args; this.passPlayer = passPlayer;
        }
    }

    // ------------------------------------------------------------------
    // Guis, books, databases, config files, worlds & player data
    // ------------------------------------------------------------------

    public static final class OpenGuiStmt extends Stmt {
        public final TargetRef target; public final String gui;
        public OpenGuiStmt(int line, TargetRef target, String gui) { super(line); this.target = target; this.gui = gui; }
    }

    public static final class SetGuiSlotStmt extends Stmt {
        public final String gui; public final int slot; public final WorldAPI.ItemSpec spec;
        public SetGuiSlotStmt(int line, String gui, int slot, WorldAPI.ItemSpec spec) {
            super(line); this.gui = gui; this.slot = slot; this.spec = spec;
        }
    }

    public static final class BookCreate extends Stmt {
        public final String name; public final WorldAPI.BookDef book;
        public BookCreate(int line, String name, WorldAPI.BookDef book) { super(line); this.name = name; this.book = book; }
    }

    public static final class GiveBookStmt extends Stmt {
        public final TargetRef target; public final String book;
        public GiveBookStmt(int line, TargetRef target, String book) { super(line); this.target = target; this.book = book; }
    }

    public static final class CreateWorldStmt extends Stmt {
        public final String world;
        public CreateWorldStmt(int line, String world) { super(line); this.world = world; }
    }

    public static final class DeleteWorldStmt extends Stmt {
        public final String world;
        public DeleteWorldStmt(int line, String world) { super(line); this.world = world; }
    }

    public static final class SetWorldWeatherStmt extends Stmt {
        public final String world; public final String weather;
        public SetWorldWeatherStmt(int line, String world, String weather) { super(line); this.world = world; this.weather = weather; }
    }

    public static final class SetWorldTimeStmt extends Stmt {
        public final String world; public final String daypart;
        public SetWorldTimeStmt(int line, String world, String daypart) { super(line); this.world = world; this.daypart = daypart; }
    }

    public static final class CreateDatabaseStmt extends Stmt {
        public final String db;
        public CreateDatabaseStmt(int line, String db) { super(line); this.db = db; }
    }

    public static final class ConfigSetStmt extends Stmt {
        public final String file; public final String key; public final ValueExpr value;
        public ConfigSetStmt(int line, String file, String key, ValueExpr value) {
            super(line); this.file = file; this.key = key; this.value = value;
        }
    }

    /** "set player's health to 10", "set player's hunger to 5" ... */
    public static final class SetPlayerStatStmt extends Stmt {
        /** health | hunger | xp | level */
        public final String stat; public final ValueExpr value;
        public SetPlayerStatStmt(int line, String stat, ValueExpr value) { super(line); this.stat = stat; this.value = value; }
    }

    /** "set player's bossbar to \"Quest\" with progress 50". */
    public static final class SetBossbarStmt extends Stmt {
        public final ValueExpr title; public final ValueExpr progress;
        public SetBossbarStmt(int line, ValueExpr title, ValueExpr progress) {
            super(line); this.title = title; this.progress = progress;
        }
    }

    /** "clear player's bossbar". */
    public static final class ClearBossbarStmt extends Stmt {
        public ClearBossbarStmt(int line) { super(line); }
    }

    // ------------------------------------------------------------------
    // Value expressions
    // ------------------------------------------------------------------

    public abstract static class ValueExpr {
        public final int line;
        ValueExpr(int line) { this.line = line; }
    }

    public static final class NumExpr extends ValueExpr {
        public final double v;
        public NumExpr(int line, double v) { super(line); this.v = v; }
    }

    /** "a plus b": a binary math expression. op is "+", "-", "*" or "/". */
    public static final class BinaryExpr extends ValueExpr {
        public final String op; public final ValueExpr left; public final ValueExpr right;
        public BinaryExpr(int line, String op, ValueExpr left, ValueExpr right) {
            super(line); this.op = op; this.left = left; this.right = right;
        }
    }

    public static final class TextExpr extends ValueExpr {
        public final String v;
        public TextExpr(int line, String v) { super(line); this.v = v; }
    }

    /** A "text ${value} text" string. Parts are String or ValueExpr in order. */
    public static final class TemplateExpr extends ValueExpr {
        public final List<Object> parts;
        public TemplateExpr(int line, List<Object> parts) { super(line); this.parts = parts; }
    }

    public static final class TruthExpr extends ValueExpr {
        public final boolean v;
        public TruthExpr(int line, boolean v) { super(line); this.v = v; }
    }

    public static final class VarTarget {
        public final String kind; // player | world | temp | list | score | database | playerdata
        public final String name;
        /** Extra key for compound targets: database name or key, playerdata key. */
        public final String key;
        public VarTarget(String kind, String name) { this(kind, name, null); }
        public VarTarget(String kind, String name, String key) { this.kind = kind; this.name = name; this.key = key; }
        public String describe() {
            switch (kind) {
                case "player": return "player's " + name;
                case "world": return "world's " + name;
                case "temp": return "temporary " + name;
                case "score": return "score \"" + name + "\"";
                case "database": return "database \"" + name + "\" at \"" + key + "\"";
                case "playerdata": return "player data \"" + name + "\"";
                default: return "list \"" + name + "\"";
            }
        }
    }

    public static final class VarGetExpr extends ValueExpr {
        public final VarTarget target;
        public VarGetExpr(int line, VarTarget target) { super(line); this.target = target; }
    }

    public static final class HealthExpr extends ValueExpr {
        public HealthExpr(int line) { super(line); }
    }

    public static final class HungerExpr extends ValueExpr {
        public HungerExpr(int line) { super(line); }
    }

    public static final class XpExpr extends ValueExpr {
        public XpExpr(int line) { super(line); }
    }

    public static final class LevelExpr extends ValueExpr {
        public LevelExpr(int line) { super(line); }
    }

    public static final class PlayerNameExpr extends ValueExpr {
        public PlayerNameExpr(int line) { super(line); }
    }

    public static final class PlayerWorldExpr extends ValueExpr {
        public PlayerWorldExpr(int line) { super(line); }
    }

    public static final class PlayerCoordExpr extends ValueExpr {
        public final String axis; // x | y | z
        public PlayerCoordExpr(int line, String axis) { super(line); this.axis = axis; }
    }

    public static final class GamemodeExpr extends ValueExpr {
        public GamemodeExpr(int line) { super(line); }
    }

    /** "player's held item": the name of the item in the player's hand. */
    public static final class HeldItemExpr extends ValueExpr {
        public HeldItemExpr(int line) { super(line); }
    }

    public static final class RandomExpr extends ValueExpr {
        public final double a; public final double b;
        public RandomExpr(int line, double a, double b) { super(line); this.a = a; this.b = b; }
    }

    /** "random item from list \"quests\"": a random element of a list. */
    public static final class RandomListExpr extends ValueExpr {
        public final String list;
        public RandomListExpr(int line, String list) { super(line); this.list = list; }
    }

    public static final class OnlineCountExpr extends ValueExpr {
        public OnlineCountExpr(int line) { super(line); }
    }

    public static final class CountItemExpr extends ValueExpr {
        public final String item;
        public CountItemExpr(int line, String item) { super(line); this.item = item; }
    }

    public static final class ScoreGetExpr extends ValueExpr {
        public final String objective;
        public ScoreGetExpr(int line, String objective) { super(line); this.objective = objective; }
    }

    public static final class LengthExpr extends ValueExpr {
        public final String list;
        public LengthExpr(int line, String list) { super(line); this.list = list; }
    }

    public static final class DatabaseGetExpr extends ValueExpr {
        public final String db; public final String key;
        public DatabaseGetExpr(int line, String db, String key) { super(line); this.db = db; this.key = key; }
    }

    public static final class ConfigGetExpr extends ValueExpr {
        public final String file; public final String key;
        public ConfigGetExpr(int line, String file, String key) { super(line); this.file = file; this.key = key; }
    }

    public static final class PlayerDataGetExpr extends ValueExpr {
        public final String key;
        public PlayerDataGetExpr(int line, String key) { super(line); this.key = key; }
    }

    // ------------------------------------------------------------------
    // Conditions
    // ------------------------------------------------------------------

    public abstract static class Condition {}

    public static final class CmpCond extends Condition {
        public final ValueExpr left; public final String op; public final ValueExpr right; // op: >= <= > < == !=
        public CmpCond(ValueExpr left, String op, ValueExpr right) { this.left = left; this.op = op; this.right = right; }
    }

    public static final class HasCond extends Condition {
        public final boolean isItem; public final String name; public final double amount;
        public HasCond(boolean isItem, String name, double amount) { this.isItem = isItem; this.name = name; this.amount = amount; }
    }

    public static final class IsHoldingCond extends Condition {
        public final String item;
        public IsHoldingCond(String item) { this.item = item; }
    }

    public static final class InDimensionCond extends Condition {
        public final String dim;
        public InDimensionCond(String dim) { this.dim = dim; }
    }

    public static final class InRegionCond extends Condition {
        public final String region;
        public InRegionCond(String region) { this.region = region; }
    }

    public static final class InWorldCond extends Condition {
        public final String world;
        public InWorldCond(String world) { this.world = world; }
    }

    public static final class InBiomeCond extends Condition {
        public final String biome;
        public InBiomeCond(String biome) { this.biome = biome; }
    }

    public static final class TouchCond extends Condition {
        public final String block; // the block the player's feet are inside
        public TouchCond(String block) { this.block = block; }
    }

    public static final class WalkCond extends Condition {
        public final String block; // the block below the player
        public WalkCond(String block) { this.block = block; }
    }

    public static final class TimeCond extends Condition {
        public final String daypart;
        public TimeCond(String daypart) { this.daypart = daypart; }
    }

    public static final class WeatherCond extends Condition {
        public final String weather;
        public WeatherCond(String weather) { this.weather = weather; }
    }

    public static final class PermissionCond extends Condition {
        public final String perm;
        public PermissionCond(String perm) { this.perm = perm; }
    }

    public static final class PlayerStateCond extends Condition {
        public final String state; // sneaking | flying | wet | ground | op
        public PlayerStateCond(String state) { this.state = state; }
    }

    public static final class InGamemodeCond extends Condition {
        public final String mode;
        public InGamemodeCond(String mode) { this.mode = mode; }
    }

    public static final class ScoreCond extends Condition {
        public final String objective; public final String op; public final double value;
        public ScoreCond(String objective, String op, double value) { this.objective = objective; this.op = op; this.value = value; }
    }

    public static final class ContainsCond extends Condition {
        public final String list; public final ValueExpr value;
        public ContainsCond(String list, ValueExpr value) { this.list = list; this.value = value; }
    }

    public static final class ChestHasCond extends Condition {
        public final WorldAPI.Vec3 loc; public final String item; public final int count;
        public ChestHasCond(WorldAPI.Vec3 loc, String item, int count) { this.loc = loc; this.item = item; this.count = count; }
    }

    public static final class TruthCond extends Condition {
        public final boolean v;
        public TruthCond(boolean v) { this.v = v; }
    }

    /** "chance 50" (percent) or "chance of 1 in 4" (a/b). */
    public static final class ChanceCond extends Condition {
        public final double percent; // 0..100
        public ChanceCond(double percent) { this.percent = percent; }
    }

    public static final class NotCond extends Condition {
        public final Condition inner;
        public NotCond(Condition inner) { this.inner = inner; }
    }

    public static final class AndCond extends Condition {
        public final List<Condition> parts;
        public AndCond(List<Condition> parts) { this.parts = parts; }
    }

    public static final class OrCond extends Condition {
        public final List<Condition> parts;
        public OrCond(List<Condition> parts) { this.parts = parts; }
    }

    // ------------------------------------------------------------------
    // Triggers
    // ------------------------------------------------------------------

    public static final class Trigger {
        public enum Kind { EVENT, STATE }
        public enum Filter { NONE, ITEM, BLOCK, MOB, REGION, TEXT, MOB_NAME, GUI }

        public final Kind kind;
        /** For EVENT triggers: "joins", "breaks", "mob dies", "custom", ... */
        public final String event;
        public final String filter;
        public final Filter filterType;
        public final List<Condition> conditions;
        /** STATE triggers with a player subject are evaluated for every online player. */
        public final boolean playerSubject;
        /** "when player first joins": fires only the first time a player joins. */
        public boolean first;
        /** For GUI click triggers: the slot, or -1 for any slot. */
        public int guiSlot = -1;

        public Trigger(Kind kind, String event, String filter, Filter filterType,
                       List<Condition> conditions, boolean playerSubject) {
            this.kind = kind;
            this.event = event;
            this.filter = filter;
            this.filterType = filterType;
            this.conditions = conditions;
            this.playerSubject = playerSubject;
        }

        public String describe() {
            StringBuilder sb = new StringBuilder();
            if (kind == Kind.EVENT) {
                sb.append("when ").append(event);
                if (filter != null) sb.append(" ").append(filter);
            } else {
                sb.append("when (state)");
            }
            return sb.toString();
        }
    }
}
