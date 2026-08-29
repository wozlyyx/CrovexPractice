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
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class KitEditMenu extends Menu {

    private final CrovexPractice plugin;

    public KitEditMenu(CrovexPractice plugin) {
        super(27, plugin.getMessageManager().getMessage("gui.kitedit.title"));
        this.plugin = plugin;
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getKits());
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int index = 0;

        for (Kit kit : kits) {
            if (index >= slots.length) break;

            ItemStack item = kit.getIcon().clone();
            ItemMeta meta = item.getItemMeta();

            meta.displayName(plugin.getMessageManager().getMessage("gui.queue.kit-name", "%kit%", kit.getDisplayName()));

            List<Component> lore = plugin.getMessageManager().getMessageList("gui.kitedit.kit-lore");

            meta.lore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slots[index], item);
            index++;
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        
        // Find kit by item icon type
        Kit selectedKit = null;
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (clicked.getType() == kit.getIcon().getType()) {
                selectedKit = kit;
                break;
            }
        }

        if (selectedKit != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            
            // Open layout editor
            new KitLayoutEditor(plugin, selectedKit).open(player);
        }
    }
}
