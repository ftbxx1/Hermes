package dev.hermes.core;

import dev.hermes.demo.MockWorld;
import dev.hermes.demo.MockWorld.MockPlayer;
import dev.hermes.demo.MockWorld.MockScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the nicer-to-write side of Hermes: forgiving English synonyms,
 * case-insensitive scripts, pure-English global variables, and functions
 * that hand values back.
 */
class ForgivingFeatureTest {

    private static final class Harness {
        final MockWorld world = new MockWorld();
        final MockScheduler scheduler = new MockScheduler();
        final TaleEngine engine = new TaleEngine(world, scheduler);
        final MockPlayer p1 = world.addPlayer("PlayerOne");

        Harness(String script) {
            assertTrue(engine.loadString(script, "forgiving.Hermes"),
                    "script should load; errors: " + engine.loadErrors());
        }
    }

    // ---------- forgiving synonyms ----------

    @Test
    void synonymSayIsTell() {
        Harness h = new Harness("""
                when player joins
                    say player "Hello there"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "Hello there"));
    }

    @Test
    void synonymGrantAndEach() {
        Harness h = new Harness("""
                when player joins
                    grant player 1 bread
                each 5 seconds
                    announce "tick"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("bread", 0));
    }

    @Test
    void synonymTpIsTeleport() {
        Harness h = new Harness("""
                mark home at 5 64 5
                when player joins
                    tp player to home
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(5.0, h.p1.location.x());
        assertEquals(5.0, h.p1.location.z());
    }

    @Test
    void keywordsAreCaseInsensitive() {
        Harness h = new Harness("""
                WHEN Player JOINS
                    GIVE Player 1 BREAD
                    SAY Player "Hi"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(1, h.p1.inventory.getOrDefault("bread", 0));
        assertTrue(h.world.messageContains("PlayerOne", "Hi"));
    }

    @Test
    void synonymLeaveIsLeaves() {
        Harness h = new Harness("""
                when player leave
                    say player "Goodbye"
                """);
        h.engine.playerEvent("leaves", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "Goodbye"));
    }

    // ---------- global variables ----------

    @Test
    void globalVariablesWorkEverywhere() {
        Harness h = new Harness("""
                when player joins
                    set global "coins" to 7
                    add 3 to global "coins"
                    if global "coins" is above 5
                        set temporary coins to global "coins"
                        tell player "Rich! ${temporary coins}"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "Rich! 10"));
        assertEquals(10.0, h.engine.vars.getWorld("coins").num);
    }

    @Test
    void globalVariableMaySkipQuotes() {
        Harness h = new Harness("""
                when player joins
                    set global inflation to 2
                    tell player "inflation ${global inflation}"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "inflation 2"));
    }

    // ---------- functions with return values ----------

    @Test
    void functionReturnsAValue() {
        Harness h = new Harness("""
                function "double" with argument <value>
                    return temporary value times 2
                when player joins
                    set player's coins to function "double" with argument 21
                    tell player "You have ${player's coins} coins"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "You have 42 coins"));
    }

    @Test
    void functionWithTwoArguments() {
        Harness h = new Harness("""
                function "score of" with argument <name> and argument <money>
                    return temporary money divided by 10
                when player joins
                    set global "rating" to function "score of" with argument "Steve" and argument 500
                """);
        h.engine.playerEvent("joins", h.p1);
        assertEquals(50.0, h.engine.vars.getWorld("rating").num);
    }

    @Test
    void returnStopsTheFunctionEarly() {
        Harness h = new Harness("""
                function "guard" with argument <x>
                    if temporary x is below 0
                        return 0
                    return temporary x
                when player joins
                    set player's coins to function "guard" with argument -5
                    tell player "${player's coins}"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "0"));
    }

    @Test
    void functionUsedInACondition() {
        Harness h = new Harness("""
                function "half" with argument <x>
                    return temporary x divided by 2
                when player joins
                    if function "half" with argument 10 is at least 5
                        say player "yes"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "yes"));
    }

    @Test
    void bareIsMeansEqualTo() {
        Harness h = new Harness("""
                when player joins
                    set temporary item to "diamond"
                    if temporary item is "diamond"
                        say player "Yes, diamond!"
                """);
        h.engine.playerEvent("joins", h.p1);
        assertTrue(h.world.messageContains("PlayerOne", "Yes, diamond!"));
    }

    @Test
    void functionWithoutDefinitionFailsNicely() {
        MockWorld world = new MockWorld();
        MockScheduler scheduler = new MockScheduler();
        TaleEngine engine = new TaleEngine(world, scheduler);
        assertFalse(engine.loadString("""
                function "double" with argument <value>
                    return temporary value times 2
                when player joins
                    set player's coins to function "triple" with argument 21
                """, "test.Hermes"));
        engine = new TaleEngine(new MockWorld(), new MockScheduler());
        assertFalse(engine.loadString("""
                when player joins
                    set player's coins to function "nope" with argument 1
                """, "test.Hermes"));
        assertTrue(engine.loadErrors().toString().contains("nope"));
    }
}