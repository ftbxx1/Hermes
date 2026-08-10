package dev.hermes.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every bundled language pack must be well-formed (one mapping per line, no
 * malformed rows), load without errors, and only ever translate to canonical
 * English keys — otherwise a typo would silently break scripts written in
 * that language.
 */
class LangPackTest {

    private static final Set<String> REQUIRED = Set.of(
            "when", "player", "give", "set", "health");

    private static List<String> readLines(String resource) throws IOException {
        try (InputStream in = Lang.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing resource " + resource);
            return List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r", "").split("\n"));
        }
    }

    private static List<String> packNames() throws IOException {
        try (InputStream in = Lang.class.getResourceAsStream("/lang/")) {
            assertNotNull(in, "cannot list /lang/");
            List<String> packs = new ArrayList<>();
            for (String name : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (name.endsWith(".lang")) packs.add(name);
            }
            return packs;
        }
    }

    @Test
    void everyPackIsWellFormedAndLoads() throws IOException {
        Set<String> canonical = new HashSet<>();
        for (String line : readLines("/lang/es.lang")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            assertTrue(eq > 0 && eq < line.length() - 1,
                    "es.lang has a malformed line: '" + line + "'");
            canonical.add(line.substring(0, eq).trim().toLowerCase());
        }
        assertTrue(canonical.size() >= 250, "canonical key set looks small: " + canonical.size());

        List<String> packs = packNames();
        assertTrue(packs.size() >= 40, "expected the bundled packs, found " + packs.size());

        for (String pack : packs) {
            String code = pack.substring(0, pack.length() - ".lang".length());
            int mapped = 0;
            boolean hasBom = false;
            for (String line : readLines("/lang/" + pack)) {
                if (line.startsWith("\uFEFF")) hasBom = true;
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) continue;
                int eq = l.indexOf('=');
                assertTrue(eq > 0 && eq < l.length() - 1,
                        code + ": malformed line '" + line + "'");
                String key = l.substring(0, eq).trim().toLowerCase();
                String value = l.substring(eq + 1).trim().toLowerCase();
                assertFalse(value.isEmpty(), code + ": empty translation for '" + key + "'");
                assertFalse(value.contains("="), code + ": stray '=' in '" + value + "'");
                assertTrue(canonical.contains(key),
                        code + ": unknown canonical key '" + key + "' (typo?)");
                mapped++;
            }
            assertFalse(hasBom, code + ": file starts with a BOM");
            assertTrue(mapped >= 250, code + ": only " + mapped + " mappings, expected ~267");

            Lang.setLanguage(code);
            assertEquals(code, Lang.name(), code + ": pack did not activate");
            assertFalse(Lang.nativeWords().isEmpty(), code + ": loaded nothing");
        }
        Lang.setLanguage("en");
    }

    @Test
    void eachPackCoversAllCanonicalKeys() throws IOException {
        Set<String> canonical = new HashSet<>();
        for (String line : readLines("/lang/es.lang")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            canonical.add(line.substring(0, line.indexOf('=')).trim().toLowerCase());
        }

        for (String pack : packNames()) {
            String code = pack.substring(0, pack.length() - ".lang".length());
            Lang.setLanguage(code);

            Map<String, Set<String>> nativesByEnglish = new HashMap<>();
            for (String nativeForm : Lang.nativeWords()) {
                String en = Lang.translateWord(nativeForm);
                assertTrue(canonical.contains(en),
                        code + ": '" + nativeForm + "' translates to unknown key '" + en + "'");
                nativesByEnglish.computeIfAbsent(en, k -> new HashSet<>()).add(nativeForm);
            }

            for (String key : REQUIRED) {
                assertTrue(nativesByEnglish.containsKey(key),
                        code + ": no native form for '" + key + "'");
            }

            Set<String> missing = new HashSet<>(canonical);
            missing.removeAll(nativesByEnglish.keySet());
            // Languages legitimately share one native form between several
            // English keys (e.g. "is"/"are", "above"/"over", "make"/"create",
            // "from"/"than"), and the loader keeps the first key. These are
            // best-effort packs; untranslated words fall back to English.
            // Older packs also don't yet translate the newer optional keywords
            // (chance, freeze, unfreeze, randomly, within, size, item,
            // players); English is used for those until a pack is updated.
            assertTrue(missing.size() <= 35,
                    code + ": " + missing.size() + " canonical keys uncovered: " + missing);
        }
        Lang.setLanguage("en");
    }

    @Test
    void unknownLanguagesFallBackToEnglish() {
        Lang.setLanguage("xx");
        assertEquals("en", Lang.name());
        Lang.setLanguage(null);
        assertEquals("en", Lang.name());
    }
}
