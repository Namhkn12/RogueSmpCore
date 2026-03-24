package com.roguesmp.dungeon.presentation;

import com.roguesmp.dungeon.presentation.effect.DungeonEffect;
import com.roguesmp.dungeon.presentation.message.ScreenMessage;
import com.roguesmp.dungeon.presentation.sound.DungeonSound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PresentationSequence {

    private final List<DungeonSound> sounds = new ArrayList<>();
    private final List<DungeonEffect> effects = new ArrayList<>();
    private final List<ScreenMessage> screens = new ArrayList<>();

    public PresentationSequence addSound(DungeonSound sound) {
        sounds.add(sound);
        return this;
    }

    public PresentationSequence addEffect(DungeonEffect effect) {
        effects.add(effect);
        return this;
    }

    public PresentationSequence addScreen(ScreenMessage screen) {
        screens.add(screen);
        return this;
    }

    public void play(Player player, PresentationManager manager) {
        manager.play(player, sounds, effects, screens);
    }
}