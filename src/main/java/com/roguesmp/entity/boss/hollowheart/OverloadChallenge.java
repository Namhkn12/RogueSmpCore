package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OverloadChallenge implements CrystalChallenge {

    public static final String ID = "crystal_overload";

    private static final double MIN_DISTANCE = 6.0;
    private static final double MAX_DISTANCE = 10.0;
    private static final int BEAM_INTERVAL = 10;
    private static final double BLAST_DAMAGE_MULTIPLIER = 1.5;
    private static final double HEAL_PERCENT_PER_CONDUIT = 0.04;

    private final CrystalContext context;
    private final List<UUID> conduits = new ArrayList<>();
    private int spawned;
    private long startedAt;
    private int chargeTicks;

    public OverloadChallenge(CrystalContext context) {
        this.context = context;
    }

    @Override
    public void start(long now) {
        startedAt = now;
        chargeTicks = context.timer(context.phase().overloadChargeTicks());
        for (Location point : context.arena().ringPoints(context.phase().conduitCount(), MIN_DISTANCE, MAX_DISTANCE)) {
            LivingEntity conduit = Absorption.spawn(CrystalMinion.CONDUIT_ID, point, null);
            if (conduit == null) return;
            conduits.add(conduit.getUniqueId());
            point.getWorld().spawnParticle(Particle.END_ROD, point.clone().add(0, 0.5, 0), 25, 0.3, 0.6, 0.3, 0.05);
        }
        spawned = conduits.size();
        context.crystal().getWorld().playSound(context.crystal().getLocation(), Sound.BLOCK_BEACON_AMBIENT, 2f, 0.5f);
    }

    @Override
    public Outcome tick(long now, int interval) {
        List<LivingEntity> alive = Absorption.alive(context.crystal(), conduits);
        if (alive.isEmpty()) return Outcome.SUCCESS;

        long elapsed = now - startedAt;
        if (elapsed >= chargeTicks) return Outcome.FAILURE;

        ShockwaveRing.chargeEffect(context.crystal(), elapsed, chargeTicks);
        if (now % BEAM_INTERVAL == 0) {
            Location crystalCenter = SpellTargets.centerOf(context.crystal());
            alive.forEach(conduit -> Absorption.trail(SpellTargets.centerOf(conduit), crystalCenter, Particle.ELECTRIC_SPARK));
        }
        return Outcome.RUNNING;
    }

    @Override
    public BarState status(long now) {
        int remaining = Absorption.alive(context.crystal(), conduits).size();
        return BarState.fill(Component.text(remaining + "/" + spawned, NamedTextColor.GRAY),
                now - startedAt, chargeTicks, BossBar.Color.RED);
    }

    @Override
    public void punish() {
        LivingEntity crystal = context.crystal();
        Location center = SpellTargets.centerOf(crystal);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 2, 1, 2, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.6f);

        double damage = EntityUtils.getAttributeOrDefault(crystal, Attribute.ATTACK_DAMAGE, 0) * BLAST_DAMAGE_MULTIPLIER;
        for (Player player : context.arena().players()) {
            DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.BLAST, ID);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(player, crystal, damage, metadata);
        }

        Absorption.absorb(crystal, Absorption.alive(crystal, conduits), HEAL_PERCENT_PER_CONDUIT, context.healMultiplier());
    }

    @Override
    public void cleanup() {
        Absorption.alive(context.crystal(), conduits).forEach(Absorption::remove);
        conduits.clear();
    }
}
