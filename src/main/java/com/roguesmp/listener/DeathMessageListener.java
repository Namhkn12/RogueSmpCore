package com.roguesmp.listener;

import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.death.DeathMessage;
import com.roguesmp.player.death.LastHit;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeathMessageListener implements Listener {
    private static final int MAX_HIT_AGE_TICKS = 1;
    private static final int KILLER_ARGUMENT = 1;

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
        boolean fresh = hit != null && Bukkit.getCurrentTick() - hit.tick() <= MAX_HIT_AGE_TICKS;

        DeathMessage message = fresh ? findMessage(hit) : null;
        if (message != null) {
            event.deathMessage(message.render(player.name(), killerName(hit.killer())));
            return;
        }
        renameVanillaKiller(event, event.getDamageSource().getCausingEntity());
    }

    private static void renameVanillaKiller(PlayerDeathEvent event, @Nullable Entity killer) {
        if (!(killer instanceof LivingEntity living)) return;
        SmpEntity smpEntity = EntityManager.getInstance().getSmpEntity(living);
        if (smpEntity == null || !(event.deathMessage() instanceof TranslatableComponent vanilla)) return;

        List<TranslationArgument> arguments = new ArrayList<>(vanilla.arguments());
        if (arguments.size() <= KILLER_ARGUMENT) return;
        arguments.set(KILLER_ARGUMENT, TranslationArgument.component(smpEntity.displayName()));
        event.deathMessage(vanilla.arguments(arguments));
    }

    private static Component killerName(@Nullable Entity killer) {
        if (killer == null) return Component.text("???");
        if (killer instanceof LivingEntity living) {
            SmpEntity smpEntity = EntityManager.getInstance().getSmpEntity(living);
            if (smpEntity != null) return smpEntity.displayName();
        }
        return killer.name();
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
