package com.roguesmp.dungeon.presentation.presenter;

import com.roguesmp.dungeon.presentation.PresentationManager;
import com.roguesmp.dungeon.presentation.PresentationSequence;
import com.roguesmp.dungeon.presentation.effect.DungeonEffect;
import com.roguesmp.dungeon.presentation.message.ScreenMessage;
import com.roguesmp.dungeon.presentation.sound.DungeonSound;
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
                                "&a&lROOM CLEARED",
                                "&7Your team have conquered this room"
                        )
                )

                .play(player, presentation);
    }

    public void onCompleteDungeon(Player player) {
        new PresentationSequence()

                .addSound(DungeonSound.of(Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f))
                .addScreen(
                        ScreenMessage.title(
                                "&a&lDUNGEON COMPLETED",
                                "&7Your team have conquered the dungeon"
                        )
                )

                .play(player, presentation);
    }

    public void onPlayerDead(Player player, Location location) {
        new PresentationSequence()
                .addScreen(
                        ScreenMessage.title("&c&lYOU DIED", "&7Your soul fades...").delay(5)
                )
                .play(player, location, presentation);

        location.getWorld().spawnParticle(Particle.SCULK_SOUL, location, 30, 0.4, 0.6, 0.4);
        location.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, location, 20, 0.3, 0.5, 0.3);
        location.getWorld().playSound(location, Sound.ENTITY_WARDEN_DEATH, 1f, 0.8f);
    }

    public void onPlayerRevive(Player player, Location location) {
        new PresentationSequence()
                .addScreen(
                        ScreenMessage.title(
                                "&6Revived",
                                "&eYour soul is restored"
                        ).delay(10)
                )
                .play(player, location, presentation);
        location.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location, 80, 0.5, 1, 0.5);
        location.getWorld().playSound(location, Sound.ITEM_TOTEM_USE, 1f, 0.8f);
    }
}