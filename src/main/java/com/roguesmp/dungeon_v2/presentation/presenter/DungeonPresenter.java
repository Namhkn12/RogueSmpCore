package com.roguesmp.dungeon_v2.presentation.presenter;

import com.roguesmp.dungeon_v2.presentation.PresentationManager;
import com.roguesmp.dungeon_v2.presentation.PresentationSequence;
import com.roguesmp.dungeon_v2.presentation.effect.DungeonEffect;
import com.roguesmp.dungeon_v2.presentation.message.ScreenMessage;
import com.roguesmp.dungeon_v2.presentation.particle.DungeonParticle;
import com.roguesmp.dungeon_v2.presentation.sound.DungeonSound;
import org.bukkit.Location;
import org.bukkit.Particle;
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

    public void onCompleteDungeon(Player player) {
        new PresentationSequence()

                .addSound(DungeonSound.of(Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f))
                .addScreen(
                        ScreenMessage.title(
                                "&a&lCHÚC MỪNG",
                                "&7Bạn đã vượt qua hầm ngục"
                        )
                )

                .play(player, presentation);
    }

    public void onPlayerDead(Player player, Location location) {
        // Skull particle — màu xanh Warden (SCULK_SOUL)
        new PresentationSequence()
                .addSound(DungeonSound.of(Sound.ENTITY_WARDEN_DEATH, 1f, 0.8f))
                .addParticle(DungeonParticle.of(
                        Particle.SCULK_SOUL,
                        30,
                        0.4, 0.6, 0.4
                ))
                .addParticle(DungeonParticle.of(
                        Particle.SCULK_CHARGE_POP,
                        20,
                        0.3, 0.5, 0.3
                ))
                .addScreen(
                        ScreenMessage.title("&c&lYOU DIED", "&7Your soul fades...").delay(5)
                )
                .play(player, location, presentation);
    }

    public void onPlayerRevive(Player player, Location location) {
        // Totem break particle
        new PresentationSequence()
                .addSound(DungeonSound.of(Sound.ITEM_TOTEM_USE, 1f, 1f))
                .addParticle(DungeonParticle.of(
                        Particle.TOTEM_OF_UNDYING,
                        80,
                        0.5, 1.0, 0.5
                ))
                .addScreen(
                        ScreenMessage.title(
                                "&6&lFate grants you another chance!",
                                "&eYour soul is restored"
                        ).delay(10)
                )
                .play(player, location, presentation);
    }
}