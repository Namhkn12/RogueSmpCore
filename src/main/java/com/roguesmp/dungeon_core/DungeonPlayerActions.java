package com.roguesmp.dungeon_core;

import com.roguesmp.command.CommandFeedback;
import com.roguesmp.dungeon_core.feature.MembershipFeature;
import com.roguesmp.dungeon_core.feature.PartyFeature;
import org.bukkit.Bukkit;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

import static com.roguesmp.command.CommandFeedback.report;

public final class DungeonPlayerActions {

    private DungeonPlayerActions() {
    }

    public static void create(Player player, String dungeonId) {
        DungeonManager manager = DungeonManager.getInstance();
        DungeonDefinition definition = Registries.DUNGEON.get(dungeonId);
        if (definition == null) {
            player.sendMessage(Utils.fromString("<red>Phó bản này không tồn tại."));
            return;
        }

        Set<UUID> members;
        try {
            members = PartyFeature.startingMembers(definition, player);
        } catch (IllegalStateException e) {
            CommandFeedback.error(player, e);
            return;
        }

        player.sendMessage(Utils.fromString("<yellow>Đang tạo phó bản..."));
        report(player, manager.create(dungeonId, player.getUniqueId(), members).thenCompose(instance -> {
                    members.stream().map(Bukkit::getPlayer).filter(member -> member != null && !member.equals(player)).forEach(member -> enter(member, instance));
                    return manager.enter(player, instance);
                }),
                "<green>Đã vào phó bản <yellow>" + definition.displayName() + "<green>.");
    }

    public static void enter(Player player, DungeonInstance instance) {
        report(player, DungeonManager.getInstance().enter(player, instance), "<green>Đã vào phó bản <yellow>" + displayName(instance) + "<green>.");
    }

    public static void acceptInvite(Player player, DungeonInstance instance) {
        FeatureContext<MembershipFeature.State> membership = membership(player, instance);
        if (membership == null) return;
        try {
            ((MembershipFeature) membership.feature()).acceptInvite(membership, player.getUniqueId());
        } catch (IllegalStateException e) {
            CommandFeedback.error(player, e);
            return;
        }
        enter(player, instance);
    }

    public static void invite(Player player, DungeonInstance instance, Player target) {
        FeatureContext<MembershipFeature.State> membership = membership(player, instance);
        if (membership == null) return;
        try {
            ((MembershipFeature) membership.feature()).invite(membership, player.getUniqueId(), target.getUniqueId());
        } catch (IllegalStateException e) {
            CommandFeedback.error(player, e);
            return;
        }

        player.sendMessage(Utils.fromString("<green>Đã mời <yellow>" + target.getName() + "<green>."));
        target.sendMessage(Utils.fromString("<yellow>" + player.getName() + "<green> mời bạn vào phó bản <yellow>" + displayName(instance)
                + "<green>. <yellow><click:run_command:'/dungeonrun join " + instance.dungeonId() + " " + player.getName() + "'>[Tham gia]</click>"));
    }

    public static void abandon(Player player, DungeonInstance instance) {
        FeatureContext<MembershipFeature.State> membership = membership(player, instance);
        if (membership == null) return;
        try {
            ((MembershipFeature) membership.feature()).abandon(membership, player.getUniqueId());
        } catch (IllegalStateException e) {
            CommandFeedback.error(player, e);
            return;
        }
        if (!instance.ownerId().equals(player.getUniqueId())) {
            player.sendMessage(Utils.fromString("<yellow>Bạn đã rời bỏ phó bản <white>" + displayName(instance) + "<yellow>."));
        }
    }

    public static @Nullable MembershipFeature membershipFeature(DungeonInstance instance) {
        FeatureContext<MembershipFeature.State> membership = DungeonManager.getInstance().feature(instance, MembershipFeature.class);
        return membership == null ? null : (MembershipFeature) membership.feature();
    }

    public static String displayName(DungeonInstance instance) {
        DungeonDefinition definition = instance.definition();
        return definition != null ? definition.displayName() : instance.dungeonId();
    }

    private static @Nullable FeatureContext<MembershipFeature.State> membership(Player player, DungeonInstance instance) {
        FeatureContext<MembershipFeature.State> membership = DungeonManager.getInstance().feature(instance, MembershipFeature.class);
        if (membership == null) player.sendMessage(Utils.fromString("<red>Phó bản này không hỗ trợ việc này."));
        return membership;
    }
}
