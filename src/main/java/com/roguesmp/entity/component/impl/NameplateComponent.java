package com.roguesmp.entity.component.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.TickingComponent;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class NameplateComponent implements TickingComponent {

    public static final Codec<NameplateComponent> CODEC = Codec.composite(
            Codec.BOOLEAN.optionalFieldOf("showHealth", true).forGetter(NameplateComponent::showHealth),
            Codec.BOOLEAN.optionalFieldOf("showName", true).forGetter(NameplateComponent::showName),
            Codec.DOUBLE.optionalFieldOf("heightOffset", 0.3).forGetter(NameplateComponent::heightOffset),
            Codec.INT.optionalFieldOf("updateInterval", SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT).forGetter(NameplateComponent::updateInterval),
            NameplateComponent::new
    );

    private final boolean showHealth;
    private final boolean showName;
    private final double heightOffset;
    private final int updateInterval;

    private @Nullable LivingEntity entity;
    private @Nullable TextDisplay display;
    private int ticksUntilUpdate = 0;

    public NameplateComponent(boolean showHealth, boolean showName, double heightOffset, int updateInterval) {
        this.showHealth = showHealth;
        this.showName = showName;
        this.heightOffset = heightOffset;
        this.updateInterval = updateInterval;
    }

    public static NameplateComponent createDefault() {
        return new NameplateComponent(true, true, 0.3, SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT);
    }

    @Override
    public @NotNull EntityComponent copy() {
        return new NameplateComponent(showHealth, showName, heightOffset, updateInterval);
    }

    @Override
    public void onSpawn(SmpEntity smpEntity) {
        this.entity = smpEntity.getEntity();

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

    @Override
    public void tick(SmpEntity smpEntity, int interval) {
        ticksUntilUpdate -= interval;
        if (ticksUntilUpdate > 0) return;
        ticksUntilUpdate = effectiveUpdateInterval();
        update(smpEntity);
    }

    private int effectiveUpdateInterval() {
        return updateInterval <= 0 ? SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT : updateInterval;
    }

    private void update(SmpEntity smpEntity) {
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
        else if (percent >= 0.15) healthColor = NamedTextColor.GOLD;
        else healthColor = NamedTextColor.RED;

        return Component.text(Math.round(currentHealth) + "/" + Math.round(maxHealth) + " ❤", healthColor)
                .decoration(TextDecoration.ITALIC, false);
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

    @Override
    public void apply(LivingEntity entity) {
        entity.customName(null);
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

    public int updateInterval() {
        return updateInterval;
    }
}
