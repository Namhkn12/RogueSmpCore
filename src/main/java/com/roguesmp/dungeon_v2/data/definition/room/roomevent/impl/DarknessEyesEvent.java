package com.roguesmp.dungeon_v2.data.definition.room.roomevent.impl;

import com.roguesmp.dungeon_v2.data.definition.room.roomevent.BaseRoomEvent;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/*
* Example json config
* {
  "type": "dark_eyes",
  "params": {
    "isDisrupt": true
  }
}
* */
public class DarknessEyesEvent extends BaseRoomEvent {

    public static final String TYPE = "dark_eyes";

    private boolean isDisrupt;
    private static final int duration= 40;
    private static final int interval = 20;

    public void setDisrupt(boolean disrupt) {
        isDisrupt = disrupt;
    }

    @Override
    protected void onStartInternal() {
        if (context == null || context.getParty() == null || context.getPartyService() == null) return;
        context.getPartyService().getOnlineMembers(context.getParty()).forEach(player -> {
            DungeonEcho.warn(player, "The darkness is watching you...");
        });
    }

    @Override
    protected void onPlayInternal() {
        if (context == null || context.getParty() == null || context.getPartyService() == null) return;

        boolean shouldBlind;

        if (isDisrupt) {
            shouldBlind = (tickCount % interval < 10) || (tickCount % interval > 15);
        } else {
            shouldBlind = true;
        }

        context.getPartyService().getOnlineMembers(context.getParty()).forEach(player -> {
            if (shouldBlind) {
                applyBlindness(player);
            } else {
                removeBlindness(player);
            }
        });
    }

    @Override
    protected void onEndInternal() {
        if (context == null || context.getParty() == null || context.getPartyService() == null) return;

        context.getPartyService().getOnlineMembers(context.getParty()).forEach(this::removeBlindness);
    }

    @Override
    protected String getType() {
        return TYPE;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new java.util.LinkedHashMap<>(super.serialize());
        data.put("isDisrupt", isDisrupt);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.isDisrupt = data.get("isDisrupt") instanceof Boolean b && b;
    }

    private void applyBlindness(Player player) {
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        duration,
                        0,
                        false,
                        false,
                        false
                )
        );
    }

    private void removeBlindness(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}
