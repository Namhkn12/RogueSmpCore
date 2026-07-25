package com.roguesmp.registry;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SkinRegistry {

    private static SkinRegistry INSTANCE;

    public static final class SkinData {
        private final String value;
        private final String signature;
        private final UUID uuid;

        public static final Codec<SkinData> CODEC = Codec.composite(
                Codec.STRING.fieldOf("value").forGetter(SkinData::value),
                Codec.STRING.fieldOf("signature").forGetter(SkinData::signature),
                Codec.UUID.optionalFieldOf("uuid", UUID::randomUUID).forGetter(SkinData::getUuid),
                SkinData::new
        );

        @GsonIgnore
        private PlayerProfile profile;

        /**
         * Create a new SkinData, will also generate the uuid and profile for this skin
         */
        public SkinData(String value, String signature, UUID uuid) {
            this.value = value;
            this.signature = signature;

            this.uuid = uuid;
            this.profile = Bukkit.createProfile(uuid, null);
            profile.setProperty(new ProfileProperty("textures", value, signature));
        }

        public PlayerProfile getProfile() {
            if (profile == null) {
                this.profile = Bukkit.createProfile(getUuid(), null);
                this.profile.setProperty(new ProfileProperty("textures", value, signature));
            }
            return profile;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String value() {
            return value;
        }

        public String signature() {
            return signature;
        }
    }

    public void fetchAndRegisterSkin(String uuid, String skinId, BiConsumer<String, SkinData> onSuccess, Consumer<String> onError) {
        String apiUrl = "https://api.mineskin.org/v2/skins/" + uuid;
        String apiKey = RogueSmpCore.getGlobalConfig().getMineskinApiKey();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/json")
                .header("User-Agent", "RogueSmpCore/1.0")
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    JsonObject json = Utils.GSON.fromJson(body, JsonObject.class);

                    if (json != null && json.has("skin")) {
                        JsonObject textureData = json.getAsJsonObject("skin")
                                .getAsJsonObject("texture")
                                .getAsJsonObject("data");

                        SkinData skinData = new SkinData(
                                textureData.get("value").getAsString(),
                                textureData.get("signature").getAsString(),
                                UUID.randomUUID()
                        );

                        onSuccess.accept(skinId, skinData);

                        // Register and Save
                        Registries.SKIN_DATA.registerAndSave(RogueSmpCore.getInstance(), skinId, skinData);

                    } else {
                        String msg = (json != null && json.has("message"))
                                ? json.get("message").getAsString()
                                : "Unknown API Error";
                        onError.accept(msg);
                    }
                })
                .exceptionally(ex -> {
                    onError.accept(ex.getMessage());
                    return null;
                });
    }

    /**
     * Return null if id is not found
     */
    public @Nullable ItemStack getHead(String id) {
        SkinData skin = getSkin(id);

        if (skin == null) return null;
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.setData(DataComponentTypes.CUSTOM_NAME, Component.text(id).decoration(TextDecoration.ITALIC, false));

        item.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(skin.getProfile()));

        return item;
    }

    public @Nullable SkinData getSkin(String id) {
        return Registries.SKIN_DATA.get(id);
    }

    public Set<String> getSkinIds() {
        return Registries.SKIN_DATA.getAll().keySet();
    }

    public static void registerSkinFetchCommand() {
        new CommandAPICommand("skinfetch")
                .withArguments(new StringArgument("mineskin_uuid"))
                .withArguments(new StringArgument("id"))
                .executes((sender, args) -> {
                    String url = (String) args.get("mineskin_uuid");
                    String name = (String) args.get("id");

                    sender.sendMessage(Utils.fromString("<yellow>Đang lượm skin: <white>" + url));

                    // async
                    SkinRegistry.getInstance().fetchAndRegisterSkin(
                            url,
                            name,
                            (id, data) -> {
                                sender.sendMessage(Utils.fromString("<green>Lượm skin thành công, id: <white>" + id));
                            },
                            (error) -> {
                                sender.sendMessage(Utils.fromString("<red>Lỗi: " + error));
                            }
                    );
                })
                .register();
    }

    public static void init() {
        INSTANCE = new SkinRegistry();
    }

    public static SkinRegistry getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SkinRegistry is null!");
        }
        return INSTANCE;
    }
}
