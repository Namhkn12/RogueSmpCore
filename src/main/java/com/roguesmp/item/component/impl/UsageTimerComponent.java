package com.roguesmp.item.component.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.TickingComponent;
import com.roguesmp.item.component.UniqueTrackingComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Surely an item can't have time longer than Max int.
 */
public class UsageTimerComponent implements ItemComponent, TickingComponent, UniqueTrackingComponent {

    public static final Codec<UsageTimerComponent> CODEC = Codec.INT.xmap(UsageTimerComponent::new, UsageTimerComponent::getBaseTickDuration);

    private static final NamespacedKey KEY = Keys.of("usage_time_left");
    private static final int INTERVAL = 20; //Every second

    private final int baseTickDuration;
    private int tickTimeLeft;

    public UsageTimerComponent(int baseTickDuration) {
        this.baseTickDuration = baseTickDuration;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new UsageTimerComponent(baseTickDuration);
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Biến mất sau ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(Utils.convertSeconds(tickTimeLeft / 20), NamedTextColor.RED)).append(Component.text(" sử dụng", NamedTextColor.GRAY)));
        lines.add(Component.text("  Chỉ tính giờ khi đang trang bị", NamedTextColor.DARK_GRAY));
        context.builder().putLines(10, lines);
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        Integer savedTickTime = pdc.get(KEY, PersistentDataType.INTEGER);
        if (savedTickTime == null) {
            tickTimeLeft = baseTickDuration;
        } else tickTimeLeft = savedTickTime;
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(KEY, PersistentDataType.INTEGER, tickTimeLeft);
    }

    @Override
    public void tick(SmpPlayer player, SmpItem item, EquipSlot equipSlot, int interval) {
        if (Bukkit.getCurrentTick() % INTERVAL != 0) return;
        Player bukkitPlayer = player.getBukkitPlayer();
        if (equipSlot == EquipSlot.PROJECTILE) {
            RogueSmpCore.LOGGER.warn("Somehow is ticking Projectile slot at UsageTimerComponent!");
            return;
        }
        tickTimeLeft = tickTimeLeft - INTERVAL;
        ItemStack itemStack = bukkitPlayer.getEquipment().getItem(equipSlot.getVanillaSlot());
        itemStack.editPersistentDataContainer(this::save);

        if (tickTimeLeft <= 0) {
            bukkitPlayer.playSound(Sound.sound(SoundEventKeys.BLOCK_LAVA_EXTINGUISH, Sound.Source.PLAYER, 1f, 0.8f));
            bukkitPlayer.sendMessage(Component.text("Vật phẩm của bạn đã hết thời hạn và tan thành khói bụi...", NamedTextColor.RED));
            itemStack.setAmount(0);
        }
    }

    public int getBaseTickDuration() {
        return baseTickDuration;
    }

    public int getTickTimeLeft() {
        return tickTimeLeft;
    }
}
