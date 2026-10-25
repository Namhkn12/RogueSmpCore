package com.roguesmp.region.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.SmpRegionFlags;
import com.roguesmp.region.event.RegionEnterEvent;
import com.roguesmp.region.flag.EnterTeleport;
import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RegionTeleportListener implements Listener {

    private static final int FX_DELAY = 20;
    private static final int FX_DURATION = 40;
    private static final Sound TELEPORT_SOUND = Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.PLAYER, 1f, 0.8f);

    private final Set<UUID> pending = new HashSet<>();

    @EventHandler
    public void onRegionEnter(RegionEnterEvent event) {
        ProtectedRegion settings = ServerRegionManager.getInstance().get(event.getWorld(), event.getRegionId());
        EnterTeleport teleport = settings == null ? null : settings.getFlag(SmpRegionFlags.ENTER_TELEPORT);
        if (teleport == null) return;

        Player player = event.getPlayer();
        if (!pending.add(player.getUniqueId())) return;

        if (teleport.fx()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, FX_DURATION, 1, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, FX_DURATION, 0, false, false, false));
            player.playSound(TELEPORT_SOUND);
        }

        Utils.runLater(() -> execute(player, teleport, event.getRegionId()), teleport.fx() ? FX_DELAY : 1);
    }

    private void execute(Player player, EnterTeleport teleport, String regionId) {
        pending.remove(player.getUniqueId());
        if (!player.isOnline()) return;

        Location target = teleport.target().resolve(player.getLocation());
        if (target == null) {
            RogueSmpCore.LOGGER.warn("Region {} has an enter teleport target that could not be resolved: {}", regionId, teleport.target().format());
            return;
        }
        player.teleportAsync(target);
    }
}
