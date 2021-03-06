package net.Indyuce.mmocore.api.player.profess.event.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.event.PlayerLevelUpEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.Indyuce.mmocore.api.player.profess.event.EventTriggerHandler;

public class LevelUpEventTrigger implements EventTriggerHandler {

	@Override
	public boolean handles(String event) {
		return event.startsWith("level-up");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void a(PlayerLevelUpEvent event) {
		PlayerData player = event.getData();
		PlayerClass profess = player.getProfess();

		if(event.hasProfession()) {
			String prof = event.getProfession().getId().toLowerCase();
			MMOCore.debug(2, "[DEBUG] Looking for triggers: level-up-" + prof);
			processTrigger(player, profess, "level-up-" + prof);
			processTrigger(player, profess, "level-up-" + prof + "-" + event.getNewLevel());
		} else {
			MMOCore.debug(2, "[DEBUG] Normal level up trigger.");
			processTrigger(player, profess, "level-up");
			processTrigger(player, profess, "level-up-" + event.getNewLevel());
			if(profess.getMaxLevel() == event.getNewLevel())
				processTrigger(player, profess, "level-up-max");
		}
	}
	
	public void processTrigger(PlayerData player, PlayerClass profess, String trigger) {
		if(profess.hasEventTriggers(trigger)) profess.getEventTriggers(trigger).getTriggers().forEach(t -> t.apply(player));
	}
}
