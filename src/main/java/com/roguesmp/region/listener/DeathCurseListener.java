package com.roguesmp.region.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.roguesmp.effect.impl.CurseOfDeathEffect;
import com.roguesmp.region.SmpRegionFlags;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DeathCurseListener implements Listener {

    @EventHandler
    public void onRespawn(PlayerPostRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player.getLastDeathLocation();
        if (deathLocation == null || !deathLocation.isWorldLoaded()) return;

        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        if (query.testState(BukkitAdapter.adapt(deathLocation), WorldGuardPlugin.inst().wrapPlayer(player), SmpRegionFlags.DEATH_CURSE)) {
            int stacks = CurseOfDeathEffect.addStack(player);
            player.sendMessage(Component.text("Bạn đang phải chịu ☠ Lời Nguyền Tử Thần (" + stacks + "/" + CurseOfDeathEffect.MAX_STACKS
                    + " cộng dồn) - hãy tìm cách giải!", NamedTextColor.DARK_RED));
            player.sendActionBar(Component.text("☠ Lời Nguyền Tử Thần (" + stacks + "/" + CurseOfDeathEffect.MAX_STACKS
                    + " cộng dồn) - hãy tìm cách giải!", NamedTextColor.DARK_RED));
        }
    }
}
