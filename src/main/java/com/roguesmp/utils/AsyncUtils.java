package com.roguesmp.utils;

import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class AsyncUtils {

    private AsyncUtils() {
    }

    /**
     * Runs the task on Bukkit's async scheduler, so it is safe for blocking IO.
     * Completes exceptionally if the task throws.
     */
    public static <T> CompletableFuture<T> supply(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Utils.runAsync(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Same as {@link #supply} but for a task with no result.
     */
    public static CompletableFuture<Void> run(Runnable task) {
        return supply(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Invokes the action right away when already on the main thread, otherwise on the next tick.
     * Use it to run Bukkit calls from async continuations; the action itself must not block.
     */
    public static <T> CompletableFuture<T> onMainThread(Supplier<CompletableFuture<T>> action) {
        if (Bukkit.isPrimaryThread()) return invoke(action);

        CompletableFuture<T> result = new CompletableFuture<>();
        Utils.runLater(() -> invoke(action).whenComplete((value, error) -> {
            if (error != null) result.completeExceptionally(error);
            else result.complete(value);
        }));
        return result;
    }

    private static <T> CompletableFuture<T> invoke(Supplier<CompletableFuture<T>> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
