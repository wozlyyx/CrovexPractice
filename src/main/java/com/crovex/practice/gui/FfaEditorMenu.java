package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Cuboid;
import com.crovex.practice.ffa.FfaArena;
import com.crovex.practice.ffa.FfaManager;
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

public class FfaEditorMenu extends Menu {

    private final CrovexPractice plugin;
    private final FfaArena ffaArena;

    public FfaEditorMenu(CrovexPractice plugin, FfaArena ffaArena) {
        super(45, plugin.getMessageManager().getMessage("gui.admin.editor-title", "%arena%", ffaArena.getName()));
        this.plugin = plugin;
        this.ffaArena = ffaArena;
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
        if (teleportMeta != null) {
            teleportMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.teleport"));
            teleportMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.ffa-editor.teleport-lore"));
            teleport.setItemMeta(teleportMeta);
        }
        inventory.setItem(20, teleport);

        // Slot 21: Toggle status
        ItemStack toggle = new ItemStack(ffaArena.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta toggleMeta = toggle.getItemMeta();
        if (toggleMeta != null) {
            toggleMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.toggle-status"));
            String statusStr = ffaArena.isEnabled() ? 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.set") : 
                    plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");
            toggleMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.ffa-editor.status-lore", "%status%", statusStr));
            toggle.setItemMeta(toggleMeta);
        }
        inventory.setItem(21, toggle);

        // Slot 13: Set Spawn
        ItemStack spawn = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta spawnMeta = spawn.getItemMeta();
        if (spawnMeta != null) {
            spawnMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.set-spawn"));
            String coordsStr = formatCoords(ffaArena.getSpawnLocation());
            spawnMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.ffa-editor.spawn-lore", "%coords%", coordsStr));
            spawn.setItemMeta(spawnMeta);
        }
        inventory.setItem(13, spawn);

        // Slot 22: Set Bounds
        ItemStack boundsItem = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta boundsMeta = boundsItem.getItemMeta();
        if (boundsMeta != null) {
            boundsMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.set-bounds"));
            List<Component> boundsLore = new ArrayList<>();
            Cuboid c = ffaArena.getBounds();
            if (c != null) {
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
            boundsLore.addAll(plugin.getMessageManager().getMessageList("gui.admin.ffa-editor.bounds-lore-help"));
            boundsMeta.lore(boundsLore);
            boundsItem.setItemMeta(boundsMeta);
        }
        inventory.setItem(22, boundsItem);

        // Slot 23: Select Kit
        ItemStack selectKit = new ItemStack(Material.CHEST);
        ItemMeta kitMeta = selectKit.getItemMeta();
        if (kitMeta != null) {
            kitMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.select-kit"));
            kitMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.ffa-editor.select-kit-lore", "%kit%", ffaArena.getKitName()));
            selectKit.setItemMeta(kitMeta);
        }
        inventory.setItem(23, selectKit);

        // Slot 24: Command Alias
        ItemStack aliasItem = new ItemStack(Material.PAPER);
        ItemMeta aliasMeta = aliasItem.getItemMeta();
        if (aliasMeta != null) {
            aliasMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.command-alias"));
            String activeAlias = ffaArena.getCommandAlias();
            if (activeAlias == null || activeAlias.isEmpty()) {
                activeAlias = plugin.getMessageManager().getRawMessage("gui.admin.editor.not-set");
            } else {
                activeAlias = "/" + activeAlias;
            }
            aliasMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.ffa-editor.command-alias-lore", "%alias%", activeAlias));
            aliasItem.setItemMeta(aliasMeta);
        }
        inventory.setItem(24, aliasItem);

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
        FfaManager ffaManager = plugin.getFfaManager();

        if (slot == 20) {
            // Teleport
            if (ffaArena.getSpawnLocation() != null) {
                player.teleport(ffaArena.getSpawnLocation());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.closeInventory();
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.msg-spawn-not-set"));
            }
        } 
        else if (slot == 21) {
            // Toggle
            if (ffaArena.getSpawnLocation() == null || ffaArena.getBounds() == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.msg-setup-required"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            ffaArena.setEnabled(!ffaArena.isEnabled());
            if (ffaArena.isEnabled()) {
                if (ffaArena.getCommandAlias() != null && !ffaArena.getCommandAlias().isEmpty()) {
                    ffaManager.registerFfaCommand(ffaArena.getCommandAlias(), ffaArena.getName());
                }
            } else {
                if (ffaArena.getCommandAlias() != null && !ffaArena.getCommandAlias().isEmpty()) {
                    ffaManager.unregisterFfaCommand(ffaArena.getCommandAlias());
                }
            }
            ffaManager.saveConfig();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, ffaArena.isEnabled() ? 1.5f : 0.5f);
            setMenuItems();
        } 
        else if (slot == 13) {
            // Set Spawn
            ffaArena.setSpawnLocation(player.getLocation());
            ffaManager.saveConfig();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.msg-spawn-set"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            setMenuItems();
        } 
        else if (slot == 22) {
            // Setup Bounds
            player.closeInventory();
            plugin.getFfaManager().startSetupSession(player.getUniqueId(), ffaArena.getName());
            ItemStack wand = new ItemStack(Material.IRON_AXE);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.getMessageManager().getMessage("ffa.setup.wand-name"));
                meta.lore(plugin.getMessageManager().getMessageList("ffa.setup.wand-lore"));
                wand.setItemMeta(meta);
            }
            player.getInventory().addItem(wand);
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.setup.wand-give"));
        }
        else if (slot == 23) {
            // Select Kit
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new FfaKitSelectMenu(plugin, ffaArena).open(player);
        } 
        else if (slot == 24) {
            // Command Alias
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.msg-enter-alias"));
            plugin.getGeneralListener().registerPendingFfaAlias(player, ffaArena);
        } 
        else if (slot == 31) {
            // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new AdminFfaMenu(plugin).open(player);
        }
    }
}
