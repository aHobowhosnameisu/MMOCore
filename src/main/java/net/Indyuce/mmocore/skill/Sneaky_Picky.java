package net.Indyuce.mmocore.skill;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.skill.Skill;
import net.Indyuce.mmocore.api.skill.SkillResult;
import net.Indyuce.mmocore.api.util.math.formula.LinearValue;
import net.mmogroup.mmolib.api.DamageType;
import net.mmogroup.mmolib.api.event.PlayerAttackEvent;

public class Sneaky_Picky extends Skill implements Listener {
	public Sneaky_Picky() {
		super();
		setMaterial(Material.DIAMOND_SWORD);
		setLore("Your attack is empowered by &f{extra}% &7when", "delivering the first blow during a fight.", "", "&9Costs {mana} {mana_name}");
		setPassive();

		addModifier("cooldown", new LinearValue(0, 0));
		addModifier("mana", new LinearValue(8, 1));
		addModifier("extra", new LinearValue(50, 20));

		Bukkit.getPluginManager().registerEvents(this, MMOCore.plugin);
	}

	@EventHandler
	public void a(PlayerAttackEvent event) {
		PlayerData data = PlayerData.get(event.getData().getUniqueId());
		if (!event.getAttack().hasType(DamageType.WEAPON) || data.isInCombat() || !data.getProfess().hasSkill(this))
			return;

		SkillResult cast = data.cast(this);
		if (!cast.isSuccessful())
			return;

		data.cast(cast.getInfo());

		event.getAttack().multiplyDamage(1 + cast.getModifier("extra") / 100);
		LivingEntity target = event.getEntity();
		target.getWorld().spawnParticle(Particle.SMOKE_NORMAL, target.getLocation().add(0, target.getHeight() / 2, 0), 64, 0, 0, 0, .05);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1, 2);
	}
}
