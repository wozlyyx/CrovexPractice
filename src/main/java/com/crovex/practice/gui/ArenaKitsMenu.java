package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.kit.Kit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ArenaKitsMenu extends Menu {

    private final CrovexPractice plugin;
    private final Arena arena;
    private final NamespacedKey kitKey;

    public ArenaKitsMenu(CrovexPractice plugin, Arena arena) {
        super(36, plugin.getMessageManager().getMessage("gui.admin.allowed-kits.title", "%arena%", arena.getName()));
        this.plugin = plugin;
        this.arena = arena;
        this.kitKey = new NamespacedKey(plugin, "kit_name");
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

        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getKits());
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int index = 0;

        List<String> allowed = arena.getAllowedKits();
        java.util.Iterator<String> iter = allowed.iterator();
        while (iter.hasNext()) {
            String kName = iter.next();
            if (kName.equals("__none__")) continue;
            Kit k = plugin.getKitManager().getKit(kName);
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

        for (Kit kit : kits) {
            // Check compatibility:
            if (kit.isAllowBlockPlace() && !arena.isSupportsBlockPlace()) {
                continue;
            }
            if (arena.isSupportsBlockPlace() && !kit.isAllowBlockPlace()) {
                continue;
            }
            if (kit.isAllowBlockBreak() && !arena.isSupportsBlockBreak()) {
                continue;
            }
            if (arena.isSupportsBlockBreak() && !kit.isAllowBlockBreak()) {
                continue;
            }
            if (kit.isAllowExplosions() && !arena.isSupportsExplosions()) {
                continue;
            }
            if (arena.isSupportsExplosions() && !kit.isAllowExplosions()) {
                continue;
            }

            if (index >= slots.length) break;

            ItemStack item = kit.getIcon().clone();
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.getName());
                meta.displayName(plugin.getMessageManager().getMessage("gui.admin.allowed-kits.kit-name", "%kit%", kit.getDisplayName()));

                boolean isAllowed = allowed.isEmpty() || allowed.contains(kit.getName().toLowerCase());

                List<Component> lore = new ArrayList<>();
                if (isAllowed) {
                    lore.add(plugin.getMessageManager().getMessage("gui.admin.allowed-kits.status-allowed"));
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(plugin.getMessageManager().getMessage("gui.admin.allowed-kits.status-blocked"));
                }
                lore.addAll(plugin.getMessageManager().getMessageList("gui.admin.allowed-kits.kit-lore"));

                meta.lore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slots[index], item);
            index++;
        }

        // Slot 31: Back to Arena Editor
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.allowed-kits.back"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(31, back);
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

        if (slot == 31) {
            // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new ArenaEditorMenu(plugin, arena).open(player);
            return;
        }

        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta != null && clickedMeta.getPersistentDataContainer().has(kitKey, PersistentDataType.STRING)) {
            String kitName = clickedMeta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
            String kitKeyName = kitName.toLowerCase();

            List<String> allowed = arena.getAllowedKits();
            boolean wasAllowed = allowed.isEmpty() || allowed.contains(kitKeyName);

            if (wasAllowed) {
                if (allowed.isEmpty()) {
                    for (Kit k : plugin.getKitManager().getKits()) {
                        // Check compatibility
                        if (k.isAllowBlockPlace() == arena.isSupportsBlockPlace()
                                && k.isAllowBlockBreak() == arena.isSupportsBlockBreak()
                                && k.isAllowExplosions() == arena.isSupportsExplosions()) {
                            if (!k.getName().equalsIgnoreCase(kitKeyName)) {
                                allowed.add(k.getName().toLowerCase());
                            }
                        }
                    }
                } else {
                    allowed.remove(kitKeyName);
                }

                if (allowed.isEmpty()) {
                    allowed.add("__none__");
                }
            } else {
                allowed.remove("__none__");
                if (!allowed.contains(kitKeyName)) {
                    allowed.add(kitKeyName);
                }
                
                // If all compatible kits are now allowed, clean up the list to default empty state
                long compatibleCount = plugin.getKitManager().getKits().stream()
                        .filter(k -> k.isAllowBlockPlace() == arena.isSupportsBlockPlace()
                                && k.isAllowBlockBreak() == arena.isSupportsBlockBreak()
                                && k.isAllowExplosions() == arena.isSupportsExplosions())
                        .count();
                if (allowed.size() == compatibleCount) {
                    allowed.clear();
                }
            }

            plugin.getArenaManager().saveArenas();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            setMenuItems();
        }
    }
}
