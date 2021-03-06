package net.Indyuce.mmocore.comp.placeholder;

import net.asangarin.hexcolors.ColorParse;
import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPIParser implements PlaceholderParser {
	public PlaceholderAPIParser() {
		PlaceholderExpansion expansion = new RPGPlaceholders();
		expansion.getPlaceholderAPI().getLocalExpansionManager().register(expansion);
	}

	@Override
	public String parse(OfflinePlayer player, String string) {
		return new ColorParse('&', PlaceholderAPI.setPlaceholders(player, string.replace("%player%", player.getName()))).toChatColor();
	}
}
