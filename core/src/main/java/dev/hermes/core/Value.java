package dev.hermes.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A Hermes value: a number, some text, true/false, a list, or "nothing".
 */
public final class Value {

    public enum Kind { NUMBER, TEXT, TRUTH, LIST, NONE }

    public final Kind kind;
    public final double num;
    public final String text;
    public final boolean truth;
    public final List<Value> items;

    private Value(Kind kind, double num, String text, boolean truth, List<Value> items) {
        this.kind = kind;
        this.num = num;
        this.text = text;
        this.truth = truth;
        this.items = items;
    }

    public static Value number(double n) { return new Value(Kind.NUMBER, n, null, false, null); }
    public static Value text(String s) { return new Value(Kind.TEXT, 0, s, false, null); }
    public static Value truth(boolean b) { return new Value(Kind.TRUTH, 0, null, b, null); }
    public static Value list(List<Value> l) { return new Value(Kind.LIST, 0, null, false, l == null ? new ArrayList<>() : l); }
    public static Value none() { return new Value(Kind.NONE, 0, null, false, null); }

    public boolean isNumber() { return kind == Kind.NUMBER; }
    public boolean isText() { return kind == Kind.TEXT; }
    public boolean isTruth() { return kind == Kind.TRUTH; }
    public boolean isList() { return kind == Kind.LIST; }
    public boolean isNone() { return kind == Kind.NONE; }

    /** How the value should appear when spoken. */
    public String display() {
        switch (kind) {
            case NUMBER: return num == Math.floor(num) ? String.valueOf((long) num) : String.valueOf(num);
            case TEXT: return text;
            case TRUTH: return truth ? "true" : "false";
            case LIST: {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(items.get(i).display());
                }
                return sb.append("]").toString();
            }
            default: return "nothing";
        }
    }

    public boolean equalsValue(Value other) {
        if (kind == Kind.NUMBER && other.kind == Kind.NUMBER) return num == other.num;
        if (kind == Kind.TEXT && other.kind == Kind.TEXT) return text.equals(other.text);
        if (kind == Kind.TRUTH && other.kind == Kind.TRUTH) return truth == other.truth;
        if (kind == Kind.NONE && other.kind == Kind.NONE) return true;
        if (kind == Kind.NUMBER && other.kind == Kind.TRUTH) return (num == 1) == other.truth;
        if (kind == Kind.TRUTH && other.kind == Kind.NUMBER) return truth == (other.num == 1);
        return false;
    }

    public int compareTo(Value other) {
        if (kind == Kind.NUMBER && other.kind == Kind.NUMBER) {
            return Double.compare(num, other.num);
        }
        if (kind == Kind.TEXT && other.kind == Kind.TEXT) {
            return text.compareTo(other.text);
        }
        return -1;
    }
}
