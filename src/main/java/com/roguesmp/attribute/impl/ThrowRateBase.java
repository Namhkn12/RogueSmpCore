package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThrowRateBase implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "throw_rate_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.THROW_RATE_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc ném";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(" "+ Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event, double value, @NotNull SmpPlayer player) {
        Projectile projectile = event.getEntity();

        if (EntityManager.getInstance().hasMetadata(projectile, "throw_rate_guard")) {
            return;
        }

        event.setCancelled(true);
        PlayerProjectile playerProjectile = player.getProjectile(projectile.getUniqueId());
        if (playerProjectile != null) {
            double bonus = playerProjectile.getActiveAttributes().getOrDefault(Attributes.THROW_RATE_PERCENT, 0d);
            value = value * (1 + bonus);
        }

        int cooldown = (int) (20 / value);

        if (projectile instanceof ThrowableProjectile) {
            Player bukkitPlayer = player.getBukkitPlayer();
            if (bukkitPlayer != null) {
                bukkitPlayer.launchProjectile(projectile.getClass(), projectile.getVelocity(), p -> {
                    ThrowableProjectile throwableProjectile = (ThrowableProjectile) p;
                    throwableProjectile.setItem(((ThrowableProjectile) projectile).getItem());
                    EntityManager.getInstance().addMetadata(p, "throw_rate_guard", true);
                    if (throwableProjectile instanceof Trident trident) {
                        trident.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                    }
                });

                ItemStack projectileItem = bukkitPlayer.getEquipment().getItemInMainHand();
                bukkitPlayer.setCooldown(projectileItem.getType(), cooldown);
                if (projectile instanceof Trident) {
                    bukkitPlayer.playSound(bukkitPlayer, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS,1f, 1f);
                    ItemStackUtils.damageItem(projectileItem, 1); //Damage as if player thrown it normally

                    Utils.runLater(() -> bukkitPlayer.playSound(bukkitPlayer, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS,1f, 1f), cooldown);
                } else bukkitPlayer.playSound(bukkitPlayer, Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS,1f, 0.5f);
            }
        }

    }
}
