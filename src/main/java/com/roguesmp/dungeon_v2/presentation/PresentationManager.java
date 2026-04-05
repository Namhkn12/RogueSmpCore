package com.roguesmp.dungeon_v2.presentation;

import com.roguesmp.dungeon_v2.presentation.effect.DungeonEffect;
import com.roguesmp.dungeon_v2.presentation.message.ScreenMessage;
import com.roguesmp.dungeon_v2.presentation.sound.DungeonSound;
import org.bukkit.entity.Player;

import java.util.List;

public class PresentationManager {

    private final SoundManager soundManager;
    private final EffectManager effectManager;
    private final ScreenMessManager screenManager;

    public PresentationManager(SoundManager soundManager,
                               EffectManager effectManager,
                               ScreenMessManager screenManager) {
        this.soundManager = soundManager;
        this.effectManager = effectManager;
        this.screenManager = screenManager;
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

    // ========================
    // BUNDLE (đa hiệu ứng cùng lúc)
    // ========================

    public void play(Player player,
                     List<DungeonSound> sounds,
                     List<DungeonEffect> effects,
                     List<ScreenMessage> screens) {

        if (sounds != null) sounds.forEach(s -> sound(player, s));
        if (effects != null) effects.forEach(e -> effect(player, e));
        if (screens != null) screens.forEach(m -> screen(player, m));
    }
}