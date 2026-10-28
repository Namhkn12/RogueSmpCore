package com.roguesmp.command;

import com.roguesmp.permission.Permissions;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.LongBinaryOperator;

public final class MoneyCommand {

    private MoneyCommand() {
    }

    public static void register() {
        new CommandAPICommand("money")
                .withPermission(Permissions.MONEY_ADMIN.node())
                .withSubcommand(new CommandAPICommand("get")
                        .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("player");
                            PlayerData data = dataOf(sender, target);
                            if (data == null) return;
                            sender.sendMessage(Utils.fromString("<yellow>" + target.getName() + "<gray> has <gold>" + data.getMoney()));
                        }))
                .withSubcommand(modify("set", (current, amount) -> amount))
                .withSubcommand(modify("add", (current, amount) -> Long.MAX_VALUE - current < amount ? Long.MAX_VALUE : current + amount))
                .withSubcommand(modify("remove", (current, amount) -> Math.max(0, current - amount)))
                .withSubcommand(new CommandAPICommand("reset")
                        .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                        .executes((sender, args) -> {
                            apply(sender, (Player) args.get("player"), 0, (current, amount) -> 0);
                        }))
                .register();
    }

    private static CommandAPICommand modify(String name, LongBinaryOperator operation) {
        return new CommandAPICommand(name)
                .withArguments(new EntitySelectorArgument.OnePlayer("player"), new LongArgument("amount", 0))
                .executes((sender, args) -> {
                    apply(sender, (Player) args.get("player"), (long) args.get("amount"), operation);
                });
    }

    private static void apply(CommandSender sender, Player target, long amount, LongBinaryOperator operation) {
        PlayerData data = dataOf(sender, target);
        if (data == null) return;

        long before = data.getMoney();
        data.setMoney(operation.applyAsLong(before, amount));
        sender.sendMessage(Utils.fromString("<yellow>" + target.getName() + "<gray>: <gold>" + before + "<gray> → <gold>" + data.getMoney()));
    }

    private static PlayerData dataOf(CommandSender sender, Player target) {
        PlayerData data = PlayerManager.getInstance().getDataManager().getData(target.getUniqueId());
        if (data == null) sender.sendMessage(Utils.fromString("<red>Player data for " + target.getName() + " is not loaded."));
        return data;
    }
}
