package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
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

public class AdminArenaMenu extends Menu {

    private final CrovexPractice plugin;
    private final int page;
    private final NamespacedKey arenaKey;

    public AdminArenaMenu(CrovexPractice plugin) {
        this(plugin, 0);
    }

    public AdminArenaMenu(CrovexPractice plugin, int page) {
        super(36, plugin.getMessageManager().getMessage("gui.admin.list-title", "%page%", String.valueOf(page + 1)));
        this.plugin = plugin;
        this.page = page;
        this.arenaKey = new NamespacedKey(plugin, "arena_name");
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

        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
        int totalArenas = arenas.size();
        int pageSize = 14;
        int startIndex = page * pageSize;
        int endIndex = Math.min(totalArenas, (page + 1) * pageSize);

        String setVal = plugin.getMessageManager().getRawMessage("gui.admin.editor.set");
        String notSetVal = plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");

        // Put arenas in slots 10 to 16, and 19 to 25
        int[] arenaSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int slotIndex = 0;

        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= arenaSlots.length) break;

            Arena arena = arenas.get(i);
            int slot = arenaSlots[slotIndex];

            ItemStack item = new ItemStack(arena.isEnabled() ? Material.MAP : Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            // Set arena name persistent data tag
            meta.getPersistentDataContainer().set(arenaKey, PersistentDataType.STRING, arena.getName());

            meta.displayName(plugin.getMessageManager().getMessage("gui.admin.arena-icon-name", "%arena%", arena.getName()));

            String statusStr = arena.isEnabled() ? setVal : notSetVal;
            String boundsStr = arena.getBounds() != null ? setVal : notSetVal;
            String spawn1Str = arena.getSpawn1() != null ? setVal : notSetVal;
            String spawn2Str = arena.getSpawn2() != null ? setVal : notSetVal;
            String specStr = arena.getSpectatorSpawn() != null ? setVal : notSetVal;

            List<Component> lore = plugin.getMessageManager().getMessageList("gui.admin.arena-icon-lore",
                    "%status%", statusStr,
                    "%bounds%", boundsStr,
                    "%spawn1%", spawn1Str,
                    "%spawn2%", spawn2Str,
                    "%specspawn%", specStr
            );

            meta.lore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot, item);
            slotIndex++;
        }

        // Paging Navigation: Slot 9 (Previous Page)
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.page-prev", "%page%", String.valueOf(page)));
            prev.setItemMeta(prevMeta);
            inventory.setItem(9, prev);
        }

        // Paging Navigation: Slot 26 (Next Page)
        if (endIndex < totalArenas) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.page-next", "%page%", String.valueOf(page + 2)));
            next.setItemMeta(nextMeta);
            inventory.setItem(26, next);
        }

        // Slot 31: Manage Kits
        ItemStack manageKits = new ItemStack(Material.CHEST);
        ItemMeta manageKitsMeta = manageKits.getItemMeta();
        if (manageKitsMeta != null) {
            manageKitsMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.manage-kits"));
            manageKitsMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.manage-kits-lore"));
            manageKits.setItemMeta(manageKitsMeta);
        }
        inventory.setItem(31, manageKits);

        // Slot 32: FFA Editor
        ItemStack ffaEditor = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta ffaMeta = ffaEditor.getItemMeta();
        if (ffaMeta != null) {
            ffaMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-editor-item-name"));
            ffaMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.ffa-editor-item-lore"));
            ffaEditor.setItemMeta(ffaMeta);
        }
        inventory.setItem(32, ffaEditor);
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
        int clickedSlot = event.getSlot();

        // Handle Manage Kits click
        if (clickedSlot == 31) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AdminKitMenu(plugin).open(player);
            return;
        }

        // Handle FFA Editor click
        if (clickedSlot == 32) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AdminFfaMenu(plugin).open(player);
            return;
        }

        // Handle navigation clicks
        if (clickedSlot == 9 && page > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AdminArenaMenu(plugin, page - 1).open(player);
            return;
        }

        if (clickedSlot == 26) {
            List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
            if ((page + 1) * 14 < arenas.size()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new AdminArenaMenu(plugin, page + 1).open(player);
                return;
            }
        }

        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta == null) return;

        // Retrieve arena name from PersistentDataContainer
        if (clickedMeta.getPersistentDataContainer().has(arenaKey, PersistentDataType.STRING)) {
            String arenaName = clickedMeta.getPersistentDataContainer().get(arenaKey, PersistentDataType.STRING);
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                new ArenaEditorMenu(plugin, arena).open(player);
            }
        }
    }
}
