package com.roguesmp.listener;

import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.death.DeathMessage;
import com.roguesmp.player.death.LastHit;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;

public class DeathMessageListener implements Listener {
    private static final int MAX_HIT_AGE_TICKS = 1;

    private final PlayerManager playerManager;

    public DeathMessageListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player);
        if (smpPlayer == null) return;

        LastHit hit = smpPlayer.getLastHit();
        smpPlayer.setLastHit(null);
        if (hit == null || Bukkit.getCurrentTick() - hit.tick() > MAX_HIT_AGE_TICKS) return;

        DeathMessage message = findMessage(hit);
        if (message == null) return;

        Component killer = hit.killer() != null ? hit.killer().name() : Component.text("???");
        event.deathMessage(message.render(player.name(), killer));
    }

    private static @Nullable DeathMessage findMessage(LastHit hit) {
        String spellId = hit.metadata().getMobSpellId();
        if (spellId != null) {
            DeathMessage message = Registries.DEATH_MESSAGE.get("spells/" + spellId);
            if (message != null) return message;
        }
        String abilityId = hit.metadata().getAbilityId();
        return abilityId != null ? Registries.DEATH_MESSAGE.get("abilities/" + abilityId) : null;
    }
}
