package com.roguesmp.dungeon.presentation.presenter;

import com.roguesmp.utils.Utils;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DungeonPresenter {

    public void onEnterDungeon(Player player, String dungeonName) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        showTitle(player, "<dark_purple><bold>" + dungeonName.toUpperCase(), "<gray>Dungeon Awakened...", 10);
    }

    public void onOpenDoor(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1f, 1f);
    }

    public void onCompleteRoom(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        showTitle(player, "<green><bold>ROOM CLEARED", "<gray>Your team have conquered this room", 0);
    }

    public void onCompleteDungeon(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
        showTitle(player, "<green><bold>DUNGEON COMPLETED", "<gray>Your team have conquered the dungeon", 0);
    }

    public void onPlayerDead(Player player, Location location) {
        showTitle(player, "<red><bold>YOU DIED", "<gray>Your soul fades...", 5);
        location.getWorld().spawnParticle(Particle.SCULK_SOUL, location, 30, 0.4, 0.6, 0.4);
        location.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, location, 20, 0.3, 0.5, 0.3);
        location.getWorld().playSound(location, Sound.ENTITY_WARDEN_DEATH, 1f, 0.8f);
    }

    public void onPlayerRevive(Player player, Location location) {
        showTitle(player, "<gold>Revived", "<yellow>Your soul is restored", 10);
        location.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location, 80, 0.5, 1, 0.5);
        location.getWorld().playSound(location, Sound.ITEM_TOTEM_USE, 1f, 0.8f);
    }

    private void showTitle(Player player, String title, String subtitle, int delayTicks) {
        Title shown = Title.title(Utils.fromString(title), Utils.fromString(subtitle));
        if (delayTicks <= 0) {
            player.showTitle(shown);
            return;
        }
        Utils.runLater(() -> {
            if (player.isOnline()) player.showTitle(shown);
        }, delayTicks);
    }
}
