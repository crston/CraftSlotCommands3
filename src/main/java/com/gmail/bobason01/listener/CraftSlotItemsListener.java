package com.gmail.bobason01.listener;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import com.gmail.bobason01.CraftSlotCommands;
import com.gmail.bobason01.util.ItemBuilder;
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

import java.util.*;

public class CraftSlotItemsListener implements Listener {

    private static final int[] COMMAND_SLOTS = {1, 2, 3, 4}; // 입력 슬롯
    private static final ItemStack[] items = new ItemStack[5]; // 0~4 전체 슬롯
    private final Set<UUID> pendingUpdate = Collections.newSetFromMap(new WeakHashMap<>());

    public CraftSlotItemsListener(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        ItemBuilder.loadFromConfig(Objects.requireNonNull(config.getConfigurationSection("slot-item")));
        for (int i = 0; i <= 4; i++) {
            items[i] = ItemBuilder.get(String.valueOf(i));
        }
    }

    @EventHandler
    public void onRecipeClick(PlayerRecipeBookClickEvent e) {
        if (isSelf2x2Crafting(e.getPlayer().getOpenInventory())) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        InventoryView view = e.getPlayer().getOpenInventory();
        if (isSelf2x2Crafting(view)) scheduleUpdate(view);
        removeFakeItems(view);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        InventoryView view = e.getPlayer().getOpenInventory();
        if (isSelf2x2Crafting(view)) scheduleUpdate(view);
        removeFakeItems(view);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        if (isSelf2x2Crafting(view)) scheduleUpdate(view);
        }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        InventoryView view = e.getView();
        if (isSelf2x2Crafting(view)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        removeFakeItems(e.getPlayer().getOpenInventory());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        removeFakeItems(e.getEntity().getOpenInventory());
    }

    private boolean isSelf2x2Crafting(InventoryView view) {
        Inventory inv = view.getTopInventory();
        if (!(inv instanceof CraftingInventory)) return false;
        if (inv.getSize() != 5) return false;
        return view.getPlayer() instanceof Player p && Objects.equals(inv.getHolder(), p);
    }

    private void scheduleUpdate(InventoryView view) {
        Player player = (Player) view.getPlayer();
        if (!pendingUpdate.add(player.getUniqueId())) return;

        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.getInstance(), () -> {
            pendingUpdate.remove(player.getUniqueId());
            if (isSelf2x2Crafting(view)) {
                removeFakeItems(view);
                addFakeItems(view);
            }
        }, 2L);
    }

    private void addFakeItems(InventoryView view) {
        Inventory inv = view.getTopInventory();

        for (int slot : COMMAND_SLOTS) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir()) {
                inv.setItem(slot, items[slot]);
            }
        }

        boolean showResult = Arrays.stream(COMMAND_SLOTS)
                .allMatch(i -> inv.getItem(i) != null && !Objects.requireNonNull(inv.getItem(i)).getType().isAir());

        if (showResult) {
            ItemStack result = inv.getItem(0);
            if (result == null || result.getType().isAir()) {
                inv.setItem(0, items[0]);
            }
        }
    }

    public static void removeFakeItems(InventoryView view) {
        Inventory inv = view.getTopInventory();
        for (int i = 0; i <= 4; i++) {
            ItemStack current = inv.getItem(i);
            if (isFakeItem(i, current)) {
                inv.setItem(i, null);
            }
        }
    }

    private static boolean isFakeItem(int slot, ItemStack item) {
        return item != null && item.isSimilar(items[slot]);
    }
}
