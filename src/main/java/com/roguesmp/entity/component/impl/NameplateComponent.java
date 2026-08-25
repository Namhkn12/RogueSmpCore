package com.roguesmp.entity.component.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Lazily updated - unlike most visual entity components, this does not tick itself. Whoever
 * changes something the nameplate displays (health, name, ...) is responsible for calling
 * {@link #update(SmpEntity)} afterward. This component covers the most common case itself -
 * {@link #onHurt} refreshes on damage taken - but anything else that mutates health/name outside
 * a {@link DamageEvent} (e.g. {@code PhaseComponent} forcing HP to a threshold) must call it too.
 */
public class NameplateComponent implements EntityComponent {

    public static final Codec<NameplateComponent> CODEC = Codec.composite(
            Codec.BOOLEAN.optionalFieldOf("showHealth", true).forGetter(NameplateComponent::showHealth),
            Codec.BOOLEAN.optionalFieldOf("showName", true).forGetter(NameplateComponent::showName),
            Codec.DOUBLE.optionalFieldOf("heightOffset", 0.3).forGetter(NameplateComponent::heightOffset),
            NameplateComponent::new
    );

    private final boolean showHealth;
    private final boolean showName;
    private final double heightOffset;

    private @Nullable LivingEntity entity;
    private @Nullable TextDisplay display;

    public NameplateComponent(boolean showHealth, boolean showName, double heightOffset) {
        this.showHealth = showHealth;
        this.showName = showName;
        this.heightOffset = heightOffset;
    }

    public static NameplateComponent createDefault() {
        return new NameplateComponent(true, true, 0.3);
    }

    @Override
    public @NotNull EntityComponent copy() {
        return new NameplateComponent(showHealth, showName, heightOffset);
    }

    @Override
    public void onSpawn(SmpEntity smpEntity) {
        this.entity = smpEntity.getEntity();
        // Hides the vanilla nametag so it doesn't stack with this floating one. Done here rather
        // than apply(LivingEntity) since this component can be attached directly to a live
        // SmpEntity (e.g. SmpEntity's own default-nameplate injection) without ever going through
        // BaseEntity#processEntity, which is the only thing that calls apply().
        entity.customName(null);

        entity.getScheduler().run(RogueSmpCore.getInstance(), task -> {
            if (!showHealth && !showName) return;
            spawnDisplay(smpEntity);
        }, null);
    }

    public void spawnDisplay(SmpEntity smpEntity) {
        if (entity == null || !entity.isValid() || entity.isDead()) return;

        display = entity.getWorld().spawn(entity.getLocation(), TextDisplay.class, textDisplay -> {
            textDisplay.setPersistent(false);
            textDisplay.setBillboard(Display.Billboard.CENTER);
            textDisplay.setSeeThrough(false);
            textDisplay.setBackgroundColor(Color.fromARGB(60, 0, 0, 0));
            textDisplay.setTransformation(new Transformation(
                    new Vector3f(0, (float) heightOffset, 0),
                    new Quaternionf(),
                    new Vector3f(1),
                    new Quaternionf()
            ));
        });

        // Riding follows the mob automatically every tick - no per-tick teleport needed anymore.
        entity.addPassenger(display);

        update(smpEntity);
    }

    /**
     * The wielder just took damage. The underlying entity's health hasn't actually been reduced
     * yet at this point ({@code DamageListener} fires this custom event before applying
     * {@code getFinalDamage()} to the real Bukkit health), so refreshing synchronously here would
     * just redraw the pre-hit value - defer one tick via the entity's own scheduler so the real
     * damage (and anything else's {@code onHurt}, e.g. {@code PhaseComponent}'s damage cap) has
     * already landed by the time this actually re-reads {@code entity.getHealth()}.
     */
    @Override
    public void onHurt(DamageEvent event, SmpEntity smpEntity) {
        if (entity == null) return;
        entity.getScheduler().run(RogueSmpCore.getInstance(), task -> update(smpEntity), null);
    }

    /**
     * Repositions nothing (riding handles that) - just rebuilds and re-renders the display text.
     * Call this whenever something the nameplate shows has changed; it is not called periodically.
     */
    public void update(SmpEntity smpEntity) {
        if (display == null || entity == null || !entity.isValid() || entity.isDead()) {
            remove();
            return;
        }

        display.text(buildText(smpEntity));
    }

    private Component buildText(SmpEntity smpEntity) {
        List<Component> lines = new ArrayList<>();

        if (showName) {
            DisplayNameComponent nameComponent = smpEntity.getComponent(EntityComponentKeys.DISPLAY_NAME);
            String name = nameComponent != null ? nameComponent.name() : entity.getType().name();
            lines.add(Utils.fromString(name));
        }

        if (showHealth) {
            lines.add(buildHealthLine());
        }

//        if (showEffects) {
//            lines.addAll(buildEffectLines());
//        }

        return Component.join(JoinConfiguration.spaces(), lines);
    }

    private Component buildHealthLine() {
        double maxHealth = EntityUtils.getMaxHealth(entity);
        double currentHealth = entity.getHealth();
        double percent = maxHealth <= 0 ? 0 : currentHealth / maxHealth;

        NamedTextColor healthColor;
        if (percent >= 0.75) healthColor = NamedTextColor.GREEN;
        else if (percent >= 0.4) healthColor = NamedTextColor.YELLOW;
        else healthColor = NamedTextColor.RED;

        return Component.text(Math.round(currentHealth), healthColor).append(Component.text("/", NamedTextColor.WHITE)).append(Component.text(Math.round(maxHealth), NamedTextColor.GREEN)).append(Component.text("❤", NamedTextColor.RED));
    }

    private void remove() {
        TextDisplay toRemove = display;
        display = null;
        if (toRemove == null || !toRemove.isValid()) return;

        // Called from onUnload, which - just like onSpawn - runs synchronously inside the owning
        // entity's chunk-tracking callback (this time on the "tracking end"/removal side). Removing
        // another entity right here re-enters the chunk system mid-update and crashes the same way
        // the spawn-side did, so defer the actual removal one tick via the display's own scheduler.
        toRemove.getScheduler().run(RogueSmpCore.getInstance(), task -> toRemove.remove(), null);
    }

    @Override
    public void onUnload(SmpEntity smpEntity) {
        remove();
    }

    public boolean showHealth() {
        return showHealth;
    }

    public boolean showName() {
        return showName;
    }

    public double heightOffset() {
        return heightOffset;
    }
}
