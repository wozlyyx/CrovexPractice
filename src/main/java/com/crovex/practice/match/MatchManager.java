package com.crovex.practice.match;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.queue.QueueType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchManager {

    private final CrovexPractice plugin;
    private final Map<UUID, Match> activeMatches = new HashMap<>();

    private final Map<UUID, Match> endedMatches = new HashMap<>();

    public MatchManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void startMatch(Player playerA, Player playerB, Kit kit, QueueType type, Arena arena) {
        Match match = new Match(plugin, playerA, playerB, kit, type, arena);
        activeMatches.put(match.getId(), match);
        match.start();
    }

    public void startPartyFFAMatch(List<Player> players, Kit kit, Arena arena) {
        Match match = new Match(plugin, players, MatchType.PARTY_FFA, kit, arena);
        activeMatches.put(match.getId(), match);
        match.start();
    }

    public void startPartySplitMatch(List<Player> teamA, List<Player> teamB, Kit kit, Arena arena) {
        Match match = new Match(plugin, teamA, teamB, MatchType.PARTY_SPLIT, kit, arena);
        activeMatches.put(match.getId(), match);
        match.start();
    }

    public void startPartyVSMatch(List<Player> teamA, List<Player> teamB, Kit kit, Arena arena) {
        Match match = new Match(plugin, teamA, teamB, MatchType.PARTY_VS, kit, arena);
        activeMatches.put(match.getId(), match);
        match.start();
    }

    public Match getMatch(UUID id) {
        Match match = activeMatches.get(id);
        if (match == null) {
            match = endedMatches.get(id);
        }
        return match;
    }

    public Match getMatch(Player player) {
        for (Match match : activeMatches.values()) {
            if (match.getInitialPlayers().contains(player.getUniqueId())) {
                return match;
            }
        }
        return null;
    }

    public Match getMatchBySpectator(Player spectator) {
        for (Match match : activeMatches.values()) {
            if (match.getSpectators().contains(spectator.getUniqueId())) {
                return match;
            }
        }
        return null;
    }

    public void removeActiveMatch(UUID id) {
        Match match = activeMatches.remove(id);
        if (match != null) {
            endedMatches.put(id, match);
            int cacheSeconds = plugin.getConfig().getInt("match.inventory-cache-seconds", 120);
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                endedMatches.remove(id);
            }, cacheSeconds * 20L);
        }
    }

    public int getActiveMatchesCount() {
        return activeMatches.size();
    }

    public Collection<Match> getMatches() {
        return activeMatches.values();
    }

    public void endAllMatches() {
        for (Match match : new HashMap<>(activeMatches).values()) {
            match.endMatch(null, null);
        }
        activeMatches.clear();
        endedMatches.clear();
    }
}
