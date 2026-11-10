package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.world.Hub;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class InHubObjective extends StateObjective {

    public static final String TYPE_KEY = "in_hub";
    public static final InHubObjective INSTANCE = new InHubObjective();

    public static final Codec<InHubObjective> CODEC = MapCodec.unit(() -> INSTANCE).codec();

    @Override
    protected boolean test(SmpPlayer smpPlayer) {
        Player player = smpPlayer.getBukkitPlayer();
        Location hub = Hub.spawn();
        return player != null && player.getWorld().getKey().equals(hub.getWorld().getKey());
    }

    @Override
    protected String label() {
        return "Đến Hub";
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }
}
