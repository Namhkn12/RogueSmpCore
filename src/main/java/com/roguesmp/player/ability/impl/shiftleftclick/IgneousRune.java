package com.roguesmp.player.ability.impl.shiftleftclick;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IgneousRune extends Ability {
    public static final String ID = "igneous_rune";

    // Level Scaling
    private static final List<Double> DAMAGE_LEVELS = List.of(16.0, 20.0, 24.0, 28.0, 35.0);
    private static final List<Double> RADIUS_LEVELS = List.of(3.0, 3.5, 4.0, 4.5, 5.0);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(240, 220, 200, 180, 160); // 12s down to 8s

    public static final AbilityInfo<IgneousRune> INFO = new AbilityInfo.Builder<IgneousRune>()
            .id(ID)
            .displayText(Component.text("Cổ Tự Hỏa Ngục", NamedTextColor.GOLD, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Đặt một cổ tự lửa trên mặt đất. Phát nổ khi kẻ địch chạm vào.", NamedTextColor.GRAY),
                    Utils.text("Sát thương: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGE_LEVELS.get(l - 1)), NamedTextColor.RED)),
                    Utils.text("Bán kính nổ: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(RADIUS_LEVELS.get(l - 1)) + "m", NamedTextColor.YELLOW))
            ))
            .displayIcon(Material.SUNFLOWER)
            .factory(IgneousRune::new)
            .trigger(AbilityTrigger.SHIFT_LEFT_CLICK)
            .build();

    public IgneousRune(SmpPlayer player, int level) { super(player, level); }

    @Override
    public void cast() {
        if (isOnCooldown()) return;

        Player p = smpPlayer.getBukkitPlayer();
        RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 15);

        // If they miss the ground, place it at their feet
        Location runeLoc = (ray != null && ray.getHitBlock() != null)
                ? ray.getHitBlock().getLocation().add(0.5, 1.1, 0.5)
                : p.getLocation().add(0, 0.1, 0);

        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));
        spawnRune(runeLoc, p);
        p.getWorld().playSound(runeLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.5f);
    }

    private void spawnRune(Location loc, Player caster) {
        World world = loc.getWorld();
        final int PREPARE_TIME = 20;
        final int MAX_DURATION = 300; // 15 seconds max

        ItemDisplay core = world.spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.MAGMA_CREAM));
            display.setBrightness(new Display.Brightness(15, 15));
            Transformation trans = display.getTransformation();
            trans.getScale().set(0.5f, 0.5f, 0.5f);
            display.setTransformation(trans);
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > MAX_DURATION || !core.isValid()) {
                    core.remove();
                    this.cancel();
                    return;
                }

                // Visual Drawing Phase
                drawRuneCircle(loc, ticks, PREPARE_TIME);

                // Spin the core
                Transformation trans = core.getTransformation();
                trans.getLeftRotation().rotateY(0.15f);
                core.setTransformation(trans);

                // Arming Flash
                if (ticks == PREPARE_TIME) {
                    world.spawnParticle(Particle.FLASH, loc, 1);
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 2.0f);
                }

                // Detection Phase (Only after preparation)
                if (ticks > PREPARE_TIME) {
                    double detectionRadius = 2.0;
                    List<LivingEntity> enemies = loc.getNearbyLivingEntities(detectionRadius).stream()
                            .filter(e -> !(e instanceof Player) && e.isValid())
                            .toList();

                    if (!enemies.isEmpty()) {
                        detonate(loc, caster);
                        core.remove();
                        this.cancel();
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    private void drawRuneCircle(Location loc, int ticks, int prepareTime) {
        double radius = 1.5;

        // --- PHASE 1: FORMATION ---
        if (ticks <= prepareTime) {
            // Corrected floating point division
            double startAngle = Math.PI * 2 * ((double) (ticks - 1) / prepareTime);
            double endAngle = Math.PI * 2 * ((double) ticks / prepareTime);

            for (double i = startAngle; i < endAngle; i += Math.PI / 32) {
                double x = Math.cos(i) * radius;
                double z = Math.sin(i) * radius;
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
            }
        }
        // --- PHASE 2: ACTIVE IDLE ---
        else {
            if (ticks % 2 == 0) {
                for (int j = 0; j < 3; j++) {
                    double randomAngle = Math.random() * Math.PI * 2;
                    double x = Math.cos(randomAngle) * radius;
                    double z = Math.sin(randomAngle) * radius;
                    loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0.02);
                }
            }
        }
    }

    private void detonate(Location loc, Player p) {
        World world = loc.getWorld();
        double radius = RADIUS_LEVELS.get(level - 1);
        double damage = DAMAGE_LEVELS.get(level - 1);

        // Visual Pillar of Fire
        for (int i = 0; i < 6; i++) {
            double yOffset = i * 0.7;
            world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, yOffset, 0), 1);
            world.spawnParticle(Particle.FLAME, loc.clone().add(0, yOffset, 0), 15, 0.4, 0.4, 0.4, 0.1);
            world.spawnParticle(Particle.LAVA, loc.clone().add(0, yOffset, 0), 5, 0.2, 0.2, 0.2, 0.05);
        }

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
        world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);
        world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1.5f, 1.5f);

        // Damage & Knockup
        for (LivingEntity e : EntityUtils.getNearbyMobs(loc, radius, 2.0, radius, living -> true)) {
            DamageUtils.damage(e, p, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));

            // Blast knock-up
            e.setVelocity(e.getVelocity().add(new Vector(0, 0.7 + (level * 0.05), 0)));
            e.setFireTicks(40 + (level * 20));
        }
    }

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}