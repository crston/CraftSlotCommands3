package com.gmail.bobason01;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

    private static final ItemFlag[] ALL_FLAGS = ItemFlag.values();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Map<Character, String> COLOR_MAP = Map.ofEntries(
            Map.entry('0', "<black>"),     Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),  Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),      Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"), Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),     Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),       Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),    Map.entry('f', "<white>"),
            Map.entry('l', "<bold>"),      Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    public static ItemStack build(ConfigurationSection config) {
        Material material;
        try {
            material = Material.valueOf(config.getString("material", "BARRIER").toUpperCase());
        } catch (IllegalArgumentException e) {
            logError("Invalid material in config: " + config.getCurrentPath());
            return errorItem("Invalid material type", config);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            logError("ItemMeta was null for material: " + material);
            return errorItem("No ItemMeta found", config);
        }

        boolean errored = false;
        List<Component> lore = new ArrayList<>();
        List<Component> errorLore = new ArrayList<>();

        // Display name
        if (config.contains("name")) {
            String name = config.getString("name");
            if (name != null) {
                meta.displayName(parse(name));
            }
        }

        // Custom model data
        if (config.contains("model")) {
            meta.setCustomModelData(config.getInt("model"));
        }

        // Hide flags
        if (config.getBoolean("hide-flags")) {
            meta.addItemFlags(ALL_FLAGS);
        }

        // Unbreakable
        if (config.getBoolean("unbreakable")) {
            meta.setUnbreakable(true);
        }

        // Durability
        if (config.contains("damage")) {
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(config.getInt("damage"));
            } else {
                errored = true;
                errorLore.add(Component.text("Item cannot have durability.", NamedTextColor.RED));
            }
        }

        // Skull meta
        if (meta instanceof SkullMeta skullMeta) {
            if (config.contains("skull-owner-uuid")) {
                try {
                    UUID uuid = UUID.fromString(Objects.requireNonNull(config.getString("skull-owner-uuid")));
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
                    skullMeta.setOwningPlayer(owner);
                } catch (Exception e) {
                    errored = true;
                    errorLore.add(Component.text("Invalid UUID.", NamedTextColor.RED));
                    logError("Invalid skull-owner-uuid in config: " + config.getCurrentPath());
                }
            } else if (config.contains("skull-texture-value")) {
                String texture = config.getString("skull-texture-value");
                if (texture != null) {
                    try {
                        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                        profile.getProperties().put("textures", new Property("textures", texture));
                        Field field = skullMeta.getClass().getDeclaredField("profile");
                        field.setAccessible(true);
                        field.set(skullMeta, profile);
                    } catch (Exception e) {
                        errored = true;
                        errorLore.add(Component.text("Failed to set skull texture.", NamedTextColor.RED));
                        logError("Reflection error setting skull texture in config: " + config.getCurrentPath());
                    }
                }
            }
        } else if (config.contains("skull-owner-uuid") || config.contains("skull-texture-value")) {
            errored = true;
            errorLore.add(Component.text("Skull data on non-skull item.", NamedTextColor.RED));
        }

        // Lore
        if (!errored && config.contains("lore")) {
            for (String line : config.getStringList("lore")) {
                lore.add(parse(line));
            }
            meta.lore(lore);
        }

        if (errored) {
            return errorItem(errorLore, config);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Parses legacy & codes into MiniMessage-compatible formatting,
     * adds <italic:false> by default.
     */
    private static Component parse(String input) {
        String converted = convertLegacyToMiniMessage(input);
        return MM.deserialize(converted);
    }

    private static String convertLegacyToMiniMessage(String input) {
        StringBuilder output = new StringBuilder();
        char[] chars = input.toCharArray();

        List<String> openTags = new ArrayList<>();
        output.append("<italic:false>"); // 기본으로 기울임 제거

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                String tag = COLOR_MAP.get(code);
                if (tag != null) {
                    // 기존 열려있는 태그 닫기
                    for (int j = openTags.size() - 1; j >= 0; j--) {
                        output.append("</").append(openTags.get(j)).append(">");
                    }
                    openTags.clear();

                    // 새 태그 열기
                    String tagName = tag.replace("<", "").replace(">", "");
                    output.append(tag).append("<italic:false>");
                    openTags.add(tagName);

                    i++; // Skip code char
                    continue;
                }
            }
            output.append(chars[i]);
        }

        // 남은 태그 닫기
        for (int j = openTags.size() - 1; j >= 0; j--) {
            output.append("</").append(openTags.get(j)).append(">");
        }

        return output.toString();
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

    private static void logError(String msg) {
        Bukkit.getLogger().warning("[ItemBuilder] " + msg);
    }
}
