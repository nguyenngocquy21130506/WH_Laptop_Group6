package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private static final Properties properties = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("D:\\ServerWH\\WH_Laptop_Group6\\sourceScript\\LoadToStaging\\src\\main\\resources\\application.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Unable to load configuration file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
