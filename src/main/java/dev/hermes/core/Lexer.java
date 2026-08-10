package dev.hermes.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Turns Hermes source text into tokens.
 *
 * <p>Hermes is line-based and indentation-based:
 * <ul>
 *   <li>blank lines and lines starting with {@code #} are skipped</li>
 *   <li>4-space (or any consistent) indentation opens a block (INDENT / DEDENT)</li>
 *   <li>{@code player's} becomes WORD(POSSESSIVE), {@code isn't} becomes WORD(NOT)</li>
 *   <li>numbers may be whole or decimal; {@code -} directly before a digit starts a number</li>
 * </ul>
 */
public final class Lexer {

    public enum Type { WORD, NUMBER, STRING, NEWLINE, INDENT, DEDENT, POSSESSIVE, NOT, EOF }

    public static final class Token {
        public final Type type;
        /** The text of the token. For words this is the canonical English form
         *  when a translation pack is active (see {@link Lang}). */
        public String text;
        /** What the user actually typed (before any translation). */
        public String raw;
        public final double num;
        public final int line;
        public final int col;

        Token(Type type, String text, double num, int line, int col) {
            this.type = type;
            this.text = text;
            this.raw = text;
            this.num = num;
            this.line = line;
            this.col = col;
        }

        /** A human-readable name used inside error messages. */
        public String describe() {
            switch (type) {
                case WORD: return "'" + text + "'";
                case NUMBER: return "'" + (num == Math.floor(num) ? String.valueOf((long) num) : String.valueOf(num)) + "'";
                case STRING: return "the text \"" + text + "\"";
                case NEWLINE: return "end of line";
                case INDENT: return "an indented block";
                case DEDENT: return "end of block";
                case POSSESSIVE: return "'s";
                case NOT: return "'not'";
                default: return "end of file";
            }
        }

        @Override
        public String toString() {
            return type + "(" + (type == Type.NUMBER ? String.valueOf(num) : text) + ")@" + line;
        }
    }

    private Lexer() {}

    public static List<Token> tokenize(String src) {
        String[] lines = src.replace("\r", "").split("\n", -1);
        List<Token> out = new ArrayList<>();
        Deque<Integer> indents = new ArrayDeque<>();
        indents.push(0);

        for (int ln = 1; ln <= lines.length; ln++) {
            String raw = lines[ln - 1];

            int indent = 0;
            while (indent < raw.length() && raw.charAt(indent) == ' ') indent++;
            if (indent < raw.length() && raw.charAt(indent) == '\t') {
                throw new VerseError(ln,
                        "I found a tab where indentation should be. Tabs confuse me — use spaces instead.",
                        null, raw);
            }
            String body = raw.substring(indent);
            String trimmed = body.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (indent > indents.peek()) {
                indents.push(indent);
                out.add(new Token(Type.INDENT, "", 0, ln, 1));
            } else if (indent < indents.peek()) {
                while (indents.peek() > indent) {
                    indents.pop();
                    out.add(new Token(Type.DEDENT, "", 0, ln, 1));
                }
                if (indents.peek() != indent) {
                    throw new VerseError(ln,
                            "This indentation doesn't line up with any previous block.",
                            null, raw);
                }
            }

            tokenizeLine(out, body, ln);
            out.add(new Token(Type.NEWLINE, "", 0, ln, Math.max(1, body.length())));
        }

        while (indents.size() > 1) {
            indents.pop();
            out.add(new Token(Type.DEDENT, "", 0, lines.length, 1));
        }
        out.add(new Token(Type.EOF, "", 0, lines.length, 1));
        translateWords(out);
        return out;
    }

    /** Replaces native words with their English forms when a language pack is active. */
    private static void translateWords(List<Token> out) {
        for (Token t : out) {
            if (t.type != Type.WORD) continue;
            String english = Lang.translateWord(t.raw);
            if (english != null && !english.equals(t.text)) {
                t.text = english;
            }
        }
    }

    private static void tokenizeLine(List<Token> out, String line, int ln) {
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);

            if (c == ' ' || c == '\t') {
                i++;
                continue;
            }
            if (c == ',') {
                i++;
                continue;
            }
            if (c == '<' || c == '>') {
                i++;
                continue;
            }
            if (c == '#') {
                return;
            }
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                boolean closed = false;
                while (i < line.length()) {
                    char d = line.charAt(i);
                    if (d == '\\' && i + 1 < line.length()) {
                        char e = line.charAt(i + 1);
                        if (e == '"') { sb.append('"'); i += 2; continue; }
                        if (e == '\\') { sb.append('\\'); i += 2; continue; }
                        if (e == 'n') { sb.append('\n'); i += 2; continue; }
                        if (e == 't') { sb.append('\t'); i += 2; continue; }
                    }
                    if (d == '"') { closed = true; i++; break; }
                    sb.append(d);
                    i++;
                }
                if (!closed) {
                    throw new VerseError(ln,
                            "This text never ends. Put a closing \" at the end of it.",
                            "tell player \"Hello there!\"");
                }
                out.add(new Token(Type.STRING, sb.toString(), 0, ln, i));
                continue;
            }

            if (Character.isDigit(c) || ((c == '-' || c == '+') && i + 1 < line.length() && Character.isDigit(line.charAt(i + 1)))) {
                int start = i;
                if (c == '-' || c == '+') i++;
                StringBuilder sb = new StringBuilder();
                if (c == '-') sb.append('-');
                boolean seenDot = false;
                while (i < line.length()) {
                    char d = line.charAt(i);
                    if (Character.isDigit(d)) { sb.append(d); i++; }
                    else if (d == '.' && !seenDot) { seenDot = true; sb.append(d); i++; }
                    else break;
                }
                if (sb.length() == 1 && sb.charAt(0) == '.') {
                    throw new VerseError(ln, "That doesn't look like a number.");
                }
                try {
                    out.add(new Token(Type.NUMBER, sb.toString(), Double.parseDouble(sb.toString()), ln, start + 1));
                } catch (NumberFormatException e) {
                    throw new VerseError(ln, "That doesn't look like a number: '" + sb + "'.");
                }
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                int start = i;
                StringBuilder sb = new StringBuilder();
                while (i < line.length()) {
                    char d = line.charAt(i);
                    if (Character.isLetterOrDigit(d) || d == '_' || d == '-') { sb.append(d); i++; }
                    else if (isCombiningMark(d)) { sb.append(d); i++; }
                    else break;
                }
                if (i < line.length() && line.charAt(i) == '\'') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == 's') {
                        out.add(new Token(Type.WORD, sb.toString().toLowerCase(), 0, ln, start + 1));
                        out.add(new Token(Type.POSSESSIVE, "'s", 0, ln, i + 1));
                        i += 2;
                        continue;
                    }
                    if (i + 1 < line.length() && line.charAt(i + 1) == 't') {
                        out.add(new Token(Type.WORD, sb.toString().toLowerCase(), 0, ln, start + 1));
                        out.add(new Token(Type.NOT, "not", 0, ln, i + 1));
                        i += 2;
                        continue;
                    }
                    throw new VerseError(ln, "I don't understand the apostrophe in '" + sb + "'.");
                }
                out.add(new Token(Type.WORD, sb.toString().toLowerCase(), 0, ln, start + 1));
                continue;
            }

            throw new VerseError(ln, "I don't understand this character: '" + c + "'");
        }
    }

    /** Combining marks (vowel signs, matras, nukta...) are part of a word in
     *  scripts like Devanagari, Bengali, Tamil, Arabic and Thai. */
    private static boolean isCombiningMark(char c) {
        int type = Character.getType(c);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}
