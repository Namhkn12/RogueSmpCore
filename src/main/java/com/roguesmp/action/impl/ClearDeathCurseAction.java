package com.roguesmp.action.impl;

import com.roguesmp.action.ActionContext;
import com.roguesmp.action.PlayerAction;
import com.roguesmp.codec.Codec;
import com.roguesmp.effect.impl.CurseOfDeathEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public record ClearDeathCurseAction() implements PlayerAction {

    public static final String TYPE_KEY = "clear_death_curse";
    public static final Codec<ClearDeathCurseAction> CODEC = Codec.unit(ClearDeathCurseAction::new);

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public boolean run(ActionContext context) {
        if (!CurseOfDeathEffect.clear(context.player())) {
            context.player().sendMessage(Component.text("Bạn không mang Lời Nguyền Tử Thần.", NamedTextColor.GRAY));
            return false;
        }
        context.player().sendMessage(Component.text("Lời Nguyền Tử Thần đã được hóa giải.", NamedTextColor.GREEN));
        playCleanseFx(context.player());
        return true;
    }

    private static void playCleanseFx(Player player) {
        Location center = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 60, 0.4, 0.6, 0.4, 0.35);
        world.spawnParticle(Particle.SOUL, center, 20, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticle(Particle.SMOKE, center, 25, 0.3, 0.5, 0.3, 0.02);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            world.spawnParticle(Particle.END_ROD, center.clone().add(Math.cos(angle) * 1.2, -0.9, Math.sin(angle) * 1.2), 1, 0, 0.6, 0, 0.08);
        }

        world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 1.4f);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1f, 1.2f);
        world.playSound(center, Sound.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 0.6f, 0.6f);
    }
}
