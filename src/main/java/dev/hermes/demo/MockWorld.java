package dev.hermes.demo;

import dev.hermes.core.Dictionary;
import dev.hermes.core.Scheduler;
import dev.hermes.core.WorldAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A headless "world" that implements the WorldAPI in plain memory, so Hermes
 * scripts can be run and tested without Minecraft. Used by the demo console
 * and by the automated tests.
 */
public final class MockWorld implements WorldAPI {

    public static final class MockPlayer implements PlayerRef {
        public final String name;
        public double health = 20;
        public int hunger = 20;
        public int xp = 0;
        public int level = 0;
        public Vec3 location = new Vec3(8, 64, 8);
        public String holding = "nothing";
        public String dimension = "overworld";
        public String gamemode = "survival";
        public boolean sneaking = false;
        public boolean onGround = true;
        public boolean wet = false;
        public boolean flying = false;
        public boolean op = false;
        public boolean frozen = false;
        public boolean sprinting = false;
        public boolean swimming = false;
        public boolean sleeping = false;
        public boolean burning = false;
        public boolean blocking = false;
        public int ping = 0;
        public String ip = "unknown";
        public String target;
        public String lastDamager;
        public String kicked;
        public final Map<String, Integer> inventory = new HashMap<>();
        public final List<String> messages = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<String> titles = new ArrayList<>();
        public final List<String> actionbars = new ArrayList<>();
        public final Map<String, Integer> scores = new HashMap<>();
        public String team;
        public final List<String> effects = new ArrayList<>();
        public String biome = "plains";
        public String bossbar;
        public double bossbarProgress = 100;
        public Vec3 respawn;
        public double walkSpeed = 0.2;
        public double flySpeed = 0.1;
        public final Map<String, String> equipment = new HashMap<>();
        public final List<String> commands = new ArrayList<>();

        public MockPlayer(String name) { this.name = name; }

        @Override public String name() { return name; }
    }

    public static final class MockMob implements MobRef {
        public final String id;
        public final String type;
        public String customName;
        public Vec3 location;
        public double health = 20;

        public MockMob(String id, String type, Vec3 location) {
            this.id = id;
            this.type = type;
            this.location = location;
        }

        @Override public String name() { return id; }
    }

    public final Map<String, MockPlayer> players = new HashMap<>();
    public final List<MockMob> mobs = new ArrayList<>();
    public final List<String> broadcastLog = new ArrayList<>();
    public final List<String> log = new ArrayList<>();
    public final Map<String, Vec3> spawn = new HashMap<>();
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<Vec3, Map<String, Integer>> chests = new HashMap<>();
    private final Map<Vec3, String> blocks = new HashMap<>();
    private boolean raining = false;
    private boolean storming = false;
    private long time = 1000;
    private final Map<String, List<String>> teams = new HashMap<>();
    private final Map<String, String> playerTeams = new HashMap<>();
    private final Map<String, List<String>> permissions = new HashMap<>();
    private final Map<Vec3, String> signText = new HashMap<>();
    private final List<String> sounds = new ArrayList<>();
    public final List<String> particles = new ArrayList<>();

    private static final class Region {
        final String name; final Vec3 min; final Vec3 max;
        Region(String name, Vec3 min, Vec3 max) { this.name = name; this.min = min; this.max = max; }
    }

    // ------------------------------------------------------------------
    // helpers for tests
    // ------------------------------------------------------------------

    public MockPlayer addPlayer(String name) {
        MockPlayer p = new MockPlayer(name);
        players.put(name, p);
        return p;
    }

    public boolean messageContains(String player, String text) {
        MockPlayer p = players.get(player);
        if (p == null) return false;
        for (String m : p.messages) {
            if (m.contains(text)) return true;
        }
        return false;
    }

    public boolean broadcastContains(String text) {
        for (String m : broadcastLog) {
            if (m.contains(text)) return true;
        }
        return false;
    }

    public boolean inBroadcastOrMessages(String player, String text) {
        return messageContains(player, text) || broadcastContains(text);
    }

    /** The block under a player's feet. */
    public void putBlockUnder(Vec3 loc, String block) {
        blocks.put(new Vec3(loc.x(), loc.y() - 1, loc.z()), Dictionary.findItem(block));
    }

    /** The block at a player's feet (for "touches"). */
    public void putBlockAt(Vec3 loc, String block) {
        blocks.put(loc.floor(), Dictionary.findItem(block));
    }

    // ------------------------------------------------------------------
    // WorldAPI
    // ------------------------------------------------------------------

