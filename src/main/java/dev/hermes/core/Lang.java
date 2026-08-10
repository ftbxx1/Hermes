package dev.hermes.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The language layer. Hermes scripts are usually written in English
 * ("give player 1 diamond"), but every keyword and vocabulary word can be
 * translated so scripts read naturally in other languages too.
 *
 * <p>Translation packs are simple text files, one line per word:
 *
 * <pre>
 *   # Spanish
 *   when=cuando
 *   player=jugador
 *   diamond sword=espada de diamante
 * </pre>
 *
 * <p>The left side is always the canonical English word or phrase; the right
 * side is the native form. Packs ship inside the jar under /lang/*.lang and
 * can be extended with custom files in a folder set with
 * {@link #setOverrideFolder}. Pick a language with {@link #setLanguage}.
 *
 * <p>The lexer translates single native words to English as it tokenizes, so
 * the parser keeps matching English keywords internally. Longer native
 * phrases ("espada de diamante" = "diamond sword") are resolved by the
 * parser's greedy lookups via {@link #translatePhrase}.
 */
public final class Lang {

    /** native phrase -> canonical English phrase. */
    private static final Map<String, String> nativeToEnglish = new HashMap<>();

    private static String current = "en";
    private static Path overrideFolder;

    private Lang() {}

    /** The active language name ("en", "es", ...). */
    public static String name() {
        return current;
    }

    /** Picks the active language and loads its pack (falls back to English). */
    public static void setLanguage(String name) {
        nativeToEnglish.clear();
        current = name == null ? "en" : name.toLowerCase().trim();
        if (current.isEmpty()) current = "en";
        if (current.equals("en")) return;
        try (InputStream in = Lang.class.getResourceAsStream("/lang/" + current + ".lang")) {
            if (in == null) {
                current = "en";
                return;
            }
            loadLines(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            current = "en";
            return;
        }
        if (overrideFolder != null) {
            Path custom = overrideFolder.resolve(current + ".lang");
            if (Files.isRegularFile(custom)) {
                try {
                    loadLines(Files.readString(custom, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // ignore: keep the bundled pack
                }
            }
        }
    }

    /** A folder searched for custom <language>.lang packs after the bundled ones. */
    public static void setOverrideFolder(Path folder) {
        overrideFolder = folder;
    }

    private static void loadLines(String text) {
        for (String line : text.replace("\r", "").split("\n")) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            int eq = l.indexOf('=');
            if (eq <= 0 || eq == l.length() - 1) continue;
            String english = l.substring(0, eq).trim().toLowerCase();
            String nativeForm = l.substring(eq + 1).trim().toLowerCase();
            if (english.isEmpty() || nativeForm.isEmpty()) continue;
            nativeToEnglish.putIfAbsent(nativeForm, english);
        }
    }

    /** All native forms that translate to a canonical English word. */
    public static List<String> nativeWords() {
        return List.copyOf(nativeToEnglish.keySet());
    }

    /** The English word for a single native word, or null to keep it as-is. */
    public static String translateWord(String word) {
        return nativeToEnglish.get(word.toLowerCase());
    }

    /** The English phrase for a native phrase, or null. */
    public static String translatePhrase(String phrase) {
        return nativeToEnglish.get(phrase.trim().toLowerCase());
    }

    /** True when the given native word should be replaced while lexing. */
    public static boolean translates(String word) {
        return nativeToEnglish.containsKey(word.toLowerCase());
    }
}
