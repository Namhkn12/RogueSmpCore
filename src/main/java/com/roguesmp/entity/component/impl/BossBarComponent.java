package com.roguesmp.entity.component.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.TickingComponent;
import com.roguesmp.utils.EntityUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

/**
 * Renders a boss's health bar to nearby players - purely visual (viewer range, title, color, fog,
 * progress). Self-sufficient via {@link TickingComponent}: computes its own health ratio and
 * refreshes the bar every shared tick, no external driver needed. Health-threshold phase triggers
 * are a separate {@link PhaseComponent} that this has no dependency on - when a threshold crossing
 * forces the entity's real HP to a specific value, this just reads that already-updated
 * {@code entity.getHealth()} on its own next tick like any other health change.
 * <p>
 * JSON-declarable via {@link #CODEC} (range/color/style/bossFog, centered on the entity itself -
 * the bar is built lazily in {@link #onSpawn}). Code-driven bosses that need a custom center
 * location instead use {@link #BossBarComponent(LivingEntity, int, BossBar.Color, BossBar.Overlay, boolean, Function)},
 * which builds immediately since no {@code onSpawn} call comes for a component attached after spawn.
 */
public class BossBarComponent implements TickingComponent {

    public static final Codec<BossBarComponent> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("range", 30).forGetter(BossBarComponent::getRange),
            Codec.enumOf(BossBar.Color.class).optionalFieldOf("color", BossBar.Color.WHITE).forGetter(BossBarComponent::getColor),
            Codec.enumOf(BossBar.Overlay.class).optionalFieldOf("style", BossBar.Overlay.PROGRESS).forGetter(BossBarComponent::getStyle),
            Codec.BOOLEAN.optionalFieldOf("bossFog", true).forGetter(BossBarComponent::isBossFog),
            BossBarComponent::new
    );

    // JSON-declared config, shared by every copy
    private final int range;
    private final BossBar.Color color;
    private final BossBar.Overlay style;
    private final boolean bossFog;

    // Per-instance runtime state, built once via build() - either from onSpawn (JSON path) or
    // immediately in the constructor (code-driven path, since onSpawn never runs for it).
    private @Nullable LivingEntity entity;
    private @Nullable BossBar bar;
    private Function<LivingEntity, Location> locationFunction = Entity::getLocation;

    public BossBarComponent(int range, BossBar.Color color, BossBar.Overlay style, boolean bossFog) {
        this.range = range;
        this.color = color;
        this.style = style;
        this.bossFog = bossFog;
    }

    public BossBarComponent(LivingEntity entity, int range, BossBar.Color color, BossBar.Overlay style) {
        this(entity, range, color, style, true);
    }

    public BossBarComponent(LivingEntity entity, int range, BossBar.Color color, BossBar.Overlay style, boolean bossFog) {
        this(entity, range, color, style, bossFog, Entity::getLocation);
    }

    public BossBarComponent(LivingEntity entity, int range, BossBar.Color color, BossBar.Overlay style, boolean bossFog, Location centerLocation) {
        this(entity, range, color, style, bossFog, b -> centerLocation);
    }

    /**
     * For code-driven bosses that want a custom center location instead of the entity's own.
     */
    public BossBarComponent(LivingEntity entity, int range, BossBar.Color color, BossBar.Overlay style,
                             boolean bossFog, Function<LivingEntity, Location> locationFunction) {
        this(range, color, style, bossFog);
        this.locationFunction = locationFunction;
        build(entity);
    }

    @Override
    public @NotNull EntityComponent copy() {
        return new BossBarComponent(range, color, style, bossFog);
    }

    @Override
    public void onSpawn(SmpEntity smpEntity) {
        if (bar != null) return; // Already built by the code-driven constructor.
        build(smpEntity.getEntity());
    }

    private void build(LivingEntity entity) {
        this.entity = entity;
        bar = BossBar.bossBar(Component.text(entity.getName()), 0f, color, style, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
        if (bossFog) {
            bar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
            bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
        }

        Location loc = locationFunction.apply(entity);
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) < range * range) {
                bar.addViewer(player);
            }
        }
    }

    public void setBossFog(boolean value) {
        if (value) {
            bar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
            bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
        } else {
            bar.removeFlag(BossBar.Flag.CREATE_WORLD_FOG);
            bar.removeFlag(BossBar.Flag.DARKEN_SCREEN);
        }
    }

    public void setTitle(String newTitle) {
        bar.name(Component.text(newTitle));
    }

    public void setColor(BossBar.Color barColor) {
        bar.color(barColor);
    }

    @Override
    public void tick(SmpEntity smpEntity, int interval) {
        if (smpEntity.dead || entity == null) return;

        double maxHealth = EntityUtils.getMaxHealth(entity);
        double progress = maxHealth <= 0 ? 0 : entity.getHealth() / maxHealth;
        update(progress);
    }

    /**
     * Refreshes viewer visibility by range and sets the bar's progress. Called every shared tick
     * via {@link #tick} with the entity's own current HP ratio; still public so code-driven bosses
     * can force a specific display value on their own (e.g. a shield-overlay effect) if needed.
     *
     * @param progress health percentage in [0, 1] to display
     */
    public void update(double progress) {
        if (entity.getHealth() <= 0) {
            entity.getWorld().hideBossBar(bar);
        }

        Location loc = locationFunction.apply(entity);
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(loc.getWorld()) && player.getLocation().distanceSquared(loc) < range * range) {
                bar.addViewer(player);
            } else {
                bar.removeViewer(player);
            }
        }

        if (!Double.isFinite(progress) || progress > 1.0 || progress < 0) {
            RogueSmpCore.LOGGER.warn("Boss Entity '{}' has invalid boss bar progress {}", entity.getName(), progress);
        } else {
            bar.progress((float) progress);
        }
    }

    @Override
    public void onUnload(SmpEntity smpEntity) {
        if (entity != null) entity.getWorld().hideBossBar(bar);
    }

    public int getRange() {
        return range;
    }

    public BossBar.Color getColor() {
        return color;
    }

    public BossBar.Overlay getStyle() {
        return style;
    }

    public boolean isBossFog() {
        return bossFog;
    }
}
