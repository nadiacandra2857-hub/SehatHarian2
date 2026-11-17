package com.sehatharian.util;

/** One context owns playback handles. Implementations must never stop another output. */
public interface AudioOutput extends AutoCloseable {
    /** Capability of the backend, not a probe of physical device availability. */
    default boolean supportsPlayback() { return true; }
    default void configureSound(boolean enabled,double volume){}
    default boolean readyForAlarm(){return supportsPlayback();}
    default void playUntil(String resource,double volume,long expires){if(System.currentTimeMillis()<=expires)play(resource,volume);}
    default void resetIdentity(){stop();}
    void play(String resource, double volume);
    void stop();
    @Override void close();
}
