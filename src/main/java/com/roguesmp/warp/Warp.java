package com.roguesmp.warp;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportOptions;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record Warp(String displayName, Key icon, List<String> lore, TeleportDestination destination,
                   TeleportOptions options, @Nullable String permission, int order) {

    public static final Key DEFAULT_ICON = Material.ENDER_PEARL.key();

    public static final Codec<Warp> CODEC = Codec.composite(
            Codec.STRING.fieldOf("display_name").forGetter(Warp::displayName),
            Codec.KEY.optionalFieldOf("icon", DEFAULT_ICON).forGetter(Warp::icon),
            Codec.listOf(Codec.STRING).optionalFieldOf("lore", List.of()).forGetter(Warp::lore),
            TeleportDestination.CODEC.fieldOf("destination").forGetter(Warp::destination),
            TeleportOptions.CODEC.optionalFieldOf("options", TeleportOptions.DEFAULT).forGetter(Warp::options),
            Codec.STRING.optionalFieldOf("permission").forGetter(warp -> Optional.ofNullable(warp.permission())),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Warp::order),
            (displayName, icon, lore, destination, options, permission, order) ->
                    new Warp(displayName, icon, lore, destination, options, permission.orElse(null), order)
    );

    public static Warp create(String id, TeleportDestination destination) {
        return new Warp("<aqua>" + id, DEFAULT_ICON, List.of(), destination, TeleportOptions.DEFAULT, null, 0);
    }

    public ItemStack createIcon() {
        ItemStack stack = ItemStack.of(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, icon);
        return stack;
    }

    public boolean canUse(Permissible permissible) {
        return permission == null || permissible.hasPermission(permission);
    }

    public Warp withDisplayName(String displayName) {
        return new Warp(displayName, icon, lore, destination, options, permission, order);
    }

    public Warp withIcon(Key icon) {
        return new Warp(displayName, icon, lore, destination, options, permission, order);
    }

    public Warp withLore(List<String> lore) {
        return new Warp(displayName, icon, lore, destination, options, permission, order);
    }

    public Warp withDestination(TeleportDestination destination) {
        return new Warp(displayName, icon, lore, destination, options, permission, order);
    }

    public Warp withOptions(TeleportOptions options) {
        return new Warp(displayName, icon, lore, destination, options, permission, order);
    }

    public Warp withPermission(@Nullable String permission) {
        return new Warp(displayName, icon, lore, destination, options, permission, order);
    }

    public Warp withOrder(int order) {
        return new Warp(displayName, icon, lore, destination, options, permission, order);
    }
}
