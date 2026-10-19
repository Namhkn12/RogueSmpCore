package com.roguesmp.effect;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Every subclass must declare its own {@code Codec<T>} (built from {@link #BASE_CODEC} plus its
 * own fields via {@link Codec#composite}) and register it in {@link EffectCodecs} under a unique
 * effect id - that's what {@link #CODEC}'s polymorphic dispatch resolves against.
 */
public abstract class SmpEffect implements Comparable<SmpEffect>, Cloneable {

    public static final int INFINITE = -1;
    public static final Codec<SmpEffect> CODEC = Codec.dispatch(
            "id",
            SmpEffect::getEffectID,
            Codec.STRING,
            Registries.EFFECT_CODEC::getOrThrow
    );

    /**
     * A record of common SmpEffect attribute to pass into subclasses codec.
     *
     * @param persistent overrides {@link #isPersistentByDefault()} when present
     */
    public record BaseProperties(
            int duration,
            DeathBehavior deathBehavior,
            DisplayMode displayMode,
            @Nullable Boolean persistent
    ) {
        public BaseProperties(int duration, DeathBehavior deathBehavior, DisplayMode displayMode) {
            this(duration, deathBehavior, displayMode, null);
        }
    }

    public static final MapCodec<BaseProperties> BASE_CODEC = Codec.composite(
            Codec.INT.fieldOf("duration").forGetter(BaseProperties::duration),
            Codec.enumOf(DeathBehavior.class)
                    .optionalFieldOf("death_behavior", DeathBehavior.REMOVE_ON_DEATH)
                    .forGetter(BaseProperties::deathBehavior),
            Codec.enumOf(DisplayMode.class)
                    .optionalFieldOf("display_mode", DisplayMode.WITH_TIME)
                    .forGetter(BaseProperties::displayMode),
            Codec.BOOLEAN.optionalFieldOf("persistent").forGetter(base -> Optional.ofNullable(base.persistent())),
            (duration, deathBehavior, displayMode, persistent) -> new BaseProperties(duration, deathBehavior, displayMode, persistent.orElse(null))
    );

    protected int duration;
    private final String effectID;
    private final DeathBehavior deathBehavior;
    private final DisplayMode displayMode;
    private final @Nullable Boolean persistent;
    private String source = "";

    public SmpEffect(String effectID, BaseProperties base) {
        this.duration = base.duration();
        this.effectID = effectID;
        this.deathBehavior = base.deathBehavior();
        this.displayMode = base.displayMode();
        this.persistent = base.persistent();
    }

    public SmpEffect(String effectID, int duration, DeathBehavior deathBehavior) {
        this(effectID, new BaseProperties(duration, deathBehavior, DisplayMode.WITH_TIME));
    }

    public SmpEffect(String effectID, int duration) {
        this(effectID, duration, DeathBehavior.REMOVE_ON_DEATH);
    }

    /**
     * Used to compare how this effect is stronger (more potent) than another effect of the same
     * source - only meaningful under {@link StackPolicy#HIGHEST_WINS}.
     */
    public double getMagnitude() {
        return 0;
    }

    public StackPolicy getStackPolicy() {
        return StackPolicy.HIGHEST_WINS;
    }

    /**
     * Player attributes granted while this effect is active - applied and removed by
     * {@link EffectStack}, keyed by this effect's source.
     */
    public Map<Attributes, Double> getAttributeBonus() {
        return Map.of();
    }

    /**
     * Whether the effect stays (written to files) after the player logged out, unless the
     * definition's {@code persistent} field overrides it.
     */
    protected abstract boolean isPersistentByDefault();

    public final boolean isPersistent() {
        return persistent != null ? persistent : isPersistentByDefault();
    }

    /**
     * This effect's own display content, regardless of {@link #getDisplayMode()} - return null for
     * no display.
     * @return Component
     */
    public abstract @Nullable Component getDisplayComponent();

    /**
     * Display effect with remaining time generally used in tab list, return null to not display.
     * @return Component
     */
    public @Nullable Component getDisplayWithTime() {
        if (displayMode == DisplayMode.HIDDEN) return null;

        Component content = getDisplayComponent();
        if (content == null) return null;

        if (displayMode == DisplayMode.WITH_TIME) {
            String time = isInfinite() ? "--:--" : Utils.intToMinuteAndSeconds(duration / 20);
            return content.append(Component.text(" " + time, NamedTextColor.GRAY));
        }
        return content;
    }

    public List<Component> getDisplayLines() {
        Component line = getDisplayWithTime();
        return line == null ? List.of() : List.of(line);
    }

    /**
     * Ticks the effect, called regularly
     *
     * @param ticks Ticks passed since the last time this method was called to check duration expiry
     * @return Returns true if effect has expired and should be removed by the EffectManager
     */
    public boolean tickDuration(int ticks) {
        if (isInfinite()) return false;
        duration -= ticks;
        return duration <= 0;
    }

    public BaseProperties getBaseProperties() {
        return new BaseProperties(duration, deathBehavior, displayMode, persistent);
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * The source id this effect is stacked under - set by {@link EffectStack} before any gain/lose
     * hook runs, so it can key per-source state such as vanilla attribute modifiers.
     */
    public String getSource() {
        return source;
    }

    public void bindSource(String source) {
        this.source = source;
    }

    public String getEffectID() {
        return effectID;
    }

    public DeathBehavior getDeathBehavior() {
        return deathBehavior;
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public boolean isInfinite() {
        return duration == INFINITE;
    }

    @Override
    public int compareTo(@NotNull SmpEffect o) {
        return Double.compare(this.getMagnitude(), o.getMagnitude());
    }

    @Override
    public SmpEffect clone() {
        try {
            return (SmpEffect) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public enum StackPolicy {
        HIGHEST_WINS,
        REPLACE
    }

    public enum DeathBehavior {
        HALVES_ON_DEATH,
        REMOVE_ON_DEATH,
        KEEP_ON_DEATH,
    }

    /**
     * Whether/how this effect shows up in a rendered effect list (tab list, PlaceholderAPI).
     */
    public enum DisplayMode {
        HIDDEN,
        WITH_TIME,
        WITHOUT_TIME
    }

    public void onTick(Entity entity, boolean oneHz, boolean twoHz) {

    }

    public void onGainEffect(Entity entity) {

    }

    public void onLoseEffect(Entity entity) {

    }

    public void onDeath(EntityDeathEvent event) {

    }

    public void onDamageEntity(DamageEvent event) {

    }

    public void onHurt(DamageEvent event) {

    }

    static List<SmpEffect> getEffects(LivingEntity entity) {
        List<SmpEffect> effects = new ArrayList<>(EffectManager.getInstance().getActiveEffects(entity).values());
        effects.removeIf(Objects::isNull);
        return effects;
    }

    static List<SmpEffect> getSortedEffects(LivingEntity entity) {
        List<SmpEffect> effects = getEffects(entity);
        effects.sort((a, b) -> Integer.compare(sortWeight(b), sortWeight(a)));
        return effects;
    }

    /**
     * Sorts by remaining duration - effects not shown with a time suffix (hidden or timeless)
     * always sort last, since remaining duration has no visible meaning for them. Infinite
     * effects sort after everything else.
     */
    private static int sortWeight(SmpEffect effect) {
        if (effect.isInfinite()) return Integer.MIN_VALUE;
        return effect.displayMode == DisplayMode.WITH_TIME ? effect.duration : -1;
    }

    static List<Component> getSortedEffectDisplayComponents(LivingEntity entity) {
        return getSortedEffects(entity).stream().flatMap(effect -> effect.getDisplayLines().stream()).toList();
    }

    /**
     * The rendered, sorted effect lines for one entity - memoized per-tick via
     * {@link EffectDisplayCache} since callers (tab list, PlaceholderAPI) ask for this once per
     * rendered line/placeholder.
     */
    public static List<Component> getSortedEffectDisplays(LivingEntity entity) {
        return EffectDisplayCache.get(entity);
    }
}
