package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KingContext {

    public static final double ARENA_RANGE = 32;
    private static final double GROUNDED_TOLERANCE = 0.6;
    private static final int FLOOR_SCAN_DEPTH = 12;
    private static final int MARK_DURATION = 200;

    private final ComboDirector director;
    private final LivingEntity owner;
    private @Nullable Player target;
    private @Nullable Player mark;
    private @Nullable RunePrison prison;
    private final List<GroundRune> runes = new ArrayList<>();
    private boolean hovering;
    private boolean immune;
    private boolean rooted;

    KingContext(ComboDirector director, LivingEntity owner, @Nullable Player target) {
        this.director = director;
        this.owner = owner;
        this.target = target;
        if (owner instanceof Mob mob) mob.setAware(false);
    }

    public LivingEntity owner() {
        return owner;
    }

    public Location home() {
        return owner.getLocation();
    }

    public List<Player> players() {
        return PlayerUtils.playersInRange(owner.getLocation(), ARENA_RANGE, false).stream()
                .filter(player -> player.isValid() && !player.isDead())
                .toList();
    }

    public @Nullable Player aimTarget() {
        if (target != null && target.isValid() && !target.isDead() && target.getWorld() == owner.getWorld()) return target;
        target = players().stream()
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(owner.getLocation())))
                .orElse(null);
        return target;
    }

    public @Nullable Player farthest() {
        return players().stream()
                .max(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(owner.getLocation())))
                .orElse(null);
    }

    public @Nullable Player marked() {
        if (mark == null || !mark.isValid()) return aimTarget();
        return mark;
    }

    public @Nullable Player consumeMark() {
        Player consumed = mark != null && mark.isValid() ? mark : null;
        mark = null;
        return consumed;
    }

    public void mark(Player player, Particle.DustOptions dust) {
        mark = player;
        addHazard(Hazard.timed(MARK_DURATION, age -> {
            if (mark != player || !player.isValid()) return true;
            Location feet = player.getLocation();
            Fx.ring(feet, 1.2, dust);
            player.getWorld().spawnParticle(Particle.DUST, feet.add(0, player.getHeight() + 0.6, 0), 4, 0.15, 0.15, 0.15, 0, dust);
            return false;
        }));
    }

    public @Nullable RunePrison prison() {
        return prison;
    }

    public void setPrison(RunePrison prison) {
        this.prison = prison;
        addHazard(prison);
    }

    public List<GroundRune> runes() {
        return runes;
    }

    public void addRune(GroundRune rune) {
        runes.add(rune);
        addHazard(rune);
    }

    public void addHazard(Hazard hazard) {
        director.addHazard(hazard);
    }

    public double baseDamage() {
        return EntityUtils.getAttributeOrDefault(owner, Attribute.ATTACK_DAMAGE, 6);
    }

    public void damage(Player player, double multiplier, DamageType type, String spellId) {
        DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(type, spellId);
        metadata.setIgnoreIframe(true);
        metadata.setDoKnockback(false);
        DamageUtils.damage(player, owner, baseDamage() * multiplier, metadata);
    }

    public void push(Player player, Location from, double horizontal, double vertical) {
        Vector push = SpellTargets.horizontalDirection(from, player.getLocation()).multiply(horizontal).setY(vertical);
        player.setVelocity(player.getVelocity().add(push));
    }

    public double floorY(Location location) {
        Location probe = location.clone();
        for (int i = 0; i < FLOOR_SCAN_DEPTH; i++) {
            if (!probe.getBlock().isPassable()) return probe.getBlockY() + 1;
            probe.subtract(0, 1, 0);
        }
        return location.getY();
    }

    public Location onFloor(Location location) {
        Location result = location.clone();
        result.setY(floorY(location.clone().add(0, 0.5, 0)));
        return result;
    }

    public boolean grounded(Player player) {
        return player.getY() - floorY(player.getLocation().add(0, 0.2, 0)) <= GROUNDED_TOLERANCE;
    }

    public void face(Location location) {
        Vector direction = location.toVector().subtract(owner.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() < 1.0E-4) return;
        Location facing = owner.getLocation().setDirection(direction);
        owner.setRotation(facing.getYaw(), 0);
    }

    public void moveTo(Location location) {
        Location next = location.clone();
        next.setYaw(owner.getLocation().getYaw());
        next.setPitch(0);
        owner.teleport(next);
    }

    public boolean isHovering() {
        return hovering;
    }

    public void hover(boolean hovering) {
        this.hovering = hovering;
        owner.setGravity(!hovering);
        owner.setVelocity(new Vector());
    }

    public boolean isImmune() {
        return immune;
    }

    public void brace(boolean braced) {
        immune = braced;
        rooted = braced;
        owner.setVelocity(new Vector());
    }

    public boolean isRooted() {
        return rooted;
    }

    void end() {
        immune = false;
        rooted = false;
        hover(false);
        if (owner instanceof Mob mob) mob.setAware(true);
    }
}
