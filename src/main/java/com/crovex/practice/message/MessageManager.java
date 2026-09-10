package com.crovex.practice.message;

import com.crovex.practice.CrovexPractice;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MessageManager {

    private final CrovexPractice plugin;
    private File file;
    private YamlConfiguration config;
    private String currentLanguage;

    public MessageManager(CrovexPractice plugin) {
        this.plugin = plugin;
        saveAllDefaultLanguages();
        loadMessages();
    }

    public void saveAllDefaultLanguages() {
        File langDir = new File(plugin.getDataFolder(), "languages");
        langDir.mkdirs();
        String[] supportedLanguages = {"messages_en.yml", "messages_tr.yml", "messages_es.yml", "messages_fr.yml", "messages.yml"};
        for (String langFile : supportedLanguages) {
            File target = new File(langDir, langFile);
            if (!target.exists()) {
                try {
                    plugin.saveResource("languages/" + langFile, false);
                } catch (IllegalArgumentException ignored) {
                    try {
                        plugin.saveResource(langFile, false);
                    } catch (IllegalArgumentException ignored2) {}
                }
            }
        }
    }

    public void loadMessages() {
        this.currentLanguage = plugin.getConfig().getString("language", "tr").toLowerCase();
        
        String fileName = "messages_" + currentLanguage + ".yml";
        File langDir = new File(plugin.getDataFolder(), "languages");
        
        file = new File(langDir, fileName);
        if (!file.exists()) {
            // Check legacy root folder
            File legacyFile = new File(plugin.getDataFolder(), fileName);
            if (legacyFile.exists()) {
                file = legacyFile;
            } else {
                // Fallback to messages.yml inside languages folder
                file = new File(langDir, "messages.yml");
                if (!file.exists()) {
                    File legacyFallback = new File(plugin.getDataFolder(), "messages.yml");
                    if (legacyFallback.exists()) {
                        file = legacyFallback;
                    } else {
                        try {
                            plugin.saveResource("languages/messages.yml", false);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        // Load defaults from corresponding JAR resource
        InputStream langStream = plugin.getResource("languages/" + fileName);
        if (langStream == null) {
            langStream = plugin.getResource(fileName);
        }
        if (langStream == null) {
            langStream = plugin.getResource("languages/messages.yml");
        }
        if (langStream == null) {
            langStream = plugin.getResource("messages.yml");
        }
        if (langStream != null) {
            YamlConfiguration defaultValues = YamlConfiguration.loadConfiguration(new InputStreamReader(langStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultValues);
        }

        plugin.getLogger().info("Dil yapilandirmasi yuklendi: " + currentLanguage.toUpperCase() + " (languages/" + file.getName() + ")");
    }

    public void setLanguage(String lang) {
        if (lang == null || lang.trim().isEmpty()) return;
        this.currentLanguage = lang.trim().toLowerCase();
        plugin.getConfig().set("language", this.currentLanguage);
        plugin.saveConfig();
        loadMessages();
    }

    public String getLanguage() {
        return currentLanguage;
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

    public List<Component> getMessageList(String path, String... placeholders) {
        List<String> list = config.getStringList(path);
        List<Component> components = new ArrayList<>();
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
