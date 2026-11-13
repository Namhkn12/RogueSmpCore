package com.roguesmp.server;

import com.roguesmp.RogueSmpCore;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResourcePackManager implements Listener {

    private static final long LOAD_TIMEOUT_SECONDS = 180L;
    private static final Component PROMPT = Component.text("Server bắt buộc cần Resource pack, không chấp nhận là bị đá!");
    private static final Component DECLINED = Component.text("Bạn cần chấp nhận Resource pack để vào server!");
    private static final Component TIMED_OUT = Component.text("Tải Resource pack quá lâu, vui lòng thử lại!");

    private final Map<UUID, CompletableFuture<Boolean>> pending = new ConcurrentHashMap<>();

    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID uuid = connection.getProfile().getId();
        if (uuid == null) return;

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        pending.put(uuid, result);
        try {
            connection.getAudience().sendResourcePacks(buildRequest(result));
            if (!result.get(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) connection.disconnect(DECLINED);
        } catch (TimeoutException e) {
            connection.disconnect(TIMED_OUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            RogueSmpCore.LOGGER.error("Resource pack wait failed for {}", uuid, e);
        } finally {
            pending.remove(uuid, result);
        }
    }

    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        CompletableFuture<Boolean> result = pending.remove(event.getPlayerUniqueId());
        if (result != null) result.complete(false);
    }

    private static ResourcePackRequest buildRequest(CompletableFuture<Boolean> result) {
        ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(RogueSmpCore.getGlobalConfig().getResourcePackUrl()))
                .hash(RogueSmpCore.getGlobalConfig().getResourcePackHash())
                .build();

        return ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .prompt(PROMPT)
                .required(true)
                .callback(ResourcePackCallback.onTerminal(
                        (id, audience) -> result.complete(true),
                        (id, audience) -> result.complete(false)))
                .build();
    }
}
