package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        Properties dbProperties = loadDatabaseProperties();

        String url = dbProperties.getProperty("db.url");
        String user = dbProperties.getProperty("db.user");
        String password = dbProperties.getProperty("db.password");

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to database: " + connection.getCatalog());
        } catch (SQLException e) {
            System.out.println("Could not connect to database: " + e.getMessage());
        }
    }

    private static Properties loadDatabaseProperties() {
        Properties properties = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("db.properties not found in src/main/resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties", e);
        }
        return properties;
    }
}