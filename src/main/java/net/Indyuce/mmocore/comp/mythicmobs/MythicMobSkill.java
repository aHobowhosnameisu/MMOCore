package net.Indyuce.mmocore.comp.mythicmobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;

import io.lumine.xikage.mythicmobs.MythicMobs;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.skill.Skill;
import net.Indyuce.mmocore.api.skill.SkillResult;
import net.Indyuce.mmocore.api.skill.SkillResult.CancelReason;
import net.Indyuce.mmocore.api.util.MMOCoreUtils;
import net.Indyuce.mmocore.api.util.math.formula.IntegerLinearValue;
import net.Indyuce.mmocore.api.util.math.formula.LinearValue;

public class MythicMobSkill extends Skill {
	private final io.lumine.xikage.mythicmobs.skills.Skill skill;

	// private final BiFunction<PlayerDataManager, SkillInfo, SkillResult> cast;

	public MythicMobSkill(String id, FileConfiguration config) {
		super(id);

		String mmId = config.getString("mythicmobs-skill-id");
		Validate.notNull(mmId, "Could not find MM skill ID");

		Optional<io.lumine.xikage.mythicmobs.skills.Skill> opt = MythicMobs.inst().getSkillManager().getSkill(mmId);
		Validate.isTrue(opt.isPresent(), "Could not find MM skill " + mmId);
		skill = opt.get();

		String format = config.getString("material");
		Validate.notNull(format, "Could not load skill material");
		setIcon(MMOCoreUtils.readIcon(format));

		setName(config.getString("name"));
		setLore(config.getStringList("lore"));

		for (String key : config.getKeys(false)) {
			Object mod = config.get(key);
			if (mod instanceof ConfigurationSection)
				addModifier(key, readLinearValue((ConfigurationSection) mod));
		}

		// cast = config.getBoolean("target") ? (data, info) -> new
		// TargetSkillResult(data, info, def(config.getDouble("range"), 50)) :
		// (data, info) -> new SkillResult(data, info);
	}

	// private double def(double d, double def) {
	// return d == 0 ? def : d;
	// }

	public String getInternalName() {
		return skill.getInternalName();
	}

	@Override
	public SkillResult whenCast(PlayerData data, SkillInfo skill) {
		SkillResult cast = new SkillResult(data, skill);
		if (!cast.isSuccessful())
			return cast;

		List<Entity> targets = new ArrayList<>();
		// targets.add(cast instanceof TargetSkillResult ? ((TargetSkillResult)
		// cast).getTarget() : stats.getPlayer());
		targets.add(data.getPlayer());

		/*
		 * cache placeholders so they can be retrieved later by MythicMobs math
		 * formulas
		 */
		data.getSkillData().cacheModifiers(this, cast);

		if (!MythicMobs.inst().getAPIHelper().castSkill(data.getPlayer(), this.skill.getInternalName(), data.getPlayer(), data.getPlayer().getEyeLocation(), targets, null, 1))
			cast.abort(CancelReason.OTHER);

		return cast;
	}

	/*
	 * used to load double modifiers from the config with a specific type, since
	 * modifiers have initially a type for mmocore default skills
	 */
	private LinearValue readLinearValue(ConfigurationSection section) {
		return section.getBoolean("int") ? new IntegerLinearValue(section) : new LinearValue(section);
	}
}
