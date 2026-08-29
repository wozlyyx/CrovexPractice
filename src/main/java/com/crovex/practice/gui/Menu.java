package com.crovex.practice.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import net.kyori.adventure.text.Component;

public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;

    public Menu(int size, Component title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public abstract void setMenuItems();

    public abstract void handleMenu(InventoryClickEvent event);

    public void open(org.bukkit.entity.Player player) {
        setMenuItems();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
