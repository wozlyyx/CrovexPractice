package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.arena.Cuboid;
import org.bukkit.Location;
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

public class ArenaEditorMenu extends Menu {

    private final CrovexPractice plugin;
    private final Arena arena;
    private boolean confirmDelete = false;

    public ArenaEditorMenu(CrovexPractice plugin, Arena arena) {
        super(45, plugin.getMessageManager().getMessage("gui.admin.editor-title", "%arena%", arena.getName()));
        this.plugin = plugin;
        this.arena = arena;
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

        for (int i = 0; i < 45; i++) {
            // Borders: top row (0-8), bottom row (36-44), sides (9, 17, 18, 26, 27, 35)
            if (i < 9 || i >= 36 || i == 9 || i == 17 || i == 18 || i == 26 || i == 27 || i == 35) {
                inventory.setItem(i, blackFiller);
            } else {
                inventory.setItem(i, grayFiller);
            }
        }

        // Slot 20: Teleport
        ItemStack teleport = new ItemStack(Material.COMPASS);
        ItemMeta teleportMeta = teleport.getItemMeta();
        teleportMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.teleport"));
        teleportMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.teleport-lore"));
        teleport.setItemMeta(teleportMeta);
        inventory.setItem(20, teleport);

        // Slot 21: Toggle Status
        ItemStack toggle = new ItemStack(arena.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta toggleMeta = toggle.getItemMeta();
        toggleMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.toggle-status"));
        String statusStr = arena.isEnabled() ? plugin.getMessageManager().getRawMessage("gui.admin.editor.set") : plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");
        toggleMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.status-lore", "%status%", statusStr));
        toggle.setItemMeta(toggleMeta);
        inventory.setItem(21, toggle);

        // Slot 11: Spawn 1
        ItemStack spawn1 = new ItemStack(Material.RED_BED);
        ItemMeta spawn1Meta = spawn1.getItemMeta();
        spawn1Meta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.set-spawn1"));
        String spawn1Str = formatCoords(arena.getSpawn1());
        spawn1Meta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.spawn-lore", "%coords%", spawn1Str));
        spawn1.setItemMeta(spawn1Meta);
        inventory.setItem(11, spawn1);

        // Slot 15: Spawn 2
        ItemStack spawn2 = new ItemStack(Material.BLUE_BED);
        ItemMeta spawn2Meta = spawn2.getItemMeta();
        spawn2Meta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.set-spawn2"));
        String spawn2Str = formatCoords(arena.getSpawn2());
        spawn2Meta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.spawn-lore", "%coords%", spawn2Str));
        spawn2.setItemMeta(spawn2Meta);
        inventory.setItem(15, spawn2);

        // Slot 13: Spec Spawn
        ItemStack spec = new ItemStack(Material.ENDER_EYE);
        ItemMeta specMeta = spec.getItemMeta();
        specMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.set-spec"));
        String specStr = formatCoords(arena.getSpectatorSpawn());
        specMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.spawn-lore", "%coords%", specStr));
        spec.setItemMeta(specMeta);
        inventory.setItem(13, spec);

        // Slot 22: Boundaries (Golden Axe)
        ItemStack bounds = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta boundsMeta = bounds.getItemMeta();
        boundsMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.set-bounds"));
        
        List<Component> boundsLore = new ArrayList<>();
        if (arena.getBounds() != null) {
            Cuboid c = arena.getBounds();
            boundsLore.addAll(plugin.getMessageManager().getMessageList("gui.admin.editor.bounds-lore",
                    "%xmin%", String.valueOf(c.getXMin()),
                    "%xmax%", String.valueOf(c.getXMax()),
                    "%ymin%", String.valueOf(c.getYMin()),
                    "%ymax%", String.valueOf(c.getYMax()),
                    "%zmin%", String.valueOf(c.getZMin()),
                    "%zmax%", String.valueOf(c.getZMax())
            ));
        } else {
            boundsLore.add(MiniMessage.miniMessage().deserialize(plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set")));
        }
        boundsLore.addAll(plugin.getMessageManager().getMessageList("gui.admin.editor.set-bounds-lore"));
        boundsMeta.lore(boundsLore);
        bounds.setItemMeta(boundsMeta);
        inventory.setItem(22, bounds);

        // Slot 25: Delete (TNT)
        ItemStack delete = new ItemStack(Material.TNT);
        ItemMeta deleteMeta = delete.getItemMeta();
        if (deleteMeta != null) {
            if (confirmDelete) {
                deleteMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.delete-confirm"));
            } else {
                deleteMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.delete"));
            }
            delete.setItemMeta(deleteMeta);
        }
        inventory.setItem(25, delete);

        // Slot 23: Physics / Arena Settings (Repeater)
        ItemStack settingsItem = new ItemStack(Material.REPEATER);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        if (settingsMeta != null) {
            settingsMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.settings"));
            settingsMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.editor.settings-lore"));
            settingsItem.setItemMeta(settingsMeta);
        }
        inventory.setItem(23, settingsItem);

        // Slot 24: Allowed Kits (Book)
        ItemStack allowedKits = new ItemStack(Material.BOOK);
        ItemMeta allowedKitsMeta = allowedKits.getItemMeta();
        if (allowedKitsMeta != null) {
            allowedKitsMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.allowed-kits"));
            List<Component> allowedLore = new ArrayList<>();
            allowedLore.add(MiniMessage.miniMessage().deserialize("<gray>Currently Allowed Kits:"));
            
            List<String> allowed = arena.getAllowedKits();
            java.util.Iterator<String> iter = allowed.iterator();
            while (iter.hasNext()) {
                String kName = iter.next();
                if (kName.equals("__none__")) continue;
                com.crovex.practice.kit.Kit k = plugin.getKitManager().getKit(kName);
                if (k == null) {
                    iter.remove();
                    continue;
                }
                boolean compatible = true;
                if (k.isAllowBlockPlace() && !arena.isSupportsBlockPlace()) {
                    compatible = false;
                } else if (arena.isSupportsBlockPlace() && !k.isAllowBlockPlace()) {
                    compatible = false;
                } else if (k.isAllowBlockBreak() && !arena.isSupportsBlockBreak()) {
                    compatible = false;
                } else if (arena.isSupportsBlockBreak() && !k.isAllowBlockBreak()) {
                    compatible = false;
                } else if (k.isAllowExplosions() && !arena.isSupportsExplosions()) {
                    compatible = false;
                } else if (arena.isSupportsExplosions() && !k.isAllowExplosions()) {
                    compatible = false;
                }
                
                if (!compatible) {
                    iter.remove();
                }
            }
            plugin.getArenaManager().saveArenas();

            if (allowed.isEmpty()) {
                allowedLore.add(MiniMessage.miniMessage().deserialize(" <green>ALL KITS ALLOWED"));
            } else if (allowed.contains("__none__")) {
                allowedLore.add(MiniMessage.miniMessage().deserialize(" <red>NO KITS ALLOWED"));
            } else {
                for (String k : allowed) {
                    com.crovex.practice.kit.Kit kitObj = plugin.getKitManager().getKit(k);
                    String dispName = kitObj != null ? kitObj.getDisplayName() : k;
                    allowedLore.add(MiniMessage.miniMessage().deserialize(" <yellow>• " + dispName));
                }
            }
            allowedLore.addAll(plugin.getMessageManager().getMessageList("gui.admin.editor.allowed-kits-lore"));
            allowedKitsMeta.lore(allowedLore);
            allowedKits.setItemMeta(allowedKitsMeta);
        }
        inventory.setItem(24, allowedKits);

        // Slot 31: Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.back"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(31, back);
    }

    private String formatCoords(Location loc) {
        if (loc == null) return plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");
        return "X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ();
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR 
                || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE 
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 20) {
            // Teleport
            if (arena.getSpawn1() != null) {
                player.teleport(arena.getSpawn1());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.closeInventory();
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.editor.msg-spawn1-not-set"));
            }
        } 
        else if (slot == 21) {
            // Toggle
            if (arena.getSpawn1() == null || arena.getSpawn2() == null || arena.getBounds() == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.editor.msg-setup-required"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            arena.setEnabled(!arena.isEnabled());
            plugin.getArenaManager().saveArenas();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, arena.isEnabled() ? 1.5f : 0.5f);
            setMenuItems();
        } 
        else if (slot == 11) {
            // Set Spawn 1
            arena.setSpawn1(player.getLocation());
            plugin.getArenaManager().saveArenas();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.editor.msg-spawn1-set", "%arena%", arena.getName()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            setMenuItems();
        } 
        else if (slot == 15) {
            // Set Spawn 2
            arena.setSpawn2(player.getLocation());
            plugin.getArenaManager().saveArenas();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.editor.msg-spawn2-set", "%arena%", arena.getName()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            setMenuItems();
        } 
        else if (slot == 13) {
            // Set Spec Spawn
            arena.setSpectatorSpawn(player.getLocation());
            plugin.getArenaManager().saveArenas();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.editor.msg-spec-set", "%arena%", arena.getName()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            setMenuItems();
        } 
        else if (slot == 22) {
            player.closeInventory();
            plugin.getArenaManager().startSetupSession(player.getUniqueId(), arena.getName());
            ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.getMessageManager().getMessage("setup.wand-name"));
                meta.lore(plugin.getMessageManager().getMessageList("setup.wand-lore"));
                wand.setItemMeta(meta);
            }
            player.getInventory().addItem(wand);
            player.sendMessage(plugin.getMessageManager().getMessage("setup.wand-give", "%arena%", arena.getName()));
        } 
        else if (slot == 23) {
            // Open Arena Settings Menu
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new ArenaSettingsMenu(plugin, arena).open(player);
        }
        else if (slot == 24) {
            // Allowed Kits
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new ArenaKitsMenu(plugin, arena).open(player);
        }
        else if (slot == 25) {
            // Delete (TNT)
            if (!confirmDelete) {
                confirmDelete = true;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                setMenuItems();
            } else {
                plugin.getArenaManager().deleteArena(arena.getName());
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.editor.msg-deleted", "%arena%", arena.getName()));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                player.closeInventory();
                new AdminArenaMenu(plugin).open(player);
            }
        } 
        else if (slot == 31) {
            // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new AdminArenaMenu(plugin).open(player);
        }
    }
}
