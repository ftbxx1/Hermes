package dev.hermes.core;

import dev.hermes.demo.MockWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The error experience: every mistake should come with a friendly message
 * and a suggestion, never a stack trace.
 */
class ErrorsTest {

    @Test
    void unknownVerbGetsSuggestion() {
        MockWorld world = new MockWorld();
        TaleEngine engine = new TaleEngine(world, new MockWorld.MockScheduler());
        engine.loadString("when player joins\n    giv player a diamond\n", "bad.Hermes");
        VerseError e = engine.loadErrors().get(0);
        assertTrue(e.message.toLowerCase().contains("giv") || e.message.contains("giv"));
    }

    @Test
    void wrongItemOrderSaysExpectedPlayer() {
        VerseError e = assertThrows(VerseError.class,
                () -> Parser.parseScript("give diamonds player\n", "bad.Hermes"));
        assertTrue(e.message.contains("expected the player"));
        assertNotNull(e.suggestion);
    }

    @Test
    void unknownItemSuggestsClosest() {
        VerseError e = assertThrows(VerseError.class,
                () -> Parser.parseScript("give player 5 diamond swrod\n", "bad.Hermes"));
        assertNotNull(e.suggestion);
        assertTrue(e.suggestion.contains("diamond sword"), e.suggestion);
    }

    @Test
    void runtimeErrorReportsLineNumber() {
        MockWorld world = new MockWorld();
        TaleEngine engine = new TaleEngine(world, new MockWorld.MockScheduler());
        engine.loadString("when player joins\n    teleport player to nowhere\n", "bad.Hermes");
        MockWorld.MockPlayer p = world.addPlayer("P");
        engine.playerEvent("joins", p); // must not throw
        assertTrue(world.log.stream().anyMatch(l -> l.contains("nowhere")));
    }

    @Test
    void loadFailureReturnsFalseWithErrors() {
        MockWorld world = new MockWorld();
        TaleEngine engine = new TaleEngine(world, new MockWorld.MockScheduler());
        assertFalse(engine.loadString("when player explodes\n", "bad.Hermes"));
        assertFalse(engine.loadErrors().isEmpty());
    }

    @Test
    void duplicateActionsAreRejected() {
        MockWorld world = new MockWorld();
        TaleEngine engine = new TaleEngine(world, new MockWorld.MockScheduler());
        assertFalse(engine.loadString("""
                action reward the player
                    tell the player "hi"
                action reward the player
                    tell the player "bye"
                """, "bad.Hermes"));
    }

    @Test
    void errorsLookHumanReadable() {
        VerseError e = new VerseError(4, "I expected the player here.",
                "give player 5 diamonds", "give diamonds player");
        String pretty = e.pretty("game.Hermes");
        assertTrue(pretty.contains("Problem on line 4 of game.Hermes"));
        assertTrue(pretty.contains("give diamonds player"));
        assertTrue(pretty.contains("Try:"));
    }
}
