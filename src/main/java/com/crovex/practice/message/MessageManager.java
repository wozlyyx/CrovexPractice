package com.crovex.practice.message;

import com.crovex.practice.CrovexPractice;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class MessageManager {

    private final CrovexPractice plugin;
    private File file;
    private YamlConfiguration config;

    public MessageManager(CrovexPractice plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        // Load defaults from jar to ensure no missing keys
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultValues = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultValues);
        }
    }

    public void reloadMessages() {
        loadMessages();
    }

    public Component getMessage(String path) {
        String msg = config.getString(path);
        if (msg == null) {
            msg = "<red>Message key not found: " + path;
        }
        return MiniMessage.miniMessage().deserialize(msg);
    }

    public Component getMessage(String path, String... placeholders) {
        String msg = config.getString(path);
        if (msg == null) {
            return MiniMessage.miniMessage().deserialize("<red>Message key not found: " + path);
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String search = placeholders[i];
                String replace = placeholders[i + 1];
                if (search != null && replace != null) {
                    msg = msg.replace(search, replace);
                }
            }
        }

        return MiniMessage.miniMessage().deserialize(msg);
    }

    public java.util.List<Component> getMessageList(String path, String... placeholders) {
        java.util.List<String> list = config.getStringList(path);
        java.util.List<Component> components = new java.util.ArrayList<>();
        if (list.isEmpty() && config.getDefaults() != null) {
            list = config.getDefaults().getStringList(path);
        }
        for (String line : list) {
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    String search = placeholders[i];
                    String replace = placeholders[i + 1];
                    if (search != null && replace != null) {
                        line = line.replace(search, replace);
                    }
                }
            }
            components.add(MiniMessage.miniMessage().deserialize(line));
        }
        return components;
    }

    public String getRawMessage(String path) {
        return config.getString(path, "Key not found: " + path);
    }
}
