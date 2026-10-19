package org.example.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record DatabaseConfig(String url, String user, String password) {

    private static final String FILE_NAME = "db.properties";

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (input == null) {
                throw new IllegalStateException(FILE_NAME + " not found in src/main/resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FILE_NAME, e);
        }
        return new DatabaseConfig(
                required(properties, "db.url"),
                required(properties, "db.user"),
                required(properties, "db.password"));
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + key + " in " + FILE_NAME);
        }
        return value;
    }
}