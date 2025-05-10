package com.gmail.bobason01;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.*;

public class CraftSlotCommands extends JavaPlugin implements Listener {

	public static CraftSlotCommands plugin;
	private CraftSlotItemsListener csil;

	@Override
	public void onEnable() {
		plugin = this;
		saveDefaultConfig();

		registerCommand();
		registerListeners();

		if (getConfig().getBoolean("items-enabled")) {
			csil = new CraftSlotItemsListener(getConfig());
			getServer().getPluginManager().registerEvents(csil, this);
		}
	}

	@Override
	public void onDisable() {
		Bukkit.getOnlinePlayers().stream()
				.map(Player::getOpenInventory) // InventoryView
				.filter(Objects::nonNull)
				.filter(view -> is2x2Crafting(view.getTopInventory())) // 검사만 topInventory 사용
				.forEach(CraftSlotItemsListener::removeCommandItems); // InventoryView 그대로 전달
	}

	private void reload() {
		reloadConfig();
		if (csil != null) {
			csil.reload(getConfig());
		}
	}

	private void registerCommand() {
		CSCCommand command = new CSCCommand();
		PluginCommand cmd = getCommand("craftslotcommands");
		if (cmd != null) {
			cmd.setExecutor(command);
			cmd.setTabCompleter(command);
		} else {
			getLogger().warning("Command 'craftslotcommands' not found in plugin.yml!");
		}
	}

	private void registerListeners() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (!isValidSlotClick(e)) return;

		e.setCancelled(true);

		Player player = (Player) e.getWhoClicked();
		int slot = e.getSlot();
		String cmd = getConfig().getString("crafting-slot." + slot);

		if (cmd == null || cmd.isBlank()) return;

		Bukkit.getScheduler().runTask(this, () -> {
			if (cmd.startsWith("*")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring(1));
			} else {
				Bukkit.dispatchCommand(player, cmd);
			}
		});
	}

	private boolean isValidSlotClick(InventoryClickEvent e) {
		Inventory inv = e.getInventory();
		if (!(inv instanceof CraftingInventory) || inv.getSize() != 5) return false;
		int slot = e.getSlot();
		if (slot < 0 || slot > 4) return false;

		return switch (e.getSlotType()) {
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
				sendPrefixed(sender, Component.text("CraftSlotCommands Version 2.0", NamedTextColor.AQUA));
			}

			return true;
		}

		@Override
		public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, String[] args) {
			if (args.length == 1) {
				return Collections.singletonList("reload");
			}
			return Collections.emptyList();
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
