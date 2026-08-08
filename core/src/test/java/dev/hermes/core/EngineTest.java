package dev.hermes.core;

import dev.hermes.demo.MockWorld;
import dev.hermes.demo.MockWorld.MockPlayer;
import dev.hermes.demo.MockWorld.MockScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end behavior tests: load real Hermes source, fire events, and check
 * that the mock world actually responds.
 */
class EngineTest {

    private static final class Harness {
        final MockWorld world = new MockWorld();
        final MockScheduler scheduler = new MockScheduler();
        final TaleEngine engine = new TaleEngine(world, scheduler);
        final MockPlayer p1 = world.addPlayer("PlayerOne");

        Harness(String script) {
            assertTrue(engine.loadString(script, "test.Hermes"),
                    "script should load; errors: " + engine.loadErrors());
        }
    }

    @Test
    void welcomesPlayerOnJoin() {
        Harness h = new Harness("""
                when player joins
                    welcome player with "Welcome to the server!"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "Welcome to the server!"));
    }

    @Test
    void interpolatesVariablesInText() {
        Harness h = new Harness("""
                when player joins
                    set player's coins to 5
                    set world's greeting to "Welcome back, ${player's name}!"
                    tell player "You have ${player's coins} coins!"
                    announce "${world's greeting} It is a nice day."
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "You have 5 coins!"));
        assertTrue(h.world.broadcastContains("Welcome back, PlayerOne! It is a nice day."));
    }

    @Test
    void interpolatesPhrasesAndNumbers() {
        Harness h = new Harness("""
                when player joins
                    set player's coins to 3
                    announce "A wild ${random number between 1 and 10} appeared!"
                    tell player "You have ${player's coins} coins now"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.broadcastContains("A wild "));
        assertTrue(h.world.broadcastContains(" appeared!"));
        assertTrue(h.world.messageContains("PlayerOne", "You have 3 coins now"));
    }

    @Test
    void killsPlayerWhoTouchesWater() {
        Harness h = new Harness("""
                when player touches water
                    kill player
                """);
        h.world.putBlockAt(h.p1.location, "water");
        h.engine.tick();
        assertEquals(0, h.p1.health);
        // edge-triggered: a second tick must not kill again (still dead, no error)
        h.engine.tick();
    }

    @Test
    void addsCoinsWhenBreakingDiamondOre() {
        Harness h = new Harness("""
                when player breaks diamond ore
                    add 10 to player's coins
                """);
        h.engine.playerEventBlock("breaks", h.p1, "diamond ore");
        assertEquals(10, h.engine.vars.getPlayer("PlayerOne", "coins").num);
        // other blocks don't count
        h.engine.playerEventBlock("breaks", h.p1, "stone");
        assertEquals(10, h.engine.vars.getPlayer("PlayerOne", "coins").num);
    }

    @Test
    void makesChampionAt100Coins() {
        Harness h = new Harness("""
                when player has 100 coins
                    give player a diamond sword
                    tell player "You are now a champion!"
                """);
        h.engine.vars.setPlayer("PlayerOne", "coins", Value.number(100));
        h.engine.tick();
        assertEquals(1, h.p1.inventory.getOrDefault("diamond sword", 0));
        assertTrue(h.world.messageContains("PlayerOne", "champion"));
    }

    @Test
    void givesItemsForItemCounts() {
        Harness h = new Harness("""
                when player has 5 diamonds
                    give player 5 emeralds
                """);
        h.world.giveItem(h.p1, "diamond", 5);
        h.engine.tick();
        assertEquals(5, h.p1.inventory.getOrDefault("emerald", 0));
        h.engine.tick(); // edge-triggered: must not double-give
        assertEquals(5, h.p1.inventory.getOrDefault("emerald", 0));
    }

    @Test
    void runsEveryBlocks() {
        Harness h = new Harness("""
                every 2 seconds
                    spawn zombie at 100 64 200
                """);
        h.scheduler.advance(2_000);
        assertEquals(1, h.world.mobs.size());
        h.scheduler.advance(4_000);
        assertEquals(3, h.world.mobs.size());
    }

    @Test
    void firesCustomEvents() {
        Harness h = new Harness("""
                when custom event "boss_killed" fires
                    announce "The boss has been defeated!"
                """);
        h.engine.fireCustomEvent("boss_killed");
        assertTrue(h.world.broadcastContains("boss has been defeated"));
    }

    @Test
    void handlesIfElseRepeatAndLoops() {
        Harness h = new Harness("""
                when player joins
                    if player's coins are at least 100
                        give player 1 diamond
                    else
                        give player 1 coal
                    repeat 2 times
                        give player 1 stick
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("coal", 0));
        assertEquals(2, h.p1.inventory.getOrDefault("stick", 0));
        assertNull(h.p1.inventory.get("diamond"));
    }

    @Test
    void loopsOverLists() {
        Harness h = new Harness("""
                create list "quests"
                add "a" to list "quests"
                add "b" to list "quests"
                when player joins
                    loop over list "quests" as task
                        tell player task
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "a"));
        assertTrue(h.world.messageContains("PlayerOne", "b"));
    }

    @Test
    void teleportsToMarkedPlace() {
        Harness h = new Harness("""
                mark home at 100 64 200
                when player types "home"
                    teleport player to home
                """);
        h.engine.playerEventText("types", h.p1, "home");
        assertEquals(100, h.p1.location.x());
        assertEquals(200, h.p1.location.z());
    }

    @Test
    void fightsNightWithZombies() {
        Harness h = new Harness("""
                when it is nighttime
                    spawn zombie at 8 64 8
                """);
        h.world.setTime("night");
        h.engine.tick();
        assertEquals(1, h.world.mobs.size());
    }

    @Test
    void damageHealAndPermissions() {
        Harness h = new Harness("""
                when player takes damage and player health is below 5
                    warn player "You are almost dead!"
                """);
        h.p1.health = 4;
        h.engine.playerEvent("takes damage", h.p1);
        assertFalse(h.p1.warnings.isEmpty());
        assertTrue(h.p1.warnings.get(0).contains("almost dead"));
    }

    @Test
    void teleportOutOfLava() {
        Harness h = new Harness("""
                mark checkpoint at 5 64 5
                when player walks on lava
                    teleport player to checkpoint
                """);
        h.world.putBlockUnder(h.p1.location, "lava");
        h.engine.tick();
        assertEquals(5, h.p1.location.x());
        assertEquals(5, h.p1.location.z());
    }

    @Test
    void regionTriggers() {
        Harness h = new Harness("""
                region "Castle" from 0 0 0 to 50 100 50
                when player enters "Castle"
                    announce "Player entered the Castle!"
                """);
        h.engine.playerEventRegion("enters", h.p1, "Castle");
        assertTrue(h.world.broadcastContains("Castle"));
    }

    @Test
    void customActionsAreCallable() {
        Harness h = new Harness("""
                action reward the player
                    give the player 10 diamonds
                    give the player 100 xp

                when player joins
                    reward the player
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(10, h.p1.inventory.getOrDefault("diamond", 0));
        assertEquals(100, h.p1.xp);
    }

    @Test
    void mobDeathTriggers() {
        Harness h = new Harness("""
                when zombie dies
                    announce "A zombie died!"
                """);
        MockWorld.MockMob zombie = (MockWorld.MockMob) h.world.spawnMob("zombie", new WorldAPI.Vec3(0, 0, 0), null);
        h.engine.mobEvent("mob dies", zombie, "zombie");
        assertTrue(h.world.broadcastContains("zombie died"));
    }

    @Test
    void namedMobTriggers() {
        Harness h = new Harness("""
                when mob named "boss" dies
                    announce "The boss has been defeated!"
                """);
        MockWorld.MockMob boss = (MockWorld.MockMob) h.world.spawnMob("zombie", new WorldAPI.Vec3(0, 0, 0), "boss");
        h.engine.mobEvent("mob dies", boss, "zombie");
        assertTrue(h.world.broadcastContains("boss has been defeated"));
    }

    @Test
    void variablesPersistAcrossEvents() {
        Harness h = new Harness("""
                when player breaks diamond ore
                    add 10 to player's coins
                when player's coins are at least 30
                    tell player "Rich now!"
                """);
        h.engine.playerEventBlock("breaks", h.p1, "diamond ore");
        h.engine.playerEventBlock("breaks", h.p1, "diamond ore");
        h.engine.playerEventBlock("breaks", h.p1, "diamond ore");
        h.engine.tick();
        assertTrue(h.world.messageContains("PlayerOne", "Rich now!"));
    }

    @Test
    void scoreboardAndTeams() {
        Harness h = new Harness("""
                when player attacks
                    add 1 to player's score "kills"
                    put player in team "red"
                """);
        h.engine.playerEvent("attacks", h.p1);
        assertEquals(1, h.p1.scores.getOrDefault("kills", 0));
        assertEquals("red", h.p1.team);
    }

    @Test
    void soundAndParticlesRun() {
        Harness h = new Harness("""
                when player joins
                    play sound "level_up" near player
                """);
        h.engine.playerEvent("joins", h.p1);
        assertFalse(h.world.log.isEmpty());
    }

    @Test
    void winGameAnnounces() {
        Harness h = new Harness("""
                when player reaches "Exit"
                    win the game
                """);
        h.engine.playerEventRegion("enters", h.p1, "Exit");
        assertTrue(h.world.broadcastContains("won"));
    }

    @Test
    void feedsAndClearsInventory() {
        Harness h = new Harness("""
                when player joins
                    feed player by 5
                    feed player
                    clear player's inventory
                """);
        h.world.giveItem(h.p1, "diamond", 3);
        h.p1.hunger = 8;
        h.engine.playerEvent("joins", h.p1);
        assertEquals(20, h.p1.hunger);
        assertTrue(h.p1.inventory.isEmpty());
    }

    @Test
    void kicksPlayerBecauseReason() {
        Harness h = new Harness("""
                when player joins
                    kick player because "You are banned!"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals("You are banned!", h.p1.kicked);
    }

    @Test
    void lightningExplodeLaunchAndParticles() {
        Harness h = new Harness("""
                when player joins
                    lightning at player
                    explode at player with power 2
                    launch player by 3
                    spawn particles "flame" near player
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.log.stream().anyMatch(l -> l.startsWith("lightning@8,8")));
        assertTrue(h.world.log.stream().anyMatch(l -> l.startsWith("explosion@8,8") && l.contains("power 2")));
        assertTrue(h.world.log.stream().anyMatch(l -> l.startsWith("launched PlayerOne by 3")));
        assertTrue(h.world.particles.stream().anyMatch(l -> l.startsWith("flame@8,8")));
    }

    @Test
    void titlesAndActionbars() {
        Harness h = new Harness("""
                when player joins
                    title player "Big" with subtitle "Small"
                    actionbar player "Keep going!"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.p1.titles.contains("Big|Small"));
        assertTrue(h.p1.actionbars.contains("Keep going!"));
    }

    @Test
    void gamemodeSetAndChecked() {
        Harness h = new Harness("""
                when player joins
                    set player's gamemode to creative
                when player is in creative mode
                    give player 1 diamond
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals("creative", h.p1.gamemode);
        h.engine.tick();
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
    }

    @Test
    void playerFactsAsValues() {
        Harness h = new Harness("""
                when player joins
                    tell player player's name
                    tell player player's world
                    tell player player's x
                    tell player player's gamemode
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "PlayerOne"));
        assertTrue(h.world.messageContains("PlayerOne", "overworld"));
        assertTrue(h.world.messageContains("PlayerOne", "8"));
        assertTrue(h.world.messageContains("PlayerOne", "survival"));
    }

    @Test
    void randomNumberStaysInBounds() {
        Harness h = new Harness("""
                repeat 20 times
                    set world's roll to random number between 1 and 10
                    if world's roll is above 10
                        announce "out of bounds"
                """);
        assertFalse(h.world.broadcastContains("out of bounds"));
        assertTrue(h.engine.vars.getWorld("roll").num >= 1);
    }

    @Test
    void onlinePlayerCountAndItemCount() {
        Harness h = new Harness("""
                when player joins
                    set world's players to number of players
                    set world's gems to count of "diamond" in player's inventory
                """);
        h.world.giveItem(h.p1, "diamond", 3);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.engine.vars.getWorld("players").num);
        assertEquals(3, h.engine.vars.getWorld("gems").num);
    }

    @Test
    void deleteListStartsFresh() {
        Harness h = new Harness("""
                create list "quests"
                add "old quest" to list "quests"
                delete list "quests"
                when player joins
                    set world's quests to length of list "quests"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(0, h.engine.vars.getWorld("quests").num);
    }

    @Test
    void killsTriggerFires() {
        Harness h = new Harness("""
                when player kills zombie
                    give player 1 rotten flesh
                when player kills any mob
                    add 1 to player's score "kills"
                """);
        h.engine.playerEventMob("kills", h.p1, "zombie");
        assertEquals(1, h.p1.inventory.getOrDefault("rotten flesh", 0));
        assertEquals(1, h.p1.scores.getOrDefault("kills", 0));
        h.engine.playerEventMob("kills", h.p1, "cow");
        assertEquals(2, h.p1.scores.getOrDefault("kills", 0));
        assertEquals(1, h.p1.inventory.getOrDefault("rotten flesh", 0));
    }

    @Test
    void eatsTriggerFires() {
        Harness h = new Harness("""
                when player eats apple
                    feed player
                """);
        h.engine.playerEventItem("eats", h.p1, "apple");
        assertEquals(20, h.p1.hunger);
    }

    @Test
    void playerStateConditions() {
        Harness h = new Harness("""
                when player is sneaking and player is on the ground
                    give player 1 diamond
                """);
        h.engine.tick();
        assertNull(h.p1.inventory.get("diamond"));
        h.p1.sneaking = true;
        h.engine.tick();
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
        h.p1.sneaking = false;
        h.engine.tick();
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
    }
}
