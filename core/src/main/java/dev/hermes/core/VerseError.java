package dev.hermes.core;

/**
 * A friendly, beginner-oriented error. Every Hermes error explains what went
 * wrong on which line, and usually suggests how to fix it.
 */
public final class VerseError extends RuntimeException {

    public final int line;
    public final String message;
    public final String suggestion;
    /** Filled in by the loader with the actual source text of the line. */
    public String sourceLine;

    public VerseError(int line, String message) {
        this(line, message, null, null);
    }

    public VerseError(int line, String message, String suggestion) {
        this(line, message, suggestion, null);
    }

    public VerseError(int line, String message, String suggestion, String sourceLine) {
        super(message);
        this.line = line;
        this.message = message;
        this.suggestion = suggestion;
        this.sourceLine = sourceLine;
    }

    /** Renders the error the way a human should see it. */
    public String pretty(String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Problem on line ").append(line);
        if (fileName != null && !fileName.isEmpty()) {
            sb.append(" of ").append(fileName);
        }
        sb.append(":\n\n");
        if (sourceLine != null && !sourceLine.isBlank()) {
            sb.append("    ").append(sourceLine.strip()).append("\n\n");
        }
        sb.append(message);
        if (suggestion != null) {
            sb.append("\n\nTry:\n\n    ").append(suggestion);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return pretty(null);
    }
}
