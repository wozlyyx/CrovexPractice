package com.crovex.practice.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cuboid {

    private final String worldName;
    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;
    private final int zMin;
    private final int zMax;

    public Cuboid(Location loc1, Location loc2) {
        this.worldName = loc1.getWorld().getName();
        this.xMin = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.xMax = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.yMin = Math.min(loc1.getBlockY(), loc2.getBlockY());
        this.yMax = Math.max(loc1.getBlockY(), loc2.getBlockY());
        this.zMin = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.zMax = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
    }

    public Cuboid(String worldName, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax) {
        this.worldName = worldName;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
    }

    public boolean contains(Location loc) {
        if (loc == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        return loc.getBlockX() >= xMin && loc.getBlockX() <= xMax
                && loc.getBlockY() >= yMin && loc.getBlockY() <= yMax
                && loc.getBlockZ() >= zMin && loc.getBlockZ() <= zMax;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getXMin() {
        return xMin;
    }

    public int getXMax() {
        return xMax;
    }

    public int getYMin() {
        return yMin;
    }

    public int getYMax() {
        return yMax;
    }

    public int getZMin() {
        return zMin;
    }

    public int getZMax() {
        return zMax;
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        World world = getWorld();
        if (world == null) return blocks;
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("world", worldName);
        map.put("xMin", xMin);
        map.put("xMax", xMax);
        map.put("yMin", yMin);
        map.put("yMax", yMax);
        map.put("zMin", zMin);
        map.put("zMax", zMax);
        return map;
    }

    public static Cuboid deserialize(Map<String, Object> map) {
        return new Cuboid(
                (String) map.get("world"),
                (Integer) map.get("xMin"),
                (Integer) map.get("xMax"),
                (Integer) map.get("yMin"),
                (Integer) map.get("yMax"),
                (Integer) map.get("zMin"),
                (Integer) map.get("zMax")
        );
    }
}
