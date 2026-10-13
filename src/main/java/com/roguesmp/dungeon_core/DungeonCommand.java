package com.roguesmp.dungeon_core;

import com.roguesmp.command.CommandFeedback;
import com.roguesmp.dungeon_core.feature.ExpiryFeature;
import com.roguesmp.dungeon_core.feature.MembershipFeature;
import com.roguesmp.permission.Permissions;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.roguesmp.command.CommandFeedback.report;

public class DungeonCommand {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final DungeonManager manager;

    public DungeonCommand(DungeonManager manager) {
        this.manager = manager;
    }

    public void register() {
        new CommandAPICommand("dungeonrun")
                .withAliases("drun")
                .withSubcommand(createSub())
                .withSubcommand(joinSub())
                .withSubcommand(inviteSub())
                .withSubcommand(listSub())
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            if (manager.getByWorld(player.getWorld()) != null) manager.leave(player);
                        }))
                .withSubcommand(adminSub())
                .register();
    }

    private CommandAPICommand createSub() {
        return new CommandAPICommand("create")
                .withArguments(dungeonArgument())
                .executesPlayer((player, args) -> {
                    String dungeonId = (String) args.get("dungeon");
                    DungeonDefinition definition = Registries.DUNGEON.get(dungeonId);
                    if (definition == null) {
                        player.sendMessage(Utils.fromString("<red>Phó bản này không tồn tại."));
                        return;
                    }
                    try {
                        MembershipFeature.checkCreate(manager, definition, dungeonId, player.getUniqueId());
                    } catch (IllegalStateException e) {
                        CommandFeedback.error(player, e);
                        return;
                    }

                    player.sendMessage(Utils.fromString("<yellow>Đang tạo phó bản..."));
                    report(player, manager.create(dungeonId, player.getUniqueId(), Set.of()).thenCompose(instance -> manager.enter(player, instance)),
                            "<green>Đã vào phó bản <yellow>" + definition.displayName() + "<green>.");
                });
    }

    private CommandAPICommand joinSub() {
        return new CommandAPICommand("join")
                .withArguments(dungeonArgument())
                .withOptionalArguments(playerArgument("owner"))
                .executesPlayer((player, args) -> {
                    String dungeonId = (String) args.get("dungeon");
                    DungeonInstance instance;
                    try {
                        instance = args.getOptional("owner").isPresent()
                                ? acceptInvite(player, (String) args.get("owner"), dungeonId)
                                : requireOwn(player, dungeonId);
                    } catch (IllegalStateException e) {
                        CommandFeedback.error(player, e);
                        return;
                    }
                    if (instance != null) report(player, manager.enter(player, instance), "<green>Đã vào phó bản <yellow>" + displayName(instance) + "<green>.");
                });
    }

    private CommandAPICommand inviteSub() {
        return new CommandAPICommand("invite")
                .withArguments(playerArgument("target"))
                .withOptionalArguments(dungeonArgument())
                .executesPlayer((player, args) -> {
                    Player target = Bukkit.getPlayerExact((String) args.get("target"));
                    if (target == null) {
                        player.sendMessage(Utils.fromString("<red>Người chơi này không trực tuyến."));
                        return;
                    }
                    String dungeonId = (String) args.getOptional("dungeon").orElse(null);
                    DungeonInstance instance = dungeonId == null ? manager.getByWorld(player.getWorld()) : manager.findFor(player.getUniqueId(), dungeonId);
                    if (instance == null) {
                        player.sendMessage(Utils.fromString("<red>Hãy đứng trong phó bản hoặc ghi rõ tên phó bản."));
                        return;
                    }

                    try {
                        FeatureContext<MembershipFeature.State> membership = requireMembership(instance);
                        ((MembershipFeature) membership.feature()).invite(membership, player.getUniqueId(), target.getUniqueId());
                    } catch (IllegalStateException e) {
                        CommandFeedback.error(player, e);
                        return;
                    }

                    player.sendMessage(Utils.fromString("<green>Đã mời <yellow>" + target.getName() + "<green>."));
                    target.sendMessage(Utils.fromString("<yellow>" + player.getName() + "<green> mời bạn vào phó bản <yellow>" + displayName(instance)
                            + "<green>. Dùng <yellow>/dungeonrun join " + instance.dungeonId() + " " + player.getName() + "<green> để tham gia."));
                });
    }

    private CommandAPICommand listSub() {
        return new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    List<DungeonInstance> mine = manager.instancesOf(player.getUniqueId());
                    if (mine.isEmpty()) {
                        player.sendMessage(Utils.fromString("<yellow>Bạn chưa tham gia phó bản nào."));
                        return;
                    }
                    for (DungeonInstance instance : mine) {
                        player.sendMessage(Utils.fromString("<yellow>" + displayName(instance) + " <gray>- hạn <white>" + deadline(instance)
                                + " <gray>(" + instance.members().size() + " thành viên)"));
                    }
                });
    }

    private CommandAPICommand adminSub() {
        return new CommandAPICommand("admin")
                .withPermission(Permissions.DUNGEON_ADMIN.node())
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            for (DungeonInstance instance : manager.instances()) {
                                sender.sendMessage(Utils.fromString("<yellow>" + instance.id() + " <gray>" + instance.dungeonId()
                                        + " owner=" + name(instance.ownerId()) + " members=" + instance.members().size() + " expires=" + deadline(instance)));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("info")
                        .withArguments(instanceArgument())
                        .executes((sender, args) -> {
                            DungeonInstance instance = instance(sender, (String) args.get("id"));
                            if (instance == null) return;

                            sender.sendMessage(Utils.fromString("<yellow>" + instance.id() + " <gray>" + instance.dungeonId()));
                            sender.sendMessage(Utils.fromString("<gray>Owner: <white>" + name(instance.ownerId())));
                            sender.sendMessage(Utils.fromString("<gray>Members: <white>" + instance.members().stream().map(DungeonCommand::name).toList()));
                            sender.sendMessage(Utils.fromString("<gray>Expires: <white>" + deadline(instance)));
                        }))
                .withSubcommand(new CommandAPICommand("extend")
                        .withArguments(instanceArgument(), new DoubleArgument("hours"))
                        .executes((sender, args) -> {
                            DungeonInstance instance = instance(sender, (String) args.get("id"));
                            if (instance == null) return;
                            FeatureContext<ExpiryFeature.State> expiry = manager.feature(instance, ExpiryFeature.class);
                            if (expiry == null) {
                                sender.sendMessage(Utils.fromString("<red>This dungeon has no expiry."));
                                return;
                            }

                            long expiresAt = expiry.state().expiresAt() + (long) ((double) args.get("hours") * 3_600_000L);
                            ExpiryFeature.setExpiresAt(expiry, expiresAt);
                            sender.sendMessage(Utils.fromString("<green>New expiry: <yellow>" + deadline(instance)));
                        }))
                .withSubcommand(new CommandAPICommand("end")
                        .withArguments(instanceArgument())
                        .executes((sender, args) -> {
                            DungeonInstance instance = instance(sender, (String) args.get("id"));
                            if (instance == null) return;
                            report(sender, manager.end(instance, "đã bị quản trị viên đóng"), "<green>Ended run <yellow>" + instance.id());
                        }))
                .withSubcommand(new CommandAPICommand("addmember")
                        .withArguments(instanceArgument(), playerArgument("player"))
                        .executes((sender, args) -> {
                            editMember(sender, (String) args.get("id"), (String) args.get("player"), true);
                        }))
                .withSubcommand(new CommandAPICommand("removemember")
                        .withArguments(instanceArgument(), playerArgument("player"))
                        .executes((sender, args) -> {
                            editMember(sender, (String) args.get("id"), (String) args.get("player"), false);
                        }))
                .withSubcommand(new CommandAPICommand("signal")
                        .withArguments(new StringArgument("signal"))
                        .withOptionalArguments(new IntegerArgument("amount", 1))
                        .executesPlayer((player, args) -> {
                            DungeonInstance instance = manager.getByWorld(player.getWorld());
                            if (instance == null) {
                                player.sendMessage(Utils.fromString("<red>Bạn không ở trong phó bản nào."));
                                return;
                            }
                            int amount = (Integer) args.getOrDefault("amount", 1);
                            manager.signal(instance, new DungeonSignal.Custom((String) args.get("signal"), amount, player));
                        }));
    }

    private void editMember(CommandSender sender, String id, String playerName, boolean add) {
        DungeonInstance instance = instance(sender, id);
        if (instance == null) return;
        OfflinePlayer player = knownPlayer(sender, playerName);
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        try {
            if (!add) {
                manager.removeMember(instance, playerId);
            } else {
                FeatureContext<MembershipFeature.State> membership = manager.feature(instance, MembershipFeature.class);
                if (membership != null) ((MembershipFeature) membership.feature()).addMember(membership, playerId);
                else manager.addMember(instance, playerId);
            }
        } catch (IllegalStateException e) {
            CommandFeedback.error(sender, e);
            return;
        }
        sender.sendMessage(Utils.fromString("<green>" + (add ? "Added " : "Removed ") + "<yellow>" + name(playerId)));
    }

    private @Nullable DungeonInstance acceptInvite(Player player, String ownerName, String dungeonId) {
        OfflinePlayer owner = knownPlayer(player, ownerName);
        if (owner == null) return null;

        DungeonInstance instance = manager.instances().stream()
                .filter(run -> run.dungeonId().equals(dungeonId) && run.ownerId().equals(owner.getUniqueId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phó bản này."));
        FeatureContext<MembershipFeature.State> membership = requireMembership(instance);
        ((MembershipFeature) membership.feature()).acceptInvite(membership, player.getUniqueId());
        return instance;
    }

    private DungeonInstance requireOwn(Player player, String dungeonId) {
        DungeonInstance instance = manager.findFor(player.getUniqueId(), dungeonId);
        if (instance == null) throw new IllegalStateException("Bạn chưa có phó bản này, hãy dùng /dungeonrun create " + dungeonId + ".");
        return instance;
    }

    private FeatureContext<MembershipFeature.State> requireMembership(DungeonInstance instance) {
        FeatureContext<MembershipFeature.State> membership = manager.feature(instance, MembershipFeature.class);
        if (membership == null) throw new IllegalStateException("Phó bản này không cho phép mời người khác.");
        return membership;
    }

    private @Nullable DungeonInstance instance(CommandSender sender, String id) {
        try {
            DungeonInstance instance = manager.get(UUID.fromString(id));
            if (instance != null) return instance;
        } catch (IllegalArgumentException ignored) {
        }
        sender.sendMessage(Utils.fromString("<red>Unknown run: " + Utils.MINI_MESSAGE.escapeTags(id)));
        return null;
    }

    private String deadline(DungeonInstance instance) {
        FeatureContext<ExpiryFeature.State> expiry = manager.feature(instance, ExpiryFeature.class);
        return expiry == null ? "không giới hạn" : DateUtils.toZonedDateTime(expiry.state().expiresAt()).format(DEADLINE_FORMAT);
    }

    private static String displayName(DungeonInstance instance) {
        DungeonDefinition definition = instance.definition();
        return definition != null ? definition.displayName() : instance.dungeonId();
    }

    private static Argument<String> playerArgument(String nodeName) {
        return new StringArgument(nodeName).replaceSuggestions(ArgumentSuggestions.strings(
                info -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new)));
    }

    private static @Nullable OfflinePlayer knownPlayer(CommandSender sender, String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
        if (player == null) sender.sendMessage(Utils.fromString("<red>Không tìm thấy người chơi này."));
        return player;
    }

    private static Argument<String> dungeonArgument() {
        return new StringArgument("dungeon").replaceSuggestions(ArgumentSuggestions.strings(
                info -> Registries.DUNGEON.getAll().keySet().toArray(String[]::new)));
    }

    private Argument<String> instanceArgument() {
        return new StringArgument("id").replaceSuggestions(ArgumentSuggestions.strings(
                info -> manager.instances().stream().map(instance -> instance.id().toString()).toArray(String[]::new)));
    }

    private static String name(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name != null ? name : playerId.toString();
    }
}
