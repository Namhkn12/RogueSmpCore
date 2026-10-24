package com.roguesmp.entity.boss.hollowheart;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrystalShardsChallenge implements CrystalChallenge {

    private static final double MIN_DISTANCE = 5.0;
    private static final double MAX_DISTANCE = 9.0;
    private static final double HEAL_PERCENT_PER_SHARD = 0.04;

    private final CrystalContext context;
    private final List<UUID> shards = new ArrayList<>();
    private int spawned;
    private long endsAt;
    private int timerTicks;

    public CrystalShardsChallenge(CrystalContext context) {
        this.context = context;
    }

    @Override
    public void start(long now) {
        timerTicks = context.timer(context.phase().shardTimerTicks());
        endsAt = now + timerTicks;
        for (Location point : context.arena().ringPoints(context.phase().shardCount(), MIN_DISTANCE, MAX_DISTANCE)) {
            LivingEntity shard = Absorption.spawn(CrystalMinion.SHARD_ID, point, null);
            if (shard == null) return;
            shards.add(shard.getUniqueId());
            point.getWorld().spawnParticle(Particle.END_ROD, point.clone().add(0, 0.5, 0), 20, 0.3, 0.5, 0.3, 0.05);
            point.getWorld().playSound(point, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.5f, 1f);
        }
        spawned = shards.size();
    }

    @Override
    public Outcome tick(long now, int interval) {
        if (Absorption.alive(context.crystal(), shards).isEmpty()) return Outcome.SUCCESS;
        return now >= endsAt ? Outcome.FAILURE : Outcome.RUNNING;
    }

    @Override
    public BarState status(long now) {
        int remaining = Absorption.alive(context.crystal(), shards).size();
        return BarState.countdown(Component.text(remaining + "/" + spawned, NamedTextColor.GRAY),
                endsAt - now, timerTicks, BossBar.Color.PURPLE);
    }

    @Override
    public void punish() {
        Absorption.absorb(context.crystal(), Absorption.alive(context.crystal(), shards), HEAL_PERCENT_PER_SHARD, context.healMultiplier());
    }

    @Override
    public void cleanup() {
        Absorption.alive(context.crystal(), shards).forEach(Absorption::remove);
        shards.clear();
    }
}
