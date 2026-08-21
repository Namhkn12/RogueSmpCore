package com.roguesmp.item.ability.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Every {@code interval} ticks (checked via {@code Bukkit.getCurrentTick() % interval == 0}, no
 * per-instance cooldown state needed), plays one randomly-picked sound from {@link #sounds} to the
 * equipper only - {@code Player#playSound(Sound)} sends the sound packet directly to that player
 * rather than broadcasting from a world location, so nobody else hears it.
 */
public class Barking implements ItemAbility {

    public static final String TYPE_KEY = "barking";

    public static final Codec<Barking> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("interval", 200).forGetter(Barking::getInterval),
            Codec.listOf(SoundEntry.CODEC).optionalFieldOf("sounds", List.of()).forGetter(Barking::getSounds),
            Barking::new
    );

    private final int interval;
    private final List<SoundEntry> sounds;

    public Barking(int interval, List<SoundEntry> sounds) {
        this.interval = Math.max(1, interval);
        this.sounds = List.copyOf(sounds);
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public List<Component> getDisplay(SmpPlayer player, SmpItem smpItem) {
        return List.of(Component.text("Thỉnh thoảng phát ra âm thanh kì lạ...", NamedTextColor.GRAY));
    }

    @Override
    public String getSimpleDescription() {
        return "Mỗi X tick, phát ngẫu nhiên 1 âm thanh trong danh sách, chỉ người trang bị nghe được";
    }

    @Override
    public void onTick(SmpPlayer player, SmpItem item, int interval) {
        if (sounds.isEmpty()) return;
        if (Bukkit.getCurrentTick() % this.interval != 0) return;

        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;

        SoundEntry entry = sounds.get(Utils.RANDOM.nextInt(sounds.size()));
        bukkitPlayer.playSound(Sound.sound(entry.key(), Sound.Source.PLAYER, entry.volume(), entry.pitch()));
    }

    public int getInterval() {
        return interval;
    }

    public List<SoundEntry> getSounds() {
        return sounds;
    }

    public record SoundEntry(Key key, float volume, float pitch) {
        public static final Codec<SoundEntry> CODEC = Codec.composite(
                Codec.KEY.fieldOf("key").forGetter(SoundEntry::key),
                Codec.FLOAT.optionalFieldOf("volume", 1f).forGetter(SoundEntry::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1f).forGetter(SoundEntry::pitch),
                SoundEntry::new
        );
    }
}
