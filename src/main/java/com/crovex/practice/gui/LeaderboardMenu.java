package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardMenu extends Menu {

    public enum LeaderboardCategory {
        ELO("elo"),
        FFA_KILLS("ffa_kills"),
        FFA_STREAK("ffa_best_streak");

        private final String column;
        LeaderboardCategory(String column) { this.column = column; }
        public String getColumn() { return column; }
    }

    private final CrovexPractice plugin;
    private LeaderboardCategory category;
    private List<PracticePlayer> entries = new ArrayList<>();

    // Podium layout: slots for positions 1-10
    private static final int[] PODIUM_SLOTS = {13, 21, 23, 28, 29, 30, 31, 32, 33, 34};

    // Podium rank medals
    private static final String[] RANK_MEDALS = {"①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩"};

    // Background glass colors for rank positions
    private static final Material[] RANK_BORDERS = {
        Material.YELLOW_STAINED_GLASS_PANE,   // 1st - Gold
        Material.LIGHT_GRAY_STAINED_GLASS_PANE, // 2nd - Silver
        Material.ORANGE_STAINED_GLASS_PANE,  // 3rd - Bronze
        Material.WHITE_STAINED_GLASS_PANE,   // 4th+
        Material.WHITE_STAINED_GLASS_PANE,
        Material.WHITE_STAINED_GLASS_PANE,
        Material.WHITE_STAINED_GLASS_PANE,
        Material.WHITE_STAINED_GLASS_PANE,
        Material.WHITE_STAINED_GLASS_PANE,
        Material.WHITE_STAINED_GLASS_PANE
    };

    public LeaderboardMenu(CrovexPractice plugin, LeaderboardCategory category, List<PracticePlayer> entries) {
        super(45, plugin.getMessageManager().getMessage("gui.leaderboard.title"));
        this.plugin = plugin;
        this.category = category;
        this.entries = entries;
    }

    @Override
    public void setMenuItems() {
        // Background fill
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 45; i++) inventory.setItem(i, filler);

        // ---- Category Buttons (row 1, slots 10, 13, 16) ----
        setCategory(10, LeaderboardCategory.ELO,        Material.DIAMOND_SWORD,   "gui.leaderboard.cat-elo");
        setCategory(13, LeaderboardCategory.FFA_KILLS,  Material.IRON_SWORD,      "gui.leaderboard.cat-ffa-kills");
        setCategory(16, LeaderboardCategory.FFA_STREAK, Material.BLAZE_ROD,       "gui.leaderboard.cat-ffa-streak");

        // ---- Divider row (slots 9, 17 borders, filler row 18-26) ----
        // Already filled by filler loop above.

        // ---- Close button ----
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(plugin.getMessageManager().getMessage("gui.leaderboard.close"));
            close.setItemMeta(closeMeta);
        }
        inventory.setItem(40, close);

        // ---- Podium entries ----
        for (int i = 0; i < entries.size() && i < PODIUM_SLOTS.length; i++) {
            PracticePlayer pp = entries.get(i);
            int slot = PODIUM_SLOTS[i];

            // Rank border
            ItemStack border = new ItemStack(RANK_BORDERS[Math.min(i, RANK_BORDERS.length - 1)]);
            ItemMeta borderMeta = border.getItemMeta();
            if (borderMeta != null) {
                String color = i == 0 ? "<gold>" : i == 1 ? "<gray>" : i == 2 ? "<#cd7f32>" : "<white>";
                borderMeta.displayName(MiniMessage.miniMessage().deserialize(color + "<bold>" + RANK_MEDALS[i] + " " + pp.getName() + "</bold>"));
                border.setItemMeta(borderMeta);
            }

            // Player skull
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(pp.getName());
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(offlinePlayer);
                skullMeta.displayName(buildRankDisplay(i, pp));
                skullMeta.lore(buildRankLore(i, pp));
                skull.setItemMeta(skullMeta);
            }
            inventory.setItem(slot, skull);
        }
    }

    private void setCategory(int slot, LeaderboardCategory cat, Material mat, String nameKey) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean isActive = this.category == cat;
            meta.displayName(plugin.getMessageManager().getMessage(nameKey));
            List<Component> lore = new ArrayList<>();
            if (isActive) {
                lore.add(MiniMessage.miniMessage().deserialize("<green>▶ Currently Displaying"));
            } else {
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to View"));
            }
            meta.lore(lore);

            // Enchantment glow for active category
            if (isActive) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private Component buildRankDisplay(int rank, PracticePlayer pp) {
        String medal = RANK_MEDALS[rank];
        String color = rank == 0 ? "<gold>" : rank == 1 ? "<gray>" : rank == 2 ? "<#cd7f32>" : "<white>";
        return MiniMessage.miniMessage().deserialize(color + "<bold>" + medal + " " + pp.getName() + "</bold>");
    }

    private List<Component> buildRankLore(int rank, PracticePlayer pp) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        switch (category) {
            case ELO:
                lore.add(MiniMessage.miniMessage().deserialize("<gray>ELO: <gold>" + pp.getElo()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Ranked W/L: <green>" + pp.getRankedWins() + "<gray>/<red>" + pp.getRankedLosses()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Unranked W/L: <green>" + pp.getUnrankedWins() + "<gray>/<red>" + pp.getUnrankedLosses()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Best Streak: <yellow>" + pp.getBestWinstreak()));
                break;
            case FFA_KILLS:
                lore.add(MiniMessage.miniMessage().deserialize("<gray>FFA Kills: <green>" + pp.getFfaKills()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>FFA Deaths: <red>" + pp.getFfaDeaths()));
                int deaths = pp.getFfaDeaths() == 0 ? 1 : pp.getFfaDeaths();
                String kdr = String.format("%.2f", (double) pp.getFfaKills() / deaths);
                lore.add(MiniMessage.miniMessage().deserialize("<gray>K/D Ratio: <aqua>" + kdr));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Best FFA Streak: <yellow>" + pp.getFfaBestStreak()));
                break;
            case FFA_STREAK:
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Best FFA Streak: <gold>" + pp.getFfaBestStreak()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>FFA Kills: <green>" + pp.getFfaKills()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>FFA Deaths: <red>" + pp.getFfaDeaths()));
                break;
        }

        lore.add(Component.empty());
        String rankLabel = rank == 0 ? "<gold>🏆 #1 Place" : rank == 1 ? "<gray>🥈 #2 Place" : rank == 2 ? "<#cd7f32>🥉 #3 Place" : "<white>#" + (rank + 1) + " Place";
        lore.add(MiniMessage.miniMessage().deserialize(rankLabel));
        return lore;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Close button
        if (slot == 40) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Category buttons
        LeaderboardCategory newCat = null;
        if (slot == 10) newCat = LeaderboardCategory.ELO;
        else if (slot == 13) newCat = LeaderboardCategory.FFA_KILLS;
        else if (slot == 16) newCat = LeaderboardCategory.FFA_STREAK;

        if (newCat != null && newCat != this.category) {
            final LeaderboardCategory selectedCat = newCat;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
            player.sendMessage(plugin.getMessageManager().getMessage("gui.leaderboard.loading"));

            plugin.getDatabaseManager()
                    .getTopPlayers(selectedCat.getColumn(), 10)
                    .thenAccept(topPlayers -> Bukkit.getScheduler().runTask(plugin, () -> {
                        LeaderboardMenu newMenu = new LeaderboardMenu(plugin, selectedCat, topPlayers);
                        newMenu.open(player);
                    }));
        }
    }
}
