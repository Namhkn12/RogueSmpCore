package com.roguesmp.item.ability.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.MovementUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityZapper implements ItemAbility {

    public static final Codec<EntityZapper> CODEC = Codec.unit(EntityZapper::new);
    public static final String TYPE_KEY = "entity_zapper";

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @Nullable List<Component> getDisplay(SmpPlayer player, SmpItem smpItem) {
        return List.of(Utils.text("Chuột phải để xóa entity khỏi world.", NamedTextColor.RED, TextDecoration.BOLD));
    }

    @Override
    public String getSimpleDescription() {
        return "Vật phẩm có thể dùng để xóa entity khỏi world.";
    }

    @Override
    public void onInteractEntity(SmpPlayer player, SmpItem item, PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player player1) {
            event.getPlayer().sendMessage(Component.text("NO!", NamedTextColor.RED));
            event.getPlayer().playSound(Sound.sound(SoundEventKeys.ENTITY_WIND_CHARGE_WIND_BURST, Sound.Source.PLAYER, 1f, 1.2f));
            MovementUtils.knockAway(player1, event.getPlayer(), 1f);
            return;
        }
        event.getRightClicked().remove();
        player.sendMessage(Component.text("Úm ba la xì bùa", NamedTextColor.AQUA));
        player.getBukkitPlayer().spawnParticle(Particle.EXPLOSION, event.getRightClicked().getLocation().add(0, 0.5, 0), 5);
    }
}
