package net.Indyuce.mmocore.skill;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.skill.Skill;
import net.Indyuce.mmocore.api.util.math.formula.LinearValue;
import net.mmogroup.mmolib.api.event.PlayerAttackEvent;

public class Fire_Berserker extends Skill implements Listener {
	public Fire_Berserker() {
		super();
		setMaterial(Material.FLINT_AND_STEEL);
		setLore("You deal &c{extra}% &7more damage when on fire.");
		setPassive();

		addModifier("extra", new LinearValue(10, 5));
		// addModifier("duration", new LinearValue(10, .1));
		// addModifier("cooldown", new LinearValue(30, 0));

		Bukkit.getPluginManager().registerEvents(this, MMOCore.plugin);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void a(PlayerAttackEvent event) {
		PlayerData data = event.getData().getMMOCore();
		if (event.getPlayer().getFireTicks() > 0) {
			Optional<SkillInfo> skill = data.getProfess().findSkill(this);
			if (skill.isPresent())
				event.getAttack().multiplyDamage(1 + skill.get().getModifier("extra", data.getSkillLevel(this)) / 100);
		}
	}
}
