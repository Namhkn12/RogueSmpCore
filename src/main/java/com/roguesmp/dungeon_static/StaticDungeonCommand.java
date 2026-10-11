package com.roguesmp.dungeon_static;

import com.roguesmp.command.CommandFeedback;
import com.roguesmp.permission.Permissions;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static com.roguesmp.command.CommandFeedback.report;

public final class StaticDungeonCommand {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private StaticDungeonCommand() {
    }

    public static void register() {
        new CommandAPICommand("staticdungeon")
                .withAliases("sdungeon")
                .withSubcommand(createSub())
                .withSubcommand(joinSub())
                .withSubcommand(inviteSub())
                .withSubcommand(listSub())
                .withSubcommand(adminSub())
                .register();
    }

    private static CommandAPICommand createSub() {
        return new CommandAPICommand("create")
                .withArguments(dungeonArgument())
                .executesPlayer((player, args) -> {
                    String dungeon = (String) args.get("dungeon");
                    StaticDungeonManager manager = StaticDungeonManager.getInstance();

                    player.sendMessage(Utils.fromString("<yellow>Đang tạo phó bản..."));
                    manager.create(player.getUniqueId(), dungeon)
                            .thenCompose(instance -> manager.enter(player, instance).thenApply(v -> instance))
                            .whenComplete((instance, error) -> Utils.runLater(() -> {
                                if (error != null) CommandFeedback.error(player, error);
                                else player.sendMessage(Utils.fromString("<green>Đã vào phó bản <yellow>" + StaticDungeonInfo.displayNameOf(dungeon) + "<green>, hạn đến <yellow>" + deadline(instance.expiresAt()) + "<green>."));
                            }));
                });
    }

    private static CommandAPICommand joinSub() {
        return new CommandAPICommand("join")
                .withArguments(dungeonArgument())
                .withOptionalArguments(playerArgument("owner"))
                .executesPlayer((player, args) -> {
                    String dungeon = (String) args.get("dungeon");
                    OfflinePlayer owner = null;
                    if (args.getOptional("owner").isPresent()) {
                        owner = knownPlayer(player, (String) args.get("owner"));
                        if (owner == null) return;
                    }
                    StaticDungeonManager manager = StaticDungeonManager.getInstance();

                    StaticDungeonInstance instance;
                    try {
                        instance = owner == null
                                ? requireOwn(manager, player, dungeon)
                                : manager.acceptInvite(player.getUniqueId(), owner.getUniqueId(), dungeon);
                    } catch (IllegalStateException e) {
                        CommandFeedback.error(player, e);
                        return;
                    }

                    report(player, manager.enter(player, instance), "<green>Đã vào phó bản <yellow>" + StaticDungeonInfo.displayNameOf(dungeon) + "<green>.");
                });
    }

    private static CommandAPICommand inviteSub() {
        return new CommandAPICommand("invite")
                .withArguments(playerArgument("target"))
                .withOptionalArguments(dungeonArgument())
                .executesPlayer((player, args) -> {
                    Player target = Bukkit.getPlayerExact((String) args.get("target"));
                    if (target == null) {
                        player.sendMessage(Utils.fromString("<red>Người chơi này không trực tuyến."));
                        return;
                    }
                    String dungeon = (String) args.getOptional("dungeon").orElse(null);
                    StaticDungeonManager manager = StaticDungeonManager.getInstance();

                    StaticDungeonInstance instance = dungeon == null
                            ? manager.getByWorld(player.getWorld())
                            : manager.findFor(player.getUniqueId(), dungeon);
                    if (instance == null) {
                        player.sendMessage(Utils.fromString("<red>Hãy đứng trong phó bản hoặc ghi rõ tên phó bản."));
                        return;
                    }

                    try {
                        manager.invite(instance, player.getUniqueId(), target.getUniqueId());
                    } catch (IllegalStateException e) {
                        CommandFeedback.error(player, e);
                        return;
                    }

                    player.sendMessage(Utils.fromString("<green>Đã mời <yellow>" + target.getName() + "<green>."));
                    target.sendMessage(Utils.fromString("<yellow>" + player.getName() + "<green> mời bạn vào phó bản <yellow>" + StaticDungeonInfo.displayNameOf(instance.dungeonId())
                            + "<green>. Dùng <yellow>/staticdungeon join " + instance.dungeonId() + " " + player.getName() + "<green> trong 60 giây."));
                });
    }

