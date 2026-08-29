package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.party.Party;
import com.crovex.practice.party.PartyChallenge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class PartyDuelMenu extends Menu {

    private final CrovexPractice plugin;
    private final NamespacedKey leaderKey;
    private final NamespacedKey actionKey;

    public PartyDuelMenu(CrovexPractice plugin) {
        super(54, plugin.getMessageManager().getMessage("gui.party-duel.title"));
        this.plugin = plugin;
        this.leaderKey = new NamespacedKey(plugin, "leader_name");
        this.actionKey = new NamespacedKey(plugin, "action_type");
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

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

        ItemStack separator = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta separatorMeta = separator.getItemMeta();
        if (separatorMeta != null) {
            separatorMeta.displayName(Component.empty());
            separator.setItemMeta(separatorMeta);
        }

        for (int i = 0; i < 54; i++) {
            // Borders: rows 0 and 5, columns 0 and 8
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, blackFiller);
            } 
            // Separator column: column 4
            else if (i % 9 == 4) {
                inventory.setItem(i, separator);
            } 
            // Slots for items
            else {
                inventory.setItem(i, grayFiller);
            }
        }

        // Header active parties in slot 2
        ItemStack activeHeader = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta activeHeaderMeta = activeHeader.getItemMeta();
        if (activeHeaderMeta != null) {
            activeHeaderMeta.displayName(plugin.getMessageManager().getMessage("gui.party-duel.active-header"));
            activeHeaderMeta.lore(plugin.getMessageManager().getMessageList("gui.party-duel.active-header-lore"));
            activeHeader.setItemMeta(activeHeaderMeta);
        }
        inventory.setItem(2, activeHeader);

        // Header incoming challenges in slot 6
        ItemStack incomingHeader = new ItemStack(Material.SHIELD);
        ItemMeta incomingHeaderMeta = incomingHeader.getItemMeta();
        if (incomingHeaderMeta != null) {
            incomingHeaderMeta.displayName(plugin.getMessageManager().getMessage("gui.party-duel.incoming-header"));
            incomingHeaderMeta.lore(plugin.getMessageManager().getMessageList("gui.party-duel.incoming-header-lore"));
            incomingHeader.setItemMeta(incomingHeaderMeta);
        }
        inventory.setItem(6, incomingHeader);

        // Back arrow at slot 49
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getMessageManager().getMessage("gui.admin.editor.back"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(49, back);
    }

    public void open(Player player) {
        setMenuItems();

        Party viewerParty = plugin.getPartyManager().getParty(player);
        if (viewerParty == null) {
            player.closeInventory();
            return;
        }

        // 1. Populating Active Parties (Left Section)
        int[] activeSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};
        int activeIndex = 0;

        for (Party party : plugin.getPartyManager().getParties()) {
            if (party.getLeader().equals(player.getUniqueId())) continue; // Skip own party

            if (activeIndex >= activeSlots.length) break;

            OfflinePlayer leader = Bukkit.getOfflinePlayer(party.getLeader());
            String leaderName = leader.getName() != null ? leader.getName() : "Bilinmeyen";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(leader);
                meta.getPersistentDataContainer().set(leaderKey, PersistentDataType.STRING, leaderName);
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "CHALLENGE");
                meta.displayName(plugin.getMessageManager().getMessage("gui.party-duel.item-name", "%leader%", leaderName));

                List<Component> lore = new ArrayList<>();
                List<Component> template = plugin.getMessageManager().getMessageList("gui.party-duel.item-lore",
                        "%leader%", leaderName,
                        "%size%", String.valueOf(party.getSize()));

                for (Component comp : template) {
                    String serialized = MiniMessage.miniMessage().serialize(comp);
                    if (serialized.contains("%members%")) {
                        String format = plugin.getMessageManager().getRawMessage("party.member-list-format");
                        if (format.isEmpty()) format = "<gray> - <yellow>%player%";
                        for (UUID memberUuid : party.getMembers()) {
                            String name = Bukkit.getOfflinePlayer(memberUuid).getName();
                            if (name != null) {
                                lore.add(MiniMessage.miniMessage().deserialize(format.replace("%player%", name)));
                            }
                        }
                    } else {
                        lore.add(comp);
                    }
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }

            inventory.setItem(activeSlots[activeIndex], head);
            activeIndex++;
        }

        // 2. Populating Incoming Challenges (Right Section)
        int[] incomingSlots = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
        int incomingIndex = 0;

        List<PartyChallenge> incomingChallenges = plugin.getPartyManager().getActiveChallenges(player.getUniqueId());
        for (PartyChallenge challenge : incomingChallenges) {
            if (incomingIndex >= incomingSlots.length) break;

            OfflinePlayer challengerLeader = Bukkit.getOfflinePlayer(challenge.getChallengerLeader());
            String challengerName = challengerLeader.getName() != null ? challengerLeader.getName() : "Bilinmeyen";
            
            Party challengerParty = plugin.getPartyManager().getParty(challenge.getChallengerLeader());
            int partySize = challengerParty != null ? challengerParty.getSize() : 1;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(challengerLeader);
                meta.getPersistentDataContainer().set(leaderKey, PersistentDataType.STRING, challengerName);
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "INCOMING");
                meta.displayName(plugin.getMessageManager().getMessage("gui.party-duel.incoming-item-name", "%leader%", challengerName));

                List<Component> lore = new ArrayList<>();
                List<Component> template = plugin.getMessageManager().getMessageList("gui.party-duel.incoming-item-lore",
                        "%leader%", challengerName,
                        "%size%", String.valueOf(partySize),
                        "%kit%", challenge.getKit().getDisplayName());

                for (Component comp : template) {
                    String serialized = MiniMessage.miniMessage().serialize(comp);
                    if (serialized.contains("%members%")) {
                        if (challengerParty != null) {
                            String format = plugin.getMessageManager().getRawMessage("party.member-list-format");
                            if (format.isEmpty()) format = "<gray> - <yellow>%player%";
                            for (UUID memberUuid : challengerParty.getMembers()) {
                                String name = Bukkit.getOfflinePlayer(memberUuid).getName();
                                if (name != null) {
                                    lore.add(MiniMessage.miniMessage().deserialize(format.replace("%player%", name)));
                                }
                            }
                        }
                    } else {
                        lore.add(comp);
                    }
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }

            addGlow(head);
            inventory.setItem(incomingSlots[incomingIndex], head);
            incomingIndex++;
        }

        player.openInventory(inventory);
    }

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR 
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE 
                || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE
                || clicked.getType() == Material.PURPLE_STAINED_GLASS_PANE
                || clicked.getType() == Material.DIAMOND_SWORD
                || clicked.getType() == Material.SHIELD) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new PartyMenu(plugin).open(player);
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            String leaderName = meta.getPersistentDataContainer().get(leaderKey, PersistentDataType.STRING);
            
            Player targetLeader = Bukkit.getPlayer(leaderName);
            if (targetLeader == null || !targetLeader.isOnline()) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                open(player); // refresh
                return;
            }

            if ("CHALLENGE".equals(action)) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new PartyKitSelectMenu(plugin, PartyKitSelectMenu.PartyAction.CHALLENGE, targetLeader).open(player);
            } 
            else if ("INCOMING".equals(action)) {
                // Check click type
                if (event.isLeftClick()) {
                    // Accept Challenge
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                    plugin.getPartyManager().acceptChallenge(player, targetLeader);
                    player.closeInventory();
                } else if (event.isRightClick()) {
                    // Decline Challenge
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f);
                    plugin.getPartyManager().declineChallenge(player, targetLeader);
                    open(player); // Refresh menu to show it removed
                }
            }
        }
    }
}
