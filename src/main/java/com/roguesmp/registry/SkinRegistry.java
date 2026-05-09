package com.roguesmp.registry;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.annotation.GsonIgnore;
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
    private static final String FOLDER_NAME = "skins.json";

    private final HashMap<String, SkinData> data = new HashMap<>();

    public static final class SkinData {
        private final String value;
        private final String signature;

        @GsonIgnore
        private PlayerProfile profile;

        public SkinData(String value, String signature) {
            this.value = value;
            this.signature = signature;

            this.profile = Bukkit.createProfile(UUID.randomUUID(), null);
            profile.setProperty(new ProfileProperty("textures", value, signature));
        }

        public PlayerProfile getProfile() {
            if (profile == null) {
                this.profile = Bukkit.createProfile(UUID.randomUUID(), null);
                this.profile.setProperty(new ProfileProperty("textures", value, signature));
            }
            return profile;
        }

        public String value() {
            return value;
        }

        public String signature() {
            return signature;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (SkinData) obj;
            return Objects.equals(this.value, that.value) &&
                    Objects.equals(this.signature, that.signature);
        }

        @Override
        public String toString() {
            return "SkinData[" +
                    "value=" + value + ", " +
                    "signature=" + signature + ']';
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
                                textureData.get("signature").getAsString()
                        );

                        onSuccess.accept(skinId, skinData);

                        // Register and Save
                        registerSkin(skinId, skinData.value, skinData.signature);
                        saveSkin();

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

    public void loadSkin() {
        File file = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER_NAME);

        if (!file.exists()) {
            RogueSmpCore.LOGGER.info("There're no files for skins... Creating new");

            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            // Create an example entry so the user sees the format
            HashMap<String, SkinData> defaultData = new HashMap<>();
            defaultData.put("example", new SkinData("ewogICJ0aW1lc3RhbXAiIDogMTYyMjI1NTc5MzY4MSwKICAicHJvZmlsZUlkIiA6ICI2OTBkMDM2OGM2NTE0OGM5ODZjMzEwN2FjMmRjNjFlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ5emZyXzciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjIyNjBmNDAyZTA1OWM0NDFmMzE1MWUxZGUxNDJhZmVlNzZmOGI2ZDQ5YmM0ODY5YjliY2U0YzBiODc1Yzc3MyIKICAgIH0KICB9Cn0=",
                    "N3MpewM+OPw+cKgxnKM+KANsZ3CVh6GOSWQa0pyC/Jd2A9d7tKY9QZ7eLD7QqrDFgZ+6N5puAgQOCbXDklnv1K+X5Vz6UBW0ceQIs+H6wm8WEP3oSr849e1uSSmlTSaiXHN/9iEKVnw/LXsyB3EbbIaUEgdJuVrOVSUEybL1MnE/O5BBxuKlZJw3/dLzYnm4LOVpq5pnwb2h8XbzyOOZyiS+DK3Gm84zIgMT6rtTgIt2anz6+AQpvndHtT7qFRv1ptpkn6Q/zCjBXXww9z+ZsyHHbkkvI4oFSjl6ZZnxELdkzxKOWJrqOZQkrQ3b92c8216E3j6I3eC/DFR3BOlaYsAxR6sZxiQGgDt4olmGhT7TucUUqIrDJWpXlV8C5MRqp0fZlehKOgANnj7fq/bxPEUOhK9bOfPvI3u2LvyOezTuoVvXz3GqUhIb4U4cMukPhqZVR3OBuU/guQoVyE9SVD319Aa6NNqk36DmqkCFQo/rLNhn2rps3SzxxMDBJd4lfXC05sfrsd78Pg20yZOjNADmbSPHimbgKvsJTmNawW8vGZ8khfDwUyhlNEFfGK5sQgZpL1tsKqsfUoA//Hx5V78GjsvN5NnqqTscMZXhabyMif2rPO87OtZE0iTEqdW5eptcLhIoBpP6oleAn8CFqOa5X2nOLdo38cDjijUPL8A="));

            try (FileWriter writer = new FileWriter(file)) {
                Utils.GSON.toJson(defaultData, writer);
            } catch (IOException e) {
                RogueSmpCore.LOGGER.error("Could not create default {}!", FOLDER_NAME);
                e.printStackTrace();
                return;
            }
        }

        // Type token for Map<String, SkinData>
        Type type = new com.google.gson.reflect.TypeToken<HashMap<String, SkinData>>(){}.getType();

        try (Reader reader = new FileReader(file)) {
            // Deserialize the JSON map
            HashMap<String, SkinData> loadedData = Utils.GSON.fromJson(reader, type);

            if (loadedData != null) {
                data.clear();
                data.putAll(loadedData);
                RogueSmpCore.LOGGER.info("Loaded {} skins from {}", data.size(), FOLDER_NAME);
            }
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to load {}!", FOLDER_NAME);
            e.printStackTrace();
        }
    }

    public void saveSkin() {
        File file = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER_NAME);

        // Ensure the directory exists before writing
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            // Serialize the current 'data' map back to the file
            Utils.GSON.toJson(this.data, writer);
            RogueSmpCore.LOGGER.info("Successfully saved {} skins to {}", data.size(), FOLDER_NAME);
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to save {}!", FOLDER_NAME);
            e.printStackTrace();
        }
    }

    public ItemStack getHead(String id) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkinData skin = getSkin(id);

        if (skin == null) return item;
        item.setData(DataComponentTypes.CUSTOM_NAME, Component.text(id).decoration(TextDecoration.ITALIC, false));

        item.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(skin.getProfile()));

        return item;
    }

    public @Nullable SkinData getSkin(String id) {
        return data.get(id);
    }

    public Set<String> getSkinIds() {
        return data.keySet();
    }

    public void registerSkin(String id, String value, String signature) {
        this.data.put(id, new SkinData(value, signature));
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
