package com.roguesmp.player.mechanic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

// Removal is handled in SmpPlayer
public class ProjectileMechanic implements PlayerMechanic {

    @Override public int getPriority() { return 105; }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        event.getProjectile().setPersistent(false);
        PlayerProjectile playerProjectile = player.trackProjectile(event.getProjectile(), player.getActiveEnchants(), player.getActiveAttributes());
        playerProjectile.setReduceDurability(true);
        playerProjectile.onProjectileLaunch(event);
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        event.getProjectile().setPersistent(false);
        PlayerProjectile playerProjectile = player.trackProjectile((Projectile) event.getProjectile(), player.getActiveEnchants(), player.getActiveAttributes());
        playerProjectile.setReduceDurability(true);
        playerProjectile.onShootArrow(event);
    }

    @Override
    public void onDamageEntity(DamageEvent event, SmpPlayer player) {
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            PlayerProjectile playerProjectile = player.getProjectile(projectile.getUniqueId());
            if (playerProjectile != null) playerProjectile.onDamageEntity(event);
        }
    }

    @Override
    public void onCombustEntity(EntityCombustByEntityEvent event, SmpPlayer player) {
        if (event.getCombuster() instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            PlayerProjectile playerProjectile = player.getProjectile(event.getEntity().getUniqueId());
            if (playerProjectile != null) playerProjectile.onCombustEntity(event);
        }
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event, SmpPlayer player) {
        PlayerProjectile playerProjectile = player.getProjectile(event.getEntity().getUniqueId());
        if (playerProjectile == null) return;

        playerProjectile.onProjectileHit(event);
        if (event.getHitBlock() != null) {
            Utils.runLater(() -> player.untrackProjectile(event.getEntity().getUniqueId()));
        }
    }
}
