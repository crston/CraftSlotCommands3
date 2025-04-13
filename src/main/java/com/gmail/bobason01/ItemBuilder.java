package com.gmail.bobason01;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public static ItemStack build(ConfigurationSection config) {
        if (config == null || !config.contains("material")) {
            return new ItemStack(Material.AIR);
        }

        try {
            Material mat = Material.valueOf(config.getString("material", "AIR"));
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;

            if (config.contains("name")) {
                String name = config.getString("name");
                if (name != null) {
                    Component displayName = deserialize(name);
                    meta.displayName(displayName);
                }
            }

            if (config.contains("model")) {
                meta.setCustomModelData(config.getInt("model"));
            }

            if (config.getBoolean("hide-flags", false)) {
                meta.addItemFlags(ItemFlag.values());
            }

            if (config.getBoolean("unbreakable", false)) {
                meta.setUnbreakable(true);
            }

            if (config.contains("lore")) {
                List<Component> lore = new ArrayList<>();
                for (String line : config.getStringList("lore")) {
                    lore.add(deserialize(line));
                }
                meta.lore(lore);
            }

            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CraftSlotCommands] Item Setting Error - " + e.getMessage());
            return new ItemStack(Material.AIR);
        }
    }

    private static Component deserialize(String text) {
        if (text.contains("<")) {
            return mm.deserialize(text);
        } else {
            return legacy.deserialize(text).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        }
    }
}
