package com.tuservidor.staffcore.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {

    private static Plugin plugin;

    private final ItemStack item;
    private final List<String> lore = new ArrayList<>();

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public static void init(Plugin owningPlugin) {
        plugin = owningPlugin;
    }

    public static ItemBuilder skull(OfflinePlayer owner) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        ItemMeta meta = builder.item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            builder.item.setItemMeta(skullMeta);
        }
        return builder;
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Messages.color(name));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(String line) {
        lore.add(Messages.color(line));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        for (String line : lines) {
            lore.add(Messages.color(line));
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder tag(String key, String value) {
        if (plugin == null) {
            return this;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder glow() {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(flags);
        item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
