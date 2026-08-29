package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.ffa.FfaArena;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class AdminFfaMenu extends Menu {

    private final CrovexPractice plugin;
    private final int page;
    private final NamespacedKey ffaKey;

    public AdminFfaMenu(CrovexPractice plugin) {
        this(plugin, 0);
    }

    public AdminFfaMenu(CrovexPractice plugin, int page) {
        super(36, plugin.getMessageManager().getMessage("gui.admin.ffa-list-title", "%page%", String.valueOf(page + 1)));
        this.plugin = plugin;
        this.page = page;
        this.ffaKey = new NamespacedKey(plugin, "ffa_name");
    }

    @Override
    public void setMenuItems() {
        // Fill border with black glass pane, and inner slots with grey glass panes
        ItemStack blackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blackFillerMeta = blackFiller.getItemMeta();
        if (blackFillerMeta != null) {
            blackFillerMeta.displayName(Component.empty());
            blackFiller.setItemMeta(blackFillerMeta);
        }

        ItemStack grayFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayFillerMeta = grayFiller.getItemMeta();
        if (grayFillerMeta != null) {
            grayFillerMeta.displayName(Component.empty());
            grayFiller.setItemMeta(grayFillerMeta);
        }

        for (int i = 0; i < 36; i++) {
            if (i < 9 || i >= 27 || i == 9 || i == 17 || i == 18 || i == 26) {
                inventory.setItem(i, blackFiller);
            } else {
                inventory.setItem(i, grayFiller);
            }
        }

        List<FfaArena> arenas = new ArrayList<>(plugin.getFfaManager().getFfaArenas());
        int total = arenas.size();
        int pageSize = 14;
        int startIndex = page * pageSize;
        int endIndex = Math.min(total, (page + 1) * pageSize);

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int slotIndex = 0;

        String setVal = plugin.getMessageManager().getRawMessage("gui.admin.editor.set");
        String notSetVal = plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");

        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= slots.length) break;

            FfaArena arena = arenas.get(i);
            int slot = slots[slotIndex];

            ItemStack item = new ItemStack(arena.isEnabled() ? Material.MAP : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(ffaKey, PersistentDataType.STRING, arena.getName());
                meta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-icon-name", "%arena%", arena.getName()));
                
                String statusStr = arena.isEnabled() ? setVal : notSetVal;
                String kitStr = (arena.getKitName() != null && !arena.getKitName().isEmpty()) ? arena.getKitName() : notSetVal;
                String boundsStr = arena.getBounds() != null ? setVal : notSetVal;
                String spawnStr = arena.getSpawnLocation() != null ? setVal : notSetVal;

                meta.lore(plugin.getMessageManager().getMessageList("gui.admin.ffa-icon-lore",
                        "%status%", statusStr,
                        "%kit%", kitStr,
                        "%bounds%", boundsStr,
                        "%spawn%", spawnStr
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slotIndex++;
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#55ff55:#3fcf3f>⬅ Previous Page</gradient>"));
                prev.setItemMeta(prevMeta);
            }
            inventory.setItem(9, prev);
        }

        if (endIndex < total) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#55ff55:#3fcf3f>Next Page ➡</gradient>"));
                next.setItemMeta(nextMeta);
            }
            inventory.setItem(26, next);
        }

        // Back arrow to Main Admin Menu (slot 31)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.back"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(31, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR 
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE 
                || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 31) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AdminArenaMenu(plugin).open(player);
            return;
        }

        if (slot == 9 && page > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AdminFfaMenu(plugin, page - 1).open(player);
            return;
        }

        if (slot == 26) {
            List<FfaArena> arenas = new ArrayList<>(plugin.getFfaManager().getFfaArenas());
            if ((page + 1) * 14 < arenas.size()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new AdminFfaMenu(plugin, page + 1).open(player);
                return;
            }
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(ffaKey, PersistentDataType.STRING)) {
            String name = meta.getPersistentDataContainer().get(ffaKey, PersistentDataType.STRING);
            FfaArena arena = plugin.getFfaManager().getFfaArena(name);
            if (arena != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                new FfaEditorMenu(plugin, arena).open(player);
            }
        }
    }
}
