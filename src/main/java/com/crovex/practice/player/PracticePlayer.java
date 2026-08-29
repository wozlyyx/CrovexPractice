package com.crovex.practice.player;

import com.crovex.practice.match.Match;
import com.crovex.practice.party.Party;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;

public class PracticePlayer {

    private final UUID uuid;
    private final String name;

    // Stats
    private int unrankedWins = 0;
    private int unrankedLosses = 0;
    private int rankedWins = 0;
    private int rankedLosses = 0;
    private int elo = 1000;
    private int winstreak = 0;
    private int bestWinstreak = 0;
    private int ffaKills = 0;
    private int ffaDeaths = 0;
    private int ffaBestStreak = 0;

    // Custom Kit Layouts (KitName -> Inventory Items Array of size 36)
    private final Map<String, ItemStack[]> kitLayouts = new HashMap<>();

    // Runtime State
    private PlayerState state = PlayerState.LOBBY;
    private Match activeMatch = null;
    private Party activeParty = null;

    public PracticePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getUnrankedWins() {
        return unrankedWins;
    }

    public void setUnrankedWins(int unrankedWins) {
        this.unrankedWins = unrankedWins;
    }

    public int getUnrankedLosses() {
        return unrankedLosses;
    }

    public void setUnrankedLosses(int unrankedLosses) {
        this.unrankedLosses = unrankedLosses;
    }

    public int getRankedWins() {
        return rankedWins;
    }

    public void setRankedWins(int rankedWins) {
        this.rankedWins = rankedWins;
    }

    public int getRankedLosses() {
        return rankedLosses;
    }

    public void setRankedLosses(int rankedLosses) {
        this.rankedLosses = rankedLosses;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getWinstreak() {
        return winstreak;
    }

    public void setWinstreak(int winstreak) {
        this.winstreak = winstreak;
    }

    public int getBestWinstreak() {
        return bestWinstreak;
    }

    public void setBestWinstreak(int bestWinstreak) {
        this.bestWinstreak = bestWinstreak;
    }

    public PlayerState getState() {
        return state;
    }

    public void setState(PlayerState state) {
        this.state = state;
    }

    public Match getActiveMatch() {
        return activeMatch;
    }

    public void setActiveMatch(Match activeMatch) {
        this.activeMatch = activeMatch;
    }

    public Party getActiveParty() {
        return activeParty;
    }

    public void setActiveParty(Party activeParty) {
        this.activeParty = activeParty;
    }

    public Map<String, ItemStack[]> getKitLayouts() {
        return kitLayouts;
    }

    public ItemStack[] getKitLayout(String kitName) {
        return kitLayouts.get(kitName);
    }

    public void setKitLayout(String kitName, ItemStack[] layout) {
        kitLayouts.put(kitName, layout);
    }

    // Serialization & Deserialization
    public String serializeKitLayouts() {
        if (kitLayouts.isEmpty()) {
            return "{}";
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<String, ItemStack[]> entry : kitLayouts.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            return config.saveToString();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error serializing kit layouts for " + name, e);
            return "{}";
        }
    }

    public void deserializeKitLayouts(String serialized) {
        if (serialized == null || serialized.isEmpty() || serialized.equals("{}")) {
            return;
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(serialized);
            for (String key : config.getKeys(false)) {
                List<?> list = config.getList(key);
                if (list != null) {
                    ItemStack[] items = list.toArray(new ItemStack[0]);
                    kitLayouts.put(key, items);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error deserializing kit layouts for " + name, e);
        }
    }

    public int getFfaKills() {
        return ffaKills;
    }

    public void setFfaKills(int ffaKills) {
        this.ffaKills = ffaKills;
    }

    public int getFfaDeaths() {
        return ffaDeaths;
    }

    public void setFfaDeaths(int ffaDeaths) {
        this.ffaDeaths = ffaDeaths;
    }

    public int getFfaBestStreak() {
        return ffaBestStreak;
    }

    public void setFfaBestStreak(int ffaBestStreak) {
        this.ffaBestStreak = ffaBestStreak;
    }
}
