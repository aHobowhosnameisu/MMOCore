package net.Indyuce.mmocore.command.rpg;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.command.api.CommandEnd;
import net.Indyuce.mmocore.command.api.CommandMap;

public class ReloadCommandMap extends CommandEnd {
	public ReloadCommandMap(CommandMap parent) {
		super(parent, "reload");
	}

	@Override
	public CommandResult execute(CommandSender sender, String[] args) {

		sender.sendMessage(ChatColor.YELLOW + "Reloading " + MMOCore.plugin.getName() + " " + MMOCore.plugin.getDescription().getVersion() + "...");
		long ms = System.currentTimeMillis();

		MMOCore.plugin.reloadConfig();
		MMOCore.plugin.reloadPlugin();
		
		PlayerData.getAll().forEach(data -> data.update());

		ms = System.currentTimeMillis() - ms;
		sender.sendMessage(ChatColor.YELLOW + MMOCore.plugin.getName() + " " + MMOCore.plugin.getDescription().getVersion() + " successfully reloaded.");
		sender.sendMessage(ChatColor.YELLOW + "Time Taken: " + ChatColor.GOLD + ms + ChatColor.YELLOW + "ms (" + ChatColor.GOLD + (double) ms / 50 + ChatColor.YELLOW + " ticks)");
		return CommandResult.SUCCESS;
	}

}
