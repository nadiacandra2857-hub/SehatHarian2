package com.sehatharian.config;
/** Explicit provisioning before starting JPro (rather than once per browser session). */
public final class SetupDatabase {
    public static void main(String[] args)throws Exception {LocalDatabase.startIfNeeded();DatabaseBootstrap.initialize();}
}
