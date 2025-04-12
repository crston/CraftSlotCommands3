package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class CraftSlotCommands extends JavaPlugin implements Listener {
	public static CraftSlotCommands plugin;
	public static FileConfiguration config;

	@Override
	public void onEnable() {
		plugin = this;
		tryMigrateOldData();
		saveDefaultConfig();
		config = getConfig();

		PluginCommand pluginCommand = Objects.requireNonNull(getCommand("craftslotcommands"));
		CSCCommand command = new CSCCommand();
		pluginCommand.setExecutor(command);
		pluginCommand.setTabCompleter(command);

		getServer().getPluginManager().registerEvents(this, this);
		if (config.getBoolean("items-enabled", true)) {
			getServer().getPluginManager().registerEvents(new CraftSlotItemsListener(config), this);
		}
	}

	@Override
	public void onDisable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			CraftSlotItemsListener.removeGhostItems(player);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player player)) return;
		if (e.getInventory().getType() != InventoryType.CRAFTING) return;

		int slot = e.getRawSlot();
		if (slot >= 0 && slot <= 4 && config.contains("crafting-slot." + slot)) {
			e.setCancelled(true);
			player.setItemOnCursor(null); // 고스트 클릭 복제 방지

			String cmd = config.getString("crafting-slot." + slot);
			if (cmd != null && !cmd.isEmpty()) {
				Bukkit.getScheduler().runTask(this, () -> {
					if (cmd.startsWith("*")) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring(1));
					} else {
						Bukkit.dispatchCommand(player, cmd);
					}
				});
			}

			// 다시 패킷으로 고스트 아이템 재전송
			Bukkit.getScheduler().runTaskLater(this, () -> CraftSlotItemsListener.sendGhostItems(player), 1L);
		} else {
			// 다른 슬롯 클릭해도 재표시
			Bukkit.getScheduler().runTaskLater(this, () -> CraftSlotItemsListener.sendGhostItems(player), 1L);
		}
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		if (e.getInventory().getType() == InventoryType.CRAFTING && e.getPlayer() instanceof Player player) {
			Bukkit.getScheduler().runTaskLater(this, () -> CraftSlotItemsListener.sendGhostItems(player), 1L);
		}
	}

	private void tryMigrateOldData() {
		File currentDir = getDataFolder();
		if (currentDir.exists()) return;

		File oldV2 = new File(getServer().getPluginsFolder(), "CraftSlotCommands2");
		File oldV1 = new File(getServer().getPluginsFolder(), "CraftSlotCommands");

		try {
			if (oldV2.exists()) {
				Files.move(oldV2.toPath(), currentDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
				getLogger().info("✅ Migrated config from CraftSlotCommands2 → CraftSlotCommands3");
			} else if (oldV1.exists()) {
				Files.move(oldV1.toPath(), currentDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
				getLogger().info("✅ Migrated config from CraftSlotCommands → CraftSlotCommands3");
			}
		} catch (IOException e) {
			getLogger().warning("⚠ Failed to migrate old data: " + e.getMessage());
		}
	}
}
