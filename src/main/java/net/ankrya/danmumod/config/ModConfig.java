package net.ankrya.danmumod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ankrya.danmumod.DanmuMod;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData configData = new ConfigData();

    public static class ConfigData {
        public int webPort = 8080;
        public boolean enableDanmu = true;
        public int displayTime = 5000;
        public double danmuSpeed = 0.7;
        public int maxDanmus = 20;
    }

    public static void loadConfig() {
        try {
            Path configFile = DanmuMod.CONFIG_PATH.resolve("config.json");

            if (!Files.exists(DanmuMod.CONFIG_PATH)) {
                Files.createDirectories(DanmuMod.CONFIG_PATH);
            }

            if (Files.exists(configFile)) {
                try (FileReader reader = new FileReader(configFile.toFile())) {
                    configData = GSON.fromJson(reader, ConfigData.class);
                    DanmuMod.info("Config loaded from " + configFile);
                }
            } else {
                saveConfig();
            }
        } catch (Exception e) {
            DanmuMod.error("Failed to load config: " + e.getMessage());
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            Path configFile = DanmuMod.CONFIG_PATH.resolve("config.json");

            try (FileWriter writer = new FileWriter(configFile.toFile())) {
                GSON.toJson(configData, writer);
                DanmuMod.info("Config saved to " + configFile);
            }
        } catch (Exception e) {
            DanmuMod.error("Failed to save config: " + e.getMessage());
        }
    }

    public static ConfigData getConfig() {
        return configData;
    }

    public static void setConfig(ConfigData newConfig) {
        configData = newConfig;
        saveConfig();
    }
}