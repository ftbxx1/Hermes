package dev.hermes.plugin;

import dev.hermes.core.Dictionary;
import dev.hermes.core.WorldAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The WorldAPI, wired to a live Paper server. This is where every Hermes
 * action actually happens: items are given, doors open, mobs spawn.
 *
 * <p>Everything runs on the main thread (the engine only ever calls us from
 * Bukkit events, scheduled tasks, or the tick task), so Bukkit calls are
 * safe without any locking.
 */
public final class BukkitWorld implements WorldAPI {

    private final HermesPlugin plugin;
    private final Scoreboard scoreboard;
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<UUID, List<org.bukkit.permissions.PermissionAttachment>> attachments = new HashMap<>();
    private final Map<UUID, org.bukkit.boss.BossBar> bossbars = new HashMap<>();

    private static final class Region {
        final Vec3 min;
        final Vec3 max;
        Region(Vec3 min, Vec3 max) { this.min = min; this.max = max; }
    }

    public BukkitWorld(HermesPlugin plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    }

    // ------------------------------------------------------------------
    // players & entities
    // ------------------------------------------------------------------

    static final class BukkitPlayer implements PlayerRef {
        private final Player player;
        BukkitPlayer(Player player) { this.player = player; }
        Player player() { return player; }
        @Override public String name() { return player.getName(); }
    }

    static final class BukkitMob implements MobRef {
        private final Entity entity;
        private final String type;
        BukkitMob(Entity entity, String type) { this.entity = entity; this.type = type; }
        Entity entity() { return entity; }
        @Override public String name() { return type + "@" + entity.getEntityId(); }
    }

    private static Player playerOf(PlayerRef p) {
        return ((BukkitPlayer) p).player();
    }

    private static Entity mobOf(MobRef m) {
        return ((BukkitMob) m).entity();
    }

    private World defaultWorld() {
        return Bukkit.getWorlds().get(0);
    }

    private static Location toBukkit(World world, Vec3 loc) {
        return new Location(world, loc.x(), loc.y(), loc.z());
    }

    private static Vec3 toHermes(Location loc) {
        return new Vec3(loc.getX(), loc.getY(), loc.getZ());
    }

    // ------------------------------------------------------------------
    // name resolution: Hermes friendly names <-> Minecraft keys
    // ------------------------------------------------------------------

    /** "diamond ore" -> Material.DIAMOND_ORE (or null). */
    private static Material materialOf(String friendly) {
        String canonical = Dictionary.findItem(friendly);
        if (canonical == null) return null;
        return Material.matchMaterial(canonical.replace(' ', '_'));
    }

    /** The friendly Hermes name of a block, e.g. "coal ore" for COAL_ORE. */
    private static String friendlyBlock(Block block) {
        String key = block.getType().getKey().getKey();
        String friendly = Dictionary.findItem(key);
        return friendly != null ? friendly : key;
    }

    /** The friendly Hermes name of a mob type, e.g. "zombie". */
    private static String friendlyMob(Entity e) {
        String key = e.getType().getKey().getKey();
        String friendly = Dictionary.findMob(key);
        return friendly != null ? friendly : key;
    }

