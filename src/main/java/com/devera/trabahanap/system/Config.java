package com.devera.trabahanap.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Config loads application properties from /config.properties on classpath.
 * Keep secrets out of source control.
 */
public final class Config {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = com.devera.trabahanap.system.Config.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("Warning: config.properties not found on classpath.");
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Config() {}

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String getOrDefault(String key, String def) {
        return props.getProperty(key, def);
    }
}

