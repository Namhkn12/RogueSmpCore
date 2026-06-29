package com.roguesmp.player.mechanic;

import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class AttributeMechanic implements PlayerMechanic {

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
    public void onHurtFatal(DamageEvent event, SmpPlayer player) {
        player.getActiveAttributes().forEach((attributes, aDouble) ->
                attributes.getAttribute().onHurtFatal(event, aDouble, player)
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
}
