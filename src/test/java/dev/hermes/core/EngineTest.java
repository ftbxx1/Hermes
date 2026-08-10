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
    void firstJoinsFiresOnlyOnce() {
        Harness h = new Harness("""
                when player first joins
                    give player 1 diamond
                when player joins
                    add 1 to world's joins
                """);
        h.engine.playerEvent("joins", h.p1);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
        assertEquals(2, h.engine.vars.getWorld("joins").num);
    }

    @Test
    void loopsOverAllPlayers() {
        Harness h = new Harness("""
                when player joins
                    loop over all players as p
                        give player 1 bread
                """);
        MockWorld.MockPlayer p2 = h.world.addPlayer("PlayerTwo");
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("bread", 0));
        assertEquals(1, p2.inventory.getOrDefault("bread", 0));
    }

    @Test
    void loopsOverNumbers() {
        Harness h = new Harness("""
                when player joins
                    loop over numbers from 1 to 3 as i
                        tell player "Number ${i}"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "Number 1"));
        assertTrue(h.world.messageContains("PlayerOne", "Number 3"));
        assertFalse(h.world.messageContains("PlayerOne", "Number 4"));
    }

    @Test
    void loopsOverInventory() {
        Harness h = new Harness("""
                when player joins
                    loop over player's inventory as item
                        tell player "You have ${item}"
                """);
        h.world.giveItem(h.p1, "bread", 1);
        h.world.giveItem(h.p1, "diamond", 2);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "You have bread"));
        assertTrue(h.world.messageContains("PlayerOne", "You have diamond"));
    }

    @Test
    void scriptCommandBindsArguments() {
        Harness h = new Harness("""
                command "/pay" with argument <amount> and argument <target>
                    tell player "You paid ${amount} to ${target}!"
                """);
        TaleEngine.RegisteredCommand rc = h.engine.commands().get(0);
        assertEquals("/pay", rc.def.name);
        h.engine.fireCommand(rc, h.p1, java.util.List.of("5", "Steve"));
        assertTrue(h.world.messageContains("PlayerOne", "You paid 5 to Steve!"));
    }

    @Test
    void actionWithParameters() {
        Harness h = new Harness("""
                action greet <name> the player
                    tell player "Hi ${name}!"
                when player joins
                    greet "Steve" the player
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "Hi Steve!"));
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

    @Test
    void unloadRemovesEverythingFromThatFile() {
        Harness h = new Harness("""
                region "Arena" from 0 0 0 to 10 10 10
                mark home at 100 64 200
                every 5 seconds
                    add 1 to world's ticks
                command "/alpha"
                    tell player "alpha"
                when player joins
                    tell player "old"
                """);
        h.engine.unload("test.Hermes");

        assertTrue(h.engine.scripts().isEmpty());
        assertTrue(h.engine.commands().isEmpty());
        assertFalse(h.world.inRegion("Arena", new WorldAPI.Vec3(5, 5, 5)));
        assertFalse(h.engine.marks.containsKey("home"));
        assertEquals(0, h.scheduler.everyTaskCount(), "timers from the unloaded file must be gone");
        h.p1.health = 20;
        h.engine.playerEvent("joins", h.p1);
        assertFalse(h.world.messageContains("PlayerOne", "old"));
    }

    @Test
    void reloadKeepsOtherScripts() {
        Harness h = new Harness("""
                when player joins
                    tell player "one"
                """);
        assertTrue(h.engine.loadString("""
                when player joins
                    tell player "two"
                """, "second.Hermes"));

        h.engine.unload("test.Hermes");
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "two"));
        assertFalse(h.world.messageContains("PlayerOne", "one"));

        assertEquals(1, h.engine.scripts().size());
        assertEquals("second.Hermes", h.engine.scripts().get(0).fileName);
    }

    @Test
    void worldVariableConditions() {
        Harness h = new Harness("""
                when world's flag is true
                    announce "event started"
                when player joins
                    if world's flag is true
                        give player 1 diamond
                """);
        h.engine.tick();
        assertFalse(h.world.broadcastContains("event started"));
        h.engine.vars.setWorld("flag", Value.truth(true));
        h.engine.tick();
        assertTrue(h.world.broadcastContains("event started"));
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
    }

    @Test
    void isInRegionWithKeyword() {
        Harness h = new Harness("""
                region "SafeZone" from 0 60 0 to 20 64 20
                when player is in region "SafeZone"
                    announce "inside"
                when player joins
                    if player is in region "SafeZone"
                        tell player "you are safe"
                """);
        h.engine.tick();
        assertTrue(h.world.broadcastContains("inside"));
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "you are safe"));
    }

    @Test
    void scoreConditionInWhenHeader() {
        Harness h = new Harness("""
                when player's score "kills" is at least 10
                    give player 1 diamond
                """);
        h.world.setScore(h.p1, "kills", 12);
        h.engine.tick();
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
    }

    @Test
    void orConditionInWhenHeader() {
        Harness h = new Harness("""
                when player's health is below 5 or player's hunger is below 5
                    warn player "Take care!"
                """);
        h.engine.tick();
        assertTrue(h.p1.warnings.isEmpty());
        h.p1.hunger = 2;
        h.engine.tick();
        assertTrue(h.p1.warnings.contains("Take care!"));
    }

    @Test
    void removesTextFromList() {
        Harness h = new Harness("""
                create list "quests"
                add "Find the key" to list "quests"
                remove "Find the key" from list "quests"
                """);
        assertEquals(0, h.engine.vars.list("quests").items.size());
    }

    @Test
    void opensGuiAndHandlesSlotClick() {
        Harness h = new Harness("""
                gui "Shop" with 9 slots
                    slot 0 has 1 diamond named "Star"
                    slot 2 has 1 emerald

                when player joins
                    open gui "Shop" to player

                when player clicks slot 2 of gui "Shop"
                    give player 5 emeralds

                when player clicks gui "Shop"
                    give player 1 iron ingot
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "[gui] Shop"));

        h.engine.playerEventGui("gui click", h.p1, "Shop", 2);
        assertEquals(5, h.p1.inventory.getOrDefault("emerald", 0));

        h.engine.playerEventGui("gui click", h.p1, "Shop", 5);
        assertEquals(2, h.p1.inventory.getOrDefault("iron ingot", 0));
    }

    @Test
    void guiSlotClickOnlyFiresItsOwnSlot() {
        Harness h = new Harness("""
                gui "Menu" with 9 slots
                    slot 0 has 1 diamond

                when player clicks slot 0 of gui "Menu"
                    give player 1 emerald
                """);
        h.engine.playerEventGui("gui click", h.p1, "Menu", 3);
        assertEquals(0, h.p1.inventory.getOrDefault("emerald", 0));
        h.engine.playerEventGui("gui click", h.p1, "Menu", 0);
        assertEquals(1, h.p1.inventory.getOrDefault("emerald", 0));
    }

    @Test
    void booksAreCreatedAndGiven() {
        Harness h = new Harness("""
                create book "Guide" with title "The Guide" with author "Hermes" with page "Page one"
                when player joins
                    give player book "Guide"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "[book] The Guide by Hermes"));
    }

    @Test
    void databasesAndPlayerDataPersist() {
        Harness h = new Harness("""
                create database "data"
                when player joins
                    set database "data" at "coins" to 5
                    add 2 to database "data" at "coins"
                    set player data "tokens" to 100
                    if database "data" at "coins" is above 5
                        set player's coins to database "data" at "coins"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(7, h.engine.vars.getDatabase("data", "coins").num);
        assertEquals(100, h.engine.vars.getPlayerData("PlayerOne", "tokens").num);
        assertEquals(7, h.engine.vars.getPlayer("PlayerOne", "coins").num);
    }

    @Test
    void configValuesAreReadAndWritten() {
        Harness h = new Harness("""
                when player joins
                    set config value "prefix" in file "config.yml" to "[Hermes]"
                    if config value "prefix" in file "config.yml" is equal to "[Hermes]"
                        tell player "prefix ok"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "prefix ok"));
    }

    @Test
    void playerInWorldCondition() {
        Harness h = new Harness("""
                when player is in world "arena"
                    tell player "in the arena"
                """);
        h.p1.dimension = "arena";
        h.engine.tick();
        assertTrue(h.world.messageContains("PlayerOne", "in the arena"));
    }

    @Test
    void worldCreateAndSetWeatherAndTime() {
        Harness h = new Harness("""
                when player joins
                    create world "arena"
                    delete world "old"
                    set weather in world "arena" to rain
                    set time in world "arena" to night
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.worldExists("arena"));
        assertFalse(h.world.worldExists("old"));
    }

    // ------------------------------------------------------------------
    // while, wait, arithmetic, stat setters, bossbar, server events, i18n
    // ------------------------------------------------------------------

    @Test
    void whileLoopRunsUntilConditionFails() {
        Harness h = new Harness("""
                when player joins
                    while player health is above 5
                        damage player by 1
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(5, h.p1.health);
    }

    @Test
    void waitDelaysTheRestOfTheBlock() {
        Harness h = new Harness("""
                when player joins
                    tell player "now"
                    wait 2 seconds
                    tell player "later"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "now"));
        assertFalse(h.world.messageContains("PlayerOne", "later"));
        h.scheduler.flush();
        assertTrue(h.world.messageContains("PlayerOne", "later"));
    }

    @Test
    void waitInsideLoopIsRejected() {
        MockWorld world = new MockWorld();
        TaleEngine engine = new TaleEngine(world, new MockScheduler());
        boolean ok = engine.loadString("""
                when player joins
                    while player health is above 1
                        wait 1 second
                """, "t.Hermes");
        assertFalse(ok);
        assertFalse(engine.loadErrors().isEmpty());
    }

    @Test
    void arithmeticInValues() {
        Harness h = new Harness("""
                when player joins
                    set player's coins to 3 plus 4 times 2
                    set player's level to 10 divided by 2 minus 1
                    set player's score "wins" to player's coins minus 10
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(14, h.engine.vars.getPlayer("PlayerOne", "coins").num);
        assertEquals(4, h.p1.level);
        assertEquals(4, h.p1.scores.getOrDefault("wins", 0));
    }

    @Test
    void statSetters() {
        Harness h = new Harness("""
                when player joins
                    set player's health to 7
                    set player's hunger to 3
                    set player's xp to 50
                    set player's level to 9
                    set player's food to 12
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(7, h.p1.health);
        assertEquals(12, h.p1.hunger);
        assertEquals(50, h.p1.xp);
        assertEquals(9, h.p1.level);
    }

    @Test
    void bossbarSetAndCleared() {
        Harness h = new Harness("""
                when player joins
                    set player's bossbar to "Quest" with progress 50
                    set player's bossbar to "Quest 2"
                    clear player's bossbar
                """);
        h.p1.bossbar = "old";
        h.engine.playerEvent("joins", h.p1);
        assertNull(h.p1.bossbar);
    }

    @Test
    void bossbarKeepsProgress() {
        Harness h = new Harness("""
                when player joins
                    set player's bossbar to "Quest" with progress 25
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals("Quest", h.p1.bossbar);
        assertEquals(25, h.p1.bossbarProgress, 0.001);
    }

    @Test
    void serverStartAndStopEvents() {
        Harness h = new Harness("""
                when server starts
                    set world's phase to "up"
                when server stops
                    set world's phase to "down"
                """);
        h.engine.serverEvent("server starts");
        assertEquals("up", h.engine.vars.getWorld("phase").text);
        h.engine.serverEvent("server stops");
        assertEquals("down", h.engine.vars.getWorld("phase").text);
    }

    @Test
    void readsHeldItem() {
        Harness h = new Harness("""
                when player joins
                    tell player "You hold: ${player's held item}"
                """);
        h.p1.holding = "diamond sword";
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "You hold: diamond sword"));
    }

    @Test
    void spanishScriptingWorks() {
        Lang.setLanguage("es");
        try {
            Harness h = new Harness("""
                    cuando jugador se une
                        dar jugador 1 diamante

                    cuando jugador tiene 5 diamante
                        dar jugador 1 espada de diamante
                    """);
            h.engine.playerEvent("joins", h.p1);
            assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
            h.world.giveItem(h.p1, "diamond", 4);
            h.engine.tick();
            assertEquals(1, h.p1.inventory.getOrDefault("diamond sword", 0));
        } finally {
            Lang.setLanguage("en");
        }
    }

    @Test
    void frenchScriptingWorks() {
        Lang.setLanguage("fr");
        try {
            Harness h = new Harness("""
                    quand joueur rejoint
                        donner joueur 1 pain
                    """);
            h.engine.playerEvent("joins", h.p1);
            assertEquals(1, h.p1.inventory.getOrDefault("bread", 0));
        } finally {
            Lang.setLanguage("en");
        }
    }

    @Test
    void germanScriptingWorks() {
        Lang.setLanguage("de");
        try {
            Harness h = new Harness("""
                    wenn spieler beitritt
                        gebe spieler 1 brot
                    """);
            h.engine.playerEvent("joins", h.p1);
            assertEquals(1, h.p1.inventory.getOrDefault("bread", 0));
        } finally {
            Lang.setLanguage("en");
        }
    }

    @Test
    void spanishKeywordsStayEnglishForUnknownWords() {
        Lang.setLanguage("es");
        try {
            Harness h = new Harness("""
                    cuando jugador se une
                        poner jugador's monedas a 5
                    """);
            h.engine.playerEvent("joins", h.p1);
            assertEquals(5, h.engine.vars.getPlayer("PlayerOne", "monedas").num);
        } finally {
            Lang.setLanguage("en");
        }
    }

    @Test
    void chanceOfOneHundredAlwaysFires() {
        Harness h = new Harness("""
                when player joins
                    if chance 100
                        give player 1 diamond
                    if chance of 1 in 1
                        give player 1 emerald
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
        assertEquals(1, h.p1.inventory.getOrDefault("emerald", 0));
    }

    @Test
    void chanceOfZeroNeverFires() {
        Harness h = new Harness("""
                when player joins
                    if chance 0
                        give player 1 diamond
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(0, h.p1.inventory.getOrDefault("diamond", 0));
    }

    @Test
    void chanceWorksAsAndCondition() {
        Harness h = new Harness("""
                when player joins
                    if chance 100 and player is sneaking
                        give player 1 diamond
                """);
        h.p1.sneaking = true;
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
    }

    @Test
    void randomItemFromList() {
        Harness h = new Harness("""
                create list "rewards"
                add "sword" to list "rewards"
                add "shield" to list "rewards"
                add "helmet" to list "rewards"
                when player joins
                    set player's prize to random item from list "rewards"
                """);
        h.engine.playerEvent("joins", h.p1);
        String prize = h.engine.vars.getPlayer("PlayerOne", "prize").text;
        assertTrue(prize.equals("sword") || prize.equals("shield") || prize.equals("helmet"),
                "prize should be one of the list items, was: " + prize);
    }

    @Test
    void randomTeleportStaysWithinRadius() {
        Harness h = new Harness("""
                when player joins
                    teleport player randomly within 50
                """);
        h.engine.playerEvent("joins", h.p1);
        double dx = h.p1.location.x() - 8;
        double dz = h.p1.location.z() - 8;
        assertTrue(Math.hypot(dx, dz) <= 50.001, "moved too far from home: " + dx + "," + dz);
    }

    @Test
    void freezeAndUnfreezePlayer() {
        Harness h = new Harness("""
                when player joins
                    freeze player
                when player breaks diamond ore
                    unfreeze player
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.isFrozen(h.p1));
        h.engine.playerEventBlock("breaks", h.p1, "diamond ore");
        assertFalse(h.world.isFrozen(h.p1));
    }

    @Test
    void giveAllPlayersGivesEveryOnlinePlayer() {
        Harness h = new Harness("""
                when server starts
                    give all players 1 diamond
                    give all players a diamond sword
                """);
        MockWorld.MockPlayer p2 = h.world.addPlayer("PlayerTwo");
        h.engine.serverEvent("server starts");
        assertEquals(1, h.p1.inventory.getOrDefault("diamond", 0));
        assertEquals(1, p2.inventory.getOrDefault("diamond", 0));
        assertEquals(1, h.p1.inventory.getOrDefault("diamond sword", 0));
        assertEquals(1, p2.inventory.getOrDefault("diamond sword", 0));
    }

    @Test
    void particleCountAndSizeAreApplied() {
        Harness h = new Harness("""
                when player joins
                    spawn particles "heart" near player with count 50 with size 2
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.particles.contains("heart@8,8 x50 size 2.0"),
                "particles logged: " + h.world.particles);
    }

    @Test
    void frozenConditionWorks() {
        Harness h = new Harness("""
                when player joins
                    if player is frozen
                        warn player "already stuck"
                    freeze player
                    if player is frozen
                        warn player "now stuck"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertFalse(h.p1.warnings.stream().anyMatch(w -> w.contains("already stuck")));
        assertTrue(h.p1.warnings.stream().anyMatch(w -> w.contains("now stuck")));
    }

    @Test
    void giveAllPlayersGivesXpAndLevels() {
        Harness h = new Harness("""
                when server starts
                    give all players 10 xp
                    give all players 5 levels
                """);
        MockWorld.MockPlayer p2 = h.world.addPlayer("PlayerTwo");
        h.engine.serverEvent("server starts");
        assertEquals(10, h.p1.xp);
        assertEquals(10, p2.xp);
        assertEquals(5, h.p1.level);
        assertEquals(5, p2.level);
    }
}
