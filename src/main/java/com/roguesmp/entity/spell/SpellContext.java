package com.roguesmp.entity.spell;

import com.roguesmp.entity.SmpEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public record SpellContext(SmpEntity caster, @Nullable Player target, long tick) {
}
