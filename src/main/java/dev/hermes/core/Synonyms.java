package dev.hermes.core;

import java.util.Map;

/**
 * Forgiving English: alternate words people naturally type instead of the
 * exact keyword. Each entry maps a synonym to the canonical keyword Hermes
 * understands, so scripts don't need to be memorised word-perfect.
 *
 * <p>The map is applied by the lexer while it already lower-cases every word,
 * so case never matters and these spellings work in every statement.
 *
 * <p>Only unambiguous words are listed — never words that are themselves
 * keywords, common action names, or real Minecraft item/enchant/particle
 * names (which would be corrupted by a rewrite).
 */
public final class Synonyms {

    private Synonyms() {}

    private static final Map<String, String> MAP = Map.ofEntries(
        Map.entry("each", "every"),
        Map.entry("say", "tell"),
        Map.entry("message", "tell"),
        Map.entry("msg", "tell"),
        Map.entry("broadcast", "announce"),
        Map.entry("alert", "warn"),
        Map.entry("grant", "give"),
        Map.entry("tp", "teleport"),
        Map.entry("hurt", "damage"),
        Map.entry("wound", "damage"),
        Map.entry("slay", "kill"),
        Map.entry("increase", "add"),
        Map.entry("decrease", "remove"),
        Map.entry("erase", "delete"),
        Map.entry("wipe", "clear"),
        Map.entry("bounce", "launch"),
        Map.entry("hurl", "throw"),
        Map.entry("quit", "leaves"),
        Map.entry("leave", "leaves")
    );

    /** Returns the canonical keyword for a word, or the word itself. */
    public static String canonical(String word) {
        return MAP.getOrDefault(word, word);
    }
}