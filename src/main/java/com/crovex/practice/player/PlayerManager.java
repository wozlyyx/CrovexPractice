package com.crovex.practice.player;

import com.crovex.practice.CrovexPractice;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerManager {

    private final CrovexPractice plugin;
    private final Map<UUID, PracticePlayer> players = new HashMap<>();

    public PlayerManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<PracticePlayer> loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        return plugin.getDatabaseManager().loadPlayer(uuid, player.getName())
                .thenApply(practicePlayer -> {
                    if (player.isOnline()) {
                        players.put(uuid, practicePlayer);
                    }
                    return practicePlayer;
                });
    }

    public void unloadPlayer(Player player) {
        PracticePlayer pp = players.remove(player.getUniqueId());
        if (pp != null) {
            plugin.getDatabaseManager().savePlayerSync(pp);
        }
    }

    public PracticePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public PracticePlayer getPlayer(Player player) {
        if (player == null) return null;
        PracticePlayer pp = players.get(player.getUniqueId());
        if (pp == null) {
            pp = new PracticePlayer(player.getUniqueId(), player.getName());
            players.put(player.getUniqueId(), pp);
        }
        return pp;
    }

    public void saveAllPlayersSync() {
        for (PracticePlayer pp : players.values()) {
            plugin.getDatabaseManager().savePlayerSync(pp);
        }
    }

    public void giveLobbyItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.getActivePotionEffects().clear();

        // 1. Unranked Queue (Iron Sword)
        ItemStack unranked = new ItemStack(Material.IRON_SWORD);
        ItemMeta unrankedMeta = unranked.getItemMeta();
        unrankedMeta.displayName(plugin.getMessageManager().getMessage("lobby.items.unranked"));
        unranked.setItemMeta(unrankedMeta);

        // 2. Ranked Queue (Diamond Sword)
        ItemStack ranked = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta rankedMeta = ranked.getItemMeta();
        rankedMeta.displayName(plugin.getMessageManager().getMessage("lobby.items.ranked"));
        ranked.setItemMeta(rankedMeta);

        // 3. Kit Editor (Book)
        ItemStack kitEdit = new ItemStack(Material.BOOK);
        ItemMeta kitEditMeta = kitEdit.getItemMeta();
        kitEditMeta.displayName(plugin.getMessageManager().getMessage("lobby.items.kitedit"));
        kitEdit.setItemMeta(kitEditMeta);

        // 4. Create/Manage Party (Nether Star)
        ItemStack party = new ItemStack(Material.NETHER_STAR);
        ItemMeta partyMeta = party.getItemMeta();
        partyMeta.displayName(plugin.getMessageManager().getMessage("lobby.items.party"));
        party.setItemMeta(partyMeta);

        // 5. Stats Viewer (Paper)
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.displayName(plugin.getMessageManager().getMessage("lobby.items.stats"));
        stats.setItemMeta(statsMeta);

        // 6. Leaderboard (Bookshelf)
        ItemStack leaderboard = new ItemStack(Material.BOOKSHELF);
        ItemMeta lbMeta = leaderboard.getItemMeta();
        lbMeta.displayName(plugin.getMessageManager().getMessage("lobby.items.leaderboard"));
        leaderboard.setItemMeta(lbMeta);

        int unrankedSlot = plugin.getConfig().getInt("lobby.items.unranked-slot", 0);
        int rankedSlot = plugin.getConfig().getInt("lobby.items.ranked-slot", 1);
        int kitEditSlot = plugin.getConfig().getInt("lobby.items.kit-editor-slot", 4);
        int lbSlot = plugin.getConfig().getInt("lobby.items.leaderboard-slot", 6);
        int partySlot = plugin.getConfig().getInt("lobby.items.party-slot", 7);
        int statsSlot = plugin.getConfig().getInt("lobby.items.stats-slot", 8);

        boolean unrankedEnabled = plugin.getConfig().getBoolean("queue.unranked-enabled", true);
        boolean rankedEnabled = plugin.getConfig().getBoolean("queue.ranked-enabled", true);

        if (unrankedEnabled) {
            player.getInventory().setItem(unrankedSlot, unranked);
        }
        if (rankedEnabled) {
            player.getInventory().setItem(rankedSlot, ranked);
        }
        player.getInventory().setItem(kitEditSlot, kitEdit);
        player.getInventory().setItem(lbSlot, leaderboard);
        player.getInventory().setItem(partySlot, party);
        player.getInventory().setItem(statsSlot, stats);

        player.updateInventory();
    }

    public void resetPlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.getActivePotionEffects().clear();
        
        PracticePlayer pp = getPlayer(player);
        if (pp != null) {
            pp.setState(PlayerState.LOBBY);
            pp.setActiveMatch(null);
        }
        
        giveLobbyItems(player);

        if (plugin.getVisibilityManager() != null) {
            plugin.getVisibilityManager().updateVisibility(player);
        }

        // Teleport to lobby spawn
        Location lobbySpawn = null;
        try {
            lobbySpawn = plugin.getConfig().getLocation("lobby-spawn");
        } catch (Exception e) {}
        if (lobbySpawn == null && !Bukkit.getWorlds().isEmpty()) {
            lobbySpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }
    }
}
