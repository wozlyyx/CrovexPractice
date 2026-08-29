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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * QueueLayoutEditor — 54-slot inventory.
 * 
 * Layout:
 *   Row 0  (slots  0-8 ) : border glass (locked)
 *   Rows 1-3 (slots 9-35): inner kit placement area
 *   Row 4  (slots 36-44) : border glass (locked)
 *   Row 5  (slots 45-53) : control bar — Cancel | ... | Info | ... | Save
 *
 * Player's bottom inventory holds ALL kits as source items.
 */
public class QueueLayoutEditor extends Menu {

    // Slots that are permanently locked (border + control bar)
    private static final Set<Integer> LOCKED_SLOTS = buildLockedSlots();

    // Inner editable slots: rows 1-3 (inclusive), cols 0-8 = slots 9..35
    // (no border restriction in editor — all 27 inner slots are free)
    private static final int INNER_START = 9;
    private static final int INNER_END   = 35; // inclusive

    // Control bar slots (row 5)
    private static final int SLOT_CANCEL = 45;
    private static final int SLOT_INFO   = 49;
    private static final int SLOT_SAVE   = 53;

    private static Set<Integer> buildLockedSlots() {
        Set<Integer> s = new java.util.HashSet<>();
        // Top border row
        for (int i = 0; i <= 8; i++) s.add(i);
        // Second border row (row 4, slots 36-44)
        for (int i = 36; i <= 44; i++) s.add(i);
        // Bottom row (control bar, row 5)
        for (int i = 45; i <= 53; i++) s.add(i);
        return java.util.Collections.unmodifiableSet(s);
    }

    private final CrovexPractice plugin;
    private final QueueType queueType;
    private final NamespacedKey kitKey;

    public QueueLayoutEditor(CrovexPractice plugin, QueueType queueType) {
        super(54,
            plugin.getMessageManager().getMessage("gui.queue-editor.title",
                "%type%", queueType == QueueType.RANKED ? "Ranked" : "Unranked")
        );
        this.plugin = plugin;
        this.queueType = queueType;
        this.kitKey = new NamespacedKey(plugin, "kit_icon");
    }

    @Override
    public void setMenuItems() {
        // Handled in open()
    }

    @Override
    public void open(Player player) {
        inventory.clear();

        Material fillerMat = plugin.getQueueGuiManager().getFillerMaterial(queueType);
        ItemStack border = new ItemStack(fillerMat);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.empty());
            border.setItemMeta(borderMeta);
        }

        // Fill border rows and control separator row
        for (int slot : LOCKED_SLOTS) {
            inventory.setItem(slot, border);
        }

        // ─── Place currently saved kit layout in inner area ───
        Map<String, Integer> currentSlots = plugin.getQueueGuiManager().getKitSlots(queueType);
        for (Map.Entry<String, Integer> entry : currentSlots.entrySet()) {
            String kitName = entry.getKey();
            int savedSlot = entry.getValue();
            // Map saved slot → editor inner area if needed
            // Saved slots are relative to the inner area (0-based from INNER_START)
            int editorSlot = savedSlot;
            // If the saved slot is in the inner area range, place it directly
            if (editorSlot >= INNER_START && editorSlot <= INNER_END && !LOCKED_SLOTS.contains(editorSlot)) {
                Kit kit = plugin.getKitManager().getKit(kitName);
                if (kit != null) {
                    inventory.setItem(editorSlot, buildKitItem(kit));
                }
            }
        }

        // ─── Control buttons ───
        // Cancel
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(plugin.getMessageManager().getMessage("gui.queue-editor.cancel"));
            cancelMeta.lore(plugin.getMessageManager().getMessageList("gui.queue-editor.cancel-lore"));
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(SLOT_CANCEL, cancel);

        // Info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(plugin.getMessageManager().getMessage("gui.queue-editor.info"));
            infoMeta.lore(plugin.getMessageManager().getMessageList("gui.queue-editor.info-lore"));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(SLOT_INFO, info);

        // Save
        ItemStack save = new ItemStack(Material.LIME_WOOL);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(plugin.getMessageManager().getMessage("gui.queue-editor.save"));
            saveMeta.lore(plugin.getMessageManager().getMessageList("gui.queue-editor.save-lore"));
            save.setItemMeta(saveMeta);
        }
        inventory.setItem(SLOT_SAVE, save);

        // ─── Give player ALL kits in their survival inventory ───
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getKits());
        for (Kit kit : kits) {
            player.getInventory().addItem(buildKitItem(kit));
        }

        player.openInventory(inventory);
    }

    private ItemStack buildKitItem(Kit kit) {
        ItemStack item = kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getMessage("gui.queue.kit-name", "%kit%", kit.getDisplayName()));
            meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.getName());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (event.getClickedInventory() == inventory) {
            // Always cancel clicks on locked slots
            if (LOCKED_SLOTS.contains(slot)) {
                event.setCancelled(true);

                if (slot == SLOT_CANCEL) {
                    player.closeInventory();
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.queue-editor.messages.cancel"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                    plugin.getPlayerManager().giveLobbyItems(player);
                } else if (slot == SLOT_SAVE) {
                    saveLayout(player);
                }
                return;
            }

            // Inner slots — only allow valid kit icons via cursor placement
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                ItemMeta meta = cursor.getItemMeta();
                if (meta == null || !meta.getPersistentDataContainer().has(kitKey, PersistentDataType.STRING)) {
                    event.setCancelled(true);
                }
            }
        }

        // Prevent non-kit items being shift-clicked from player inventory into chest
        if (event.getClickedInventory() != inventory && event.isShiftClick()) {
            ItemStack shiftItem = event.getCurrentItem();
            if (shiftItem != null && shiftItem.getType() != Material.AIR) {
                ItemMeta meta = shiftItem.getItemMeta();
                if (meta == null || !meta.getPersistentDataContainer().has(kitKey, PersistentDataType.STRING)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void saveLayout(Player player) {
        Map<String, Integer> newSlots = new HashMap<>();
        for (int i = INNER_START; i <= INNER_END; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(kitKey, PersistentDataType.STRING)) {
                String kitName = meta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
                if (kitName != null) {
                    newSlots.put(kitName.toLowerCase(), i);
                }
            }
        }

        plugin.getQueueGuiManager().setKitSlots(queueType, newSlots);
        player.closeInventory();
        String typeStr = queueType == QueueType.RANKED ? "Ranked" : "Unranked";
        player.sendMessage(plugin.getMessageManager().getMessage("gui.queue-editor.messages.save-success", "%type%", typeStr));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        plugin.getPlayerManager().giveLobbyItems(player);
    }
}
