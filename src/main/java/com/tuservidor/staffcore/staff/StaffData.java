package com.tuservidor.staffcore.staff;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StaffData {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack[] extraContents;

    private final int level;
    private final float exp;

    private final double health;
    private final int food;
    private final float saturation;
    private final boolean allowFlight;
    private final boolean flying;
    private final boolean canPickupItems;
    private final float walkSpeed;
    private final float flySpeed;

    private final GameMode gameMode;
    private final int slot;

    public StaffData(Player player) {
        this.contents = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.extraContents = player.getInventory().getExtraContents().clone();

        this.level = player.getLevel();
        this.exp = player.getExp();

        this.health = player.getHealth();
        this.food = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.canPickupItems = player.getCanPickupItems();
        this.walkSpeed = player.getWalkSpeed();
        this.flySpeed = player.getFlySpeed();

        this.gameMode = player.getGameMode();
        this.slot = player.getInventory().getHeldItemSlot();

        player.getInventory().clear();
    }

    public void restore(Player player) {
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setExtraContents(extraContents);

        player.setLevel(level);
        player.setExp(exp);

        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(food);
        player.setSaturation(saturation);

        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setCanPickupItems(canPickupItems);
        player.setWalkSpeed(walkSpeed);
        player.setFlySpeed(flySpeed);
        player.setFlying(flying && allowFlight);
        player.getInventory().setHeldItemSlot(slot);
    }
}