    /** "speed" -> PotionEffectType.SPEED (or null). */
    private static PotionEffectType effectTypeOf(String friendly) {
        String canonical = Dictionary.findEffect(friendly);
        if (canonical == null) return null;
        String raw = Dictionary.effectKey(canonical);
        PotionEffectType t = Registry.EFFECT.get(NamespacedKey.minecraft(raw.toLowerCase(Locale.ROOT)));
        if (t != null) return t;
        try {
            return PotionEffectType.getByName(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** "level up" -> Sound.ENTITY_PLAYER_LEVELUP. */
    private static Sound soundOf(String friendly) {
        String canonical = Dictionary.findSound(friendly);
        if (canonical == null) canonical = friendly;
        String raw = Dictionary.soundKey(canonical);
        Sound s = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(
                raw.toLowerCase(Locale.ROOT).replace('_', '.')));
        if (s != null) return s;
        try {
            return Sound.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return Sound.UI_BUTTON_CLICK;
        }
    }

    /** "heart" -> Particle.HEART. */
    private static Particle particleOf(String friendly) {
        String canonical = Dictionary.findParticle(friendly);
        if (canonical == null) canonical = friendly;
        String raw = Dictionary.particleKey(canonical);
        try {
            return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Particle.CLOUD;
        }
    }

    // ------------------------------------------------------------------
    // locations
    // ------------------------------------------------------------------

    @Override public Vec3 playerLocation(PlayerRef p) { return toHermes(playerOf(p).getLocation()); }
    @Override public void teleport(PlayerRef p, Vec3 loc) { playerOf(p).teleport(toBukkit(playerOf(p).getWorld(), loc)); }
    @Override public void teleportToSpawn(PlayerRef p) { playerOf(p).teleport(playerOf(p).getWorld().getSpawnLocation()); }
    @Override public Vec3 worldSpawn() { return toHermes(defaultWorld().getSpawnLocation()); }

    // ------------------------------------------------------------------
    // chat
    // ------------------------------------------------------------------

    @Override public void sendMessage(PlayerRef p, String msg) { playerOf(p).sendMessage(msg); }
    @Override public void broadcast(String msg) { Bukkit.broadcastMessage(msg); }
    @Override public void welcome(PlayerRef p, String msg) {
        Player player = playerOf(p);
        player.sendMessage("§6[§eHermes§6] §f" + msg);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }
    @Override public void warn(PlayerRef p, String msg) {
        playerOf(p).sendMessage("§c[!] §7" + msg);
        playerOf(p).playSound(playerOf(p).getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.7f);
    }
    @Override public void announce(String msg) {
        Bukkit.broadcastMessage("§d[Hermes] §f" + msg);
    }

    // ------------------------------------------------------------------
    // stats
    // ------------------------------------------------------------------

    @Override public double health(PlayerRef p) { return playerOf(p).getHealth(); }
    @Override public void kill(PlayerRef p) { playerOf(p).setHealth(0); }
    @Override public void damage(PlayerRef p, double amount) { playerOf(p).damage(amount); }
    @Override public void heal(PlayerRef p, double amount) {
        Player pl = playerOf(p);
        pl.setHealth(Math.min(pl.getHealth() + amount, pl.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
    }
    @Override public int hunger(PlayerRef p) { return playerOf(p).getFoodLevel(); }
    @Override public void feed(PlayerRef p, double amount) {
        Player pl = playerOf(p);
        pl.setFoodLevel(Math.min(20, pl.getFoodLevel() + (int) amount));
        pl.setSaturation(Math.min(20f, pl.getSaturation() + (int) amount));
    }
    @Override public int xp(PlayerRef p) { return (int) playerOf(p).getExp() * 100; }
    @Override public void giveXp(PlayerRef p, int amount) { playerOf(p).giveExp(amount); }
    @Override public int level(PlayerRef p) { return playerOf(p).getLevel(); }
    @Override public void giveLevels(PlayerRef p, int amount) { playerOf(p).giveExpLevels(amount); }

    @Override public void setHealth(PlayerRef p, double amount) {
        Player pl = playerOf(p);
        double max = pl.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        pl.setHealth(Math.max(0, Math.min(amount, max)));
    }
    @Override public void setHunger(PlayerRef p, int amount) {
        playerOf(p).setFoodLevel(Math.max(0, Math.min(20, amount)));
    }
    @Override public void setXp(PlayerRef p, int amount) {
        Player pl = playerOf(p);
        pl.setExp(0);
        pl.setLevel(0);
        pl.setTotalExperience(Math.max(0, amount));
    }
    @Override public void setLevel(PlayerRef p, int amount) {
        playerOf(p).setLevel(Math.max(0, amount));
    }

    @Override public void setBossbar(PlayerRef p, String title, double progress) {
        Player pl = playerOf(p);
        org.bukkit.boss.BossBar bar = bossbars.get(pl.getUniqueId());
        if (bar == null) {
            NamespacedKey key = new NamespacedKey(plugin, "bossbar-" + pl.getUniqueId());
            bar = Bukkit.createBossBar(key, title, org.bukkit.boss.BarColor.PURPLE, org.bukkit.boss.BarStyle.SOLID);
            bossbars.put(pl.getUniqueId(), bar);
        }
        bar.setTitle(title);
        bar.setProgress(Math.max(0, Math.min(100, progress)) / 100.0);
        bar.addPlayer(pl);
    }
    @Override public void clearBossbar(PlayerRef p) {
        Player pl = playerOf(p);
        org.bukkit.boss.BossBar bar = bossbars.remove(pl.getUniqueId());
        if (bar != null) {
            bar.removePlayer(pl);
            Bukkit.removeBossBar(new NamespacedKey(plugin, "bossbar-" + pl.getUniqueId()));
        }
    }
    @Override public String heldItem(PlayerRef p) {
        ItemStack hand = playerOf(p).getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return "nothing";
        String key = hand.getType().getKey().getKey();
        String friendly = Dictionary.findItem(key);
        return friendly != null ? friendly : key.replace('_', ' ');
    }

    // ------------------------------------------------------------------
    // player state
    // ------------------------------------------------------------------

    @Override public void setGamemode(PlayerRef p, String mode) {
        try {
            playerOf(p).setGameMode(GameMode.valueOf(mode.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Hermes: unknown gamemode '" + mode + "'");
        }
    }

    @Override public String gamemode(PlayerRef p) {
        return playerOf(p).getGameMode().name().toLowerCase(Locale.ROOT);
    }

    @Override public boolean isSneaking(PlayerRef p) { return playerOf(p).isSneaking(); }
    @Override public boolean isOnGround(PlayerRef p) { return playerOf(p).isOnGround(); }
    @Override public boolean isWet(PlayerRef p) { return playerOf(p).isInWater() || playerOf(p).isInRain(); }
    @Override public boolean isFlying(PlayerRef p) { return playerOf(p).isFlying(); }
    @Override public boolean isOp(PlayerRef p) { return playerOf(p).isOp(); }
    @Override public void setFrozen(PlayerRef p, boolean frozen) {
        Player player = playerOf(p);
        player.setWalkSpeed(frozen ? 0f : 0.2f);
        player.setFlySpeed(frozen ? 0f : 0.1f);
    }
    @Override public boolean isFrozen(PlayerRef p) { return playerOf(p).getWalkSpeed() == 0f; }

    // ------------------------------------------------------------------
    // inventory
    // ------------------------------------------------------------------

    @Override public void giveItem(PlayerRef p, String item, int count) {
        Material m = materialOf(item);
        if (m == null) {
            plugin.getLogger().warning("Hermes: unknown item '" + item + "'");
            return;
        }
        ItemStack stack = new ItemStack(m, count);
        Map<Integer, ItemStack> leftover = playerOf(p).getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            World world = playerOf(p).getWorld();
            for (ItemStack rest : leftover.values()) world.dropItemNaturally(playerOf(p).getLocation(), rest);
        }
    }

    @Override public boolean takeItem(PlayerRef p, String item, int count) {
        Material m = materialOf(item);
        if (m == null) return false;
        return playerOf(p).getInventory().removeItem(new ItemStack(m, count)).isEmpty();
    }

    @Override public int countItem(PlayerRef p, String item) {
        Material m = materialOf(item);
        if (m == null) return 0;
        int total = 0;
        for (ItemStack s : playerOf(p).getInventory().getContents()) {
            if (s != null && s.getType() == m) total += s.getAmount();
        }
        return total;
    }

    @Override public boolean isHolding(PlayerRef p, String item) {
        Material m = materialOf(item);
        if (m == null) return false;
        return playerOf(p).getInventory().getItemInMainHand().getType() == m;
    }

    @Override public void giveItemSpec(PlayerRef p, ItemSpec spec) {
        Material m = materialOf(spec.item());
        if (m == null) {
            plugin.getLogger().warning("Hermes: unknown item '" + spec.item() + "'");
            return;
        }
        ItemStack stack = new ItemStack(m, spec.count());
        if (spec.name() != null) {
            stack.editMeta(meta -> meta.displayName(Component.text(spec.name())));
        }
        if (!spec.lore().isEmpty()) {
            stack.editMeta(meta -> meta.lore(spec.lore().stream().map(Component::text).toList()));
        }
        if (!spec.enchants().isEmpty()) {
            stack.editMeta(meta -> {
                for (EnchantSpec e : spec.enchants()) {
                    org.bukkit.enchantments.Enchantment en = enchantmentOf(e.enchant());
                    if (en != null) meta.addEnchant(en, Math.max(1, e.level()), true);
                }
            });
        }
        Map<Integer, ItemStack> leftover = playerOf(p).getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            World world = playerOf(p).getWorld();
            for (ItemStack rest : leftover.values()) world.dropItemNaturally(playerOf(p).getLocation(), rest);
        }
    }

    @Override public void giveBook(PlayerRef p, BookDef book) {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
        stack.editMeta(meta -> {
            if (meta instanceof org.bukkit.inventory.meta.BookMeta bm) {
                bm.setTitle(book.title());
                bm.setAuthor(book.author());
                for (String page : book.pages()) bm.addPage(page);
            }
        });
        Map<Integer, ItemStack> leftover = playerOf(p).getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            World world = playerOf(p).getWorld();
            for (ItemStack rest : leftover.values()) world.dropItemNaturally(playerOf(p).getLocation(), rest);
        }
    }

    private static org.bukkit.enchantments.Enchantment enchantmentOf(String friendly) {
        String canonical = Dictionary.findEnchant(friendly);
        if (canonical == null) canonical = friendly;
        NamespacedKey key = NamespacedKey.minecraft(canonical.toLowerCase(Locale.ROOT));
        return Registry.ENCHANTMENT.get(key);
    }

    @Override public void clearInventory(PlayerRef p) {
        playerOf(p).getInventory().clear();
    }

    // ------------------------------------------------------------------
    // messaging & kicking
    // ------------------------------------------------------------------

    @Override public void sendTitle(PlayerRef p, String title, String subtitle) {
        playerOf(p).sendTitle(title == null ? "" : title,
                subtitle == null ? "" : subtitle, 10, 40, 10);
    }

    @Override public void sendActionbar(PlayerRef p, String msg) {
        playerOf(p).sendActionBar(msg);
    }

    @Override public void kick(PlayerRef p, String reason) {
        playerOf(p).kick(Component.text(reason != null ? reason : "Kicked by Hermes"));
    }

    // ------------------------------------------------------------------
    // world events
    // ------------------------------------------------------------------

    @Override public void strikeLightning(Vec3 loc) {
        World w = defaultWorld();
        w.strikeLightning(toBukkit(w, loc));
    }

    @Override public void explode(Vec3 loc, double power) {
        World w = defaultWorld();
        w.createExplosion(toBukkit(w, loc), (float) power, false, false);
    }

    @Override public void launch(PlayerRef p, double amount) {
        playerOf(p).setVelocity(playerOf(p).getVelocity().add(
                new org.bukkit.util.Vector(0, amount, 0)));
    }

    // ------------------------------------------------------------------
    // world
    // ------------------------------------------------------------------

    @Override public void setWeather(String weather) {
        for (World w : Bukkit.getWorlds()) {
            switch (weather) {
                case "rain":
                case "storm":
                    w.setStorm(true);
                    w.setWeatherDuration(20 * 60 * 5);
                    break;
                default:
                    w.setStorm(false);
                    w.setClearWeatherDuration(20 * 60 * 5);
                    break;
            }
        }
    }

    @Override public void setTime(String daypart) {
        long t;
        switch (daypart) {
            case "dawn": t = 0; break;
            case "noon": t = 6000; break;
            case "afternoon": t = 9000; break;
            case "dusk": case "evening": t = 13000; break;
            case "night": t = 14000; break;
            case "midnight": t = 18000; break;
            default: t = 1000; break; // day / morning
        }
        defaultWorld().setTime(t);
    }

    @Override public boolean isNight() {
        long t = defaultWorld().getTime();
        return t >= 13000 && t < 23000;
    }

    @Override public boolean isWeather(String weather) {
        World w = defaultWorld();
        switch (weather) {
            case "rain":
            case "storm":
                return w.hasStorm();
            default:
                return w.isClearWeather();
        }
    }

    @Override public String dimensionOf(PlayerRef p) {
        switch (playerOf(p).getWorld().getEnvironment()) {
            case NETHER: return "nether";
            case THE_END: return "the end";
            default: return "overworld";
        }
    }

    @Override public String biomeAt(Vec3 loc) {
        NamespacedKey key = defaultWorld().getBiome((int) loc.x(), (int) loc.y(), (int) loc.z()).getKey();
        String friendly = Dictionary.findBiome(key.getKey().replace('_', ' '));
        return friendly != null ? friendly : key.getKey();
    }

    @Override public void setWorldWeather(String worldName, String weather) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return;
        if (weather.equals("rain") || weather.equals("storm")) {
            w.setStorm(true);
            w.setWeatherDuration(20 * 60 * 5);
        } else {
            w.setStorm(false);
            w.setClearWeatherDuration(20 * 60 * 5);
        }
    }

    @Override public void setWorldTime(String worldName, String daypart) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return;
        long t;
        switch (daypart) {
            case "dawn": t = 0; break;
            case "noon": t = 6000; break;
            case "afternoon": t = 9000; break;
            case "dusk": case "evening": t = 13000; break;
            case "night": t = 14000; break;
            case "midnight": t = 18000; break;
            default: t = 1000; break;
        }
        w.setTime(t);
    }

    @Override public String playerWorld(PlayerRef p) {
        return playerOf(p).getWorld().getName();
    }

    @Override public void createWorld(String name) {
        if (Bukkit.getWorld(name) != null) return;
        WorldCreator creator = new WorldCreator(name);
        creator.createWorld();
    }

    @Override public void deleteWorld(String name) {
        World w = Bukkit.getWorld(name);
        if (w == null) return;
        Bukkit.unloadWorld(w, true);
    }

    @Override public boolean worldExists(String name) {
        return Bukkit.getWorld(name) != null;
    }

    @Override public String configValue(String file, String key) {
        return plugin.getConfig().getString(key, "");
    }

    @Override public void setConfigValue(String file, String key, String value) {
        plugin.getConfig().set(key, value);
        plugin.saveConfig();
    }

    @Override public void openGui(PlayerRef p, String title, List<ItemSpec> slots) {
        Inventory inv = Bukkit.createInventory(null, inventorySize(slots.size()), Component.text(title));
        for (int i = 0; i < slots.size() && i < inv.getSize(); i++) {
            ItemSpec spec = slots.get(i);
            if (spec == null) continue;
            inv.setItem(i, toItemStack(spec));
        }
        playerOf(p).openInventory(inv);
    }

    @Override public void closeGui(PlayerRef p) {
        playerOf(p).closeInventory();
    }

    private static int inventorySize(int slots) {
        int size = 9;
        while (size < slots && size < 54) size += 9;
        return size;
    }

    private static ItemStack toItemStack(ItemSpec spec) {
        Material m = materialOf(spec.item());
        if (m == null) m = Material.STONE;
        ItemStack stack = new ItemStack(m, Math.max(1, spec.count()));
        if (spec.name() != null) {
            stack.editMeta(meta -> meta.displayName(Component.text(spec.name())));
        }
        if (!spec.lore().isEmpty()) {
            stack.editMeta(meta -> meta.lore(spec.lore().stream().map(Component::text).toList()));
        }
        if (!spec.enchants().isEmpty()) {
            stack.editMeta(meta -> {
                for (EnchantSpec e : spec.enchants()) {
                    org.bukkit.enchantments.Enchantment en = enchantmentOf(e.enchant());
                    if (en != null) meta.addEnchant(en, Math.max(1, e.level()), true);
                }
            });
        }
        return stack;
    }

    // ------------------------------------------------------------------
    // entities
    // ------------------------------------------------------------------

    @Override public MobRef spawnMob(String mob, Vec3 loc, String customName) {
        EntityTypeHolder type = entityTypeOf(mob);
        if (type == null) {
            plugin.getLogger().warning("Hermes: unknown mob '" + mob + "'");
            return null;
        }
        World world = defaultWorld();
        Entity e = world.spawnEntity(toBukkit(world, loc), type.type());
        if (e instanceof LivingEntity le) {
            le.setRemoveWhenFarAway(false);
            if (customName != null && !customName.isBlank()) {
                le.customName(Component.text(customName));
                le.setCustomNameVisible(true);
            }
        }
        return new BukkitMob(e, type.friendly());
    }

    private record EntityTypeHolder(org.bukkit.entity.EntityType type, String friendly) {}

    private static EntityTypeHolder entityTypeOf(String friendly) {
        String canonical = Dictionary.findMob(friendly);
        if (canonical == null) return null;
        org.bukkit.entity.EntityType t = Registry.ENTITY_TYPE.get(
                NamespacedKey.minecraft(canonical.replace(' ', '_')));
        if (t != null) return new EntityTypeHolder(t, canonical);
        try {
            return new EntityTypeHolder(org.bukkit.entity.EntityType.valueOf(
                    canonical.replace(' ', '_').toUpperCase(Locale.ROOT)), canonical);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override public void killMob(MobRef m) { mobOf(m).remove(); }
    @Override public void damageMob(MobRef m, double amount) {
        if (mobOf(m) instanceof LivingEntity le) le.damage(amount);
    }
    @Override public void healMob(MobRef m, double amount) {
        if (mobOf(m) instanceof LivingEntity le) le.setHealth(Math.min(le.getHealth() + amount, le.getMaxHealth()));
    }
    @Override public void teleportMob(MobRef m, Vec3 loc) { mobOf(m).teleport(toBukkit(mobOf(m).getWorld(), loc)); }
    @Override public Vec3 mobLocation(MobRef m) { return toHermes(mobOf(m).getLocation()); }
    @Override public String mobType(MobRef m) { return friendlyMob(mobOf(m)); }
    @Override public String mobCustomName(MobRef m) {
        return mobOf(m).customName() == null ? null
                : PlainTextComponentSerializer.plainText().serialize(mobOf(m).customName());
    }

    // ------------------------------------------------------------------
    // effects
    // ------------------------------------------------------------------

    @Override public void effectOnPlayer(PlayerRef p, String effect, int seconds) {
        PotionEffectType t = effectTypeOf(effect);
        if (t == null) return;
        playerOf(p).addPotionEffect(new PotionEffect(t, seconds * 20, 1, true, true));
    }
    @Override public void effectOnMob(MobRef m, String effect, int seconds) {
        PotionEffectType t = effectTypeOf(effect);
        if (t == null || !(mobOf(m) instanceof LivingEntity le)) return;
        le.addPotionEffect(new PotionEffect(t, seconds * 20, 1, true, true));
    }
    @Override public void removeEffectFromPlayer(PlayerRef p, String effect) {
        PotionEffectType t = effectTypeOf(effect);
        if (t != null) playerOf(p).removePotionEffect(t);
    }
    @Override public void removeEffectFromMob(MobRef m, String effect) {
        PotionEffectType t = effectTypeOf(effect);
        if (t != null && mobOf(m) instanceof LivingEntity le) le.removePotionEffect(t);
    }

    // ------------------------------------------------------------------
    // blocks & world interactions
    // ------------------------------------------------------------------

    @Override public void setBlock(Vec3 loc, String block) {
        Material m = materialOf(block);
        if (m == null) return;
        defaultWorld().getBlockAt((int) loc.x(), (int) loc.y(), (int) loc.z()).setType(m);
    }

    @Override public String blockAt(Vec3 loc) {
        Block b = defaultWorld().getBlockAt((int) loc.x(), (int) loc.y(), (int) loc.z());
        return b.isEmpty() ? "air" : friendlyBlock(b);
    }

    @Override public void openDoorNear(PlayerRef p) { setDoorNear(playerOf(p), true); }
    @Override public void closeDoorNear(PlayerRef p) { setDoorNear(playerOf(p), false); }

    private void setDoorNear(Player player, boolean open) {
        Location c = player.getLocation();
        for (int dx = -3; dx <= 3; dx++) for (int dy = -3; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
            Block b = c.getBlock().getRelative(dx, dy, dz);
            if (b.getBlockData() instanceof Openable o) o.setOpen(open);
        }
    }

    @Override public void pressButtonNear(PlayerRef p) {
        Location c = playerOf(p).getLocation();
        for (int dx = -3; dx <= 3; dx++) for (int dy = -3; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
            Block b = c.getBlock().getRelative(dx, dy, dz);
            if (b.getBlockData() instanceof Powerable powerable) {
                powerable.setPowered(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (b.getBlockData() instanceof Powerable p2) p2.setPowered(false);
                }, 5L);
            }
        }
    }

    @Override public void pullLeverNear(PlayerRef p) {
        Location c = playerOf(p).getLocation();
        for (int dx = -3; dx <= 3; dx++) for (int dy = -3; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
            Block b = c.getBlock().getRelative(dx, dy, dz);
            if (b.getBlockData() instanceof Switch lever) {
                lever.setPowered(!lever.isPowered());
            }
        }
    }

    @Override public void writeSign(Vec3 loc, List<String> lines) {
        Block b = defaultWorld().getBlockAt((int) loc.x(), (int) loc.y(), (int) loc.z());
        if (!(b.getState() instanceof Sign sign)) return;
        for (int i = 0; i < 4 && i < lines.size(); i++) {
            sign.line(i, Component.text(lines.get(i)));
        }
        sign.update();
    }

    @Override public void powerBlock(Vec3 loc) {
        Block b = defaultWorld().getBlockAt((int) loc.x(), (int) loc.y(), (int) loc.z());
        Material lamp = materialOf("redstone lamp");
        if (lamp == null) lamp = Material.REDSTONE_LAMP;
        b.setType(lamp);
        if (b.getBlockData() instanceof org.bukkit.block.data.Lightable rl) rl.setLit(true);
    }

    @Override public void unpowerBlock(Vec3 loc) {
        Block b = defaultWorld().getBlockAt((int) loc.x(), (int) loc.y(), (int) loc.z());
        if (b.getBlockData() instanceof org.bukkit.block.data.Lightable rl) {
            rl.setLit(false);
            b.setBlockData((org.bukkit.block.data.BlockData) rl);
        }
    }

    private Inventory chestInventory(Vec3 loc) {
        Block b = defaultWorld().getBlockAt((int) loc.x(), (int) loc.y(), (int) loc.z());
        if (b.getState() instanceof Chest chest) return chest.getInventory();
        return null;
    }

    @Override public boolean chestHas(Vec3 loc, String item, int count) {
        Inventory inv = chestInventory(loc);
        Material m = materialOf(item);
        if (inv == null || m == null) return false;
        int total = 0;
        for (ItemStack s : inv.getContents()) {
            if (s != null && s.getType() == m) total += s.getAmount();
        }
        return total >= count;
    }

    @Override public void chestAdd(Vec3 loc, String item, int count) {
        Inventory inv = chestInventory(loc);
        Material m = materialOf(item);
        if (inv == null || m == null) return;
        inv.addItem(new ItemStack(m, count));
    }

    @Override public void chestTake(Vec3 loc, String item, int count) {
        Inventory inv = chestInventory(loc);
        Material m = materialOf(item);
        if (inv == null || m == null) return;
        inv.removeItem(new ItemStack(m, count));
    }

    // ------------------------------------------------------------------
    // regions
    // ------------------------------------------------------------------

    /** Regions defined by scripts; used by the engine and the listener. */
    Map<String, Region> regions() { return regions; }

    @Override public void defineRegion(String name, Vec3 a, Vec3 b) {
        regions.put(name, new Region(
                new Vec3(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                new Vec3(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()))));
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

    /** The region names a location is inside. */
    List<String> regionsAt(Vec3 loc) {
        List<String> hits = new ArrayList<>();
        for (Map.Entry<String, Region> e : regions.entrySet()) {
            Region r = e.getValue();
            if (loc.x() >= r.min.x() && loc.x() <= r.max.x()
                    && loc.y() >= r.min.y() && loc.y() <= r.max.y()
                    && loc.z() >= r.min.z() && loc.z() <= r.max.z()) {
                hits.add(e.getKey());
            }
        }
        return hits;
    }

    // ------------------------------------------------------------------
    // sound & particles
    // ------------------------------------------------------------------

    @Override public void playSound(String sound, Vec3 loc) {
        World w = defaultWorld();
        w.playSound(toBukkit(w, loc), soundOf(sound), 1.0f, 1.0f);
    }

    @Override public void spawnParticles(String particle, Vec3 loc) {
        spawnParticles(particle, loc, 30, 1.0);
    }

    @Override public void spawnParticles(String particle, Vec3 loc, int count, double size) {
        World w = defaultWorld();
        double off = 0.5 * size;
        w.spawnParticle(particleOf(particle), toBukkit(w, loc), count, off, off, off, 0);
    }

    // ------------------------------------------------------------------
    // permissions (managed by Hermes)
    // ------------------------------------------------------------------

    @Override public boolean hasPermission(PlayerRef p, String perm) {
        return playerOf(p).hasPermission("Hermes.perm." + perm);
    }

    @Override public void grantPermission(PlayerRef p, String perm) {
        Player player = playerOf(p);
        org.bukkit.permissions.PermissionAttachment a = player.addAttachment(plugin, "Hermes.perm." + perm, true);
        attachments.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(a);
    }

    @Override public void revokePermission(PlayerRef p, String perm) {
        Player player = playerOf(p);
        List<org.bukkit.permissions.PermissionAttachment> list = attachments.get(player.getUniqueId());
        if (list == null) return;
        list.removeIf(a -> {
            boolean match = Boolean.TRUE.equals(a.getPermissions().get("Hermes.perm." + perm));
            if (match) player.removeAttachment(a);
            return match;
        });
    }

    // ------------------------------------------------------------------
    // scoreboards & teams
    // ------------------------------------------------------------------

    @Override public int score(PlayerRef p, String objective) {
        Objective o = scoreboard.getObjective(objective);
        if (o == null) return 0;
        Score s = o.getScore(playerOf(p).getName());
        return s.isScoreSet() ? s.getScore() : 0;
    }

    @Override public void setScore(PlayerRef p, String objective, int value) {
        Objective o = scoreboard.getObjective(objective);
        if (o == null) {
            o = scoreboard.registerNewObjective(objective, org.bukkit.scoreboard.Criteria.DUMMY,
                    Component.text(objective));
        }
        o.getScore(playerOf(p).getName()).setScore(value);
    }

    @Override public void addScore(PlayerRef p, String objective, int delta) {
        setScore(p, objective, score(p, objective) + delta);
    }

    @Override public void createTeam(String name) {
        if (scoreboard.getTeam(name) == null) scoreboard.registerNewTeam(name);
    }

    @Override public void putInTeam(PlayerRef p, String team) {
        Team t = scoreboard.getTeam(team);
        if (t == null) t = scoreboard.registerNewTeam(team);
        t.addEntry(playerOf(p).getName());
    }

    @Override public void removeFromTeam(PlayerRef p, String team) {
        Team t = scoreboard.getTeam(team);
        if (t != null) t.removeEntry(playerOf(p).getName());
    }

    // ------------------------------------------------------------------
    // general
    // ------------------------------------------------------------------

    @Override public List<PlayerRef> onlinePlayers() {
        List<PlayerRef> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) out.add(new BukkitPlayer(p));
        return out;
    }

    @Override public void log(String msg) { plugin.getLogger().info(msg); }

    @Override public void save() {
        Bukkit.getWorlds().forEach(World::save);
    }

    @Override public void shutdown() {
        for (List<org.bukkit.permissions.PermissionAttachment> list : attachments.values()) {
            for (org.bukkit.permissions.PermissionAttachment a : list) a.remove();
        }
        attachments.clear();
        for (org.bukkit.boss.BossBar bar : bossbars.values()) {
            for (org.bukkit.entity.Player pl : bar.getPlayers()) bar.removePlayer(pl);
        }
        bossbars.clear();
    }
}
