package com.roguesmp.menu;

import com.roguesmp.gui.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public record MenuEntry(int slot, Supplier<ItemStack> icon, Function<Player, @Nullable BaseGui> factory) {
}
