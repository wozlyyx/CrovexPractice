package com.crovex.practice.arena;

import com.crovex.practice.CrovexPractice;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BlockRestoreManager {

    private final CrovexPractice plugin;
    private final File folder;
    private final Map<UUID, YamlConfiguration> configs = new ConcurrentHashMap<>();
    private final Map<UUID, File> files = new ConcurrentHashMap<>();

    public BlockRestoreManager(CrovexPractice plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "block_changes");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public void startTracking(UUID matchId, Arena arena) {
        File file = new File(folder, matchId.toString() + ".yml");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Match " + matchId + " icin block change dosyasi olusturulamadi!", e);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("arena", arena.getName());
        config.set("changes", new ArrayList<>());
        configs.put(matchId, config);
        files.put(matchId, file);
        saveAsync(matchId);
    }

    public void recordChange(UUID matchId, Location loc, BlockData originalData) {
        YamlConfiguration config = configs.get(matchId);
        if (config == null) return;

        synchronized (config) {
            List<Map<String, Object>> changes = (List<Map<String, Object>>) config.getList("changes");
            if (changes == null) {
                changes = new ArrayList<>();
            }

            // Check if this location is already recorded to preserve the very first original state
            boolean alreadyRecorded = false;
            for (Map<String, Object> change : changes) {
                String worldName = (String) change.get("world");
                Integer x = (Integer) change.get("x");
                Integer y = (Integer) change.get("y");
                Integer z = (Integer) change.get("z");
                if (worldName != null && x != null && y != null && z != null &&
                        worldName.equals(loc.getWorld().getName()) && 
                        x == loc.getBlockX() && y == loc.getBlockY() && z == loc.getBlockZ()) {
                    alreadyRecorded = true;
                    break;
                }
            }

            if (!alreadyRecorded) {
                Map<String, Object> change = new HashMap<>();
                change.put("world", loc.getWorld().getName());
                change.put("x", loc.getBlockX());
                change.put("y", loc.getBlockY());
                change.put("z", loc.getBlockZ());
                change.put("blockData", originalData.getAsString());
                changes.add(change);
                config.set("changes", changes);
                saveAsync(matchId);
            }
        }
    }

    private void saveAsync(UUID matchId) {
        YamlConfiguration config = configs.get(matchId);
        File file = files.get(matchId);
        if (config == null || file == null) return;

        final String data;
        synchronized (config) {
            data = config.saveToString();
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.nio.file.Files.write(file.toPath(), data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Match " + matchId + " icin block change dosyasi kaydedilemedi!", e);
            }
        });
    }

    public void restoreAndCleanup(UUID matchId) {
        YamlConfiguration config = configs.remove(matchId);
        File file = files.remove(matchId);

        if (config != null) {
            restoreFromConfig(config);
        }

        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private void restoreFromConfig(YamlConfiguration config) {
        List<?> changesList = config.getList("changes");
        if (changesList == null) return;

        for (Object obj : changesList) {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                String worldName = (String) map.get("world");
                Integer x = (Integer) map.get("x");
                Integer y = (Integer) map.get("y");
                Integer z = (Integer) map.get("z");
                String blockDataStr = (String) map.get("blockData");

                if (worldName != null && x != null && y != null && z != null && blockDataStr != null) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location loc = new Location(world, x, y, z);
                        try {
                            BlockData data = Bukkit.createBlockData(blockDataStr);
                            loc.getBlock().setBlockData(data, false);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Blok geri yuklenirken hata olustu: " + loc.toString(), e);
                        }
                    }
                }
            }
        }
    }

    public void recoverPendingRestores() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return;

        plugin.getLogger().info("Cokme sonrasi kurtarilacak " + files.length + " adet bekleyen blok duzen dosyasi bulundu. Geri yukleniyor...");

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                restoreFromConfig(config);
                
                String arenaName = config.getString("arena");
                if (arenaName != null) {
                    Arena arena = plugin.getArenaManager().getArena(arenaName);
                    if (arena != null) {
                        arena.setInUse(false);
                    }
                }
                
                file.delete();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Dosya kurtarilirken hata olustu: " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Kurtarma islemi tamamlandi.");
    }
}
