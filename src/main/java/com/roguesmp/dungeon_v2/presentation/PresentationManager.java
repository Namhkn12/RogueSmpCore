package com.roguesmp.dungeon_v2.presentation;

import com.roguesmp.dungeon_v2.presentation.effect.DungeonEffect;
import com.roguesmp.dungeon_v2.presentation.message.ScreenMessage;
import com.roguesmp.dungeon_v2.presentation.particle.DungeonParticle;
import com.roguesmp.dungeon_v2.presentation.sound.DungeonSound;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class PresentationManager {

    private final SoundManager soundManager;
    private final EffectManager effectManager;
    private final ScreenMessManager screenManager;
    private final ParticleManager particleManager;

    public PresentationManager(SoundManager soundManager,
                               EffectManager effectManager,
                               ScreenMessManager screenManager, ParticleManager particleManager) {
        this.soundManager = soundManager;
        this.effectManager = effectManager;
        this.screenManager = screenManager;
        this.particleManager = particleManager;
    }

    // ========================
    // SINGLE
    // ========================

    public void sound(Player player, DungeonSound sound) {
        soundManager.play(player, sound);
    }

    public void effect(Player player, DungeonEffect effect) {
        effectManager.apply(player, effect);
    }

    public void screen(Player player, ScreenMessage message) {
        screenManager.send(player, message);
    }

    public void particle(Player player, Location location, DungeonParticle data) {
        particleManager.spawn(player, location, data);
    }

    public void play(Player player,
                     List<DungeonSound> sounds,
                     List<DungeonEffect> effects,
                     List<ScreenMessage> screens) {

        if (sounds != null) sounds.forEach(s -> sound(player, s));
        if (effects != null) effects.forEach(e -> effect(player, e));
        if (screens != null) screens.forEach(m -> screen(player, m));
    }

    public void play(Player player,
                     List<DungeonSound> sounds,
                     List<DungeonEffect> effects,
                     List<ScreenMessage> screens,
                     List<DungeonParticle> particles,
                     Location location) {

        if (sounds != null) sounds.forEach(s -> sound(player, s));
        if (effects != null) effects.forEach(e -> effect(player, e));
        if (screens != null) screens.forEach(m -> screen(player, m));
        if (particles != null && location != null)
            particles.forEach(p -> particle(player, location, p));
    }
}