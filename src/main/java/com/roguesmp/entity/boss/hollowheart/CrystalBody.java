package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class CrystalBody {

    private final LivingEntity entity;
    private final BlockData look;
    private final float scale;
    private @Nullable BlockDisplay display;

    public CrystalBody(LivingEntity entity, BlockData look, float scale) {
        this.entity = entity;
        this.look = look;
        this.scale = scale;
    }

    public void setup() {
        if (entity instanceof Mob mob) mob.setAI(false);
        entity.setSilent(true);
        entity.setGravity(false);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
        entity.getScheduler().run(RogueSmpCore.getInstance(), task -> spawnDisplay(), null);
    }

    private void spawnDisplay() {
        if (!entity.isValid()) return;
        float size = scale * (float) EntityUtils.getAttributeOrDefault(entity, Attribute.SCALE, 1);
        float offset = -size / 2;
        display = entity.getWorld().spawn(entity.getLocation(), BlockDisplay.class, blockDisplay -> {
            blockDisplay.setBlock(look);
            blockDisplay.setPersistent(false);
            blockDisplay.setTeleportDuration(2);
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(offset, 0, offset), new AxisAngle4f(), new Vector3f(size, size, size), new AxisAngle4f()));
        });
    }

    public void follow() {
        if (display != null && display.isValid()) display.teleport(entity.getLocation());
    }

    public void setGlow(boolean glowing, Color color) {
        if (display == null || !display.isValid()) return;
        display.setGlowColorOverride(color);
        display.setGlowing(glowing);
    }

    public void remove() {
        BlockDisplay current = display;
        display = null;
        if (current != null) Absorption.remove(current);
    }
}
