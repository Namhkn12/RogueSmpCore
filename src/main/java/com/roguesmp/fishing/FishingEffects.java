package com.roguesmp.fishing;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class FishingEffects {

    private static final int PROGRESS_SOUND_INTERVAL = 6;
    private static final int STRUGGLE_SOUND_INTERVAL = 10;
    private static final int PARTICLE_INTERVAL = 4;
    private static final double HOTSPOT_RING_RADIUS = 0.7;
    private static final int HOTSPOT_RING_POINTS = 10;

    private FishingEffects() {
    }

    public static void bite(Player player, Location hook, FishTier tier, boolean inHotspot) {
        play(player, hook, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.8f, 1.0f);
        play(player, hook, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f + 0.15f * tier.ordinal());
        hook.getWorld().spawnParticle(Particle.SPLASH, hook, 20, 0.3, 0.1, 0.3, 0.1);

        if (!inHotspot) return;
        play(player, hook, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
        hook.getWorld().spawnParticle(Particle.GLOW, hook, 12, 0.4, 0.3, 0.4, 0.02);
    }

    public static void progress(Player player, Location hook, FishingMinigame game, FishTier tier, boolean inHotspot, int tick, boolean wasOnTarget) {
        boolean onTarget = game.isOnTarget();
        World world = hook.getWorld();

        if (onTarget && !wasOnTarget) play(player, hook, Sound.UI_BUTTON_CLICK, 0.2f, 1.6f);

        if (onTarget && tick % PROGRESS_SOUND_INTERVAL == 0) {
            float pitch = (float) Math.min(2.0, 0.6 + game.progress() * 1.4);
            play(player, hook, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.15f, pitch);
        } else if (!onTarget && tick % STRUGGLE_SOUND_INTERVAL == 0) {
            play(player, hook, Sound.BLOCK_NOTE_BLOCK_BASS, 0.2f, 0.6f);
        }

        if (tick % PARTICLE_INTERVAL != 0) return;

        if (onTarget) {
            world.spawnParticle(Particle.DUST, hook, 3, 0.25, 0.1, 0.25, 0, dust(tier));
        } else {
            world.spawnParticle(Particle.SPLASH, hook, 4, 0.3, 0.05, 0.3, 0.05);
        }
        if (inHotspot) world.spawnParticle(Particle.GLOW, hook, 2, 0.3, 0.2, 0.3, 0.01);
    }

    public static void caught(Player player, Location hook, FishTier tier, boolean inHotspot) {
        World world = hook.getWorld();
        play(player, hook, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 0.9f + 0.12f * tier.ordinal());
        play(player, hook, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 1.1f);
        world.spawnParticle(Particle.SPLASH, hook, 30, 0.4, 0.2, 0.4, 0.2);
        world.spawnParticle(Particle.DUST, hook, 30, 0.4, 0.4, 0.4, 0, dust(tier));

        if (tier.ordinal() >= FishTier.EPIC.ordinal()) world.spawnParticle(Particle.TOTEM_OF_UNDYING, hook, 20, 0.3, 0.5, 0.3, 0.3);
        if (tier == FishTier.LEGENDARY) play(player, hook, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.0f);
        if (inHotspot) world.spawnParticle(Particle.HAPPY_VILLAGER, hook, 10, 0.4, 0.4, 0.4, 0);
    }

    public static void escaped(Player player, Location hook) {
        play(player, hook, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 0.6f);
        play(player, hook, Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.5f);
        hook.getWorld().spawnParticle(Particle.SMOKE, hook, 8, 0.2, 0.1, 0.2, 0.02);
    }

    public static void ambient(Player viewer, Location at) {
        viewer.spawnParticle(Particle.END_ROD, at, 1, 0.2, 0.3, 0.2, 0.01);
    }

    public static void hotspotEnter(Player player, Location hook) {
        play(player, hook, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f);
    }

    public static void hotspotPulse(Location hook) {
        World world = hook.getWorld();
        for (int i = 0; i < HOTSPOT_RING_POINTS; i++) {
            double angle = 2 * Math.PI * i / HOTSPOT_RING_POINTS;
            world.spawnParticle(Particle.GLOW, hook.getX() + Math.cos(angle) * HOTSPOT_RING_RADIUS, hook.getY() + 0.1,
                    hook.getZ() + Math.sin(angle) * HOTSPOT_RING_RADIUS, 1, 0, 0, 0, 0);
        }
        world.spawnParticle(Particle.HAPPY_VILLAGER, hook, 3, 0.3, 0.3, 0.3, 0);
    }

    private static void play(Player player, Location at, Sound sound, float volume, float pitch) {
        player.playSound(at, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private static Particle.DustOptions dust(FishTier tier) {
        return new Particle.DustOptions(Color.fromRGB(tier.color().value()), 1.0f);
    }
}
