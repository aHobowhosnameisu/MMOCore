package net.Indyuce.mmocore.manager.data.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.file.FileConfiguration;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.ConfigFile;
import net.Indyuce.mmocore.api.player.OfflinePlayerData;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.Indyuce.mmocore.api.player.profess.SavedClassInformation;
import net.Indyuce.mmocore.api.player.stats.StatType;
import net.Indyuce.mmocore.manager.data.PlayerDataManager;

public class YAMLPlayerDataManager extends PlayerDataManager {

	@Override
	public void loadData(PlayerData data) {
		FileConfiguration config = new ConfigFile(data.getPlayer()).getConfig();

		data.setClassPoints(config.getInt("class-points"));
		data.setSkillPoints(config.getInt("skill-points"));
		data.setAttributePoints(config.getInt("attribute-points"));
		data.setAttributeReallocationPoints(config.getInt("attribute-realloc-points"));
		data.setLevel(config.getInt("level"));
		data.setExperience(config.getInt("experience"));
		if (config.contains("class"))
			data.setClass(MMOCore.plugin.classManager.get(config.getString("class")));
		data.setMana(data.getStats().getStat(StatType.MAX_MANA));
		data.setStamina(data.getStats().getStat(StatType.MAX_STAMINA));
		data.setStellium(data.getStats().getStat(StatType.MAX_STELLIUM));
		if (config.contains("guild"))
			data.setGuild(MMOCore.plugin.dataProvider.getGuildManager().stillInGuild(data.getUniqueId(), config.getString("guild")));
		if (config.contains("attribute"))
			data.getAttributes().load(config.getConfigurationSection("attribute"));
		if (config.contains("profession"))
			data.getCollectionSkills().load(config.getConfigurationSection("profession"));
		if (config.contains("quest"))
			data.getQuestData().load(config.getConfigurationSection("quest"));
		data.getQuestData().updateBossBar();
		if (config.contains("waypoints"))
			data.getWaypoints().addAll(config.getStringList("waypoints"));
		MMOCore.plugin.waypointManager.getDefault().forEach(waypoint -> data.getWaypoints().add(waypoint.getId()));
		if (config.contains("friends"))
			config.getStringList("friends").forEach(str -> data.getFriends().add(UUID.fromString(str)));
		if (config.contains("skill"))
			config.getConfigurationSection("skill").getKeys(false).forEach(id -> data.setSkillLevel(id, config.getInt("skill." + id)));
		if (config.contains("bound-skills"))
			for (String id : config.getStringList("bound-skills"))
				if (data.getProfess().hasSkill(id))
					data.getBoundSkills().add(data.getProfess().getSkill(id));

		/*
		 * load class slots, use try so the player can log in.
		 */
		if (config.contains("class-info"))
			for (String key : config.getConfigurationSection("class-info").getKeys(false))
				try {
					PlayerClass profess = MMOCore.plugin.classManager.get(key);
					Validate.notNull(profess, "Could not find class '" + key + "'");
					data.applyClassInfo(profess, new SavedClassInformation(config.getConfigurationSection("class-info." + key)));
				} catch (IllegalArgumentException exception) {
					MMOCore.log(Level.WARNING, "Could not load class info '" + key + "': " + exception.getMessage());
				}
	}

	@Override
	public void saveData(PlayerData data) {
		ConfigFile file = new ConfigFile(data.getUniqueId());
		FileConfiguration config = file.getConfig();

		config.set("class-points", data.getClassPoints());
		config.set("skill-points", data.getSkillPoints());
		config.set("attribute-points", data.getAttributePoints());
		// config.set("skill-realloc-points", skillReallocationPoints);
		config.set("attribute-realloc-points", data.getAttributeReallocationPoints());
		config.set("level", data.getLevel());
		config.set("experience", data.getExperience());
		config.set("class", data.getProfess().getId());
		config.set("waypoints", new ArrayList<>(data.getWaypoints()));
		config.set("friends", data.getFriends().stream().map(uuid -> uuid.toString()).collect(Collectors.toList()));
		config.set("last-login", data.getLastLogin());
		config.set("guild", data.hasGuild() ? data.getGuild().getId() : null);

		config.set("skill", null);
		data.mapSkillLevels().entrySet().forEach(entry -> config.set("skill." + entry.getKey(), entry.getValue()));

		List<String> boundSkills = new ArrayList<>();
		data.getBoundSkills().forEach(skill -> boundSkills.add(skill.getSkill().getId()));
		config.set("bound-skills", boundSkills);

		config.set("attribute", null);
		config.createSection("attribute");
		data.getAttributes().save(config.getConfigurationSection("attribute"));

		config.set("profession", null);
		config.createSection("profession");
		data.getCollectionSkills().save(config.getConfigurationSection("profession"));

		config.set("quest", null);
		config.createSection("quest");
		data.getQuestData().save(config.getConfigurationSection("quest"));

		config.set("class-info", null);
		for (String key : data.getSavedClasses()) {
			SavedClassInformation info = data.getClassInfo(key);
			config.set("class-info." + key + ".level", info.getLevel());
			config.set("class-info." + key + ".experience", info.getExperience());
			config.set("class-info." + key + ".skill-points", info.getSkillPoints());
			config.set("class-info." + key + ".attribute-points", info.getAttributePoints());
			config.set("class-info." + key + ".attribute-realloc-points", info.getAttributeReallocationPoints());
			info.getSkillKeys().forEach(skill -> config.set("class-info." + key + ".skill." + skill, info.getSkillLevel(skill)));
			info.getAttributeKeys()
					.forEach(attribute -> config.set("class-info." + key + ".attribute." + attribute, info.getAttributeLevel(attribute)));
		}

		file.save();
	}

	@Override
	public OfflinePlayerData getOffline(UUID uuid) {
		return isLoaded(uuid) ? get(uuid) : new YAMLOfflinePlayerData(uuid);
	}

	@Override
	public void remove(PlayerData data) {}
}
