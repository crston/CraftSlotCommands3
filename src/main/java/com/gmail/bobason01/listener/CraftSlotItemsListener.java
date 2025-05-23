package com.gmail.bobason01.listener;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import com.gmail.bobason01.CraftSlotCommands;
import com.gmail.bobason01.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
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
import java.util.concurrent.ConcurrentHashMap;

public class CraftSlotItemsListener implements Listener {

    private static final int[] COMMAND_SLOTS = {1, 2, 3, 4};
    private static final ItemStack[] items = new ItemStack[5];
    private static final boolean[] useSlot = new boolean[5];
    private final Set<UUID> pendingUpdate = ConcurrentHashMap.newKeySet();

    public CraftSlotItemsListener(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        ItemBuilder.loadFromConfig(Objects.requireNonNull(config.getConfigurationSection("slot-item")));
        ConfigurationSection useSlotSection = config.getConfigurationSection("use-slot");

        for (int i = 0; i <= 4; i++) {
            items[i] = ItemBuilder.get(String.valueOf(i));
            useSlot[i] = useSlotSection != null && useSlotSection.getBoolean(String.valueOf(i), true);
        }
    }

    public static boolean useSlot(int i) {
        return i < 0 || i >= useSlot.length || !useSlot[i];
    }

    @EventHandler
    public void onRecipeClick(PlayerRecipeBookClickEvent e) {
        if (isSelf2x2Crafting(e.getPlayer().getOpenInventory())) e.setCancelled(true);
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

    public static boolean isSelf2x2Crafting(InventoryView view) {
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
        Player player = (Player) view.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Inventory inv = view.getTopInventory();
        int size = inv.getSize();

        for (int slot : COMMAND_SLOTS) {
            if (useSlot(slot) || slot >= size) continue;

            ItemStack item = items[slot];
            if (item == null) continue;

            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir()) {
                inv.setItem(slot, item);
            }
        }

        if (useSlot[0] && items[0] != null && size > 0) {
            boolean showResult = true;
            for (int slot : COMMAND_SLOTS) {
                if (useSlot(slot)) continue;
                ItemStack i = inv.getItem(slot);
                if (i == null || i.getType().isAir()) {
                    showResult = false;
                    break;
                }
            }

            if (showResult) {
                ItemStack result = inv.getItem(0);
                if (result == null || result.getType().isAir()) {
                    inv.setItem(0, items[0]);
                }
            }
        }
    }

    public static void removeFakeItems(InventoryView view) {
        Inventory inv = view.getTopInventory();
        int size = inv.getSize();

        for (int i = 0; i <= 4 && i < size; i++) {
            if (useSlot(i)) continue;

            ItemStack expected = items[i];
            if (expected == null) continue;

            ItemStack current = inv.getItem(i);
            if (current != null && current.isSimilar(expected)) {
                inv.setItem(i, null);
            }
        }
    }
}