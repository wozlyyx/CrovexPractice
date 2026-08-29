package com.crovex.practice.gui;

import com.crovex.practice.CrovexPractice;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class PostMatchInventoryGUI extends Menu {

    private final String targetName;
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final double health;
    private final int food;
    private final int hits;

    public PostMatchInventoryGUI(String targetName, ItemStack[] contents, ItemStack[] armor, double health, int food, int hits) {
        super(45, CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.title", "%player%", targetName));
        this.targetName = targetName;
        this.contents = contents;
        this.armor = armor;
        this.health = health;
        this.food = food;
        this.hits = hits;
    }

    @Override
    public void setMenuItems() {
        // Fill background slots for row 5 (slots 36 to 44)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, filler);
        }

        // Place inventory contents in 0 to 35
        for (int i = 0; i < 36; i++) {
            if (contents != null && i < contents.length && contents[i] != null) {
                inventory.setItem(i, addDurabilityLore(contents[i].clone()));
            }
        }

        // Place armor in 36 to 39 (Helmet -> Chestplate -> Leggings -> Boots)
        if (armor != null) {
            if (armor.length > 3 && armor[3] != null) inventory.setItem(36, addDurabilityLore(armor[3].clone())); // Helmet
            if (armor.length > 2 && armor[2] != null) inventory.setItem(37, addDurabilityLore(armor[2].clone())); // Chestplate
            if (armor.length > 1 && armor[1] != null) inventory.setItem(38, addDurabilityLore(armor[1].clone())); // Leggings
            if (armor.length > 0 && armor[0] != null) inventory.setItem(39, addDurabilityLore(armor[0].clone())); // Boots
        }

        // Place off-hand in slot 40
        if (contents != null && contents.length > 40 && contents[40] != null && contents[40].getType() != Material.AIR) {
            inventory.setItem(40, addDurabilityLore(contents[40].clone()));
        }

        // Slot 41: Health & Food
        ItemStack stats = new ItemStack(Material.APPLE);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.displayName(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.status-title"));
        List<Component> statsLore = CrovexPractice.getInstance().getMessageManager().getMessageList("gui.postmatch.status-lore",
                "%health%", String.format("%.1f", health),
                "%food%", String.valueOf(food)
        );
        statsMeta.lore(statsLore);
        stats.setItemMeta(statsMeta);
        inventory.setItem(41, stats);

        // Slot 42: Armor & Off-hand Durability Summary (Anvil)
        ItemStack armorSummary = new ItemStack(Material.ANVIL);
        ItemMeta summaryMeta = armorSummary.getItemMeta();
        if (summaryMeta != null) {
            summaryMeta.displayName(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.summary-title"));
            List<Component> summaryLore = new ArrayList<>();
            summaryLore.add(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.summary-helmet",
                    "%durability%", getDurabilityString(armor != null && armor.length > 3 ? armor[3] : null)));
            summaryLore.add(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.summary-chestplate",
                    "%durability%", getDurabilityString(armor != null && armor.length > 2 ? armor[2] : null)));
            summaryLore.add(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.summary-leggings",
                    "%durability%", getDurabilityString(armor != null && armor.length > 1 ? armor[1] : null)));
            summaryLore.add(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.summary-boots",
                    "%durability%", getDurabilityString(armor != null && armor.length > 0 ? armor[0] : null)));
            if (contents != null && contents.length > 40 && contents[40] != null && contents[40].getType() != Material.AIR) {
                summaryLore.add(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.summary-offhand",
                        "%durability%", getDurabilityString(contents[40])));
            }
            summaryMeta.lore(summaryLore);
            armorSummary.setItemMeta(summaryMeta);
        }
        inventory.setItem(42, armorSummary);

        // Slot 43: Hits
        ItemStack combat = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta combatMeta = combat.getItemMeta();
        combatMeta.displayName(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.combat-title"));
        List<Component> combatLore = CrovexPractice.getInstance().getMessageManager().getMessageList("gui.postmatch.combat-lore",
                "%hits%", String.valueOf(hits)
        );
        combatMeta.lore(combatLore);
        combat.setItemMeta(combatMeta);
        inventory.setItem(43, combat);
    }

    private ItemStack addDurabilityLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable) {
            org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
            int maxDur = item.getType().getMaxDurability();
            if (maxDur > 0) {
                int curDur = maxDur - damageable.getDamage();
                int pct = (int) Math.round((curDur * 100.0) / maxDur);
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                else lore = new ArrayList<>(lore);
                
                lore.add(Component.empty());
                lore.add(CrovexPractice.getInstance().getMessageManager().getMessage("gui.postmatch.durability-lore",
                        "%current%", String.valueOf(curDur),
                        "%max%", String.valueOf(maxDur),
                        "%percent%", String.valueOf(pct)
                ));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private String getDurabilityString(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return CrovexPractice.getInstance().getMessageManager().getRawMessage("gui.postmatch.durability-none");
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable) {
            org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
            int maxDur = item.getType().getMaxDurability();
            if (maxDur > 0) {
                int curDur = maxDur - damageable.getDamage();
                int pct = (int) Math.round((curDur * 100.0) / maxDur);
                String color = "<green>";
                if (pct < 25) color = "<red>";
                else if (pct < 50) color = "<gold>";
                return curDur + "/" + maxDur + " (" + color + pct + "%<gray>)";
            }
        }
        return CrovexPractice.getInstance().getMessageManager().getRawMessage("gui.postmatch.durability-unbreakable");
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        // Post match inventory is read-only
        event.setCancelled(true);
    }
}
