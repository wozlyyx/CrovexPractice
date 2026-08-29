package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class KitLayoutEditor extends Menu {

    private final CrovexPractice plugin;
    private final Kit kit;
    private final List<Integer> controlSlots = new ArrayList<>();

    public KitLayoutEditor(CrovexPractice plugin, Kit kit) {
        super(54, plugin.getMessageManager().getMessage("gui.kitedit.editor-title", "%kit%", kit.getDisplayName()));
        this.plugin = plugin;
        this.kit = kit;
        
        for (int i = 36; i < 54; i++) {
            controlSlots.add(i);
        }
    }

    @Override
    public void setMenuItems() {
        // Handled in open() method below
    }

    @Override
    public void open(Player player) {
        // Clear inventory first
        inventory.clear();

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        ItemStack[] layout = pp != null ? pp.getKitLayout(kit.getName()) : null;
        if (layout == null) {
            layout = kit.getInventoryContents();
        }

        // Place layout in slots 0 to 35
        for (int i = 0; i < 36; i++) {
            if (layout != null && i < layout.length && layout[i] != null) {
                inventory.setItem(i, layout[i].clone());
            }
        }

        // Create divider row and buttons in 36 to 53
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        for (int i = 36; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 47: Reset
        ItemStack reset = new ItemStack(Material.REDSTONE);
        ItemMeta resetMeta = reset.getItemMeta();
        resetMeta.displayName(plugin.getMessageManager().getMessage("gui.kitedit.editor.reset"));
        reset.setItemMeta(resetMeta);
        inventory.setItem(47, reset);

        // Slot 49: Exit without saving
        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.displayName(plugin.getMessageManager().getMessage("gui.kitedit.editor.exit"));
        exit.setItemMeta(exitMeta);
        inventory.setItem(49, exit);

        // Slot 51: Save
        ItemStack save = new ItemStack(Material.LIME_DYE);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.displayName(plugin.getMessageManager().getMessage("gui.kitedit.editor.save"));
        save.setItemMeta(saveMeta);
        inventory.setItem(51, save);

        player.openInventory(inventory);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        
        // Prevent shift click from player inventory into the top chest control panel
        if (event.getClickedInventory() != inventory) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();

        // Control Panel Clicks
        if (slot >= 36) {
            event.setCancelled(true);
            
            if (slot == 49) {
                // Exit without saving
                player.closeInventory();
                player.sendMessage(plugin.getMessageManager().getMessage("gui.kitedit.messages.exit-no-save"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            } 
            else if (slot == 47) {
                // Reset to default
                PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
                if (pp != null) {
                    pp.setKitLayout(kit.getName(), null);
                    plugin.getDatabaseManager().savePlayer(pp);
                }
                player.closeInventory();
                player.sendMessage(plugin.getMessageManager().getMessage("gui.kitedit.messages.reset-success", "%kit%", kit.getDisplayName()));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.2f);
            } 
            else if (slot == 51) {
                // Save Layout
                ItemStack[] newLayout = new ItemStack[36];
                for (int i = 0; i < 36; i++) {
                    newLayout[i] = inventory.getItem(i);
                }

                PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
                if (pp != null) {
                    pp.setKitLayout(kit.getName(), newLayout);
                    plugin.getDatabaseManager().savePlayer(pp);
                }

                player.closeInventory();
                player.sendMessage(plugin.getMessageManager().getMessage("gui.kitedit.messages.save-success", "%kit%", kit.getDisplayName()));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }
}
