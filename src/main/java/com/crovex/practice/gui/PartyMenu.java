package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.party.Party;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
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
import java.util.UUID;

public class PartyMenu extends Menu {

    private final CrovexPractice plugin;

    public PartyMenu(CrovexPractice plugin) {
        super(27, plugin.getMessageManager().getMessage("gui.party.title"));
        this.plugin = plugin;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        // Fill background with black glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        Player viewer = null;
        // Search for the player who has this inventory open if we need it,
        // but since this menu is constructed per player, we will get the viewer dynamically in handleMenu,
        // and we will rely on open(Player) to fetch the party data.
    }

    @Override
    public void open(Player player) {
        setMenuItems(); // populate filler

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return;

        Party party = pp.getActiveParty();
        if (party == null) {
            // Not in a party: display a single "Create Party" button in the center (slot 13)
            ItemStack createBtn = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = createBtn.getItemMeta();
            meta.displayName(plugin.getMessageManager().getMessage("lobby.items.party"));
            createBtn.setItemMeta(meta);
            inventory.setItem(13, createBtn);
        } else {
            // Party details (Slot 10)
            ItemStack details = new ItemStack(Material.BOOK);
            ItemMeta detailsMeta = details.getItemMeta();
            detailsMeta.displayName(plugin.getMessageManager().getMessage("gui.party.members-title"));

            List<Component> lore = new ArrayList<>();
            String leaderName = Bukkit.getOfflinePlayer(party.getLeader()).getName();
            if (leaderName == null) leaderName = "Unknown";
            
            List<Component> template = plugin.getMessageManager().getMessageList("gui.party.members-lore",
                    "%leader%", leaderName,
                    "%size%", String.valueOf(party.getSize()));

            for (Component comp : template) {
                String serialized = MiniMessage.miniMessage().serialize(comp);
                if (serialized.contains("%members%")) {
                    String format = plugin.getMessageManager().getRawMessage("party.member-list-format");
                    if (format.isEmpty()) format = "<gray> - <yellow>%player%";
                    for (UUID mUuid : party.getMembers()) {
                        String name = Bukkit.getOfflinePlayer(mUuid).getName();
                        if (name != null) {
                            lore.add(MiniMessage.miniMessage().deserialize(format.replace("%player%", name)));
                        }
                    }
                } else {
                    lore.add(comp);
                }
            }
            detailsMeta.lore(lore);
            details.setItemMeta(detailsMeta);
            inventory.setItem(10, details);

            // Party FFA (Slot 12)
            ItemStack ffa = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta ffaMeta = ffa.getItemMeta();
            ffaMeta.displayName(plugin.getMessageManager().getMessage("gui.party.ffa-title"));
            ffaMeta.lore(plugin.getMessageManager().getMessageList("gui.party.ffa-lore"));
            ffa.setItemMeta(ffaMeta);
            inventory.setItem(12, ffa);

            // Party Split (Slot 13)
            ItemStack split = new ItemStack(Material.IRON_SWORD);
            ItemMeta splitMeta = split.getItemMeta();
            splitMeta.displayName(plugin.getMessageManager().getMessage("gui.party.split-title"));
            splitMeta.lore(plugin.getMessageManager().getMessageList("gui.party.split-lore"));
            split.setItemMeta(splitMeta);
            inventory.setItem(13, split);

            // Party Duel (Slot 14)
            ItemStack duel = new ItemStack(Material.TARGET);
            ItemMeta duelMeta = duel.getItemMeta();
            duelMeta.displayName(plugin.getMessageManager().getMessage("gui.party.duel-title"));
            duelMeta.lore(plugin.getMessageManager().getMessageList("gui.party.duel-lore"));
            duel.setItemMeta(duelMeta);
            inventory.setItem(14, duel);

            // Leave/Disband Party (Slot 16)
            boolean isLeader = party.isLeader(player.getUniqueId());
            ItemStack action = new ItemStack(Material.BARRIER);
            ItemMeta actionMeta = action.getItemMeta();
            if (isLeader) {
                actionMeta.displayName(plugin.getMessageManager().getMessage("gui.party.disband-title"));
                actionMeta.lore(plugin.getMessageManager().getMessageList("gui.party.disband-lore"));
            } else {
                actionMeta.displayName(plugin.getMessageManager().getMessage("gui.party.leave-title"));
                actionMeta.lore(plugin.getMessageManager().getMessageList("gui.party.leave-lore"));
            }
            action.setItemMeta(actionMeta);
            inventory.setItem(16, action);
        }

        player.openInventory(inventory);
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
            if (event.getSlot() == 13) {
                player.closeInventory();
                plugin.getPartyManager().createParty(player);
            }
            return;
        }

        boolean isLeader = party.isLeader(player.getUniqueId());
        int slot = event.getSlot();

        if (slot == 12) {
            // Party FFA
            if (!isLeader) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return;
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new PartyKitSelectMenu(plugin, PartyKitSelectMenu.PartyAction.FFA, null).open(player);
        } 
        else if (slot == 13) {
            // Party Split
            if (!isLeader) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return;
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new PartyKitSelectMenu(plugin, PartyKitSelectMenu.PartyAction.SPLIT, null).open(player);
        } 
        else if (slot == 14) {
            // Party Duel
            if (!isLeader) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return;
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new PartyDuelMenu(plugin).open(player);
        }
        else if (slot == 16) {
            // Leave / Disband
            player.closeInventory();
            plugin.getPartyManager().leaveParty(player);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }
    }
}
