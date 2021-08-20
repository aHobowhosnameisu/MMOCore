package net.Indyuce.mmocore.api.player.profess;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.MMOLineConfig;
import io.lumine.mythic.lib.api.util.PostLoadObject;
import io.lumine.mythic.lib.version.VersionMaterial;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.experience.ExpCurve;
import net.Indyuce.mmocore.api.experience.source.type.ExperienceSource;
import net.Indyuce.mmocore.api.player.profess.event.EventTrigger;
import net.Indyuce.mmocore.api.player.profess.resource.ManaDisplayOptions;
import net.Indyuce.mmocore.api.player.profess.resource.PlayerResource;
import net.Indyuce.mmocore.api.player.profess.resource.ResourceHandler;
import net.Indyuce.mmocore.api.player.stats.StatType;
import net.Indyuce.mmocore.skill.Skill;
import net.Indyuce.mmocore.skill.Skill.SkillInfo;
import net.Indyuce.mmocore.api.util.MMOCoreUtils;
import net.Indyuce.mmocore.api.util.math.formula.LinearValue;
import net.Indyuce.mmocore.api.util.math.particle.CastingParticle;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public class PlayerClass extends PostLoadObject {
	private final String name, id, actionBarFormat;
	private final List<String> description = new ArrayList<>(), attrDescription = new ArrayList<>();
	private final ItemStack icon;
	private final Map<ClassOption, Boolean> options = new HashMap<>();
	private final ManaDisplayOptions manaDisplay;
	private final int maxLevel, displayOrder;
	private final ExpCurve expCurve;

	private final Map<StatType, LinearValue> stats = new HashMap<>();
	private final Map<String, SkillInfo> skills = new LinkedHashMap<>();
	private final List<Subclass> subclasses = new ArrayList<>();

	private final Map<PlayerResource, ResourceHandler> resourceHandlers = new HashMap<>();
	private final Map<String, EventTrigger> eventTriggers = new HashMap<>();

	private final CastingParticle castParticle;

	public PlayerClass(String id, FileConfiguration config) {
		super(config);

		this.id = id.toUpperCase().replace("-", "_").replace(" ", "_");

		name = MythicLib.plugin.parseColors(config.getString("display.name", "INVALID DISPLAY NAME"));
		icon = MMOCoreUtils.readIcon(config.getString("display.item", "BARRIER"));

		if (config.contains("display.texture") && icon.getType() == VersionMaterial.PLAYER_HEAD.toMaterial())
			try {
				ItemMeta meta = icon.getItemMeta();
				Field profileField = meta.getClass().getDeclaredField("profile");
				profileField.setAccessible(true);
				GameProfile gp = new GameProfile(UUID.randomUUID(), null);
				gp.getProperties().put("textures", new Property("textures", config.getString("display.texture")));
				profileField.set(meta, gp);
				icon.setItemMeta(meta);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
					| SecurityException exception) {
				throw new IllegalArgumentException("Could not apply playerhead texture: " + exception.getMessage());
			}

		for (String string : config.getStringList("display.lore"))
			description.add(ChatColor.GRAY + MythicLib.plugin.parseColors(string));
		for (String string : config.getStringList("display.attribute-lore"))
			attrDescription.add(ChatColor.GRAY + MythicLib.plugin.parseColors(string));
		manaDisplay = config.contains("mana") ? new ManaDisplayOptions(config.getConfigurationSection("mana"))
				: ManaDisplayOptions.DEFAULT;
		maxLevel = config.getInt("max-level");
		displayOrder = config.getInt("display.order");
		actionBarFormat = config.contains("action-bar", true) ? config.getString("action-bar") : null;

		expCurve = config.contains("exp-curve")
				? MMOCore.plugin.experience.getOrThrow(
						config.get("exp-curve").toString().toLowerCase().replace("_", "-").replace(" ", "-"))
				: ExpCurve.DEFAULT;

		if (config.contains("attributes"))
			for (String key : config.getConfigurationSection("attributes").getKeys(false))
				try {
					stats.put(StatType.valueOf(key.toUpperCase().replace("-", "_")),
							new LinearValue(config.getConfigurationSection("attributes." + key)));
				} catch (IllegalArgumentException exception) {
					MMOCore.plugin.getLogger().log(Level.WARNING, "Could not load stat info '" + key + "' from class '"
							+ id + "': " + exception.getMessage());
				}

		if (config.contains("skills"))
			for (String key : config.getConfigurationSection("skills").getKeys(false))
				try {
					Validate.isTrue(MMOCore.plugin.skillManager.has(key), "Could not find skill " + key);
					skills.put(key.toUpperCase(), MMOCore.plugin.skillManager.get(key)
							.newSkillInfo(config.getConfigurationSection("skills." + key)));
				} catch (IllegalArgumentException exception) {
					MMOCore.plugin.getLogger().log(Level.WARNING, "Could not load skill info '" + key + "' from class '"
							+ id + "': " + exception.getMessage());
				}

		castParticle = config.contains("cast-particle")
				? new CastingParticle(config.getConfigurationSection("cast-particle"))
				: new CastingParticle(Particle.SPELL_INSTANT);

		if (config.contains("options"))
			for (String key : config.getConfigurationSection("options").getKeys(false))
				try {
					setOption(ClassOption.valueOf(key.toUpperCase().replace("-", "_").replace(" ", "_")),
							config.getBoolean("options." + key));
				} catch (IllegalArgumentException exception) {
					MMOCore.plugin.getLogger().log(Level.WARNING,
							"Could not load option '" + key + "' from class '" + key + "': " + exception.getMessage());
				}

		if (config.contains("main-exp-sources"))
			for (String key : config.getStringList("main-exp-sources"))
				try {
					ExperienceSource<?> source = MMOCore.plugin.loadManager.loadExperienceSource(new MMOLineConfig(key),
							null);
					source.setClass(this);
					MMOCore.plugin.professionManager.registerExpSource(source);
				} catch (IllegalArgumentException exception) {
					MMOCore.plugin.getLogger().log(Level.WARNING, "Could not load exp source '" + key + "' from class '"
							+ id + "': " + exception.getMessage());
				}

		if (config.contains("triggers"))
			for (String key : config.getConfigurationSection("triggers").getKeys(false)) {
				try {
					String format = key.toLowerCase().replace("_", "-").replace(" ", "-");
					eventTriggers.put(format, new EventTrigger(format, config.getStringList("triggers." + key)));
				} catch (IllegalArgumentException exception) {
					MMOCore.log(Level.WARNING, "[PlayerClasses:" + id + "] " + exception.getMessage());
				}
			}

		/*
		 * must make sure all the resourceHandlers are registered when the placer class
		 * is initialized.
		 */
		for (PlayerResource resource : PlayerResource.values()) {
			if (config.isConfigurationSection("resource." + resource.name().toLowerCase()))
				try {
					resourceHandlers.put(resource, new ResourceHandler(resource,
							config.getConfigurationSection("resource." + resource.name().toLowerCase())));
				} catch (IllegalArgumentException exception) {
					MMOCore.log(Level.WARNING, "[PlayerClasses:" + id + "] Could not load special resource regen for "
							+ resource.name() + ": " + exception.getMessage());
					resourceHandlers.put(resource, new ResourceHandler(resource));
				}
			else
				resourceHandlers.put(resource, new ResourceHandler(resource));
		}
	}

	/*
	 * used to generate display class
	 */
	public PlayerClass(String id, String name, Material material) {
		super(null);

		this.id = id;
		this.name = name;
		manaDisplay = ManaDisplayOptions.DEFAULT;
		maxLevel = 0;
		displayOrder = 0;
		expCurve = ExpCurve.DEFAULT;
		castParticle = new CastingParticle(Particle.SPELL_INSTANT);
		actionBarFormat = "";

		this.icon = new ItemStack(material);
		setOption(ClassOption.DISPLAY, false);
		setOption(ClassOption.DEFAULT, false);

		for (PlayerResource resource : PlayerResource.values())
			resourceHandlers.put(resource, new ResourceHandler(resource));
	}

	@Override
	protected void whenPostLoaded(ConfigurationSection config) {
		if (config.contains("subclasses"))
			for (String key : config.getConfigurationSection("subclasses").getKeys(false))
				try {
					subclasses.add(new Subclass(
							MMOCore.plugin.classManager
									.getOrThrow(key.toUpperCase().replace("-", "_").replace(" ", "_")),
							config.getInt("subclasses." + key)));
				} catch (IllegalArgumentException exception) {
					MMOCore.plugin.getLogger().log(Level.WARNING, "Could not load subclass '" + key + "' from class '"
							+ getId() + "': " + exception.getMessage());
				}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ManaDisplayOptions getManaDisplay() {
		return manaDisplay;
	}

	public ResourceHandler getHandler(PlayerResource resource) {
		return resourceHandlers.get(resource);
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public ExpCurve getExpCurve() {
		return expCurve;
	}

	public ItemStack getIcon() {
		return icon.clone();
	}

	public CastingParticle getCastParticle() {
		return castParticle;
	}

	public List<String> getDescription() {
		return description;
	}

	public List<String> getAttributeDescription() {
		return attrDescription;
	}

	public void setOption(ClassOption option, boolean value) {
		options.put(option, value);
	}

	public boolean hasOption(ClassOption option) {
		return options.containsKey(option) ? options.get(option) : option.getDefault();
	}

	public void setStat(StatType type, double base, double perLevel) {
		stats.put(type, new LinearValue(base, perLevel));
	}

	public double calculateStat(StatType stat, int level) {
		return getStatInfo(stat).calculate(level);
	}

	public List<Subclass> getSubclasses() {
		return subclasses;
	}

	public boolean hasSkill(Skill skill) {
		return hasSkill(skill.getId());
	}

	public boolean hasSkill(String id) {
		return skills.containsKey(id);
	}

	public SkillInfo getSkill(Skill skill) {
		return getSkill(skill.getId());
	}

	/*
	 * reduces map checkups when skills are being checked on events that are
	 * commonly called like EntityDamageEvent or regen events.
	 */
	public Optional<SkillInfo> findSkill(Skill skill) {
		return skills.containsKey(skill.getId()) ? Optional.of(skills.get(skill.getId())) : Optional.empty();
	}

	public SkillInfo getSkill(String id) {
		return skills.get(id);
	}

	public Set<String> getEventTriggers() {
		return eventTriggers.keySet();
	}

	public boolean hasEventTriggers(String name) {
		return eventTriggers.containsKey(name);
	}

	public EventTrigger getEventTriggers(String name) {
		return eventTriggers.get(name);
	}

	public Collection<SkillInfo> getSkills() {
		return skills.values();
	}

	private LinearValue getStatInfo(StatType type) {
		return stats.containsKey(type) ? stats.get(type) : type.getDefault();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PlayerClass && ((PlayerClass) obj).id.equals(id);
	}

	public String getActionBar() {
		return actionBarFormat;
	}

	public boolean hasActionBar() {
		return actionBarFormat != null;
	}
}
