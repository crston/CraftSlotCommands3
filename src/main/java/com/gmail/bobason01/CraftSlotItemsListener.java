package com.gmail.bobason01;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CraftSlotItemsListener implements Listener {

    private static ItemStack i0, i1, i2, i3, i4;

    public CraftSlotItemsListener(FileConfiguration config) {
        reload(config);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        sendGhostItemsLater(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        sendGhostItemsLater((Player) e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        removeGhostItems(e.getPlayer());
    }

    public static void sendGhostItemsLater(Player player) {
        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.plugin, () -> {
            if (!player.isOnline() || player.isDead()) return;
            sendGhostItems(player);
        }, 2L);
    }

    public static void sendGhostItems(Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
        packet.getIntegers().write(0, 0); // Player inventory window

        List<ItemStack> items = new ArrayList<>();
        items.add(i0 != null ? i0 : new ItemStack(org.bukkit.Material.AIR));
        items.add(i1 != null ? i1 : new ItemStack(org.bukkit.Material.AIR));
        items.add(i2 != null ? i2 : new ItemStack(org.bukkit.Material.AIR));
        items.add(i3 != null ? i3 : new ItemStack(org.bukkit.Material.AIR));
        items.add(i4 != null ? i4 : new ItemStack(org.bukkit.Material.AIR));

        while (items.size() < 46) {
            items.add(null);
        }

        packet.getItemListModifier().write(0, items);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            CraftSlotCommands.plugin.getLogger().warning("Failed to send ghost items: " + e.getMessage());
        }
    }

    public static void removeGhostItems(Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
        packet.getIntegers().write(0, 0);

        List<ItemStack> empty = new ArrayList<>();
        for (int i = 0; i < 46; i++) empty.add(null);
        packet.getItemListModifier().write(0, empty);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            CraftSlotCommands.plugin.getLogger().warning("Failed to clear ghost items: " + e.getMessage());
        }
    }

    public void reload(FileConfiguration config) {
        i0 = safeBuild(config.getConfigurationSection("slot-item.0"));
        i1 = safeBuild(config.getConfigurationSection("slot-item.1"));
        i2 = safeBuild(config.getConfigurationSection("slot-item.2"));
        i3 = safeBuild(config.getConfigurationSection("slot-item.3"));
        i4 = safeBuild(config.getConfigurationSection("slot-item.4"));
    }

    private ItemStack safeBuild(org.bukkit.configuration.ConfigurationSection section) {
        return ItemBuilder.build(section);
    }
}
