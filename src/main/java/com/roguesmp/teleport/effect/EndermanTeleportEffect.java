package com.roguesmp.teleport.effect;

import com.roguesmp.codec.Codec;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public record EndermanTeleportEffect(float departPitch, float arrivePitch, int slowness, int darkness) implements TeleportEffect {

    public static final String TYPE_KEY = "enderman";
    public static final EndermanTeleportEffect DEFAULT = new EndermanTeleportEffect(0.6f, 1.4f, 2, 1);

    public static final Codec<EndermanTeleportEffect> CODEC = Codec.composite(
            Codec.FLOAT.optionalFieldOf("depart_pitch", DEFAULT.departPitch()).forGetter(EndermanTeleportEffect::departPitch),
            Codec.FLOAT.optionalFieldOf("arrive_pitch", DEFAULT.arrivePitch()).forGetter(EndermanTeleportEffect::arrivePitch),
            Codec.INT.optionalFieldOf("slowness", DEFAULT.slowness()).forGetter(EndermanTeleportEffect::slowness),
            Codec.INT.optionalFieldOf("darkness", DEFAULT.darkness()).forGetter(EndermanTeleportEffect::darkness),
            EndermanTeleportEffect::new
    );

    private static final Key SOUND = Key.key("entity.enderman.teleport");
    private static final int EFFECT_LINGER = 20;

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void onWarmupStart(Player player, int warmupTicks) {
        int duration = warmupTicks + EFFECT_LINGER;
        if (slowness >= 0) player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, slowness, false, false, false));
        if (darkness >= 0) player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, darkness, false, false, false));
        player.playSound(Sound.sound(SOUND, Sound.Source.PLAYER, 1f, departPitch));
    }

    @Override
    public void onFailed(Player player) {
        removeOwn(player, PotionEffectType.SLOWNESS, slowness);
        removeOwn(player, PotionEffectType.DARKNESS, darkness);
    }

    private static void removeOwn(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getAmplifier() == amplifier && !current.hasParticles() && !current.hasIcon()) player.removePotionEffect(type);
    }

    @Override
    public void onArrive(Player player) {
        player.playSound(Sound.sound(SOUND, Sound.Source.PLAYER, 1f, arrivePitch));
    }
}
