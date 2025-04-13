package com.gmail.bobason01;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.*;

public class ItemBuilder {

    public static ItemStack build(ConfigurationSection config) {
        Material material;
        try {
            material = Material.valueOf(config.getString("material", "BARRIER"));
        } catch (IllegalArgumentException e) {
            return errorItem("Invalid material type", config);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return errorItem("No ItemMeta found", config);

        boolean errored = false;
        List<Component> lore = new ArrayList<>();
        List<Component> errorLore = new ArrayList<>();

        // Display name
        if (config.contains("name")) {
            String name = config.getString("name");
            if (name != null) {
                meta.displayName(Component.text(name.replace('&', '§')));
            }
        }

        if (config.contains("model")) meta.setCustomModelData(config.getInt("model"));
        if (config.getBoolean("hide-flags")) meta.addItemFlags(ItemFlag.values());
        if (config.getBoolean("unbreakable")) meta.setUnbreakable(true);

        if (config.contains("damage") && meta instanceof Damageable damageable) {
            damageable.setDamage(config.getInt("damage"));
        } else if (config.contains("damage")) {
            errored = true;
            errorLore.add(Component.text("Item cannot have durability.", NamedTextColor.RED));
        }

        // Skull properties
        if (meta instanceof SkullMeta skullMeta) {
            if (config.contains("skull-owner-uuid")) {
                try {
                    String uuidStr = config.getString("skull-owner-uuid");
                    if (uuidStr != null) {
                        UUID uuid = UUID.fromString(uuidStr);
                        OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
                        skullMeta.setOwningPlayer(owner);
                    }
                } catch (Exception e) {
                    errored = true;
                    errorLore.add(Component.text("Invalid UUID.", NamedTextColor.RED));
                }
            } else if (config.contains("skull-texture-value")) {
                try {
                    String texture = config.getString("skull-texture-value");
                    if (texture != null) {
                        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                        profile.getProperties().put("textures", new Property("textures", texture));
                        Field field = skullMeta.getClass().getDeclaredField("profile");
                        field.setAccessible(true);
                        field.set(skullMeta, profile);
                    }
                } catch (Exception e) {
                    errored = true;
                    errorLore.add(Component.text("Failed to set skull texture.", NamedTextColor.RED));
                }
            }
        } else if (config.contains("skull-owner-uuid") || config.contains("skull-texture-value")) {
            errored = true;
            errorLore.add(Component.text("Skull data on non-skull item.", NamedTextColor.RED));
        }

        // Lore
        if (!errored && config.contains("lore")) {
            for (String line : config.getStringList("lore")) {
                lore.add(Component.text(line.replace('&', '§')));
            }
            meta.lore(lore);
        }

        if (errored) {
            return errorItem(errorLore, config);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack errorItem(String errorMsg, ConfigurationSection config) {
        return errorItem(List.of(Component.text(errorMsg, NamedTextColor.RED)), config);
    }

    private static ItemStack errorItem(List<Component> errorLore, ConfigurationSection config) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("ERROR!", NamedTextColor.DARK_RED));
            List<Component> fullLore = new ArrayList<>(errorLore);
            fullLore.add(Component.empty());
            fullLore.add(Component.text("Config path: ", NamedTextColor.GRAY)
                    .append(Component.text(Objects.requireNonNull(config.getCurrentPath()), NamedTextColor.YELLOW)));
            meta.lore(fullLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
