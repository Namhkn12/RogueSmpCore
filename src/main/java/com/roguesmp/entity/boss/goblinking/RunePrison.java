package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class RunePrison implements Hazard {

    private static final double RADIUS = 3;
    private static final double BLAST_RADIUS = 8;
    private static final double BLAST_MULTIPLIER = 1.6;
    private static final int HITS_TO_BREAK = 4;

    private final KingContext ctx;
    private final Location center;
    private final Interaction rune;
    private final BlockDisplay display;
    private final String id;
    private int fuse;
    private int hitsLeft = HITS_TO_BREAK;
    private long lastAttack;
    private boolean finished;

    public RunePrison(KingContext ctx, Location center, int fuse, String id) {
        this.ctx = ctx;
        this.center = center.clone();
        this.fuse = fuse;
        this.id = id;
        Location runeLocation = center.clone().add(0, 0.2, 0);
        this.rune = center.getWorld().spawn(runeLocation, Interaction.class, interaction -> {
            interaction.setInteractionWidth(0.9f);
            interaction.setInteractionHeight(0.9f);
            interaction.setResponsive(true);
            interaction.setPersistent(false);
        });
        this.display = center.getWorld().spawn(runeLocation, BlockDisplay.class, blockDisplay -> {
            blockDisplay.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
            blockDisplay.setPersistent(false);
            blockDisplay.setGlowing(true);
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(-0.35f, 0, -0.35f), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
        });
        center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f, 0.7f);
    }

    public Location center() {
        return center.clone();
    }

    public boolean isActive() {
        return !finished;
    }

    @Override
    public boolean tick(int interval) {
        if (finished) return true;
        fuse -= interval;
        if (fuse <= 0) {
            detonate();
            return true;
        }

        Fx.ring(center, RADIUS, Fx.ARCANE);
        Fx.ring(center, RADIUS * 0.5, Particle.WITCH);
        for (Player player : ctx.players()) {
            if (SpellTargets.horizontalDistance(player.getLocation(), center) > RADIUS) continue;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 3, false, false));
        }

        Interaction.PreviousInteraction attack = rune.getLastAttack();
        if (attack != null && attack.getTimestamp() != lastAttack) {
            lastAttack = attack.getTimestamp();
            hitsLeft--;
            center.getWorld().playSound(center, Sound.BLOCK_GLASS_HIT, 1.2f, 1.4f);
            if (hitsLeft <= 0) shatter();
        }
        return finished;
    }

    private void shatter() {
        finished = true;
        center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 0.5, 0), 40, 0.5, 0.5, 0.5, 0.05);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
        remove();
    }

    public void detonate() {
        if (finished) return;
        finished = true;
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.WITCH, center, 60, BLAST_RADIUS * 0.4, 0.5, BLAST_RADIUS * 0.4, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        for (Player player : ctx.players()) {
            if (player.getLocation().distanceSquared(center) > BLAST_RADIUS * BLAST_RADIUS) continue;
            ctx.damage(player, BLAST_MULTIPLIER, DamageType.MAGIC, id);
            ctx.push(player, center, 0.8, 0.5);
        }
        remove();
    }

    @Override
    public void remove() {
        finished = true;
        rune.getScheduler().run(RogueSmpCore.getInstance(), task -> rune.remove(), null);
        display.getScheduler().run(RogueSmpCore.getInstance(), task -> display.remove(), null);
    }
}