    @Override public Vec3 playerLocation(PlayerRef p) { return player(p).location; }
    @Override public void teleport(PlayerRef p, Vec3 loc) { player(p).location = loc; }
    @Override public void teleportToSpawn(PlayerRef p) { player(p).location = new Vec3(0, 64, 0); }
    @Override public Vec3 worldSpawn() { return new Vec3(0, 64, 0); }

    @Override public void sendMessage(PlayerRef p, String msg) { player(p).messages.add(msg); }
    @Override public void broadcast(String msg) { broadcastLog.add(msg); }
    @Override public void welcome(PlayerRef p, String msg) { player(p).messages.add("[welcome] " + msg); }
    @Override public void warn(PlayerRef p, String msg) { player(p).warnings.add(msg); }
    @Override public void announce(String msg) { broadcastLog.add("[announce] " + msg); }

    @Override public double health(PlayerRef p) { return player(p).health; }
    @Override public void kill(PlayerRef p) { player(p).health = 0; }
    @Override public void damage(PlayerRef p, double amount) { player(p).health = Math.max(0, player(p).health - amount); }
    @Override public void heal(PlayerRef p, double amount) { player(p).health = Math.min(20, player(p).health + amount); }
    @Override public int hunger(PlayerRef p) { return player(p).hunger; }
    @Override public void feed(PlayerRef p, double amount) {
        player(p).hunger = Math.min(20, player(p).hunger + (int) amount);
    }
    @Override public int xp(PlayerRef p) { return player(p).xp; }
    @Override public void giveXp(PlayerRef p, int amount) { player(p).xp += amount; }
    @Override public int level(PlayerRef p) { return player(p).level; }
    @Override public void giveLevels(PlayerRef p, int amount) { player(p).level += amount; }

    @Override public void setHealth(PlayerRef p, double amount) {
        player(p).health = Math.max(0, Math.min(20, amount));
    }
    @Override public void setHunger(PlayerRef p, int amount) {
        player(p).hunger = Math.max(0, Math.min(20, amount));
    }
    @Override public void setXp(PlayerRef p, int amount) { player(p).xp = Math.max(0, amount); }
    @Override public void setLevel(PlayerRef p, int amount) { player(p).level = Math.max(0, amount); }

    @Override public void setBossbar(PlayerRef p, String title, double progress) {
        player(p).bossbar = title;
        player(p).bossbarProgress = Math.max(0, Math.min(100, progress));
    }
    @Override public void clearBossbar(PlayerRef p) {
        player(p).bossbar = null;
        player(p).bossbarProgress = 100;
    }
    @Override public String heldItem(PlayerRef p) {
        return player(p).holding == null ? "nothing" : player(p).holding;
    }

    @Override public void setGamemode(PlayerRef p, String mode) { player(p).gamemode = mode; }
    @Override public String gamemode(PlayerRef p) { return player(p).gamemode; }
    @Override public boolean isSneaking(PlayerRef p) { return player(p).sneaking; }
    @Override public boolean isOnGround(PlayerRef p) { return player(p).onGround; }
    @Override public boolean isWet(PlayerRef p) { return player(p).wet; }
    @Override public boolean isFlying(PlayerRef p) { return player(p).flying; }
    @Override public boolean isOp(PlayerRef p) { return player(p).op; }
    @Override public void setFrozen(PlayerRef p, boolean frozen) { player(p).frozen = frozen; }
    @Override public boolean isFrozen(PlayerRef p) { return player(p).frozen; }
    @Override public boolean isSprinting(PlayerRef p) { return player(p).sprinting; }
    @Override public boolean isSwimming(PlayerRef p) { return player(p).swimming; }
    @Override public boolean isSleeping(PlayerRef p) { return player(p).sleeping; }
    @Override public boolean isBurning(PlayerRef p) { return player(p).burning; }
    @Override public boolean isBlocking(PlayerRef p) { return player(p).blocking; }
    @Override public int ping(PlayerRef p) { return player(p).ping; }
    @Override public String ip(PlayerRef p) { return player(p).ip; }
    @Override public String targetEntity(PlayerRef p) { return player(p).target; }
    @Override public String lastDamager(PlayerRef p) { return player(p).lastDamager; }
    @Override public String blockStandingIn(PlayerRef p) {
        Vec3 loc = player(p).location;
        return blockAt(new Vec3(loc.x(), loc.y(), loc.z()));
    }

    @Override public void clearInventory(PlayerRef p) { player(p).inventory.clear(); }

    @Override public void sendTitle(PlayerRef p, String title, String subtitle) {
        player(p).titles.add((title == null ? "" : title) + "|" + (subtitle == null ? "" : subtitle));
    }
    @Override public void sendActionbar(PlayerRef p, String msg) { player(p).actionbars.add(msg); }
    @Override public void kick(PlayerRef p, String reason) { player(p).kicked = reason; }

