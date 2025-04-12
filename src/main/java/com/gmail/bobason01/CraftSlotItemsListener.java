package com.gmail.bobason01;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        sendGhostItemsLater(e.getPlayer());
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
        sendGhostItemsLater(e.getPlayer());
    }

    private void sendGhostItemsLater(Player player) {
        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.plugin, () -> {
            if (!player.isOnline() || player.isDead()) return;
            sendGhostItems(player);
        }, 1L);
    }

    public static void sendGhostItems(Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
        packet.getIntegers().write(0, 0); // windowId 0 = 플레이어 인벤토리

        List<ItemStack> items = new ArrayList<>();
        items.add(i0); // crafting result
        items.add(i1); // crafting grid
        items.add(i2);
        items.add(i3);
        items.add(i4);

        while (items.size() < 46) items.add(null); // 나머지 인벤토리 슬롯

        packet.getItemListModifier().write(0, items);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            CraftSlotCommands.plugin.getLogger().warning("Failed to send ghost items to " + player.getName());
        }
    }

    public static void removeGhostItems(Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
        packet.getIntegers().write(0, 0);

        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 46; i++) items.add(null);
        packet.getItemListModifier().write(0, items);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            CraftSlotCommands.plugin.getLogger().warning("Failed to clear ghost items for " + player.getName());
        }
    }

    public void reload(FileConfiguration config) {
        i0 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.0")));
        i1 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.1")));
        i2 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.2")));
        i3 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.3")));
        i4 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.4")));
    }
}
