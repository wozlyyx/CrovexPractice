package com.crovex.practice.queue;

import com.crovex.practice.CrovexPractice;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class QueueGuiManager {

    private final CrovexPractice plugin;
    
    private File unrankedFile;
    private YamlConfiguration unrankedConfig;
    
    private File rankedFile;
    private YamlConfiguration rankedConfig;

    public QueueGuiManager(CrovexPractice plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        plugin.getDataFolder().mkdirs();
        
        unrankedFile = new File(plugin.getDataFolder(), "unranked.yml");
        if (!unrankedFile.exists()) {
            createDefaultConfig(unrankedFile, QueueType.UNRANKED);
        }
        unrankedConfig = YamlConfiguration.loadConfiguration(unrankedFile);
        if (unrankedConfig.contains("title")) {
            String title = unrankedConfig.getString("title", "");
            if (title.contains("Derecesiz Sıra Seçimi") || title.contains("Derecesiz Sıra")) {
                unrankedConfig.set("title", "<gradient:#55ff55:#77ff77><bold>Unranked Queue Selection</bold></gradient>");
                saveConfig(QueueType.UNRANKED);
            }
        }

        rankedFile = new File(plugin.getDataFolder(), "ranked.yml");
        if (!rankedFile.exists()) {
            createDefaultConfig(rankedFile, QueueType.RANKED);
        }
        rankedConfig = YamlConfiguration.loadConfiguration(rankedFile);
        if (rankedConfig.contains("title")) {
            String title = rankedConfig.getString("title", "");
            if (title.contains("Dereceli Sıra Seçimi") || title.contains("Dereceli Sıra")) {
                rankedConfig.set("title", "<gradient:#5555ff:#7777ff><bold>Ranked Queue Selection</bold></gradient>");
                saveConfig(QueueType.RANKED);
            }
        }
    }

    private void createDefaultConfig(File file, QueueType type) {
        try {
            file.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (type == QueueType.UNRANKED) {
                config.set("title", "<gradient:#55ff55:#77ff77><bold>Unranked Queue Selection</bold></gradient>");
            } else {
                config.set("title", "<gradient:#5555ff:#7777ff><bold>Ranked Queue Selection</bold></gradient>");
            }
            config.set("size", 54);
            config.set("filler", "BLACK_STAINED_GLASS_PANE");

            // Default kit slots — inner area row 1 (slots 10-16)
            config.set("kits.sumo", 10);
            config.set("kits.boxing", 12);
            config.set("kits.nodebuff", 14);
            config.set("kits.builduhc", 16);

            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating Queue GUI configuration file: " + file.getName(), e);
        }
    }

    public String getGuiTitle(QueueType type) {
        YamlConfiguration config = getConfig(type);
        return config.getString("title", type == QueueType.UNRANKED ? "Unranked Queue" : "Ranked Queue");
    }

    public int getGuiSize(QueueType type) {
        YamlConfiguration config = getConfig(type);
        return config.getInt("size", 27);
    }

    public Material getFillerMaterial(QueueType type) {
        YamlConfiguration config = getConfig(type);
        String matStr = config.getString("filler", "BLACK_STAINED_GLASS_PANE");
        try {
            return Material.valueOf(matStr.toUpperCase());
        } catch (Exception e) {
            return Material.BLACK_STAINED_GLASS_PANE;
        }
    }

    public Map<String, Integer> getKitSlots(QueueType type) {
        YamlConfiguration config = getConfig(type);
        Map<String, Integer> slots = new HashMap<>();
        if (config.contains("kits") && config.getConfigurationSection("kits") != null) {
            for (String key : config.getConfigurationSection("kits").getKeys(false)) {
                slots.put(key.toLowerCase(), config.getInt("kits." + key));
            }
        }
        return slots;
    }

    public void setKitSlots(QueueType type, Map<String, Integer> slots) {
        YamlConfiguration config = getConfig(type);
        config.set("kits", null); // Clear old slots
        for (Map.Entry<String, Integer> entry : slots.entrySet()) {
            config.set("kits." + entry.getKey().toLowerCase(), entry.getValue());
        }
        saveConfig(type);
    }

    private YamlConfiguration getConfig(QueueType type) {
        return type == QueueType.UNRANKED ? unrankedConfig : rankedConfig;
    }

    private File getFile(QueueType type) {
        return type == QueueType.UNRANKED ? unrankedFile : rankedFile;
    }

    public void saveConfig(QueueType type) {
        try {
            getConfig(type).save(getFile(type));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save Queue GUI configuration file: " + getFile(type).getName(), e);
        }
    }
}
