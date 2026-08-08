package dev.hermes.core;

import java.util.List;

/**
 * The bridge between the Hermes interpreter and whatever world it runs in â€”
 * a real Minecraft server (see the Paper plugin) or the headless MockWorld
 * used for demos and tests.
 *
 * <p>Hermes never touches Bukkit directly. It only knows about this interface,
 * which is why the language can run anywhere (Paper today, Fabric/NeoForge
 * tomorrow) without changing a single line of script.
 */
public interface WorldAPI {

    /** A player, however the host world represents it. */
    interface PlayerRef {
        String name();
    }

    /** A mob/entity, however the host world represents it. */
    interface MobRef {
        String name();
    }

    /** A point in the world. */
    record Vec3(double x, double y, double z) {
        public static Vec3 of(double x, double y, double z) { return new Vec3(x, y, z); }
        public Vec3 floor() { return new Vec3(Math.floor(x), Math.floor(y), Math.floor(z)); }
    }

    // ---------- locations ----------
    Vec3 playerLocation(PlayerRef p);
    void teleport(PlayerRef p, Vec3 loc);
    void teleportToSpawn(PlayerRef p);
    Vec3 worldSpawn();

    // ---------- chat ----------
    void sendMessage(PlayerRef p, String msg);
    void broadcast(String msg);
    void welcome(PlayerRef p, String msg);
    void warn(PlayerRef p, String msg);
    void announce(String msg);

    // ---------- stats ----------
    double health(PlayerRef p);
    void kill(PlayerRef p);
    void damage(PlayerRef p, double amount);
    void heal(PlayerRef p, double amount);
    int hunger(PlayerRef p);
    void feed(PlayerRef p, double amount);
    int xp(PlayerRef p);
    void giveXp(PlayerRef p, int amount);
    int level(PlayerRef p);
    void giveLevels(PlayerRef p, int amount);

    // ---------- player state ----------
    void setGamemode(PlayerRef p, String mode);
    String gamemode(PlayerRef p);
    boolean isSneaking(PlayerRef p);
    boolean isOnGround(PlayerRef p);
    boolean isWet(PlayerRef p);
    boolean isFlying(PlayerRef p);
    boolean isOp(PlayerRef p);

    // ---------- inventory ----------
    void giveItem(PlayerRef p, String item, int count);
    boolean takeItem(PlayerRef p, String item, int count);
    int countItem(PlayerRef p, String item);
    boolean isHolding(PlayerRef p, String item);
    void clearInventory(PlayerRef p);

    // ---------- messaging ----------
    void sendTitle(PlayerRef p, String title, String subtitle);
    void sendActionbar(PlayerRef p, String msg);
    void kick(PlayerRef p, String reason);

    // ---------- world events ----------
    void strikeLightning(Vec3 loc);
    void explode(Vec3 loc, double power);
    void launch(PlayerRef p, double amount);

    // ---------- world ----------
    void setWeather(String weather);
    void setTime(String daypart);
    boolean isNight();
    boolean isWeather(String weather);
    String dimensionOf(PlayerRef p);
    String biomeAt(Vec3 loc);

    // ---------- entities ----------
    MobRef spawnMob(String mob, Vec3 loc, String customName);
    void killMob(MobRef m);
    void damageMob(MobRef m, double amount);
    void healMob(MobRef m, double amount);
    void teleportMob(MobRef m, Vec3 loc);
    Vec3 mobLocation(MobRef m);
    String mobType(MobRef m);
    String mobCustomName(MobRef m);

    // ---------- effects ----------
    void effectOnPlayer(PlayerRef p, String effect, int seconds);
    void effectOnMob(MobRef m, String effect, int seconds);
    void removeEffectFromPlayer(PlayerRef p, String effect);
    void removeEffectFromMob(MobRef m, String effect);

    // ---------- blocks & world interactions ----------
    void setBlock(Vec3 loc, String block);
    String blockAt(Vec3 loc);
    void openDoorNear(PlayerRef p);
    void closeDoorNear(PlayerRef p);
    void pressButtonNear(PlayerRef p);
    void pullLeverNear(PlayerRef p);
    void writeSign(Vec3 loc, List<String> lines);
    void powerBlock(Vec3 loc);
    void unpowerBlock(Vec3 loc);
    boolean chestHas(Vec3 loc, String item, int count);
    void chestAdd(Vec3 loc, String item, int count);
    void chestTake(Vec3 loc, String item, int count);

    // ---------- regions ----------
    void defineRegion(String name, Vec3 a, Vec3 b);
    boolean inRegion(String name, Vec3 loc);

    // ---------- sound & particles ----------
    void playSound(String sound, Vec3 loc);
    void spawnParticles(String particle, Vec3 loc);

    // ---------- permissions (managed by Hermes) ----------
    boolean hasPermission(PlayerRef p, String perm);
    void grantPermission(PlayerRef p, String perm);
    void revokePermission(PlayerRef p, String perm);

    // ---------- scoreboards & teams ----------
    int score(PlayerRef p, String objective);
    void setScore(PlayerRef p, String objective, int value);
    void addScore(PlayerRef p, String objective, int delta);
    void createTeam(String name);
    void putInTeam(PlayerRef p, String team);
    void removeFromTeam(PlayerRef p, String team);

    // ---------- general ----------
    List<PlayerRef> onlinePlayers();
    void log(String msg);
    void save();
    void shutdown();
}
