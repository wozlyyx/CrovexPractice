package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
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

import java.util.ArrayList;
import java.util.List;

public class AdminKitMenu extends Menu {

    private final CrovexPractice plugin;
    private final NamespacedKey kitKey;

    public AdminKitMenu(CrovexPractice plugin) {
        super(36, plugin.getMessageManager().getMessage("gui.admin.kits.title"));
        this.plugin = plugin;
        this.kitKey = new NamespacedKey(plugin, "kit_name");
    }

    @Override
    public void setMenuItems() {
        // Fill background with grey glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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

        for (Kit kit : kits) {
            if (index >= slots.length) break;

            ItemStack item = kit.getIcon().clone();
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.getName());
                meta.displayName(plugin.getMessageManager().getMessage("gui.admin.kits.kit-icon-name", "%kit%", kit.getDisplayName()));

                List<Component> lore = plugin.getMessageManager().getMessageList("gui.admin.kits.kit-icon-lore",
                        "%type%", kit.getType().name()
                );
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slots[index], item);
            index++;
        }

        // Slot 31: Create Kit (Anvil)
        ItemStack createKit = new ItemStack(Material.ANVIL);
        ItemMeta createMeta = createKit.getItemMeta();
        if (createMeta != null) {
            createMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kits.create-name"));
            createMeta.lore(plugin.getMessageManager().getMessageList("gui.admin.kits.create-lore"));
            createKit.setItemMeta(createMeta);
        }
        inventory.setItem(31, createKit);

        // Slot 35: Back to Arena management
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.kits.back-name"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(35, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 31) {
            // Create Kit
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kits.msg-enter-name"));
            plugin.getGeneralListener().registerPendingKitCreation(player);
            return;
        }

        if (slot == 35) {
            // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new AdminArenaMenu(plugin).open(player);
            return;
        }

        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta != null && clickedMeta.getPersistentDataContainer().has(kitKey, PersistentDataType.STRING)) {
            String kitName = clickedMeta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                new KitSettingsMenu(plugin, kit).open(player);
            }
        }
    }
}
