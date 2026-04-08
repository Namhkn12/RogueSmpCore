package com.roguesmp.dungeon_v2.data.definition.room.roomevent.impl;

import com.roguesmp.dungeon_v2.data.definition.room.roomevent.BaseRoomEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class PartyStrengthBuffRoomEvent extends BaseRoomEvent {

    public static final String TYPE = "party_strength_buff";

    private int durationTicks = 20 * 60 * 3;
    private int amplifier = 1;

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    @Override
    protected String getType() {
        return TYPE;
    }

    @Override
    protected void onStartInternal() {
        if (context == null || context.getParty() == null || context.getPartyService() == null) return;
        context.getPartyService().getOnlineMembers(context.getParty()).forEach(player -> player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        durationTicks,
                        amplifier,
                        false,
                        true,
                        true
                )
        ));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new java.util.LinkedHashMap<>(super.serialize());
        data.put("durationTicks", durationTicks);
        data.put("amplifier", amplifier);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.durationTicks = data.get("durationTicks") instanceof Number n ? n.intValue() : 20 * 60 * 3;
        this.amplifier = data.get("amplifier") instanceof Number n ? n.intValue() : 1;
    }
}
