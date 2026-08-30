package com.crovex.practice.placeholder;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.player.PracticePlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PracticePlaceholder extends PlaceholderExpansion {

    private final CrovexPractice plugin;

    public PracticePlaceholder(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "crovexpractice";
    }

    @Override
    public @NotNull String getAuthor() {
        return "wozly_v2";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "elo":
                return String.valueOf(pp.getElo());
            case "wins":
                return String.valueOf(pp.getRankedWins() + pp.getUnrankedWins());
            case "losses":
                return String.valueOf(pp.getRankedLosses() + pp.getUnrankedLosses());
            case "ranked_wins":
                return String.valueOf(pp.getRankedWins());
            case "ranked_losses":
                return String.valueOf(pp.getRankedLosses());
            case "unranked_wins":
                return String.valueOf(pp.getUnrankedWins());
            case "unranked_losses":
                return String.valueOf(pp.getUnrankedLosses());
            case "winstreak":
                return String.valueOf(pp.getWinstreak());
            case "best_winstreak":
                return String.valueOf(pp.getBestWinstreak());
            case "ffa_players":
                return String.valueOf(plugin.getFfaManager().getPlayerCount());
            case "ffa_kills":
                return String.valueOf(pp.getFfaKills());
            case "ffa_deaths":
                return String.valueOf(pp.getFfaDeaths());
            case "ffa_streak":
                return String.valueOf(plugin.getFfaManager().getCurrentStreak(player));
            case "ffa_best_streak":
                return String.valueOf(pp.getFfaBestStreak());
            case "state":
                return pp.getState().name();
            case "queued_players":
                return String.valueOf(plugin.getQueueManager().getTotalQueuedCount());
            case "active_matches":
                return String.valueOf(plugin.getMatchManager().getActiveMatchesCount());
        }

        return null;
    }
}
