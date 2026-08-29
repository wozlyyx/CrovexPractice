package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public class ArenaSettingsMenu extends Menu {
    private final CrovexPractice plugin;
    private final Arena arena;

    public ArenaSettingsMenu(CrovexPractice plugin, Arena arena) {
        super(27, plugin.getMessageManager().getMessage("gui.admin.settings-title", "%arena%", arena.getName()));
        this.plugin = plugin;
        this.arena = arena;
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 11: Supports Block Place Toggle
        ItemStack placeSupport = new ItemStack(arena.isSupportsBlockPlace() ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA);
        ItemMeta placeSupportMeta = placeSupport.getItemMeta();
        if (placeSupportMeta != null) {
            String placeStatus = arena.isSupportsBlockPlace() ? 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.set") : 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");
            placeSupportMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.supports-block-place"));
            placeSupportMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.supports-block-place-lore", "%status%", placeStatus));
            placeSupport.setItemMeta(placeSupportMeta);
        }
        inventory.setItem(11, placeSupport);

        // Slot 13: Supports Block Break Toggle
        ItemStack breakSupport = new ItemStack(arena.isSupportsBlockBreak() ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA);
        ItemMeta breakSupportMeta = breakSupport.getItemMeta();
        if (breakSupportMeta != null) {
            String breakStatus = arena.isSupportsBlockBreak() ? 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.set") : 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");
            breakSupportMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.supports-block-break"));
            breakSupportMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.supports-block-break-lore", "%status%", breakStatus));
            breakSupport.setItemMeta(breakSupportMeta);
        }
        inventory.setItem(13, breakSupport);

        // Slot 15: Supports Explosions Toggle
        ItemStack explodeSupport = new ItemStack(arena.isSupportsExplosions() ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA);
        ItemMeta explodeSupportMeta = explodeSupport.getItemMeta();
        if (explodeSupportMeta != null) {
            String explodeStatus = arena.isSupportsExplosions() ? 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.set") : 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");
            explodeSupportMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.supports-explosions"));
            explodeSupportMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.supports-explosions-lore", "%status%", explodeStatus));
            explodeSupport.setItemMeta(explodeSupportMeta);
        }
        inventory.setItem(15, explodeSupport);

        // Slot 22: Back (Arrow)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.back"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(22, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 11) {
            // Toggle Supports Block Place
            arena.setSupportsBlockPlace(!arena.isSupportsBlockPlace());
            plugin.getArenaManager().saveArenas();
            plugin.getArenaManager().pruneIncompatibleKits();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, arena.isSupportsBlockPlace() ? 1.5f : 0.5f);
            setMenuItems();
        }
        else if (slot == 13) {
            // Toggle Supports Block Break
            arena.setSupportsBlockBreak(!arena.isSupportsBlockBreak());
            plugin.getArenaManager().saveArenas();
            plugin.getArenaManager().pruneIncompatibleKits();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, arena.isSupportsBlockBreak() ? 1.5f : 0.5f);
            setMenuItems();
        }
        else if (slot == 15) {
            // Toggle Supports Explosions
            arena.setSupportsExplosions(!arena.isSupportsExplosions());
            plugin.getArenaManager().saveArenas();
            plugin.getArenaManager().pruneIncompatibleKits();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, arena.isSupportsExplosions() ? 1.5f : 0.5f);
            setMenuItems();
        }
        else if (slot == 22) {
            // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new ArenaEditorMenu(plugin, arena).open(player);
        }
    }
}
