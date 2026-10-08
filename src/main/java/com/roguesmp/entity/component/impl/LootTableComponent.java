package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.loot.LootTable;
import com.roguesmp.registry.Registries;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Rolls every table in {@code lootTableIds} on death and drops the combined results at the
 * entity's death location. Vanilla drops are already cleared upstream
 * ({@code EntityListener#onDeath}), so this is the only source of drops for an entity carrying it.
 */
public record LootTableComponent(List<String> lootTableIds) implements EntityComponent {

    public static final Codec<LootTableComponent> CODEC = Codec.listOf(Codec.STRING)
            .xmap(LootTableComponent::new, LootTableComponent::lootTableIds);

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }

    @Override
    public void onDeath(EntityDeathEvent event, SmpEntity entity) {
        LivingEntity livingEntity = event.getEntity();

        Player killer = livingEntity.getKiller();
        SmpPlayer smpPlayer = killer != null ? PlayerManager.getInstance().getSmpPlayer(killer) : null;

        LootContext context = LootContext.builder(smpPlayer)
                .origin(LootOrigin.ENTITY, livingEntity)
                .build();

        for (String lootTableId : lootTableIds) {
            LootTable table = Registries.LOOT_TABLE.get(lootTableId);
            if (table == null) continue;

            for (ItemStack drop : table.roll(context)) {
                livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), drop);
            }
        }
    }
}
