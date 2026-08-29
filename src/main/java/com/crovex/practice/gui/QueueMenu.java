package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.queue.QueueType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

/**
 * QueueMenu — 54 slot inventory that shows all kits for a queue type.
 * Only the outer border (row 0, row 5, col 0, col 8) is filled with glass.
 * Inner 4×7 area (slots 10-16, 19-25, 28-34, 37-43) is used for kits.
 */
public class QueueMenu extends Menu {

    // All 54 border slots (top row, bottom row, left col, right col)
    private static final java.util.Set<Integer> BORDER_SLOTS = buildBorderSlots();

    // Inner playable slots (rows 1-4, cols 1-7 of a 6-row chest)
    private static final int[] INNER_SLOTS = buildInnerSlots();

    private final CrovexPractice plugin;
    private final NamespacedKey kitKey;
    private final QueueType queueType;

    private static java.util.Set<Integer> buildBorderSlots() {
        java.util.Set<Integer> s = new java.util.LinkedHashSet<>();
        // Top row 0-8
        for (int i = 0; i <= 8; i++) s.add(i);
        // Bottom row 45-53
        for (int i = 45; i <= 53; i++) s.add(i);
        // Left column (every 9th, col 0)
        for (int i = 0; i < 54; i += 9) s.add(i);
        // Right column (every 9th, col 8)
        for (int i = 8; i < 54; i += 9) s.add(i);
        return java.util.Collections.unmodifiableSet(s);
    }

    private static int[] buildInnerSlots() {
        java.util.List<Integer> list = new java.util.ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                list.add(row * 9 + col);
            }
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    public QueueMenu(CrovexPractice plugin, QueueType queueType) {
        super(54,
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                plugin.getQueueGuiManager().getGuiTitle(queueType))
        );
        this.plugin = plugin;
        this.queueType = queueType;
        this.kitKey = new NamespacedKey(plugin, "kit_name");
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        Material fillerMat = plugin.getQueueGuiManager().getFillerMaterial(queueType);
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }

        // Fill only border slots with glass
        for (int slot : BORDER_SLOTS) {
            inventory.setItem(slot, filler);
        }

        // Read configured slot assignments
        Map<String, Integer> configSlots = plugin.getQueueGuiManager().getKitSlots(queueType);
        java.util.Set<Integer> usedInnerSlots = new java.util.HashSet<>();

        // 1. Place kits that have a configured inner slot
        for (Map.Entry<String, Integer> entry : configSlots.entrySet()) {
            String kitName = entry.getKey();
            int slot = entry.getValue();
            // Only use inner slots (not border)
            if (BORDER_SLOTS.contains(slot)) continue;
            if (slot < 0 || slot >= 54) continue;

            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit != null && plugin.getArenaManager().hasCompatibleArena(kit)) {
                inventory.setItem(slot, buildKitItem(kit));
                usedInnerSlots.add(slot);
            }
        }

        // 2. Place unmapped kits in the next free inner slot
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (configSlots.containsKey(kit.getName().toLowerCase())) continue;
            if (!plugin.getArenaManager().hasCompatibleArena(kit)) continue;
            // Find first free inner slot
            for (int innerSlot : INNER_SLOTS) {
                if (!usedInnerSlots.contains(innerSlot)) {
                    inventory.setItem(innerSlot, buildKitItem(kit));
                    usedInnerSlots.add(innerSlot);
                    break;
                }
            }
        }
    }

    private ItemStack buildKitItem(Kit kit) {
        ItemStack item = kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.getName());
            meta.displayName(plugin.getMessageManager().getMessage("gui.queue.kit-name", "%kit%", kit.getDisplayName()));

            int inQueue = plugin.getQueueManager().getQueuedCount(kit.getName(), queueType);
            int activeMatches = 0;
            for (var match : plugin.getMatchManager().getMatches()) {
                if (match.getKit().getName().equalsIgnoreCase(kit.getName())) activeMatches++;
            }
            meta.lore(plugin.getMessageManager().getMessageList("gui.queue.kit-lore",
                    "%queued%", String.valueOf(inQueue),
                    "%matches%", String.valueOf(activeMatches)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta != null && clickedMeta.getPersistentDataContainer().has(kitKey, PersistentDataType.STRING)) {
            String kitName = clickedMeta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
            Kit selectedKit = plugin.getKitManager().getKit(kitName);
            if (selectedKit != null) {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                plugin.getQueueManager().addToQueue(player, selectedKit, queueType);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
        }
    }
}
