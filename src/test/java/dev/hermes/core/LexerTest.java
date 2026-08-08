package dev.hermes.core;

import dev.hermes.core.Lexer.Token;
import dev.hermes.core.Lexer.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private List<Token> lex(String src) {
        return Lexer.tokenize(src);
    }

    @Test
    void tokenizesSimpleTrigger() {
        List<Token> toks = lex("when player joins\n    kill player\n");
        List<String> words = toks.stream().filter(t -> t.type == Type.WORD).map(t -> t.text).toList();
        assertEquals(List.of("when", "player", "joins", "kill", "player"), words);
        assertTrue(toks.stream().anyMatch(t -> t.type == Type.INDENT));
        assertTrue(toks.stream().anyMatch(t -> t.type == Type.DEDENT));
    }

    @Test
    void handlesPossessiveAndContractions() {
        List<Token> toks = lex("set player's coins to 100\nif player isn't in the nether\n    stop\n");
        assertEquals(Type.WORD, toks.get(0).type);
        assertEquals("player", toks.get(1).text);
        assertEquals(Type.POSSESSIVE, toks.get(2).type);
        assertEquals("coins", toks.get(3).text);
        assertTrue(toks.stream().anyMatch(t -> t.type == Type.NOT));
    }

    @Test
    void skipsCommentsAndBlankLines() {
        List<Token> toks = lex("# hi there\n\nwhen player joins\n    # inner\n    tell player \"hi\"\n");
        assertFalse(toks.stream().anyMatch(t -> t.text.contains("hi there")));
        assertTrue(toks.stream().anyMatch(t -> t.type == Type.WORD && t.text.equals("joins")));
    }

    @Test
    void parsesNumbersAndStrings() {
        List<Token> toks = lex("damage player by 10.5\ntell player \"say \\\"hi\\\"\"\n");
        Token num = toks.stream().filter(t -> t.type == Type.NUMBER).findFirst().orElseThrow();
        assertEquals(10.5, num.num);
        Token str = toks.stream().filter(t -> t.type == Type.STRING).findFirst().orElseThrow();
        assertEquals("say \"hi\"", str.text);
    }

    @Test
    void toleratesCommasInCoordinates() {
        List<Token> toks = lex("mark home at 100, 64, 200\n");
        assertFalse(toks.stream().anyMatch(t -> t.type == Type.WORD && t.text.equals(",")));
    }

    @Test
    void rejectsTabs() {
        VerseError e = assertThrows(VerseError.class, () -> lex("when player joins\n\tkill player\n"));
        assertTrue(e.message.toLowerCase().contains("tab"));
    }

    @Test
    void rejectsMismatchedIndent() {
        VerseError e = assertThrows(VerseError.class, () -> lex("when player joins\n    tell player \"a\"\n  stop\n"));
        assertTrue(e.message.toLowerCase().contains("indentation"));
    }

    @Test
    void rejectsUnclosedString() {
        VerseError e = assertThrows(VerseError.class, () -> lex("tell player \"oops\n"));
        assertTrue(e.message.toLowerCase().contains("never ends"));
    }
}
