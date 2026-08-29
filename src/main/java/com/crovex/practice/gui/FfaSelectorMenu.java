package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.ffa.FfaArena;
import com.crovex.practice.kit.Kit;
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

public class FfaSelectorMenu extends Menu {

    private final CrovexPractice plugin;
    private final NamespacedKey arenaKey;

    public FfaSelectorMenu(CrovexPractice plugin) {
        super(calculateSize(plugin), plugin.getMessageManager().getMessage("gui.ffa-selector.title"));
        this.plugin = plugin;
        this.arenaKey = new NamespacedKey(plugin, "ffa_name");
    }

    private static int calculateSize(CrovexPractice plugin) {
        int total = 0;
        for (FfaArena arena : plugin.getFfaManager().getFfaArenas()) {
            if (arena.isEnabled() && arena.getSpawnLocation() != null && arena.getBounds() != null) {
                total++;
            }
        }
        if (total <= 7) return 27;
        if (total <= 14) return 36;
        if (total <= 21) return 45;
        return 54;
    }

    @Override
    public void setMenuItems() {
        int size = inventory.getSize();
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        List<FfaArena> arenas = new ArrayList<>();
        for (FfaArena arena : plugin.getFfaManager().getFfaArenas()) {
            if (arena.isEnabled() && arena.getSpawnLocation() != null && arena.getBounds() != null) {
                arenas.add(arena);
            }
        }

        List<Integer> slots = new ArrayList<>();
        if (size == 27) {
            for (int i : new int[]{10, 11, 12, 13, 14, 15, 16}) slots.add(i);
        } else if (size == 36) {
            for (int i : new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25}) slots.add(i);
        } else if (size == 45) {
            for (int i : new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34}) slots.add(i);
        } else {
            for (int r = 1; r <= 4; r++) {
                for (int c = 1; c <= 7; c++) {
                    slots.add(r * 9 + c);
                }
            }
        }

        int index = 0;
        for (FfaArena arena : arenas) {
            if (index >= slots.size()) break;

            Kit kit = plugin.getKitManager().getKit(arena.getKitName());
            ItemStack item = (kit != null && kit.getIcon() != null) ? kit.getIcon().clone() : new ItemStack(Material.IRON_SWORD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(arenaKey, PersistentDataType.STRING, arena.getName());
                meta.displayName(plugin.getMessageManager().getMessage("gui.ffa-selector.item-name", "%arena%", arena.getName()));
                
                String kitDisplayName = kit != null ? kit.getDisplayName() : arena.getKitName();
                String playersCount = String.valueOf(plugin.getFfaManager().getPlayerCount(arena.getName()));

                meta.lore(plugin.getMessageManager().getMessageList("gui.ffa-selector.item-lore",
                        "%kit%", kitDisplayName,
                        "%players%", playersCount
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(slots.get(index), item);
            index++;
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemMeta meta = clicked.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(arenaKey, PersistentDataType.STRING)) {
            String name = meta.getPersistentDataContainer().get(arenaKey, PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            plugin.getFfaManager().joinFfa(player, name);
        }
    }
}
