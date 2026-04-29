package com.roguesmp.utils;

import com.roguesmp.constant.DamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

public class DamageDisplayUtils {

    public static void spawnDamageDisplay(Location hitLocation, double damage, DamageType type, boolean isCritical) {
        Component text = formatDamage(damage, type, isCritical);

        // Initial Physics Constants (Fast & Snappy)
//        double vx = (Utils.RANDOM.nextDouble() - 0.5) * 0.22;
//        double vz = (Utils.RANDOM.nextDouble() - 0.5) * 0.22;
//        final double[] vy = {0.45}; // Upward burst
//        final double gravity = 0.08;

        TextDisplay display = hitLocation.getWorld().spawn(hitLocation, TextDisplay.class, entity -> {
            entity.setPersistent(false);

            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            entity.setShadowed(true);
        });

        Utils.runLater(display::remove, 16);

//        new BukkitRunnable() {
//            int ticks = 0;
//            final int maxTicks = 16;
//            double offX = 0, offY = 0, offZ = 0;
//            final Transformation trans = display.getTransformation();
//            @Override
//            public void run() {
//                if (!display.isValid() || ticks >= maxTicks) {
//                    display.remove();
//                    this.cancel();
//                    return;
//                }
//
//                display.setInterpolationDelay(0);
//                display.setInterpolationDuration(1);
//
////                Transformation trans = display.getTransformation();
//
//                offX += vx;
//                offZ += vz;
//                offY += vy[0];
//                vy[0] -= gravity;
//
//                trans.getTranslation().set((float) offX, (float) offY, (float) offZ);
//
//                display.setTransformation(trans);
//                ticks++;
//            }
//        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 2);
    }

    private static Component formatDamage(double damage, DamageType type, boolean isCritical) {
        String val = Utils.formatDecimal(damage);

        TextColor color = switch (type) {
            case MELEE, MELEE_ABILITY -> NamedTextColor.WHITE;
            case PROJECTILE, PROJECTILE_ABILITY -> NamedTextColor.YELLOW;
            case MAGIC -> NamedTextColor.LIGHT_PURPLE;
            case FIRE -> NamedTextColor.GOLD;
            case BLAST -> NamedTextColor.RED;
            case AILMENT -> NamedTextColor.GREEN;
            case TRUE -> NamedTextColor.AQUA;
            case FALL, THORNS -> NamedTextColor.GRAY;
            default -> NamedTextColor.DARK_GRAY;
        };

        String prefix = switch (type) {
            case MELEE -> "⚔ ";
            case PROJECTILE -> "\uD83C\uDFF9 ";
            case MELEE_ABILITY, PROJECTILE_ABILITY -> "★ ";
            case MAGIC -> "✦ ";
            case AILMENT -> "☣ ";
            case FIRE -> "🔥 ";
            case TRUE -> "⚡ ";
            default -> isCritical ? "✦ " : "";
        };

        return Component.text(prefix + val, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, type.name().contains("ABILITY"));
    }
}