    @Override public void strikeLightning(Vec3 loc) {
        log.add("lightning@" + (long) loc.x() + "," + (long) loc.z());
    }
    @Override public void explode(Vec3 loc, double power) {
        log.add("explosion@" + (long) loc.x() + "," + (long) loc.z() + " power " + power);
    }
    @Override public void launch(PlayerRef p, double amount) { log.add("launched " + p.name() + " by " + amount); }
    @Override public void push(PlayerRef p, String direction, double strength) {
        log.add("pushed " + p.name() + " " + direction + " by " + strength);
    }
    @Override public void dropItems(Vec3 loc, ItemSpec spec) {
        String canonical = Dictionary.findItem(spec.item());
        if (canonical == null) canonical = spec.item();
        log.add("dropped " + canonical + " x" + spec.count() + "@" + (long) loc.x() + "," + (long) loc.z());
    }
    @Override public void launchFirework(Vec3 loc) {
        log.add("firework@" + (long) loc.x() + "," + (long) loc.z());
    }
    @Override public void runCommand(PlayerRef p, String command) { player(p).commands.add(command); }
    @Override public void sendResourcePack(PlayerRef p, String url) { log.add("resourcePack " + p.name() + " <- " + url); }
    @Override public void setRespawnPoint(PlayerRef p, Vec3 loc) { player(p).respawn = loc; }
    @Override public void swingHand(PlayerRef p) { log.add("swung " + p.name() + " hand"); }
    @Override public void lookAt(PlayerRef p, Vec3 loc) {
        log.add("looked " + p.name() + " at " + (long) loc.x() + "," + (long) loc.z());
    }
    @Override public void setSpeed(PlayerRef p, String kind, double speed) {
        if (kind.equals("fly")) player(p).flySpeed = speed;
        else player(p).walkSpeed = speed;
    }
    @Override public void setEquipment(PlayerRef p, String slot, ItemSpec spec) {
        String canonical = Dictionary.findItem(spec.item());
        if (canonical == null) canonical = spec.item();
        player(p).equipment.put(slot, canonical);
    }

    @Override public void giveItem(PlayerRef p, String item, int count) {
        String canonical = Dictionary.findItem(item);
        if (canonical == null) canonical = item;
        player(p).inventory.merge(canonical, count, Integer::sum);
    }

    @Override public void giveItemSpec(PlayerRef p, ItemSpec spec) {
        giveItem(p, spec.item(), spec.count());
    }

    @Override public void giveBook(PlayerRef p, BookDef book) {
        player(p).messages.add("[book] " + book.title() + " by " + book.author());
    }

    @Override public boolean takeItem(PlayerRef p, String item, int count) {
        String canonical = Dictionary.findItem(item);
        if (canonical == null) canonical = item;
        int have = player(p).inventory.getOrDefault(canonical, 0);
        if (have < count) return false;
        if (have == count) player(p).inventory.remove(canonical);
        else player(p).inventory.put(canonical, have - count);
        return true;
    }

    @Override public int countItem(PlayerRef p, String item) {
        String canonical = Dictionary.findItem(item);
        if (canonical == null) canonical = item;
        return player(p).inventory.getOrDefault(canonical, 0);
    }

    @Override public boolean isHolding(PlayerRef p, String item) {
        return Dictionary.findItem(player(p).holding) == null
                ? player(p).holding.equals(item)
                : Dictionary.findItem(item).equals(Dictionary.findItem(player(p).holding));
    }

    @Override public void setWeather(String weather) {
        raining = weather.equals("rain") || weather.equals("storm");
        storming = weather.equals("storm");
    }

    @Override public void setTime(String daypart) {
        switch (daypart) {
            case "dawn": time = 0; break;
            case "day": case "morning": time = 1000; break;
            case "noon": time = 6000; break;
            case "afternoon": time = 9000; break;
            case "dusk": case "evening": time = 13000; break;
            case "night": time = 14000; break;
            case "midnight": time = 18000; break;
            default: break;
        }
    }

    @Override public boolean isNight() { return time >= 13000 || time < 0; }

    @Override public boolean isWeather(String weather) {
        return weather.equals("rain") ? raining : weather.equals("storm") ? storming : !raining;
    }

    private final Map<String, Boolean> worldWeather = new HashMap<>();
    private final Map<String, Long> worldTime = new HashMap<>();
    private final Map<String, Boolean> worlds = new HashMap<>();
    private final Map<String, Map<String, String>> configFiles = new HashMap<>();

