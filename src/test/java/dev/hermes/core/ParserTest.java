package dev.hermes.core;

import dev.hermes.core.Ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private Script parse(String src) {
        return Parser.parseScript(src, "test.Hermes");
    }

    @Test
    void parsesTheClassicTrigger() {
        Script script = parse("when player touches water\n    kill player\n");
        assertEquals(1, script.body.size());
        WhenBlock w = (WhenBlock) script.body.get(0);
        assertEquals(Trigger.Kind.STATE, w.trigger.kind);
        assertTrue(w.trigger.conditions.get(0) instanceof TouchCond);
        assertEquals(1, w.body.size());
        assertInstanceOf(KillStmt.class, w.body.get(0));
    }

    @Test
    void parsesEventWithItemFilter() {
        Script script = parse("when player breaks diamond ore\n    add 10 to player's coins\n");
        WhenBlock w = (WhenBlock) script.body.get(0);
        assertEquals(Trigger.Kind.EVENT, w.trigger.kind);
        assertEquals("breaks", w.trigger.event);
        assertEquals("diamond ore", w.trigger.filter);
        assertInstanceOf(AddStmt.class, w.body.get(0));
    }

    @Test
    void parsesConditionsAfterAnd() {
        Script script = parse("when player breaks diamond ore and player is in the nether\n    give player 5 diamonds\n");
        WhenBlock w = (WhenBlock) script.body.get(0);
        assertEquals(1, w.trigger.conditions.size());
        assertInstanceOf(InDimensionCond.class, w.trigger.conditions.get(0));
    }

    @Test
    void parsesAllTheExampleTriggers() {
        Script script = parse("""
                when player joins the world
                    welcome player with "Welcome to the server!"

                when player walks on lava
                    damage player by 10

                when player has 5 diamonds
                    give player a diamond sword

                when player health is below 5
                    warn player "You are almost dead!"

                when it is nighttime
                    spawn zombies

                when zombie dies
                    announce "A zombie died!"

                when mob named "boss" dies
                    announce "The boss is dead!"

                when player enters area "Spawn"
                    give player speed for 10 seconds
                """);
        assertEquals(8, script.body.size());
        assertInstanceOf(WelcomeStmt.class, ((WhenBlock) script.body.get(0)).body.get(0));
        assertInstanceOf(WalkCond.class, ((WhenBlock) script.body.get(1)).trigger.conditions.get(0));
        assertInstanceOf(HasCond.class, ((WhenBlock) script.body.get(2)).trigger.conditions.get(0));
        assertInstanceOf(CmpCond.class, ((WhenBlock) script.body.get(3)).trigger.conditions.get(0));
        assertInstanceOf(TimeCond.class, ((WhenBlock) script.body.get(4)).trigger.conditions.get(0));
        assertEquals("mob dies", ((WhenBlock) script.body.get(5)).trigger.event);
        assertEquals("boss", ((WhenBlock) script.body.get(6)).trigger.filter);
        assertEquals(Trigger.Filter.MOB_NAME, ((WhenBlock) script.body.get(6)).trigger.filterType);
        assertEquals("Spawn", ((WhenBlock) script.body.get(7)).trigger.filter);
    }

    @Test
    void parsesActionsEveryAndVariables() {
        Script script = parse("""
                action reward the player
                    give the player 10 diamonds
                    give the player 100 xp
                    tell the player "You received a reward!"

                every 10 seconds
                    spawn zombie at 100 64 200

                set world's flag to true

                when player types "home"
                    teleport player to home

                mark home at 100 64 200
                """);
        ActionDef a = (ActionDef) script.body.get(0);
        assertEquals("reward", a.name);
        assertInstanceOf(EveryBlock.class, script.body.get(1));
        assertInstanceOf(SetStmt.class, script.body.get(2));
        WhenBlock w = (WhenBlock) script.body.get(3);
        assertEquals("home", w.trigger.filter);
        assertEquals(Trigger.Filter.TEXT, w.trigger.filterType);
        assertInstanceOf(MarkDef.class, script.body.get(4));
    }

    @Test
    void givesFriendlyErrorForWrongWordOrder() {
        VerseError e = assertThrows(VerseError.class, () -> parse("give diamonds player\n"));
        assertTrue(e.message.contains("expected the player"), e.message);
        assertNotNull(e.suggestion);
    }

    @Test
    void suggestsSimilarItems() {
        VerseError e = assertThrows(VerseError.class, () -> parse("give player 5 diamond swrod\n"));
        assertTrue(e.message.toLowerCase().contains("item"));
        assertTrue(e.suggestion != null && e.suggestion.contains("diamond sword"), e.suggestion);
    }

    @Test
    void complainsAboutUnknownEvents() {
        VerseError e = assertThrows(VerseError.class, () -> parse("when player does a dance\n    kill player\n"));
        assertTrue(e.message.toLowerCase().contains("event"), e.message);
    }

    @Test
    void complainsAboutMissingIndent() {
        VerseError e = assertThrows(VerseError.class, () -> parse("when player joins\nkill player\n"));
        assertTrue(e.message.toLowerCase().contains("indent"), e.message);
    }

    @Test
    void parsesListsAndLoops() {
        Script script = parse("""
                create list "quests"
                add "Find the key" to list "quests"
                if list "quests" contains "Find the key"
                    tell player "You found it!"
                else
                    tell player "Keep looking!"
                loop over list "quests" as task
                    tell player task
                repeat 3 times
                    tell player "Knock knock"
                """);
        assertEquals(5, script.body.size());
        assertInstanceOf(ListAddStmt.class, script.body.get(1));
        IfStmt iff = (IfStmt) script.body.get(2);
        assertNotNull(iff.elseBody);
        assertInstanceOf(LoopStmt.class, script.body.get(3));
        assertInstanceOf(RepeatStmt.class, script.body.get(4));
    }

    @Test
    void parsesScoreboardsTeamsAndChests() {
        Script script = parse("""
                set player's score "kills" to 0
                add 5 to player's score "kills"
                create team "red"
                put player in team "red"
                put 5 diamonds in chest at 10 64 20
                if chest at 10 64 20 has 5 diamonds
                    take 5 diamonds from chest at 10 64 20
                """);
        SetStmt score = (SetStmt) script.body.get(0);
        assertEquals("score", score.target.kind);
        assertEquals("kills", score.target.name);
        assertInstanceOf(TeamCreate.class, script.body.get(2));
        TeamStmt join = (TeamStmt) script.body.get(3);
        assertEquals("join", join.op);
        assertInstanceOf(ChestStmt.class, script.body.get(4));
        assertInstanceOf(ChestHasCond.class, ((IfStmt) script.body.get(5)).cond);    }
}
