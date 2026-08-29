package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.kit.Kit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class KitInventoryEditorMenu extends Menu {

    private final CrovexPractice plugin;
    private final Kit kit;

    public KitInventoryEditorMenu(CrovexPractice plugin, Kit kit) {
        super(54, plugin.getMessageManager().getMessage("gui.admin.kit-editor.inv-title", "%kit%", kit.getDisplayName()));
        this.plugin = plugin;
        this.kit = kit;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        // 1. Populate top 36 inventory contents of the kit
        ItemStack[] invContents = kit.getInventoryContents();
        for (int i = 0; i < 36; i++) {
            if (i < invContents.length && invContents[i] != null) {
                inventory.setItem(i, invContents[i].clone());
            }
        }

        // 2. Populate armor slots (36-39) -> Helmet, Chestplate, Leggings, Boots
        ItemStack[] armorContents = kit.getArmorContents();
        // Helmet is armorContents[3]
        if (armorContents.length > 3 && armorContents[3] != null) {
            inventory.setItem(36, armorContents[3].clone());
        }
        // Chestplate is armorContents[2]
        if (armorContents.length > 2 && armorContents[2] != null) {
            inventory.setItem(37, armorContents[2].clone());
        }
        // Leggings is armorContents[1]
        if (armorContents.length > 1 && armorContents[1] != null) {
            inventory.setItem(38, armorContents[1].clone());
        }
        // Boots is armorContents[0]
        if (armorContents.length > 0 && armorContents[0] != null) {
            inventory.setItem(39, armorContents[0].clone());
        }

        // 3. Fill slots 40-44 with grey stained glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 40; i < 45; i++) {
            inventory.setItem(i, filler);
        }

        // 4. Populate controls: Slots 45-53
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.inv.cancel"));
            cancelMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.inv.cancel-lore"));
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(45, cancel);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.inv.info"));
            infoMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.inv.info-lore"));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(49, info);

        ItemStack save = new ItemStack(Material.LIME_WOOL);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.inv.save"));
            saveMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.inv.save-lore"));
            save.setItemMeta(saveMeta);
        }
        inventory.setItem(53, save);

        // Fill remaining spaces in bottom row with filler glass
        for (int i = 46; i < 53; i++) {
            if (i != 49) {
                inventory.setItem(i, filler);
            }
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // If clicking in player inventory, do not cancel (allow drag and drop)
        if (event.getClickedInventory() != inventory) {
            return;
        }

        // If clicking in editor controls (>= 40), cancel and process buttons
        if (slot >= 40) {
            event.setCancelled(true);

            if (slot == 45) {
                // Cancel
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                new KitSettingsMenu(plugin, kit).open(player);
            } 
            else if (slot == 53) {
                // Save
                ItemStack[] newInv = new ItemStack[36];
                for (int i = 0; i < 36; i++) {
                    newInv[i] = inventory.getItem(i);
                }

                ItemStack[] newArmor = new ItemStack[4];
                // Helmet is slot 36 -> armorContents[3]
                newArmor[3] = inventory.getItem(36);
                // Chestplate is slot 37 -> armorContents[2]
                newArmor[2] = inventory.getItem(37);
                // Leggings is slot 38 -> armorContents[1]
                newArmor[1] = inventory.getItem(38);
                // Boots is slot 39 -> armorContents[0]
                newArmor[0] = inventory.getItem(39);

                kit.setInventoryContents(newInv);
                kit.setArmorContents(newArmor);
                plugin.getKitManager().saveKits();

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-inv-set", "%kit%", kit.getDisplayName()));
                new KitSettingsMenu(plugin, kit).open(player);
            }
        }
    }
}
