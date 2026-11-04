package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ThrownAxe implements Hazard {

    private static final double SPEED = 0.8;
    private static final double HIT_RADIUS = 1.3;
    private static final double CURVE = 3.0;
    private static final int MAX_LIFE = 200;

    private final KingContext ctx;
    private final LivingEntity owner;
    private final ItemDisplay display;
    private final Location start;
    private final Vector outward;
    private final Vector side;
    private final double distance;
    private final boolean curved;
    private final double multiplier;
    private final String id;
    private final Set<UUID> hitOut = new HashSet<>();
    private final Set<UUID> hitBack = new HashSet<>();
    private Location position;
    private double travelled;
    private boolean returning;
    private int life;
    private float spin;

    public ThrownAxe(KingContext ctx, Location target, boolean curved, double multiplier, String id) {
        this.ctx = ctx;
        this.owner = ctx.owner();
        this.start = owner.getLocation().add(0, 1.2, 0);
        Vector delta = target.clone().add(0, 1, 0).toVector().subtract(start.toVector());
        this.distance = Math.max(4, delta.length() + 2);
        this.outward = delta.lengthSquared() < 1.0E-4 ? owner.getLocation().getDirection() : delta.normalize();
        this.side = new Vector(-outward.getZ(), 0, outward.getX()).normalize();
        this.curved = curved;
        this.multiplier = multiplier;
        this.id = id;
        this.position = start.clone();
        this.display = owner.getWorld().spawn(start, ItemDisplay.class, item -> {
            item.setItemStack(axeItem(owner));
            item.setPersistent(false);
            item.setTeleportDuration(2);
        });
        owner.getWorld().playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
    }

    private static ItemStack axeItem(LivingEntity owner) {
        EntityEquipment equipment = owner.getEquipment();
        if (equipment != null && !equipment.getItemInMainHand().getType().isAir()) return equipment.getItemInMainHand().clone();
        return new ItemStack(Material.GOLDEN_AXE);
    }

    @Override
    public boolean tick(int interval) {
        life += interval;
        if (life > MAX_LIFE || !owner.isValid()) {
            remove();
            return true;
        }

        Location next;
        if (!returning) {
            travelled = Math.min(distance, travelled + SPEED * interval);
            double t = travelled / distance;
            next = start.clone().add(outward.clone().multiply(travelled));
            if (curved) next.add(side.clone().multiply(Math.sin(t * Math.PI) * CURVE));
            if (travelled >= distance || !next.getBlock().isPassable()) {
                returning = true;
                owner.getWorld().playSound(position, Sound.ITEM_TRIDENT_RETURN, 1.5f, 0.7f);
            }
            if (!next.getBlock().isPassable()) next = position.clone();
        } else {
            Vector back = owner.getLocation().add(0, 1.2, 0).toVector().subtract(position.toVector());
            if (back.length() <= SPEED * interval * 1.5) {
                remove();
                return true;
            }
            next = position.clone().add(back.normalize().multiply(SPEED * interval));
            if (curved) next.add(side.clone().multiply(-0.15 * interval));
        }

        position = next;
        spin += 0.9f * interval;
        display.teleport(position);
        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(spin, 0, 0, 1),
                new Vector3f(1.6f, 1.6f, 1.6f), new AxisAngle4f()));
        position.getWorld().spawnParticle(Particle.SWEEP_ATTACK, position, 1, 0, 0, 0, 0);

        Set<UUID> hit = returning ? hitBack : hitOut;
        for (Player player : ctx.players()) {
            if (hit.contains(player.getUniqueId())) continue;
            if (player.getLocation().add(0, 1, 0).distanceSquared(position) > HIT_RADIUS * HIT_RADIUS) continue;
            hit.add(player.getUniqueId());
            ctx.damage(player, multiplier, DamageType.MELEE, id);
            ctx.push(player, position, 0.5, 0.3);
        }
        return false;
    }

    @Override
    public void remove() {
        display.getScheduler().run(RogueSmpCore.getInstance(), task -> display.remove(), null);
    }
}
