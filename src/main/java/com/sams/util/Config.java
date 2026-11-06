package com.sams.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads db.properties from the classpath once and exposes typed getters.
 * Optional override: if the file  $CATALINA_BASE/conf/sams.properties  exists its values win,
 * so the WAR never has to be edited when the database password changes.
 * Command-line overrides (-Dsams.db.url=...) win over both.
 */
public final class Config {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) throw new IllegalStateException("db.properties not found on classpath");
            PROPS.load(in);
            String base = System.getProperty("catalina.base");
            File external = base == null ? null : new File(base, "conf/sams.properties");
            if (external != null && external.isFile()) {
                try (InputStream ext = new FileInputStream(external)) { PROPS.load(ext); }
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Config() { }

    public static String get(String key, String def) {
        // allow -Dsams.db.url=... style overrides on the command line
        String sys = System.getProperty("sams." + key);
        if (sys != null) return sys;
        String env = System.getenv("SAMS_" + key.toUpperCase(java.util.Locale.ROOT).replace('.', '_'));
        return env != null ? env : PROPS.getProperty(key, def);
    }

    public static int getInt(String key, int def) {
        try { return Integer.parseInt(get(key, String.valueOf(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    /** Minimum attendance percentage (default 75). */
    public static double threshold() { return getInt("attendance.threshold", 75); }
}