    private static CommandAPICommand listSub() {
        return new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    List<StaticDungeonInstance> mine = StaticDungeonManager.getInstance().instancesOf(player.getUniqueId());
                    if (mine.isEmpty()) {
                        player.sendMessage(Utils.fromString("<yellow>Bạn chưa tham gia phó bản nào."));
                        return;
                    }
                    for (StaticDungeonInstance instance : mine) {
                        player.sendMessage(Utils.fromString("<yellow>" + StaticDungeonInfo.displayNameOf(instance.dungeonId()) + " <gray>- hạn <white>" + deadline(instance.expiresAt())
                                + " <gray>(" + members(instance) + " thành viên)"));
                    }
                });
    }

    private static CommandAPICommand adminSub() {
        return new CommandAPICommand("admin")
                .withPermission(Permissions.STATIC_DUNGEON_ADMIN.node())
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            for (StaticDungeonInstance instance : StaticDungeonManager.getInstance().instances()) {
                                sender.sendMessage(Utils.fromString("<yellow>" + instance.id() + " <gray>" + instance.dungeonId()
                                        + " owner=" + name(instance.ownerId()) + " members=" + members(instance) + " expires=" + deadline(instance.expiresAt())));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("info")
                        .withArguments(instanceArgument())
                        .executes((sender, args) -> {
                            StaticDungeonInstance instance = instance(sender, (String) args.get("id"));
                            if (instance == null) return;

                            sender.sendMessage(Utils.fromString("<yellow>" + instance.id() + " <gray>" + instance.dungeonId()));
                            sender.sendMessage(Utils.fromString("<gray>Owner: <white>" + name(instance.ownerId())));
                            sender.sendMessage(Utils.fromString("<gray>Members: <white>" + instance.members().stream().map(StaticDungeonCommand::name).toList()));
                            sender.sendMessage(Utils.fromString("<gray>Expires: <white>" + deadline(instance.expiresAt())
                                    + " <gray>(in " + Duration.ofMillis(Math.max(0, instance.expiresAt() - System.currentTimeMillis())).toHours() + "h)"));
                        }))
                .withSubcommand(new CommandAPICommand("extend")
                        .withArguments(instanceArgument(), new DoubleArgument("hours"))
                        .executes((sender, args) -> {
                            StaticDungeonInstance instance = instance(sender, (String) args.get("id"));
                            if (instance == null) return;

                            long expiresAt = instance.expiresAt() + (long) ((double) args.get("hours") * 3_600_000L);
                            StaticDungeonManager.getInstance().extend(instance, expiresAt);
                            sender.sendMessage(Utils.fromString("<green>New expiry: <yellow>" + deadline(expiresAt)));
                        }))
                .withSubcommand(new CommandAPICommand("destroy")
                        .withArguments(instanceArgument())
                        .executes((sender, args) -> {
                            StaticDungeonInstance instance = instance(sender, (String) args.get("id"));
                            if (instance == null) return;

                            report(sender, StaticDungeonManager.getInstance().destroy(instance, "đã bị quản trị viên đóng"), "<green>Destroyed instance <yellow>" + instance.id());
                        }))
                .withSubcommand(new CommandAPICommand("addmember")
                        .withArguments(instanceArgument(), playerArgument("player"))
                        .executes((sender, args) -> {
                            editMember(sender, args.get("id"), args.get("player"), true);
                        }))
                .withSubcommand(new CommandAPICommand("removemember")
                        .withArguments(instanceArgument(), playerArgument("player"))
                        .executes((sender, args) -> {
                            editMember(sender, args.get("id"), args.get("player"), false);
                        }))
                .withSubcommand(new CommandAPICommand("expire")
                        .executes((sender, args) -> {
                            StaticDungeonManager.getInstance().expireDue();
                            sender.sendMessage(Utils.fromString("<green>Expired instances are being destroyed."));
                        }));
    }

    private static void editMember(CommandSender sender, Object id, Object playerName, boolean add) {
        StaticDungeonInstance instance = instance(sender, (String) id);
        if (instance == null) return;

        OfflinePlayer player = knownPlayer(sender, (String) playerName);
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        try {
            if (add) StaticDungeonManager.getInstance().addMember(instance, playerId);
            else StaticDungeonManager.getInstance().removeMember(instance, playerId);
        } catch (IllegalStateException e) {
            CommandFeedback.error(sender, e);
            return;
        }
        sender.sendMessage(Utils.fromString("<green>" + (add ? "Added " : "Removed ") + "<yellow>" + name(playerId)));
    }

    private static StaticDungeonInstance requireOwn(StaticDungeonManager manager, Player player, String dungeon) {
        StaticDungeonInstance instance = manager.findFor(player.getUniqueId(), dungeon);
        if (instance == null) throw new IllegalStateException("Bạn chưa có phó bản này, hãy dùng /staticdungeon create " + dungeon + ".");
        return instance;
    }

    private static @Nullable StaticDungeonInstance instance(CommandSender sender, String id) {
        try {
            StaticDungeonInstance instance = StaticDungeonManager.getInstance().get(UUID.fromString(id));
            if (instance != null) return instance;
        } catch (IllegalArgumentException ignored) {
        }
        sender.sendMessage(Utils.fromString("<red>Unknown instance: " + Utils.MINI_MESSAGE.escapeTags(id)));
        return null;
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
                info -> Registries.STATIC_DUNGEON_INFO.getAll().keySet().toArray(String[]::new)));
    }

    private static Argument<String> instanceArgument() {
        return new StringArgument("id").replaceSuggestions(ArgumentSuggestions.strings(
                info -> StaticDungeonManager.getInstance().instances().stream().map(instance -> instance.id().toString()).toArray(String[]::new)));
    }

    private static String deadline(long epochMillis) {
        return DateUtils.toZonedDateTime(epochMillis).format(DEADLINE_FORMAT);
    }

    private static int members(StaticDungeonInstance instance) {
        return instance.members().size();
    }

    private static String name(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name != null ? name : playerId.toString();
    }
}
