package com.roguesmp.quest;

import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class QuestNotifications {

    public enum RewardDelivery {
        GIVEN,
        INBOX,
        NONE
    }

    private static final String DIVIDER = "<dark_gray><st>                                                  </st></dark_gray>";

    private QuestNotifications() {
    }

    public static void completed(Player player, Quest quest, RewardDelivery delivery) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        player.sendMessage(Utils.fromString(DIVIDER));
        player.sendMessage(Utils.fromString("  <green><bold>✔ NHIỆM VỤ HOÀN THÀNH</bold></green>"));
        player.sendMessage(Utils.fromString("  " + name(quest)));
        if (delivery != RewardDelivery.NONE && !quest.getRewards().isEmpty()) {
            player.sendMessage(Component.empty());
            player.sendMessage(Utils.fromString("  <yellow>🎁 Phần thưởng:</yellow>"));
            for (QuestReward reward : quest.getRewards()) {
                for (Component line : reward.getDisplay(smpPlayer)) {
                    player.sendMessage(Utils.fromString("   <dark_gray>•</dark_gray> ").append(line));
                }
            }
            if (delivery == RewardDelivery.INBOX) {
                player.sendMessage(Utils.fromString("  <aqua>✉ Phần thưởng đã được gửi vào hộp thư <gray>(/quest inbox)</gray></aqua>"));
            }
        }
        player.sendMessage(Utils.fromString(DIVIDER));

        Location location = player.getLocation();
        player.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0, 1, 0), 60, 0.4, 0.6, 0.4, 0.4);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.clone().add(0, 1, 0), 20, 0.6, 0.8, 0.6, 0);
    }

    public static void started(Player player, Quest quest) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        player.sendMessage(Utils.fromString(DIVIDER));
        player.sendMessage(Utils.fromString("  <aqua><bold>✦ NHIỆM VỤ MỚI</bold></aqua>"));
        player.sendMessage(Utils.fromString("  " + name(quest)));
        for (String line : quest.getDescription()) {
            player.sendMessage(Utils.fromString("  " + line));
        }
        if (!quest.getObjectives().isEmpty()) {
            player.sendMessage(Component.empty());
            player.sendMessage(Utils.fromString("  <yellow>Mục tiêu:</yellow>"));
            quest.getObjectives().values().forEach(objective -> objective.getDisplay(smpPlayer)
                    .forEach(line -> player.sendMessage(Utils.fromString("   <dark_gray>•</dark_gray> ").append(line))));
        }
        player.sendMessage(Utils.fromString("  <dark_gray>/quest để xem chi tiết</dark_gray>"));
        player.sendMessage(Utils.fromString(DIVIDER));

        Location location = player.getLocation();
        player.playSound(location, Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        player.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        player.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);
        player.getWorld().spawnParticle(Particle.ENCHANT, location.clone().add(0, 1.2, 0), 80, 0.5, 0.8, 0.5, 1.0);
    }

    private static String name(Quest quest) {
        return quest.getName() != null ? quest.getName() : quest.getId();
    }
}
