package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CraftSlotCommands extends JavaPlugin implements Listener {

	public static CraftSlotCommands plugin;
	private CraftSlotItemsListener csil;

	@Override
	public void onEnable() {
		plugin = this;
		saveDefaultConfig();

		CSCCommand command = new CSCCommand();
		Objects.requireNonNull(getCommand("craftslotcommands")).setExecutor(command);
		Objects.requireNonNull(getCommand("craftslotcommands")).setTabCompleter(command);

		getServer().getPluginManager().registerEvents(this, this);

		if (getConfig().getBoolean("items-enabled")) {
			csil = new CraftSlotItemsListener(getConfig());
			getServer().getPluginManager().registerEvents(csil, this);
		}
	}

	@Override
	public void onDisable() {
		Bukkit.getOnlinePlayers().stream()
				.filter(p -> is2x2Crafting(p.getOpenInventory().getTopInventory()))
				.forEach(p -> CraftSlotItemsListener.removeCommandItems(p.getOpenInventory()));
	}

	private void reload() {
		reloadConfig();
		if (csil != null) csil.reload(getConfig());
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (!isValidSlotClick(e)) return;

		e.setCancelled(true);
		Player player = (Player) e.getWhoClicked();
		int slot = e.getSlot();

		String cmd = getConfig().getString("crafting-slot." + slot);
		if (cmd == null || cmd.isEmpty()) return;

		Bukkit.getScheduler().runTask(this, () -> {
			if (cmd.startsWith("*")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring(1));
			} else {
				Bukkit.dispatchCommand(player, cmd);
			}
		});
	}

	private boolean isValidSlotClick(InventoryClickEvent e) {
		return e.getInventory() instanceof CraftingInventory && e.getInventory().getSize() == 5
				&& e.getSlot() >= 0 && e.getSlot() <= 4
				&& switch (e.getSlotType()) {
			case CONTAINER, ARMOR, FUEL, OUTSIDE, QUICKBAR -> false;
			default -> true;
		};
	}

	private boolean is2x2Crafting(Inventory inv) {
		return inv instanceof CraftingInventory && inv.getSize() == 5;
	}

	public static class CSCCommand implements CommandExecutor, TabCompleter {

		@Override
		public boolean onCommand(CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
			if (!sender.hasPermission("csc.admin")) {
				send(sender, Component.text("You do not have permission to use this command.", NamedTextColor.RED));
				return true;
			}

			if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
				plugin.reload();
				sendPrefixed(sender, Component.text("Successfully reloaded.", NamedTextColor.GREEN));
			} else {
				sendPrefixed(sender, Component.text("Version 2.0", NamedTextColor.GREEN));
			}

			return true;
		}

		@Override
		public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, String[] args) {
			return args.length == 1 ? Collections.singletonList("reload") : Collections.emptyList();
		}

		private void sendPrefixed(CommandSender sender, Component message) {
			Component prefix = Component.text("[CraftSlotCommands] ", NamedTextColor.AQUA);
			send(sender, prefix.append(message));
		}

		private void send(CommandSender sender, Component message) {
			if (sender instanceof Player player) {
				player.sendMessage(message);
			} else {
				Bukkit.getConsoleSender().sendMessage(message);
			}
		}
	}
}
