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
        }, 2L); // 최소한의 딜레이로 안정성 확보
    }

    public static void sendGhostItems(Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
        packet.getIntegers().write(0, 0); // 0번 창: 플레이어 기본 인벤토리

        List<ItemStack> items = new ArrayList<>();
        items.add(i0); // 결과 슬롯
        items.add(i1);
        items.add(i2);
        items.add(i3);
        items.add(i4);

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
        // 빈 아이템을 전송하여 초기화
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
        i0 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.0")));
        i1 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.1")));
        i2 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.2")));
        i3 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.3")));
        i4 = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item.4")));
    }
}
