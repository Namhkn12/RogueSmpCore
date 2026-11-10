package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Momentum implements SmpEnchant {

    private static final int MAX_STACKS = 5;
    private static final long STACK_WINDOW_MILLIS = 2000;
    private static final double BONUS_PER_STACK_PER_LEVEL = 0.04;

    private final Map<UUID, Combo> combos = new ConcurrentHashMap<>();

    @Override
    public @NotNull String getId() {
        return "momentum";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.MOMENTUM;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Liên Hoàn";
    }

    @Override
    public @NotNull List<Component> getDescription(int level) {
        String bonus = level > 0 ? Math.round(BONUS_PER_STACK_PER_LEVEL * level * 100) + "%" : "4% mỗi cấp";
        return List.of(
                Component.text("Đánh liên tiếp cùng một mục tiêu trong 2 giây"),
                Component.text("tích 1 tầng (tối đa " + MAX_STACKS + ")."),
                Utils.fromString("Mỗi tầng tăng <red>" + bonus + "</red> sát thương cận chiến.")
        );
    }

    @Override
    public Material getIcon() {
        return Material.BLAZE_POWDER;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (!event.isBasic(DamageType.MELEE)) return;
        if (player.getAttackStrength() <= 0.96) return;

        long now = System.currentTimeMillis();
        UUID target = event.getVictim().getUniqueId();
        Combo previous = combos.get(player.getUuid());
        int stacks = previous != null && previous.target().equals(target) && now - previous.lastHit() <= STACK_WINDOW_MILLIS
                ? Math.min(previous.stacks() + 1, MAX_STACKS)
                : 0;
        combos.put(player.getUuid(), new Combo(target, stacks, now));

        if (stacks > 0) {
            event.addDamageModifier(BONUS_PER_STACK_PER_LEVEL * level * stacks, DamageOperation.INCREASE_BASE);
        }
    }

    @Override
    public void onEquipmentChange(@NotNull SmpPlayer player, int level) {
        if (level == 0) combos.remove(player.getUuid());
    }

    private record Combo(UUID target, int stacks, long lastHit) {
    }
}
