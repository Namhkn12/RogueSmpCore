package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Channeling implements SmpEnchant {

    public static double DMG_PER_LEVEL = 10;

    @Override
    public @NotNull String getId() {
        return "channeling";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.CHANNELING;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Chớp điện";
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Giáng sấm sét xuống kẻ địch bị trúng tên, gây " + Utils.formatDecimal(DMG_PER_LEVEL) + " sát thương phép mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.TRIDENT;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event, int level, @NotNull SmpPlayer player) {
        World world = player.getBukkitPlayer().getWorld();

        Entity hitEntity = event.getHitEntity();
        if (hitEntity == null) return;

        world.strikeLightningEffect(hitEntity.getLocation());
        if (hitEntity instanceof LivingEntity living) {
            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.MAGIC);
            metadata.setIgnoreIframe(true);
            DamageUtils.damage(living, player.getBukkitPlayer(), DMG_PER_LEVEL * level, metadata);
        }

    }
}