    @Override public void setWorldWeather(String world, String weather) {
        worldWeather.put(world, weather.equals("rain") || weather.equals("storm"));
    }
    @Override public void setWorldTime(String world, String daypart) {
        worldTime.put(world, (long) time);
    }
    @Override public String playerWorld(PlayerRef p) { return player(p).dimension; }
    @Override public void createWorld(String name) { worlds.put(name, true); }
    @Override public void deleteWorld(String name) { worlds.remove(name); }
    @Override public boolean worldExists(String name) { return worlds.containsKey(name); }

    @Override public String configValue(String file, String key) {
        Map<String, String> m = configFiles.get(file);
        return m != null ? m.getOrDefault(key, "") : "";
    }
    @Override public void setConfigValue(String file, String key, String value) {
        configFiles.computeIfAbsent(file, k -> new HashMap<>()).put(key, value);
    }

    @Override public void openGui(PlayerRef p, String title, List<ItemSpec> slots) {
        player(p).messages.add("[gui] " + title);
    }
    @Override public void closeGui(PlayerRef p) { }

    @Override public String dimensionOf(PlayerRef p) { return player(p).dimension; }
    @Override public String biomeAt(Vec3 loc) {
        MockPlayer p = playerAt(loc);
        return p != null ? p.biome : "unknown";
    }

    @Override public MobRef spawnMob(String mob, Vec3 loc, String customName) {
        MockMob m = new MockMob("mob" + (mobs.size() + 1), mob, loc);
        m.customName = customName;
        mobs.add(m);
        return m;
    }

    @Override public void killMob(MobRef m) { ((MockMob) m).health = 0; }
    @Override public void damageMob(MobRef m, double amount) { ((MockMob) m).health -= amount; }
    @Override public void healMob(MobRef m, double amount) { ((MockMob) m).health += amount; }
    @Override public void teleportMob(MobRef m, Vec3 loc) { ((MockMob) m).location = loc; }
    @Override public Vec3 mobLocation(MobRef m) { return ((MockMob) m).location; }
    @Override public String mobType(MobRef m) { return ((MockMob) m).type; }
    @Override public String mobCustomName(MobRef m) { return ((MockMob) m).customName; }

    @Override public void effectOnPlayer(PlayerRef p, String effect, int seconds) {
        player(p).effects.add(effect + ":" + seconds);
    }
    @Override public void effectOnMob(MobRef m, String effect, int seconds) {
        ((MockMob) m).health += 0; // effects on mobs are not tracked in the mock
    }
    @Override public void removeEffectFromPlayer(PlayerRef p, String effect) {
        player(p).effects.removeIf(e -> e.startsWith(effect + ":"));
    }
    @Override public void removeEffectFromMob(MobRef m, String effect) { }

    @Override public void setBlock(Vec3 loc, String block) {
        blocks.put(loc.floor(), Dictionary.findItem(block));
    }
    @Override public String blockAt(Vec3 loc) {
        String b = blocks.get(loc.floor());
        return b != null ? b : "air";
    }
    @Override public void openDoorNear(PlayerRef p) { log.add("door opened near " + p.name()); }
    @Override public void closeDoorNear(PlayerRef p) { log.add("door closed near " + p.name()); }
    @Override public void pressButtonNear(PlayerRef p) { log.add("button pressed near " + p.name()); }
    @Override public void pullLeverNear(PlayerRef p) { log.add("lever pulled near " + p.name()); }
    @Override public void writeSign(Vec3 loc, List<String> lines) {
        signText.put(loc.floor(), String.join("|", lines));
    }
    @Override public void powerBlock(Vec3 loc) { blocks.put(loc.floor(), Dictionary.findItem("redstone lamp")); }
    @Override public void unpowerBlock(Vec3 loc) { blocks.remove(loc.floor()); }
    @Override public boolean chestHas(Vec3 loc, String item, int count) {
        Map<String, Integer> c = chests.get(loc.floor());
        return c != null && c.getOrDefault(Dictionary.findItem(item), 0) >= count;
    }
    @Override public void chestAdd(Vec3 loc, String item, int count) {
        chests.computeIfAbsent(loc.floor(), k -> new HashMap<>())
                .merge(Dictionary.findItem(item), count, Integer::sum);
    }
    @Override public void chestTake(Vec3 loc, String item, int count) {
        Map<String, Integer> c = chests.get(loc.floor());
        if (c == null) return;
        String canonical = Dictionary.findItem(item);
        int have = c.getOrDefault(canonical, 0);
        if (have <= count) c.remove(canonical);
        else c.put(canonical, have - count);
    }

