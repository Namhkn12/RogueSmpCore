package com.roguesmp.dungeon.presentation.presenter;

import com.roguesmp.dungeon.presentation.PresentationManager;
import com.roguesmp.dungeon.presentation.PresentationSequence;
import com.roguesmp.dungeon.presentation.effect.DungeonEffect;
import com.roguesmp.dungeon.presentation.message.ScreenMessage;
import com.roguesmp.dungeon.presentation.sound.DungeonSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DungeonPresenter {

    private final PresentationManager presentation;

    public DungeonPresenter(PresentationManager presentation) {
        this.presentation = presentation;
    }

    public void onEnterDungeon(Player player, String dungeonName) {
        new PresentationSequence()

                .addEffect(DungeonEffect.of(
                        new PotionEffect(PotionEffectType.BLINDNESS, 60, 1)
                ))
                .addSound(DungeonSound.of(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f))
                .addScreen(
                        ScreenMessage.title(
                                "&5&l" + dungeonName.toUpperCase(),
                                "&7Dungeon Awakened..."
                        ).delay(10)
                )

                .play(player, presentation);
    }

    public void onOpenDoor(Player player) {
        new PresentationSequence()

                .addSound(DungeonSound.of(Sound.BLOCK_END_PORTAL_SPAWN, 1f, 1f))
                .play(player, presentation);
    }

    public void onCompleteRoom(Player player) {
        new PresentationSequence()

                .addSound(DungeonSound.of(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f))
                .addScreen(
                        ScreenMessage.title(
                                "&a&lHOÀN THÀNH",
                                "&7Bạn đã vượt qua căn phòng"
                        )
                )

                .play(player, presentation);
    }
}