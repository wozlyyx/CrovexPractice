package com.crovex.practice.arena;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import com.crovex.practice.CrovexPractice;
import com.crovex.practice.kit.Kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arena {

    private final String name;
    private Cuboid bounds;
    private Location spawn1;
    private Location spawn2;
    private Location spectatorSpawn;
    private boolean enabled;
    private boolean inUse;
    private final List<String> allowedKits = new ArrayList<>();
    private boolean supportsBlockPlace;
    private boolean supportsBlockBreak;
    private boolean supportsExplosions;

    // Block changes tracking (Location -> Original BlockData) for restoration
    private final Map<Location, BlockData> originalBlocks = new HashMap<>();

    public Arena(String name) {
        this.name = name;
        this.enabled = false;
        this.inUse = false;
        this.supportsBlockPlace = false;
        this.supportsBlockBreak = false;
        this.supportsExplosions = false;
    }

    public Arena(String name, Cuboid bounds, Location spawn1, Location spawn2, Location spectatorSpawn, boolean enabled) {
        this.name = name;
        this.bounds = bounds;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.spectatorSpawn = spectatorSpawn;
        this.enabled = enabled;
        this.inUse = false;
        this.supportsBlockPlace = false;
        this.supportsBlockBreak = false;
        this.supportsExplosions = false;
    }

    public String getName() {
        return name;
    }

    public Cuboid getBounds() {
        return bounds;
    }

    public void setBounds(Cuboid bounds) {
        this.bounds = bounds;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public boolean isEnabled() {
        return enabled && bounds != null && spawn1 != null && spawn2 != null;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public List<String> getAllowedKits() {
        return allowedKits;
    }

    public boolean isKitAllowed(String kitName) {
        Kit kit = CrovexPractice.getInstance().getKitManager().getKit(kitName);
        if (kit == null) {
            return false;
        }

        // Check compatibility with arena settings
        if (kit.isAllowBlockPlace() != this.supportsBlockPlace) {
            return false;
        }
        if (kit.isAllowBlockBreak() != this.supportsBlockBreak) {
            return false;
        }
        if (kit.isAllowExplosions() != this.supportsExplosions) {
            return false;
        }

        if (allowedKits.isEmpty()) {
            return true;
        }
        return allowedKits.contains(kitName.toLowerCase());
    }

    public Map<Location, BlockData> getOriginalBlocks() {
        return originalBlocks;
    }

    /**
     * Record a block's state before it gets changed.
     */
    public void recordBlockChange(Location loc) {
        if (!originalBlocks.containsKey(loc)) {
            originalBlocks.put(loc.clone(), loc.getBlock().getBlockData().clone());
        }
    }

    /**
     * Resets all modified blocks in this arena to their original state.
     */
    public void rollbackBlocks() {
        if (originalBlocks.isEmpty()) {
            return;
        }
        
        // Restore each block state without triggering block physics
        originalBlocks.forEach((loc, data) -> {
            loc.getBlock().setBlockData(data, false);
        });
        
        originalBlocks.clear();
    }

    public boolean isSupportsBlockPlace() {
        return supportsBlockPlace;
    }

    public void setSupportsBlockPlace(boolean supportsBlockPlace) {
        this.supportsBlockPlace = supportsBlockPlace;
    }

    public boolean isSupportsBlockBreak() {
        return supportsBlockBreak;
    }

    public void setSupportsBlockBreak(boolean supportsBlockBreak) {
        this.supportsBlockBreak = supportsBlockBreak;
    }

    public boolean isSupportsExplosions() {
        return supportsExplosions;
    }

    public void setSupportsExplosions(boolean supportsExplosions) {
        this.supportsExplosions = supportsExplosions;
    }
}
