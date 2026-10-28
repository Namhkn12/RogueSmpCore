package com.roguesmp.action.impl;

import com.roguesmp.action.ActionContext;
import com.roguesmp.action.PlayerAction;
import com.roguesmp.codec.Codec;
import com.roguesmp.utils.MovementUtils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public record EntityZapperAction() implements PlayerAction {

    public static final String TYPE_KEY = "entity_zapper";
    public static final Codec<EntityZapperAction> CODEC = Codec.unit(EntityZapperAction::new);

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public boolean run(ActionContext context) {
        Entity target = context.target();
        if (target == null) return false;

        Player player = context.player();
        if (target instanceof Player targetPlayer) {
            player.sendMessage(Component.text("NO!", NamedTextColor.RED));
            player.playSound(Sound.sound(SoundEventKeys.ENTITY_WIND_CHARGE_WIND_BURST, Sound.Source.PLAYER, 1f, 1.2f));
            MovementUtils.knockAway(targetPlayer, player, 1f);
            return true;
        }
        target.getPassengers().forEach(Entity::remove);
        target.remove();
        player.sendMessage(Component.text("Úm ba la xì bùa", NamedTextColor.AQUA));
        player.spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 0.5, 0), 5);
        return true;
    }
}
