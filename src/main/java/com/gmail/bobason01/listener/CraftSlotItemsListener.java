package com.gmail.bobason01.listener;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import com.gmail.bobason01.CraftSlotCommands;
import com.gmail.bobason01.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class CraftSlotItemsListener implements Listener {

    private static final List<Integer> COMMAND_SLOTS = List.of(1, 2, 3, 4);
    private static final ItemStack[] items = new ItemStack[5];
    private static final boolean[] useSlot = new boolean[5];
    private final NamespacedKey fakeItemKey = new NamespacedKey(CraftSlotCommands.getInstance(), "fake-item");

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
    public void onInventoryOpen(InventoryOpenEvent e) {
        InventoryView view = e.getPlayer().getOpenInventory();
        if (!shouldShowMenuItems(view)) return;

        removeAllTaggedFakeItems(view);
        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.getInstance(), () -> {
            if (shouldShowMenuItems(view)) {
                addFakeItems(view);
            }
        }, 2L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        if (!shouldShowMenuItems(view)) return;

        removeAllTaggedFakeItems(view);
        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.getInstance(), () -> {
            if (shouldShowMenuItems(view)) {
                addFakeItems(view);
            }
        }, 2L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        InventoryView view = e.getPlayer().getOpenInventory();
        if (!shouldShowMenuItems(view)) return;

        removeAllTaggedFakeItems(view);
        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.getInstance(), () -> {
            if (shouldShowMenuItems(view)) {
                addFakeItems(view);
            }
        }, 2L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (shouldShowMenuItems(e.getView())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        clearFakeItemsEverywhere(e.getPlayer());
        e.getPlayer().closeInventory();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        clearFakeItemsEverywhere(e.getEntity());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(CraftSlotCommands.getInstance(), () -> {
            InventoryView view = e.getPlayer().getOpenInventory();
            if (shouldShowMenuItems(view)) {
                addFakeItems(view);
            }
        }, 2L);
    }

    @EventHandler
    public void onRecipeClick(PlayerRecipeBookClickEvent e) {
        if (shouldShowMenuItems(e.getPlayer().getOpenInventory())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        ItemStack item = e.getEntity().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(fakeItemKey, PersistentDataType.BYTE)) {
            e.setCancelled(true);
        }
    }

    private void addFakeItems(InventoryView view) {
        if (!shouldShowMenuItems(view)) return;

        Player player = (Player) view.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Inventory inv = view.getTopInventory();

        for (int slot : COMMAND_SLOTS) {
            if (useSlot(slot) || slot >= inv.getSize()) continue;

            ItemStack base = items[slot];
            if (base == null) continue;

            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir()) {
                ItemStack clone = base.clone();
                clone.editMeta(meta -> meta.getPersistentDataContainer().set(fakeItemKey, PersistentDataType.BYTE, (byte) 1));
                inv.setItem(slot, clone);
            }
        }

        if (items[0] != null) {
            boolean showResult = true;
            for (int slot : COMMAND_SLOTS) {
                if (useSlot(slot)) continue;
                ItemStack i = inv.getItem(slot);
                if (i == null || i.getType().isAir()) {
                    showResult = false;
                    break;
                }
            }

            if (showResult && (inv.getItem(0) == null || Objects.requireNonNull(inv.getItem(0)).getType().isAir())) {
                ItemStack clone = items[0].clone();
                clone.editMeta(meta -> meta.getPersistentDataContainer().set(fakeItemKey, PersistentDataType.BYTE, (byte) 1));
                inv.setItem(0, clone);
            }
        }
    }

    public static void removeAllTaggedFakeItems(InventoryView view) {
        Inventory inv = view.getTopInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(CraftSlotCommands.getInstance().getFakeItemKey(), PersistentDataType.BYTE)) {
                inv.setItem(i, null);
            }
        }
    }

    private void clearFakeItemsEverywhere(Player player) {
        InventoryView view = player.getOpenInventory();
        removeAllTaggedFakeItems(view);

        PlayerInventory inv = player.getInventory();
        for (int i = 1; i <= 4; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(fakeItemKey, PersistentDataType.BYTE)) {
                inv.setItem(i, null);
            }
        }
    }

    public static boolean isSelf2x2Crafting(InventoryView view) {
        Inventory inv = view.getTopInventory();
        return inv instanceof CraftingInventory
                && inv.getSize() == 5
                && inv.getType() == InventoryType.CRAFTING
                && view.getPlayer() instanceof Player p
                && Objects.equals(inv.getHolder(), p);
    }

    private boolean shouldShowMenuItems(InventoryView view) {
        return isSelf2x2Crafting(view);
    }
}
