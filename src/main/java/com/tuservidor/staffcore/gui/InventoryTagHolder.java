package com.tuservidor.staffcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class InventoryTagHolder implements InventoryHolder {

    public static final String INSPECT = "inspect";
    public static final String ENDER = "ender";

    private final String tag;
    private Inventory inventory;

    public InventoryTagHolder(String tag) {
        this.tag = tag;
    }

    @Override
    public Inventory getInventory() {
        return inventory == null ? Bukkit.createInventory(null, 9) : inventory;
    }

    public String tag() {
        return tag;
    }

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }
}
