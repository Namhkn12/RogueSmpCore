package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class CrystalEffects {

    private static final BlockData AMETHYST = Material.AMETHYST_BLOCK.createBlockData();
    private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(0xFFD166), 1.4f);
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(0x9B5DE5), 1.6f);
    private static final Particle.DustOptions DARK_PURPLE = new Particle.DustOptions(Color.fromRGB(0x4A1D6E), 1.4f);
    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(0xE63946), 1.6f);
    private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(0x80ED99), 1.4f);

    private final LivingEntity crystal;
    private final Arena arena;

    public CrystalEffects(LivingEntity crystal, Arena arena) {
        this.crystal = crystal;
        this.arena = arena;
    }

    private Location center() {
        return SpellTargets.centerOf(crystal);
    }

    private Location base() {
        return crystal.getLocation().add(0, 0.2, 0);
    }

    private World world() {
        return crystal.getWorld();
    }

    private void sound(Sound sound, float volume, float pitch) {
        world().playSound(center(), sound, volume, pitch);
    }

    private void ring(Location around, double radius, Particle particle, Object data) {
        int points = Math.max(16, (int) (radius * 12));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            Location point = around.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            if (data == null) world().spawnParticle(particle, point, 1, 0, 0, 0, 0);
            else world().spawnParticle(particle, point, 1, 0, 0, 0, 0, data);
        }
    }

    private void column(Particle particle, Object data, double height) {
        Location start = base();
        for (double y = 0; y <= height; y += 0.25) {
            Location point = start.clone().add(0, y, 0);
            if (data == null) world().spawnParticle(particle, point, 2, 0.1, 0, 0.1, 0);
            else world().spawnParticle(particle, point, 2, 0.1, 0, 0.1, 0, data);
        }
    }

    public void awaken() {
        sound(Sound.BLOCK_BEACON_POWER_SELECT, 2f, 0.6f);
        sound(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 0.8f);
        world().spawnParticle(Particle.END_ROD, center(), 50, 1.2, 1.2, 1.2, 0.08);
        ring(base(), 2.5, Particle.DUST, PURPLE);
    }

    public void telegraph() {
        sound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
        sound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 0.7f);
        ring(base(), 1.6, Particle.DUST, YELLOW);
        ring(base().add(0, 1, 0), 1.2, Particle.DUST, YELLOW);
    }

    public void challengeStart() {
        sound(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1.2f);
        world().spawnParticle(Particle.WITCH, center(), 40, 1.0, 1.0, 1.0, 0.1);
        ring(base(), 3.0, Particle.DUST, PURPLE);
    }

    public void coreOpen() {
        sound(Sound.BLOCK_BEACON_ACTIVATE, 2f, 1.4f);
        sound(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 1.6f);
        sound(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.8f);
        column(Particle.END_ROD, null, 4.0);
        world().spawnParticle(Particle.TOTEM_OF_UNDYING, center(), 60, 0.8, 1.2, 0.8, 0.3);
        ring(base(), 2.0, Particle.DUST, GREEN);
    }

    public void coreClose() {
        sound(Sound.BLOCK_AMETHYST_BLOCK_PLACE, 2f, 0.6f);
        sound(Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 1.2f);
        world().spawnParticle(Particle.REVERSE_PORTAL, center(), 50, 1.5, 1.5, 1.5, 0.05);
    }

    public void failure() {
        sound(Sound.BLOCK_BEACON_DEACTIVATE, 2f, 0.6f);
        sound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);
        world().spawnParticle(Particle.SQUID_INK, center(), 40, 1.2, 1.0, 1.2, 0.05);
        ring(base(), 2.5, Particle.DUST, RED);
        ring(base().add(0, 1.2, 0), 1.8, Particle.DUST, RED);
    }

    public void phaseShatter() {
        sound(Sound.BLOCK_AMETHYST_BLOCK_BREAK, 2f, 0.5f);
        sound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2f, 0.7f);
        sound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.4f);
        world().spawnParticle(Particle.BLOCK, center(), 120, 1.0, 1.0, 1.0, 0.2, AMETHYST);
        world().spawnParticle(Particle.END_ROD, center(), 80, 1.5, 1.5, 1.5, 0.15);
        world().spawnParticle(Particle.EXPLOSION, center(), 1);
    }

    public void phaseBegin() {
        sound(Sound.BLOCK_BEACON_POWER_SELECT, 2f, 0.8f);
        sound(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
        ring(base(), 2.0, Particle.END_ROD, null);
        ring(base(), 4.0, Particle.DUST, PURPLE);
    }

    public void lastStand() {
        sound(Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);
        sound(Sound.BLOCK_AMETHYST_BLOCK_BREAK, 2f, 0.4f);
        world().spawnParticle(Particle.BLOCK, center(), 200, 1.2, 1.2, 1.2, 0.3, AMETHYST);
        world().spawnParticle(Particle.SOUL_FIRE_FLAME, center(), 60, 1.0, 1.0, 1.0, 0.08);
        column(Particle.DUST, PURPLE, 5.0);
        ring(base(), arena.radius() * 0.5, Particle.DUST, DARK_PURPLE);
    }

    public void hazardStart() {
        sound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.4f);
        sound(Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.8f, 1.6f);
        ring(base(), 1.4, Particle.DUST, DARK_PURPLE);
    }

    public void hazardEnd() {
        sound(Sound.BLOCK_AMETHYST_CLUSTER_STEP, 1.2f, 0.8f);
        world().spawnParticle(Particle.WITCH, center(), 12, 0.6, 0.6, 0.6, 0.02);
    }

    public void death() {
        sound(Sound.BLOCK_AMETHYST_BLOCK_BREAK, 3f, 0.5f);
        sound(Sound.BLOCK_GLASS_BREAK, 2f, 0.6f);
        sound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
        world().spawnParticle(Particle.EXPLOSION_EMITTER, center(), 1);
        world().spawnParticle(Particle.BLOCK, center(), 300, 1.5, 1.5, 1.5, 0.4, AMETHYST);
        world().spawnParticle(Particle.END_ROD, center(), 150, 2.0, 2.0, 2.0, 0.25);
        for (Player player : arena.players()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }
}
