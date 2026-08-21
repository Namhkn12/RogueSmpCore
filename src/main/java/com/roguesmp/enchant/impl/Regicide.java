package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.EntityUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Regicide implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "regicide";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.REGICIDE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Diệt vương";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND, EquipSlot.PROJECTILE);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng sát thương gây ra cho quái Elite và Boss thêm 10% mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.WITHER_SKELETON_SKULL;
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        DamageType damageType = event.getDamageType();
        if (!DamageType.isRegicideAffected(damageType)) return;
        Entity victim = event.getVictim();
        if (!EntityUtils.isElite(victim) && !EntityUtils.isBoss(victim)) return;
        event.addDamageModifier(level * 0.1, DamageOperation.INCREASE_BASE);
    }
}
