package com.roguesmp.player.death;

import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public record LastHit(DamageEvent.Metadata metadata, @Nullable Entity killer, int tick) {
}
