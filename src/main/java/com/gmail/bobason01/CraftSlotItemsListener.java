package com.gmail.bobason01;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class CraftSlotItemsListener implements Listener {

    private static final int[] COMMAND_SLOTS = {1, 2, 3, 4}; // 1~4: 입력 슬롯
    private static final ItemStack[] items = new ItemStack[5]; // 0~4: UI 아이템 전체

    public CraftSlotItemsListener(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        for (int i = 0; i <= 4; i++) {
            items[i] = ItemBuilder.build(Objects.requireNonNull(config.getConfigurationSection("slot-item." + i)));
        }
    }

    @EventHandler
    public void onRecipeClick(PlayerRecipeBookClickEvent e) {
        if (is2x2Crafting(e.getPlayer().getOpenInventory().getTopInventory())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!is2x2Crafting(e.getInventory())) return;
        removeCommandItems(e.getView());
        scheduleAddItems(e.getWhoClicked().getOpenInventory());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!is2x2Crafting(e.getInventory())) return;
        removeCommandItems(e.getView());
        scheduleAddItems(e.getPlayer().getOpenInventory());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        InventoryView view = e.getPlayer().getOpenInventory();
        if (is2x2Crafting(view.getTopInventory())) {
            removeCommandItems(view);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        InventoryView view = e.getEntity().getOpenInventory();
        if (is2x2Crafting(view.getTopInventory())) {
            removeCommandItems(view);
        }
    }

    private boolean is2x2Crafting(Inventory inv) {
        return inv instanceof CraftingInventory && inv.getSize() == 5;
    }

    private void scheduleAddItems(InventoryView view) {
        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.plugin, () -> {
            if (is2x2Crafting(view.getTopInventory())) {
                addCommandItems(view);
            }
        }, 2L);
    }

    private void addCommandItems(InventoryView view) {
        Inventory inv = view.getTopInventory();

        // 먼저 1~4번 커맨드 아이템을 채운다
        for (int slot : COMMAND_SLOTS) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir()) {
                inv.setItem(slot, items[slot]);
            }
        }

        // 슬롯 1~4가 모두 채워졌을 때만 0번을 세팅 (클라이언트에 안 보이는 것 방지)
        boolean showResultSlot = true;
        for (int i = 1; i <= 4; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) {
                showResultSlot = false;
                break;
            }
        }
        if (showResultSlot && (inv.getItem(0) == null || Objects.requireNonNull(inv.getItem(0)).getType().isAir())) {
            inv.setItem(0, items[0]);
        }

        if (view.getPlayer() instanceof Player player) {
            player.updateInventory();
        }
    }

    public static void removeCommandItems(InventoryView view) {
        Inventory inv = view.getTopInventory();
        for (int i = 0; i <= 4; i++) {
            ItemStack current = inv.getItem(i);
            if (current != null && current.isSimilar(items[i])) {
                inv.setItem(i, null);
            }
        }
    }
}
