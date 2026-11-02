package com.sehatharian.util;

/** One context owns playback handles. Implementations must never stop another output. */
public interface AudioOutput extends AutoCloseable {
    /** Capability of the backend, not a probe of physical device availability. */
    default boolean supportsPlayback() { return true; }
    void play(String resource, double volume);
    void stop();
    @Override void close();
}
