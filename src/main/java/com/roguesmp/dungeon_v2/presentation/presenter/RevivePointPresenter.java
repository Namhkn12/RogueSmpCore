package com.roguesmp.dungeon_v2.presentation.presenter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RevivePointPresenter {

    private static final double ORBIT_RADIUS = 0.8D;
    private static final double FLOAT_HEIGHT = 1.2D;
    private static final float DISPLAY_SCALE = 0.75F;
    private static final float ORBIT_DEGREE_PER_TICK = 12.0F;

    public ItemDisplay spawnRevivePoint(Player deadPlayer, Location deadLocation) {
        if (deadPlayer == null || deadLocation == null || deadLocation.getWorld() == null) return null;

        ItemStack playerHead = ItemStack.of(Material.PLAYER_HEAD);
        if (playerHead.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(deadPlayer);
            playerHead.setItemMeta(skullMeta);
        }

        Location spawnLoc = deadLocation.clone().add(0, FLOAT_HEIGHT, 0);
        return spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(playerHead);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            display.setBillboard(Display.Billboard.FIXED);
            display.setGlowing(true);
            display.setTransformation(new Transformation(
                    new Vector3f(0F, 0F, 0F),
                    new Quaternionf(),
                    new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                    new Quaternionf()
            ));
        });
    }

    public void tickRevivePoint(ItemDisplay display, Location center, int tick) {
        if (display == null || display.isDead() || center == null || center.getWorld() == null) return;

        float angleDeg = (tick * ORBIT_DEGREE_PER_TICK) % 360F;
        double angleRad = Math.toRadians(angleDeg);

        double orbitX = Math.cos(angleRad) * ORBIT_RADIUS;
        double orbitZ = Math.sin(angleRad) * ORBIT_RADIUS;

        Location orbitLoc = center.clone().add(orbitX, FLOAT_HEIGHT, orbitZ);
        display.teleport(orbitLoc);

        Quaternionf spin = new Quaternionf().rotateY((float) (angleRad + Math.PI / 2));
        display.setTransformation(new Transformation(
                new Vector3f(0F, 0F, 0F),
                spin,
                new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                new Quaternionf()
        ));

        center.getWorld().spawnParticle(Particle.END_ROD, orbitLoc.clone().add(0, 0.15, 0), 1, 0.03, 0.03, 0.03, 0.0);
        if (tick % 2 == 0) {
            center.getWorld().spawnParticle(Particle.ENCHANT, center.clone().add(0, 0.15, 0), 2, 0.45, 0.10, 0.45, 0.0);
        }
    }

    public void removeRevivePoint(ItemDisplay display) {
        if (display == null) return;
        if (!display.isDead()) {
            display.remove();
        }
    }
}
