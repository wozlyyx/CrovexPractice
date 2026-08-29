package com.crovex.practice.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class Kit {

    private final String name;
    private String displayName;
    private ItemStack icon;
    private ItemStack[] inventoryContents;
    private ItemStack[] armorContents;
    private List<PotionEffect> activeEffects;
    private KitType type;
    private boolean allowBlockPlace;
    private boolean allowBlockBreak;
    private boolean allowExplosions;

    public Kit(String name) {
        this.name = name;
        this.displayName = name;
        this.icon = new ItemStack(Material.DIAMOND_SWORD);
        this.inventoryContents = new ItemStack[36];
        this.armorContents = new ItemStack[4];
        this.activeEffects = new ArrayList<>();
        this.type = KitType.NORMAL;
        this.allowBlockPlace = false;
        this.allowBlockBreak = false;
        this.allowExplosions = false;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public ItemStack[] getInventoryContents() {
        return inventoryContents;
    }

    public void setInventoryContents(ItemStack[] inventoryContents) {
        this.inventoryContents = inventoryContents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public void setArmorContents(ItemStack[] armorContents) {
        this.armorContents = armorContents;
    }

    public List<PotionEffect> getActiveEffects() {
        return activeEffects;
    }

    public void setActiveEffects(List<PotionEffect> activeEffects) {
        this.activeEffects = activeEffects;
    }

    public KitType getType() {
        return type;
    }

    public void setType(KitType type) {
        this.type = type;
    }

    /**
     * Apply this kit to a player (incorporates custom layout if available).
     */
    public void applyToPlayer(Player player, ItemStack[] customLayout) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getActivePotionEffects().clear();

        // Apply inventory layout
        if (customLayout != null) {
            player.getInventory().setContents(customLayout);
        } else if (inventoryContents != null) {
            player.getInventory().setContents(inventoryContents);
        }

        // Apply armor
        if (armorContents != null) {
            player.getInventory().setArmorContents(armorContents);
        } else {
            player.getInventory().setArmorContents(new ItemStack[4]);
        }

        // Apply potion effects
        for (PotionEffect effect : activeEffects) {
            player.addPotionEffect(effect);
        }

        player.updateInventory();
    }

    public boolean isAllowBlockPlace() {
        return allowBlockPlace;
    }

    public void setAllowBlockPlace(boolean allowBlockPlace) {
        this.allowBlockPlace = allowBlockPlace;
    }

    public boolean isAllowBlockBreak() {
        return allowBlockBreak;
    }

    public void setAllowBlockBreak(boolean allowBlockBreak) {
        this.allowBlockBreak = allowBlockBreak;
    }

    public boolean isAllowExplosions() {
        return allowExplosions;
    }

    public void setAllowExplosions(boolean allowExplosions) {
        this.allowExplosions = allowExplosions;
    }
}
