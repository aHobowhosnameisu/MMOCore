package net.Indyuce.mmocore.command.rpg.booster;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.experience.Booster;
import net.Indyuce.mmocore.api.util.math.format.DelayFormat;
import net.mmogroup.mmolib.MMOLib;
import net.mmogroup.mmolib.command.api.CommandTreeNode;

public class ListCommandTreeNode extends CommandTreeNode {
	public ListCommandTreeNode(CommandTreeNode parent) {
		super(parent, "list");
	}

	@Override
	public CommandResult execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return CommandResult.FAILURE;

		sender.sendMessage(ChatColor.YELLOW + "----------------------------------------------------");
		for (Booster booster : MMOCore.plugin.boosterManager.getBoosters())
			if (!booster.isTimedOut())
				MMOLib.plugin.getVersion().getWrapper().sendJson((Player) sender, "{\"text\":\"" + ChatColor.YELLOW + "- " + ChatColor.GOLD
						+ MMOCore.plugin.configManager.decimal.format((1 + booster.getExtra())) + "x" + ChatColor.YELLOW + " Booster - "
						+ ChatColor.GOLD + (!booster.hasProfession() ? "ExploreAttributesCommand" : booster.getProfession().getName())
						+ ChatColor.YELLOW + " - " + ChatColor.GOLD
						+ new DelayFormat().format(booster.getCreationDate() + booster.getLength() - System.currentTimeMillis())
						+ "\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/mmocore booster remove " + booster.getUniqueId().toString()
						+ "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"Click to remove.\"}}}");
		sender.sendMessage(ChatColor.YELLOW + "----------------------------------------------------");

		return CommandResult.SUCCESS;
	}
}