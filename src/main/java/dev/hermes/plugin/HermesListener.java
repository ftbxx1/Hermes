package dev.hermes.plugin;

import dev.hermes.core.Dictionary;
import dev.hermes.core.WorldAPI;
import dev.hermes.core.WorldAPI.MobRef;
import dev.hermes.core.WorldAPI.PlayerRef;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bridges Minecraft events to Hermes "when" triggers. Every handler fires the
 * engine on the main thread, with the right event name and filter:
 *
 * <pre>
 *   when player joins              -> PlayerJoinEvent
 *   when player breaks diamond ore -> BlockBreakEvent ("diamond ore")
 *   when player types "home"       -> AsyncChatEvent ("home")
 *   when zombie dies               -> EntityDeathEvent ("zombie")
 *   when player enters "Arena"     -> PlayerMoveEvent + region tracking
 * </pre>
 */
public final class HermesListener implements Listener {

    private final HermesPlugin plugin;
    private final Map<UUID, Long> lastJump = new HashMap<>();
    private final Map<UUID, Set<String>> lastRegions = new HashMap<>();

    public HermesListener(HermesPlugin plugin) {
        this.plugin = plugin;
    }

    private PlayerRef of(Player p) {
        return new BukkitWorld.BukkitPlayer(p);
    }

    private MobRef ofMob(Entity e) {
        String type = Dictionary.findMob(e.getType().getKey().getKey());
        return new BukkitWorld.BukkitMob(e, type != null ? type : "entity");
    }

    // ------------------------------------------------------------------
    // player life
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        plugin.engine().playerEvent("joins", of(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        plugin.engine().playerEvent("leaves", of(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        plugin.engine().playerEvent("dies", of(e.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        plugin.engine().playerEvent("respawns", of(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking()) plugin.engine().playerEvent("sneaks", of(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSprint(PlayerToggleSprintEvent e) {
        plugin.engine().playerEvent(e.isSprinting() ? "starts sprinting" : "stops sprinting", of(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelUp(PlayerLevelChangeEvent e) {
        if (e.getOldLevel() < e.getNewLevel()) plugin.engine().playerEvent("levels up", of(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.engine().playerEvent("fishes", of(e.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent e) {
        String item = friendlyItemKey(e.getItem().getType());
        plugin.engine().playerEventItem("eats", of(e.getPlayer()), item);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getX() == e.getTo().getX()
                && e.getFrom().getY() == e.getTo().getY()
                && e.getFrom().getZ() == e.getTo().getZ()) {
            return;
        }
        Player p = e.getPlayer();
        plugin.engine().playerEvent("moves", of(p));

        // jump detection: leaving the ground, moving up
        if (!p.isOnGround() && e.getTo().getY() > e.getFrom().getY()
                && p.getVelocity().getY() > 0.1) {
            long now = System.currentTimeMillis();
            if (now - lastJump.getOrDefault(p.getUniqueId(), 0L) > 400) {
                lastJump.put(p.getUniqueId(), now);
                plugin.engine().playerEvent("jumps", of(p));
            }
        }

        // region tracking
        List<String> inside = plugin.world().regionsAt(new WorldAPI.Vec3(
                e.getTo().getX(), e.getTo().getY(), e.getTo().getZ()));
        Set<String> before = lastRegions.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        for (String region : inside) {
            if (!before.contains(region)) {
                plugin.engine().playerEventRegion("enters", of(p), region);
            }
        }
        before.clear();
        before.addAll(inside);
    }

    // ------------------------------------------------------------------
    // blocks & items
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) {
        String block = friendlyBlockKey(e.getBlock().getType());
        plugin.engine().playerEventBlock("breaks", of(e.getPlayer()), block);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent e) {
        String block = friendlyBlockKey(e.getBlock().getType());
        plugin.engine().playerEventBlock("places", of(e.getPlayer()), block);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (e.getClickedBlock() != null) {
            String block = friendlyBlockKey(e.getClickedBlock().getType());
            plugin.engine().playerEventBlock("interacts with", of(e.getPlayer()), block);
        }
        ItemStack hand = e.getItem();
        if (hand != null && hand.getType() != Material.AIR) {
            String item = friendlyItemKey(hand.getType());
            plugin.engine().playerEventItem("uses", of(e.getPlayer()), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            String item = friendlyItemKey(e.getItem().getItemStack().getType());
            plugin.engine().playerEventItem("picks up", of(p), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent e) {
        String item = friendlyItemKey(e.getItemDrop().getItemStack().getType());
        plugin.engine().playerEventItem("drops", of(e.getPlayer()), item);
    }

    // ------------------------------------------------------------------
    // damage
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            plugin.engine().playerEvent("takes damage", of(p));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Player p) {
            plugin.engine().playerEvent("attacks", of(p));
        } else if (damager instanceof org.bukkit.entity.Mob) {
            plugin.engine().mobEvent("mob attacks", ofMob(damager),
                    Dictionary.findMob(damager.getType().getKey().getKey()));
        }
    }

    // ------------------------------------------------------------------
    // chat
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        if (text.isBlank()) return;
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.engine().playerEvent("chats", of(p));
            plugin.engine().playerEventText("types", of(p), text);
        });
    }

    // ------------------------------------------------------------------
    // mobs
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) return;
        String type = Dictionary.findMob(entity.getType().getKey().getKey());
        if (type == null) return;
        MobRef m = ofMob(entity);
        plugin.engine().mobEvent("mob dies", m, type);
        plugin.engine().mobEventByName("mob dies", m, plugin.world().mobCustomName(m));
        Entity killer = e.getEntity().getKiller();
        if (killer instanceof Player kp) {
            plugin.engine().playerEventMob("kills", of(kp), type);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitySpawn(EntitySpawnEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) return;
        String type = Dictionary.findMob(entity.getType().getKey().getKey());
        if (type == null) return;
        plugin.engine().mobEvent("mob spawns", ofMob(entity), type);
    }

    // ------------------------------------------------------------------

    /** Clicks inside a Hermes gui become "gui click" events with the gui name and slot. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGuiClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getInventory().getHolder() != null) return; // only our plain Hermes inventories
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (title.isEmpty()) return;
        plugin.engine().playerEventGui("gui click", of(p), title, e.getRawSlot());
    }

    private static String friendlyItemKey(Material m) {
        String key = m.getKey().getKey();
        String friendly = Dictionary.findItem(key);
        return friendly != null ? friendly : key;
    }

    private static String friendlyBlockKey(Material m) {
        return friendlyItemKey(m);
    }
}
