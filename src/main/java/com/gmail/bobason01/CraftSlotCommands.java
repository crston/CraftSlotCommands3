package com.gmail.bobason01;

import com.gmail.bobason01.listener.CraftSlotItemsListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static com.gmail.bobason01.listener.CraftSlotItemsListener.isSelf2x2Crafting;

public class CraftSlotCommands extends JavaPlugin implements Listener {

    private static CraftSlotCommands instance;
    private CraftSlotItemsListener craftSlotItemsListener;
    private boolean skipCreativeClick = true;

    public static CraftSlotCommands getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        registerCommand();
        registerEvents();

        if (getConfig().getBoolean("items-enabled", true)) {
            craftSlotItemsListener = new CraftSlotItemsListener(getConfig());
            Bukkit.getPluginManager().registerEvents(craftSlotItemsListener, this);
        }

        skipCreativeClick = getConfig().getBoolean("optimize.skip-creative", true);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();
            if (isSelf2x2Crafting(view)) {
                Inventory inv = view.getTopInventory();
                for (int i = 0; i <= 4; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.equals(view.getItem(i))) {
                        inv.setItem(i, null); // ensure no menu item remains
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
    }

    public void reloadPlugin() {
        reloadConfig();
        if (craftSlotItemsListener != null) {
            craftSlotItemsListener.reload(getConfig());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory)) return;

        InventoryView view = event.getView();
        if (!isSelf2x2Crafting(view)) return;

        Player player = (Player) event.getWhoClicked();
        if (skipCreativeClick && player.getGameMode() == GameMode.CREATIVE) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot > 4) return;

        if (CraftSlotItemsListener.useSlot(rawSlot)) return;

        String command = getConfig().getString("crafting-slot." + rawSlot);
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