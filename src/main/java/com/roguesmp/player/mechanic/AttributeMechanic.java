package com.roguesmp.player.mechanic;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AttributeMechanic implements PlayerMechanic {

    // Owned per-player (one AttributeMechanic per SmpPlayer) - the value each attribute was
    // resolved at as of the last onEquipmentChange call, so a fresh diff against
    // player.getActiveAttributes() tells us exactly which attributes actually changed this time.
    private final Map<Attributes, Double> lastKnownValues = new EnumMap<>(Attributes.class);

    @Override
    public int getPriority() {
        return 100; // Core Layer
    }

    @Override
    public void tick(int periodIncrement, SmpPlayer player) {
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().tick(player, periodIncrement, aDouble)
        );
    }

    @Override
    public void onDamageEntity(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled() || !(event.getDamager() instanceof Player)) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onDamageEntity(event, aDouble, player)
        );
    }

    @Override
    public void onDamageEntityFinal(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled() || !(event.getDamager() instanceof Player)) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onDamageEntityFinal(event, aDouble, player)
        );
    }

    @Override
    public void onKillEntity(EntityDeathEvent event, SmpPlayer player) {
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onKillEntity(event, aDouble, player)
        );
    }

    @Override
    public void onHurt(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onHurt(event, aDouble, player)
        );
    }

    @Override
    public void onHurtFinal(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onHurtFinal(event, aDouble, player)
        );
    }

    @Override
    public void onHurtFatal(DamageEvent event, SmpPlayer player) {
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onHurtFatal(event, aDouble, player)
        );
    }

    @Override
    public void onAbilityCast(AbilityCastEvent event, SmpPlayer player) {
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onAbilityCast(event, aDouble, player)
        );
    }

    @Override
    public void onConsume(PlayerItemConsumeEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onConsume(event, aDouble, player)
        );
    }

    @Override
    public void onExpChange(PlayerExpChangeEvent event, SmpPlayer player) {
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onExpChange(event, aDouble, player)
        );
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onBlockBreak(event, aDouble, player)
        );
    }

    @Override
    public void onCombust(EntityCombustEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onCombust(event, aDouble, player)
        );
    }

    @Override
    public void onCombustEntity(EntityCombustByEntityEvent event, SmpPlayer player) {
        if (event.isCancelled() || !(event.getCombuster() instanceof Player)) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onCombustEntity(event, aDouble, player)
        );
    }

    @Override
    public void onConsumeArrow(ArrowConsumeEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onConsumeArrow(event, aDouble, player)
        );
    }

    @Override
    public void onEquipmentChange(EquipSlot slot, @Nullable SmpItem newItem, SmpPlayer player) {
        Map<Attributes, Double> current = player.getActiveAttributes();

        Set<Attributes> touched = new HashSet<>(lastKnownValues.keySet());
        touched.addAll(current.keySet());
        for (Attributes attribute : touched) {
            double before = lastKnownValues.getOrDefault(attribute, 0d);
            double after = current.getOrDefault(attribute, 0d);
            if (!Utils.isEffectiveZero(before - after)) {
                attribute.getAttribute().onEquipmentChange(player, after);
            }
        }

        lastKnownValues.clear();
        lastKnownValues.putAll(current);
    }
}
