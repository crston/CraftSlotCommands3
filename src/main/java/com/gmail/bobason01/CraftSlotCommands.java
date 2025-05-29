package com.gmail.bobason01;

import com.gmail.bobason01.listener.CraftSlotItemsListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static com.gmail.bobason01.listener.CraftSlotItemsListener.*;

public class CraftSlotCommands extends JavaPlugin implements Listener {

    private static CraftSlotCommands instance;
    private CraftSlotItemsListener craftSlotItemsListener;
    private ConfigurationSection craftingSlotSection;
    private final NamespacedKey fakeItemKey = new NamespacedKey(this, "fake-item");

    public static CraftSlotCommands getInstance() {
        return instance;
    }

    public NamespacedKey getFakeItemKey() {
        return fakeItemKey;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        registerCommand();
        registerEvents();
        reloadPlugin();
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();
            Inventory top = view.getTopInventory();

            if (top instanceof CraftingInventory craftingInventory && isSelf2x2Crafting(view)) {
                for (int i = 0; i <= 4; i++) {
                    ItemStack item = craftingInventory.getItem(i);
                    if (item == null || item.getType().isAir()) continue;

                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) continue;

                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    if (pdc.has(getFakeItemKey(), PersistentDataType.BYTE)) {
                        craftingInventory.setItem(i, null);
                    }
                }
            }

            player.closeInventory();
        }
    }

    private void registerCommand() {
        PluginCommand cmd = getCommand("craftslotcommands");
        if (cmd != null) {
            CSCCommand command = new CSCCommand();
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        } else {
            getLogger().warning("Command 'craftslotcommands' not defined in plugin.yml!");
        }
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, this);
        craftSlotItemsListener = new CraftSlotItemsListener(getConfig());
        Bukkit.getPluginManager().registerEvents(craftSlotItemsListener, this);
    }

    public void reloadPlugin() {
        reloadConfig();
        craftingSlotSection = getConfig().getConfigurationSection("crafting-slot");
        if (craftSlotItemsListener != null) {
            craftSlotItemsListener.reload(getConfig());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof CraftingInventory)) return;
        if (!isSelf2x2Crafting(player.getOpenInventory())) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot > 4) return;
        if (CraftSlotItemsListener.useSlot(rawSlot)) return;

        if (craftingSlotSection == null) return;
        String command = craftingSlotSection.getString(String.valueOf(rawSlot));
        if (command == null || command.isBlank()) return;

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(this, () -> {
            if (command.startsWith("*")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring(1));
            } else {
                Bukkit.dispatchCommand(player, command);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InventoryView view = player.getOpenInventory();
        Inventory top = view.getTopInventory();

        if (top instanceof CraftingInventory craftingInventory && isSelf2x2Crafting(view)) {
            for (int i = 0; i <= 4; i++) {
                ItemStack item = craftingInventory.getItem(i);
                if (item == null || item.getType().isAir()) continue;

                ItemMeta meta = item.getItemMeta();
                if (meta == null) continue;

                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc.has(getFakeItemKey(), PersistentDataType.BYTE)) {
                    craftingInventory.setItem(i, null);
                }
            }
        }
    }

    public static class CSCCommand implements CommandExecutor, TabCompleter {

        @Override
        public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
            if (!sender.hasPermission("csc.admin")) {
                send(sender, Component.text("You do not have permission.", NamedTextColor.RED));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                CraftSlotCommands.getInstance().reloadPlugin();
                sendPrefixed(sender, Component.text("Reloaded successfully.", NamedTextColor.GREEN));
            } else {
                sendPrefixed(sender, Component.text("CraftSlotCommands3", NamedTextColor.AQUA));
            }

            return true;
        }

        @Override
        public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, String[] args) {
            if (args.length == 1) return List.of("reload");
            return Collections.emptyList();
        }

        private void sendPrefixed(CommandSender sender, Component msg) {
            send(sender, Component.text("[CSC3] ", NamedTextColor.GRAY).append(msg));
        }

        private void send(CommandSender sender, Component msg) {
            if (sender instanceof Player player) {
                player.sendMessage(msg);
            } else {
                Bukkit.getConsoleSender().sendMessage(msg);
            }
        }
    }
}
