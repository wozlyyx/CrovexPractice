package com.crovex.practice.ffa;

import com.crovex.practice.arena.Cuboid;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class FfaArena {
    private final String name;
    private boolean enabled;
    private String kitName;
    private Location spawnLocation;
    private Cuboid bounds;
    private String commandAlias;

    public FfaArena(String name) {
        this.name = name;
        this.enabled = false;
        this.kitName = "NoDebuff";
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKitName() {
        return kitName;
    }

    public void setKitName(String kitName) {
        this.kitName = kitName;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Cuboid getBounds() {
        return bounds;
    }

    public void setBounds(Cuboid bounds) {
        this.bounds = bounds;
    }

    public String getCommandAlias() {
        return commandAlias;
    }

    public void setCommandAlias(String commandAlias) {
        this.commandAlias = commandAlias;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("enabled", enabled);
        map.put("kit", kitName);
        if (spawnLocation != null) {
            map.put("spawn", spawnLocation);
        }
        if (bounds != null) {
            map.put("bounds", bounds.serialize());
        }
        if (commandAlias != null && !commandAlias.isEmpty()) {
            map.put("command-alias", commandAlias);
        }
        return map;
    }

    public static FfaArena deserialize(String name, org.bukkit.configuration.ConfigurationSection section) {
        FfaArena arena = new FfaArena(name);
        arena.setEnabled(section.getBoolean("enabled", false));
        arena.setKitName(section.getString("kit", "NoDebuff"));
        arena.setSpawnLocation(section.getLocation("spawn"));
        if (section.contains("bounds")) {
            org.bukkit.configuration.ConfigurationSection boundsSec = section.getConfigurationSection("bounds");
            if (boundsSec != null) {
                arena.setBounds(Cuboid.deserialize(boundsSec.getValues(false)));
            }
        }
        arena.setCommandAlias(section.getString("command-alias"));
        return arena;
    }
}
