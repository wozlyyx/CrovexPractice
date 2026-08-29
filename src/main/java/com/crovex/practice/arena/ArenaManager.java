package com.crovex.practice.arena;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.kit.Kit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ArenaManager {

    private final CrovexPractice plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<UUID, ArenaSetupSession> setupSessions = new HashMap<>();
    private File configFile;
    private YamlConfiguration config;

    public ArenaManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void loadArenas() {
        arenas.clear();
        configFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "arenas.yml dosyasi olusturulamadi!", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section != null) {
                Location spawn1 = section.getLocation("spawn1");
                Location spawn2 = section.getLocation("spawn2");
                Location specSpawn = section.getLocation("specSpawn");
                boolean enabled = section.getBoolean("enabled", false);
                boolean supportsBlockPlace = section.getBoolean("supportsBlockPlace", false);
                boolean supportsBlockBreak = section.getBoolean("supportsBlockBreak", false);
                boolean supportsExplosions = section.getBoolean("supportsExplosions", false);
                
                Cuboid bounds = null;
                if (section.contains("bounds")) {
                    Map<String, Object> boundsMap = section.getConfigurationSection("bounds").getValues(false);
                    bounds = Cuboid.deserialize(boundsMap);
                }

                Arena arena = new Arena(key, bounds, spawn1, spawn2, specSpawn, enabled);
                arena.setSupportsBlockPlace(supportsBlockPlace);
                arena.setSupportsBlockBreak(supportsBlockBreak);
                arena.setSupportsExplosions(supportsExplosions);
                if (section.contains("allowedKits")) {
                    List<String> list = section.getStringList("allowedKits");
                    arena.getAllowedKits().clear();
                    for (String s : list) {
                        arena.getAllowedKits().add(s.toLowerCase());
                    }
                }
                arenas.put(key.toLowerCase(), arena);
            }
        }
        pruneIncompatibleKits();
        plugin.getLogger().info(arenas.size() + " adet arena yuklendi.");
    }

    public void saveArenas() {
        if (config == null || configFile == null) return;

        // Clear existing config keys to prevent leftover data
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        for (Arena arena : arenas.values()) {
            String path = arena.getName();
            config.set(path + ".enabled", arena.isEnabled());
            config.set(path + ".supportsBlockPlace", arena.isSupportsBlockPlace());
            config.set(path + ".supportsBlockBreak", arena.isSupportsBlockBreak());
            config.set(path + ".supportsExplosions", arena.isSupportsExplosions());
            config.set(path + ".spawn1", arena.getSpawn1());
            config.set(path + ".spawn2", arena.getSpawn2());
            config.set(path + ".specSpawn", arena.getSpectatorSpawn());
            if (arena.getBounds() != null) {
                config.createSection(path + ".bounds", arena.getBounds().serialize());
            }
            config.set(path + ".allowedKits", arena.getAllowedKits());
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "arenas.yml dosyasi kaydedilemedi!", e);
        }
    }

    public void createArena(String name) {
        arenas.put(name.toLowerCase(), new Arena(name));
        saveArenas();
    }

    public void addArena(Arena arena) {
        arenas.put(arena.getName().toLowerCase(), arena);
    }

    public void deleteArena(String name) {
        arenas.remove(name.toLowerCase());
        saveArenas();
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Map<UUID, ArenaSetupSession> getSetupSessions() {
        return setupSessions;
    }

    public ArenaSetupSession getSetupSession(UUID uuid) {
        return setupSessions.get(uuid);
    }

    public void startSetupSession(UUID uuid, String arenaName) {
        setupSessions.put(uuid, new ArenaSetupSession(arenaName));
    }

    public void removeSetupSession(UUID uuid) {
        setupSessions.remove(uuid);
    }

    /**
     * Find a random enabled and non-busy arena that allows the specified kit.
     */
    public Arena findAvailableArena(Kit kit) {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isEnabled() && !arena.isInUse()) {
                if (kit == null || arena.isKitAllowed(kit.getName())) {
                    available.add(arena);
                }
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        return available.get(new Random().nextInt(available.size()));
    }

    /**
     * Prune all incompatible allowed kits from all arenas and save to config.
     */
    public void pruneIncompatibleKits() {
        boolean changed = false;
        for (Arena arena : arenas.values()) {
            List<String> allowed = arena.getAllowedKits();
            if (allowed.isEmpty() || (allowed.size() == 1 && allowed.get(0).equals("__none__"))) {
                continue;
            }

            java.util.Iterator<String> iter = allowed.iterator();
            while (iter.hasNext()) {
                String kName = iter.next();
                Kit k = plugin.getKitManager().getKit(kName);
                if (k == null) {
                    iter.remove();
                    changed = true;
                    continue;
                }

                boolean compatible = true;
                if (k.isAllowBlockPlace() != arena.isSupportsBlockPlace()) {
                    compatible = false;
                }
                if (k.isAllowBlockBreak() != arena.isSupportsBlockBreak()) {
                    compatible = false;
                }
                if (k.isAllowExplosions() != arena.isSupportsExplosions()) {
                    compatible = false;
                }

                if (!compatible) {
                    iter.remove();
                    changed = true;
                }
            }

            if (allowed.isEmpty()) {
                allowed.add("__none__");
                changed = true;
            }
        }
        if (changed) {
            saveArenas();
        }
    }

    /**
     * Checks if there is at least one enabled arena compatible with the specified kit.
     */
    public boolean hasCompatibleArena(Kit kit) {
        if (kit == null) return false;
        for (Arena arena : arenas.values()) {
            if (arena.isEnabled() && arena.isKitAllowed(kit.getName())) {
                return true;
            }
        }
        return false;
    }
}
