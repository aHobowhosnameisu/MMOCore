package net.Indyuce.mmocore.manager;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.profess.ClassOption;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.Indyuce.mmocore.api.player.profess.event.EventTriggerHandler;
import net.Indyuce.mmocore.api.player.profess.event.trigger.AttackEventTrigger;
import net.Indyuce.mmocore.api.player.profess.event.trigger.BlockBrokenTrigger;
import net.Indyuce.mmocore.api.player.profess.event.trigger.BlockPlacedTrigger;
import net.Indyuce.mmocore.api.player.profess.event.trigger.ClassChosenEventTrigger;
import net.Indyuce.mmocore.api.player.profess.event.trigger.LevelUpEventTrigger;

public class ClassManager extends MMOManager {
	private final Map<String, PlayerClass> map = new HashMap<>();

	/*
	 * cached default class. if there are two default classes, the last one
	 * overrides the previous.
	 */
	private PlayerClass defaultClass;

	/*
	 * same different types of trigger events to be able to map them later in
	 * the player class instances.
	 */
	private final Set<EventTriggerHandler> triggerHandlers = new HashSet<>();

	public ClassManager() {
		registerEvent(new LevelUpEventTrigger());
		registerEvent(new AttackEventTrigger());
		registerEvent(new ClassChosenEventTrigger());
		registerEvent(new BlockBrokenTrigger());
		registerEvent(new BlockPlacedTrigger());
	}

	public void registerEvent(EventTriggerHandler handler) {
		triggerHandlers.add(handler);
	}

	public void register(PlayerClass playerClass) {
		map.put(playerClass.getId(), playerClass);
	}

	public boolean has(String id) {
		return map.containsKey(id);
	}

	public PlayerClass get(String id) {
		return map.get(id);
	}

	public PlayerClass getOrThrow(String id) {
		Validate.isTrue(map.containsKey(id), "Could not find class with ID '" + id + "'");
		return map.get(id);
	}

	public Collection<PlayerClass> getAll() {
		return map.values();
	}

	public PlayerClass getDefaultClass() {
		return defaultClass;
	}

	public void reloadPlayerClasses() {
		PlayerData.getAll().forEach(data -> data.setClass(get(data.getProfess().getId())));
	}

	@Override
	public void reload() {
		for (File file : new File(MMOCore.plugin.getDataFolder() + "/classes").listFiles())
			try {
				String id = file.getName().substring(0, file.getName().length() - 4);
				register(new PlayerClass(id, YamlConfiguration.loadConfiguration(file)));
			} catch (IllegalArgumentException exception) {
				MMOCore.plugin.getLogger().log(Level.WARNING, "Could not load class '" + file.getName() + "': " + exception.getMessage());
			}

		for (PlayerClass profess : map.values())
			try {
				profess.postLoad();
			} catch (IllegalArgumentException exception) {
				MMOCore.plugin.getLogger().log(Level.WARNING, "Could not post-load class '" + profess.getId() + "': " + exception.getMessage());
			}

		defaultClass = map.values().stream().filter(profess -> profess.hasOption(ClassOption.DEFAULT)).findFirst()
				.orElse(new PlayerClass("HUMAN", "Human", Material.LEATHER_BOOTS));

		/*
		 * register event triggers
		 */
		triggerHandlers.forEach(handler -> Bukkit.getPluginManager().registerEvents(handler, MMOCore.plugin));
	}

	@Override
	public void clear() {
		map.clear();
		triggerHandlers.forEach(handler -> HandlerList.unregisterAll(handler));

		/*
		 * do not clear the list of trigger listeners, since it's only setup
		 * once the server loads and it is never modified.
		 */
	}
}
