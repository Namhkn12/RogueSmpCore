package com.roguesmp.enchant.impl;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * While active, keeps the player under a permanent vanilla {@link PotionEffectType#GLOWING} -
 * applied/removed via {@link #onEquipmentChange}, not {@code onDamageEntity}/{@code tick}, so it
 * only touches the potion effect when the enchant's active level actually changes rather than
 * reapplying it constantly.
 */
public class Glowing implements SmpEnchant {

    private static final String SOURCE = "enchant/glowing";
    private static final SmpEffect.BaseProperties GLOWING = new SmpEffect.BaseProperties(
            SmpEffect.INFINITE, SmpEffect.DeathBehavior.KEEP_ON_DEATH, SmpEffect.DisplayMode.WITHOUT_TIME, false);

    @Override
    public @NotNull String getId() {
        return "glowing";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.GLOWING;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Phát sáng";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(getSimpleName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Người mang sẽ phát sáng khi trang bị";
    }

    @Override
    public Material getIcon() {
        return Material.GLOW_INK_SAC;
    }

    @Override
    public void onEquipmentChange(@NotNull SmpPlayer player, int level) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;

        EffectManager effectManager = EffectManager.getInstance();
        effectManager.clearEffects(bukkitPlayer, SOURCE);
        if (level > 0) effectManager.addEffect(bukkitPlayer, SOURCE, new VanillaPotionEffect(GLOWING, PotionEffectType.GLOWING, level - 1));
    }
}
