package dev.hermes.core;

/**
 * Lets the engine schedule repeating work ("every 10 seconds") without
 * knowing anything about the host platform.
 */
public interface Scheduler {

    void runEvery(long millis, Runnable task);

    void runLater(long millis, Runnable task);

    /** Stops a repeating task previously given to {@link #runEvery}. */
    void cancelEvery(Runnable task);
}
