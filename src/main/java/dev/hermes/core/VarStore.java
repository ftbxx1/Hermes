package dev.hermes.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where Hermes variables live: world variables (shared), player variables
 * (per player) and lists. Persistable to a small text file so values survive
 * server restarts.
 */
public final class VarStore {

    private final Map<String, Value> worldVars = new HashMap<>();
    private final Map<String, Map<String, Value>> playerVars = new HashMap<>();

    // ---------- world ----------
    public Value getWorld(String name) {
        return worldVars.getOrDefault(name, Value.none());
    }

    public void setWorld(String name, Value v) {
        worldVars.put(name, v);
    }

    public boolean worldHas(String name) {
        return worldVars.containsKey(name);
    }

    // ---------- player ----------
    public Value getPlayer(String player, String name) {
        Map<String, Value> m = playerVars.get(player);
        if (m == null) return Value.none();
        return m.getOrDefault(name, Value.none());
    }

    public void setPlayer(String player, String name, Value v) {
        playerVars.computeIfAbsent(player, k -> new HashMap<>()).put(name, v);
    }

    public boolean playerHas(String player, String name) {
        Map<String, Value> m = playerVars.get(player);
        return m != null && m.containsKey(name);
    }

    // ---------- lists (stored as world variables of kind LIST) ----------
    public Value list(String name) {
        Value v = worldVars.get(name);
        if (v == null || !v.isList()) {
            Value fresh = Value.list(null);
            worldVars.put(name, fresh);
            return fresh;
        }
        return v;
    }

    /** Removes a list entirely; the next use starts fresh. */
    public void deleteList(String name) {
        worldVars.remove(name);
    }

    // ---------- introspection ----------
    public Map<String, Map<String, Value>> playerVars() {
        return playerVars;
    }

    public Map<String, Value> worldVars() {
        return worldVars;
    }

    // ---------- persistence ----------
    public void save(Path file) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Value> e : worldVars.entrySet()) {
            sb.append("W ").append(e.getKey()).append(" = ").append(serialize(e.getValue())).append('\n');
        }
        for (Map.Entry<String, Map<String, Value>> e : playerVars.entrySet()) {
            for (Map.Entry<String, Value> v : e.getValue().entrySet()) {
                sb.append("P ").append(e.getKey()).append(' ').append(v.getKey())
                        .append(" = ").append(serialize(v.getValue())).append('\n');
            }
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString());
        } catch (IOException ex) {
            // persistence is best-effort
        }
    }

    public void load(Path file) {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                if (line.startsWith("W ")) {
                    int eq = line.indexOf(" = ");
                    if (eq < 3) continue;
                    worldVars.put(line.substring(2, eq), deserialize(line.substring(eq + 3)));
                } else if (line.startsWith("P ")) {
                    int eq = line.indexOf(" = ");
                    if (eq < 4) continue;
                    String head = line.substring(2, eq);
                    int sp = head.indexOf(' ');
                    if (sp < 0) continue;
                    setPlayer(head.substring(0, sp), head.substring(sp + 1), deserialize(line.substring(eq + 3)));
                }
            }
        } catch (IOException ex) {
            // best-effort
        }
    }

    private static String serialize(Value v) {
        switch (v.kind) {
            case NUMBER: return "n" + (v.num == Math.floor(v.num) ? String.valueOf((long) v.num) : String.valueOf(v.num));
            case TEXT: return "t" + v.text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
            case TRUTH: return v.truth ? "b1" : "b0";
            case LIST: {
                StringBuilder sb = new StringBuilder("l");
                for (Value item : v.items) {
                    sb.append('|').append(serialize(item));
                }
                return sb.toString();
            }
            default: return "x";
        }
    }

    private static Value deserialize(String s) {
        if (s.isEmpty()) return Value.none();
        char kind = s.charAt(0);
        String rest = s.substring(1);
        switch (kind) {
            case 'n': return Value.number(Double.parseDouble(rest));
            case 't': return Value.text(rest.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\"));
            case 'b': return Value.truth(rest.equals("1"));
            case 'l': {
                List<Value> items = new java.util.ArrayList<>();
                for (String part : rest.split("\\|", -1)) {
                    if (!part.isEmpty()) items.add(deserialize(part));
                }
                return Value.list(items);
            }
            default: return Value.none();
        }
    }
}
