package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.party.Party;
import com.crovex.practice.player.PracticePlayer;
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

public class PartyKitSelectMenu extends Menu {

    public enum PartyAction {
        FFA,
        SPLIT,
        CHALLENGE
    }

    private final CrovexPractice plugin;
    private final PartyAction action;
    private final Player targetLeader;
    private final NamespacedKey kitKey;

    public PartyKitSelectMenu(CrovexPractice plugin, PartyAction action, Player targetLeader) {
        super(calculateSize(plugin), plugin.getMessageManager().getMessage("gui.party.kitselect-title"));
        this.plugin = plugin;
        this.action = action;
        this.targetLeader = targetLeader;
        this.kitKey = new NamespacedKey(plugin, "kit_name");
    }

    private static int calculateSize(CrovexPractice plugin) {
        int totalKits = plugin.getKitManager().getKits().size();
        if (totalKits <= 7) return 27;
        if (totalKits <= 14) return 36;
        if (totalKits <= 21) return 45;
        return 54;
    }

    @Override
    public void setMenuItems() {
        int size = inventory.getSize();
        // Fill background with black glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getKits());
        
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
            if (kits.size() > slots.size()) {
                slots.clear();
                for (int i = 0; i < 54; i++) {
                    slots.add(i);
                }
            }
        }

        int index = 0;
        for (Kit kit : kits) {
            if (!plugin.getArenaManager().hasCompatibleArena(kit)) continue;
            if (index >= slots.size()) break;

            ItemStack item = kit.getIcon().clone();
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.getName());
                meta.displayName(plugin.getMessageManager().getMessage("gui.queue.kit-name", "%kit%", kit.getDisplayName()));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(MiniMessage.miniMessage().deserialize("<yellow>Click to Select!"));
                meta.lore(lore);

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
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return;

        Party party = pp.getActiveParty();
        if (party == null) {
            player.closeInventory();
            return;
        }

        ItemMeta clickedMeta = clicked.getItemMeta();
        Kit selectedKit = null;
        if (clickedMeta != null && clickedMeta.getPersistentDataContainer().has(kitKey, PersistentDataType.STRING)) {
            String kitName = clickedMeta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
            selectedKit = plugin.getKitManager().getKit(kitName);
        }

        if (selectedKit != null) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

            if (action == PartyAction.FFA) {
                plugin.getPartyManager().startFFA(party, selectedKit);
            } 
            else if (action == PartyAction.SPLIT) {
                plugin.getPartyManager().startSplit(party, selectedKit);
            } 
            else if (action == PartyAction.CHALLENGE) {
                if (targetLeader != null && targetLeader.isOnline()) {
                    plugin.getPartyManager().sendChallenge(player, targetLeader, selectedKit);
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                }
            }
        }
    }
}
