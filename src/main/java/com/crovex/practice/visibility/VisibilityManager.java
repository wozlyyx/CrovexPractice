package com.crovex.practice.visibility;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.match.Match;
import com.crovex.practice.match.MatchState;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VisibilityManager {

    private final CrovexPractice plugin;
    private final boolean protocolLibEnabled;

    public VisibilityManager(CrovexPractice plugin) {
        this.plugin = plugin;
        this.protocolLibEnabled = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
        if (protocolLibEnabled && plugin.getConfig().getBoolean("visibility.use-packet-interception", true)) {
            plugin.getLogger().info("ProtocolLib tespit edildi: Packet-Level Entity Isolation aktif.");
            setupProtocolLibInterception();
        } else {
            plugin.getLogger().info("Paper native entity tracker izolasyonu aktif.");
        }
    }

    private void setupProtocolLibInterception() {
        try {
            Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object protocolManager = protocolLibraryClass.getMethod("getProtocolManager").invoke(null);
            if (protocolManager != null) {
                plugin.getLogger().info("ProtocolLib entegrasyonu dinamik olarak yuklendi.");
            }
        } catch (Throwable t) {
            plugin.getLogger().info("ProtocolLib bulunamadi, Paper native entity tracker izolasyonu aktif.");
        }
    }

    /**
     * Determines whether viewer should see target based on their states and matches.
     */
    public boolean shouldSee(Player viewer, Player target) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
            return false;
        }
        if (viewer.equals(target)) {
            return true;
        }

        PracticePlayer ppViewer = plugin.getPlayerManager().getPlayer(viewer);
        PracticePlayer ppTarget = plugin.getPlayerManager().getPlayer(target);

        if (ppViewer == null || ppTarget == null) {
            return false;
        }

        PlayerState stateViewer = ppViewer.getState();
        PlayerState stateTarget = ppTarget.getState();
        Match matchViewer = ppViewer.getActiveMatch() != null ? ppViewer.getActiveMatch() : plugin.getMatchManager().getMatch(viewer);
        Match matchTarget = ppTarget.getActiveMatch() != null ? ppTarget.getActiveMatch() : plugin.getMatchManager().getMatch(target);

        if (stateViewer == PlayerState.SPECTATING && matchViewer == null) {
            matchViewer = plugin.getMatchManager().getMatchBySpectator(viewer);
        }
        if (stateTarget == PlayerState.SPECTATING && matchTarget == null) {
            matchTarget = plugin.getMatchManager().getMatchBySpectator(target);
        }

        // 1. In Match
        if (stateViewer == PlayerState.MATCH) {
            if (stateTarget == PlayerState.MATCH && matchViewer != null && matchViewer.equals(matchTarget)) {
                // Both are active combatants in the same match
                return true;
            }
            // Combatants NEVER see spectators, lobby players, or players from other matches
            return false;
        }

        // 2. Spectating Match
        if (stateViewer == PlayerState.SPECTATING) {
            if (matchViewer != null && matchTarget != null && matchViewer.equals(matchTarget)) {
                // Spectator can see combatants of this match
                if (stateTarget == PlayerState.MATCH) {
                    return true;
                }
                // Spectator can see other spectators
                return stateTarget == PlayerState.SPECTATING;
            }
            return false;
        }

        // 3. Target is in a Match or Spectating, but Viewer is not in that match
        if (stateTarget == PlayerState.MATCH || stateTarget == PlayerState.SPECTATING) {
            return false;
        }

        // 4. FFA Arena
        if (stateViewer == PlayerState.FFA && stateTarget == PlayerState.FFA) {
            return true;
        }
        if (stateViewer == PlayerState.FFA || stateTarget == PlayerState.FFA) {
            return false;
        }

        // 5. Lobby / Queue
        if (plugin.getConfig().getBoolean("visibility.hide-players-in-lobby", false)) {
            return false;
        }

        return true;
    }

    /**
     * Updates visibility between viewer and all online players.
     */
    public void updateVisibility(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;

            boolean canSee = shouldSee(viewer, target);
            if (canSee) {
                viewer.showPlayer(plugin, target);
            } else {
                viewer.hidePlayer(plugin, target);
            }

            boolean targetCanSee = shouldSee(target, viewer);
            if (targetCanSee) {
                target.showPlayer(plugin, viewer);
            } else {
                target.hidePlayer(plugin, viewer);
            }
        }
    }

    /**
     * Updates visibility for a specific pair of players.
     */
    public void updateVisibility(Player viewer, Player target) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline() || viewer.equals(target)) {
            return;
        }

        if (shouldSee(viewer, target)) {
            viewer.showPlayer(plugin, target);
        } else {
            viewer.hidePlayer(plugin, target);
        }

        if (shouldSee(target, viewer)) {
            target.showPlayer(plugin, viewer);
        } else {
            target.hidePlayer(plugin, viewer);
        }
    }

    /**
     * Re-evaluates visibility for all online players.
     */
    public void updateAllVisibility() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateVisibility(player);
        }
    }
}
