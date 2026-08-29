package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.kit.KitType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public class KitSettingsMenu extends Menu {

    private final CrovexPractice plugin;
    private final Kit kit;
    private boolean confirmDelete = false;

    public KitSettingsMenu(CrovexPractice plugin, Kit kit) {
        super(54, plugin.getMessageManager().getMessage("gui.admin.kit-editor.title", "%kit%", kit.getDisplayName()));
        this.plugin = plugin;
        this.kit = kit;
    }

    @Override
    public void setMenuItems() {
        // Fill background with black glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 10: Set Icon (Item in hand)
        ItemStack setIcon = new ItemStack(Material.GOLD_INGOT);
        ItemMeta iconMeta = setIcon.getItemMeta();
        if (iconMeta != null) {
            iconMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.set-icon"));
            iconMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.set-icon-lore"));
            setIcon.setItemMeta(iconMeta);
        }
        inventory.setItem(10, setIcon);

        // Slot 11: Set Inventory (Quick capture)
        ItemStack setInv = new ItemStack(Material.CHEST);
        ItemMeta invMeta = setInv.getItemMeta();
        if (invMeta != null) {
            invMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.set-inv"));
            invMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.set-inv-lore"));
            setInv.setItemMeta(invMeta);
        }
        inventory.setItem(11, setInv);

        // Slot 12: Edit Inventory Layout (Visual editor)
        ItemStack editInv = new ItemStack(Material.ENDER_CHEST);
        ItemMeta editInvMeta = editInv.getItemMeta();
        if (editInvMeta != null) {
            editInvMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.edit-inv"));
            editInvMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.edit-inv-lore"));
            editInv.setItemMeta(editInvMeta);
        }
        inventory.setItem(12, editInv);

        // Slot 14: Cycle Rules
        ItemStack cycleRules = new ItemStack(Material.COMPARATOR);
        ItemMeta rulesMeta = cycleRules.getItemMeta();
        if (rulesMeta != null) {
            rulesMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.cycle-rules"));
            rulesMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.cycle-rules-lore", "%type%", kit.getType().name()));
            cycleRules.setItemMeta(rulesMeta);
        }
        inventory.setItem(14, cycleRules);

        // Slot 16: Delete
        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = delete.getItemMeta();
        if (deleteMeta != null) {
            if (confirmDelete) {
                deleteMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.delete-confirm"));
            } else {
                deleteMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.delete"));
            }
            delete.setItemMeta(deleteMeta);
        }
        inventory.setItem(16, delete);

        // Slot 19: Block Place Toggle
        ItemStack blockPlace = new ItemStack(kit.isAllowBlockPlace() ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA);
        ItemMeta blockPlaceMeta = blockPlace.getItemMeta();
        if (blockPlaceMeta != null) {
            String status = kit.isAllowBlockPlace() ? 
                    plugin.getMessageManager().getRawMessage("gui.admin.kit-editor.status-on") : 
                    plugin.getMessageManager().getRawMessage("gui.admin.kit-editor.status-off");
            blockPlaceMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.block-place"));
            blockPlaceMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.block-place-lore", "%status%", status));
            blockPlace.setItemMeta(blockPlaceMeta);
        }
        inventory.setItem(19, blockPlace);

        // Slot 20: Block Break Toggle
        ItemStack blockBreak = new ItemStack(kit.isAllowBlockBreak() ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA);
        ItemMeta blockBreakMeta = blockBreak.getItemMeta();
        if (blockBreakMeta != null) {
            String status = kit.isAllowBlockBreak() ? 
                    plugin.getMessageManager().getRawMessage("gui.admin.kit-editor.status-on") : 
                    plugin.getMessageManager().getRawMessage("gui.admin.kit-editor.status-off");
            blockBreakMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.block-break"));
            blockBreakMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.block-break-lore", "%status%", status));
            blockBreak.setItemMeta(blockBreakMeta);
        }
        inventory.setItem(20, blockBreak);

        // Slot 21: Explosions Toggle
        ItemStack explosions = new ItemStack(kit.isAllowExplosions() ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA);
        ItemMeta explosionsMeta = explosions.getItemMeta();
        if (explosionsMeta != null) {
            String status = kit.isAllowExplosions() ? 
                    plugin.getMessageManager().getRawMessage("gui.admin.kit-editor.status-on") : 
                    plugin.getMessageManager().getRawMessage("gui.admin.kit-editor.status-off");
            explosionsMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.explosions"));
            explosionsMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kit-editor.explosions-lore", "%status%", status));
            explosions.setItemMeta(explosionsMeta);
        }
        inventory.setItem(21, explosions);

        // Slot 49: Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kit-editor.back"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(49, back);
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

        if (slot == 10) {
            // Set Icon
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType() == Material.AIR) {
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-hold-item"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            ItemStack newIcon = inHand.clone();
            newIcon.setAmount(1);
            kit.setIcon(newIcon);
            plugin.getKitManager().saveKits();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-icon-set", "%kit%", kit.getDisplayName()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            setMenuItems();
        } 
        else if (slot == 11) {
            // Set Inventory (Quick capture)
            ItemStack[] contents = player.getInventory().getContents();
            ItemStack[] invContents = new ItemStack[36];
            System.arraycopy(contents, 0, invContents, 0, 36);
            
            ItemStack[] armorContents = player.getInventory().getArmorContents();
            
            kit.setInventoryContents(invContents);
            kit.setArmorContents(armorContents);
            plugin.getKitManager().saveKits();
            
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-inv-set", "%kit%", kit.getDisplayName()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        } 
        else if (slot == 12) {
            // Edit Inventory Layout (Visual editor)
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new KitInventoryEditorMenu(plugin, kit).open(player);
        } 
        else if (slot == 14) {
            // Cycle Rules
            KitType[] types = KitType.values();
            int nextOrdinal = (kit.getType().ordinal() + 1) % types.length;
            kit.setType(types[nextOrdinal]);
            plugin.getKitManager().saveKits();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
            setMenuItems();
        } 
        else if (slot == 16) {
            // Delete
            if (!confirmDelete) {
                confirmDelete = true;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                setMenuItems();
            } else {
                plugin.getKitManager().deleteKit(kit.getName());
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-deleted", "%kit%", kit.getDisplayName()));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                player.closeInventory();
                new AdminKitMenu(plugin).open(player);
            }
        } 
        else if (slot == 19) {
            // Toggle Block Place
            kit.setAllowBlockPlace(!kit.isAllowBlockPlace());
            plugin.getKitManager().saveKits();
            plugin.getArenaManager().pruneIncompatibleKits();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, kit.isAllowBlockPlace() ? 1.4f : 0.8f);
            setMenuItems();
        }
        else if (slot == 20) {
            // Toggle Block Break
            kit.setAllowBlockBreak(!kit.isAllowBlockBreak());
            plugin.getKitManager().saveKits();
            plugin.getArenaManager().pruneIncompatibleKits();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, kit.isAllowBlockBreak() ? 1.4f : 0.8f);
            setMenuItems();
        }
        else if (slot == 21) {
            // Toggle Explosions
            kit.setAllowExplosions(!kit.isAllowExplosions());
            plugin.getKitManager().saveKits();
            plugin.getArenaManager().pruneIncompatibleKits();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, kit.isAllowExplosions() ? 1.4f : 0.8f);
            setMenuItems();
        }
        else if (slot == 49) {
            // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new AdminKitMenu(plugin).open(player);
        }
    }
}
