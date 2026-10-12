package com.roguesmp.command;

import com.roguesmp.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class CommandFeedback {

    private CommandFeedback() {
    }

    public static void error(CommandSender sender, Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        sender.sendMessage(Utils.fromString("<red>" + Utils.MINI_MESSAGE.escapeTags(String.valueOf(cause.getMessage()))));
    }

    public static <T> void report(CommandSender sender, CompletableFuture<T> future, String success) {
        future.whenComplete((value, error) -> Utils.runLater(() -> {
            if (error == null) sender.sendMessage(Utils.fromString(success));
            else error(sender, error);
        }));
    }
}