    @Override public void defineRegion(String name, Vec3 a, Vec3 b) {
        regions.put(name, new Region(name, a, b));
    }
    @Override public void undefineRegion(String name) {
        regions.remove(name);
    }

    @Override public boolean inRegion(String name, Vec3 loc) {
        Region r = regions.get(name);
        if (r == null) return false;
        return loc.x() >= r.min.x() && loc.x() <= r.max.x()
                && loc.y() >= r.min.y() && loc.y() <= r.max.y()
                && loc.z() >= r.min.z() && loc.z() <= r.max.z();
    }

    @Override public void playSound(String sound, Vec3 loc) { sounds.add(sound + "@" + (long) loc.x() + "," + (long) loc.z()); }
    @Override public void spawnParticles(String particle, Vec3 loc) { spawnParticles(particle, loc, 30, 1.0); }
    @Override public void spawnParticles(String particle, Vec3 loc, int count, double size) {
        particles.add(particle + "@" + (long) loc.x() + "," + (long) loc.z() + " x" + count + " size " + size);
    }

    @Override public boolean hasPermission(PlayerRef p, String perm) {
        List<String> perms = permissions.get(p.name());
        return perms != null && perms.contains(perm);
    }
    @Override public void grantPermission(PlayerRef p, String perm) {
        permissions.computeIfAbsent(p.name(), k -> new ArrayList<>()).add(perm);
    }
    @Override public void revokePermission(PlayerRef p, String perm) {
        List<String> perms = permissions.get(p.name());
        if (perms != null) perms.remove(perm);
    }

    @Override public int score(PlayerRef p, String objective) {
        return player(p).scores.getOrDefault(objective, 0);
    }
    @Override public void setScore(PlayerRef p, String objective, int value) {
        player(p).scores.put(objective, value);
    }
    @Override public void addScore(PlayerRef p, String objective, int delta) {
        player(p).scores.merge(objective, delta, Integer::sum);
    }
    @Override public void createTeam(String name) {
        teams.putIfAbsent(name, new ArrayList<>());
    }
    @Override public void putInTeam(PlayerRef p, String team) {
        playerTeams.put(p.name(), team);
        teams.computeIfAbsent(team, k -> new ArrayList<>()).add(p.name());
        ((MockPlayer) p).team = team;
    }
    @Override public void removeFromTeam(PlayerRef p, String team) {
        List<String> members = teams.get(team);
        if (members != null) members.remove(p.name());
        playerTeams.remove(p.name());
        ((MockPlayer) p).team = null;
    }

    @Override public List<PlayerRef> onlinePlayers() {
        return new ArrayList<>(players.values());
    }
    @Override public void log(String msg) { log.add(msg); }
    @Override public void save() { }
    @Override public void shutdown() { }

    // ------------------------------------------------------------------

    private MockPlayer player(PlayerRef p) {
        MockPlayer mp = players.get(p.name());
        if (mp == null) throw new IllegalArgumentException("Unknown player: " + p.name());
        return mp;
    }

    private MockPlayer playerAt(Vec3 loc) {
        for (MockPlayer p : players.values()) {
            if (p.location.floor().equals(loc.floor())) return p;
        }
        return null;
    }

    /** A scheduler driven by simulated time (used by the demo console). */
    public static final class MockScheduler implements Scheduler {
        private final List<Task> everyTasks = new ArrayList<>();
        private final List<Task> laterTasks = new ArrayList<>();
        private long now = 0;

        private static final class Task {
            final long interval; final Runnable runnable; long last;
            Task(long interval, Runnable runnable) { this.interval = interval; this.runnable = runnable; }
        }

        @Override public void runEvery(long millis, Runnable task) {
            everyTasks.add(new Task(millis, task));
        }

        @Override public void cancelEvery(Runnable task) {
            everyTasks.removeIf(t -> t.runnable == task);
        }

        @Override public void runLater(long millis, Runnable task) {
            laterTasks.add(new Task(millis, task));
        }

        /** Advances simulated time and runs everything that became due. */
        public void advance(long millis) {
            now += millis;
            for (Task t : laterTasks) {
                if (t.last == 0) { t.last = now; }
                if (now - t.last >= t.interval) {
                    t.last = now;
                    t.runnable.run();
                }
            }
            for (Task t : everyTasks) {
                while (now - t.last >= t.interval) {
                    t.last += t.interval;
                    t.runnable.run();
                }
            }
        }

        /** Runs any due work without advancing time. */
        public void flush() {
            for (Task t : laterTasks) {
                t.runnable.run();
            }
            laterTasks.clear();
        }

        /** Number of repeating tasks still scheduled. */
        public int everyTaskCount() {
            return everyTasks.size();
        }
    }
}
