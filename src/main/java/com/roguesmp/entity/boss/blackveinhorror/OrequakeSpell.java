package com.roguesmp.entity.boss.blackveinhorror;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class OrequakeSpell extends ActiveSpell {

    public static final String ID = "orequake";

    private static final double TRIGGER_RANGE = 10.0;
    private static final int ROCK_COUNT = 4;
    private static final double MIN_THROW_DISTANCE = 4.0;
    private static final double MAX_THROW_DISTANCE = 8.0;
    private static final int FLIGHT_TICKS = 20;
    private static final int MAX_FLIGHT_TICKS = 80;
    private static final double ROCK_GRAVITY = 0.04;
    private static final double ROCK_DRAG = 0.98;
    private static final double IMPACT_RADIUS = 2.5;
    private static final double IMPACT_DAMAGE_MULTIPLIER = 0.8;
    private static final BlockData ROCK = Material.COBBLED_DEEPSLATE.createBlockData();

    private static final BlockData LIFE_STONE = Material.DEEPSLATE_EMERALD_ORE.createBlockData();
    private static final int MAX_LIFE_STONES = 4;
    private static final int LIFE_STONE_HITS = 3;
    private static final int LIFE_STONE_HEAL_DELAY = 160;
    private static final double LIFE_STONE_HEAL_PERCENT = 0.05;

    private static final int COOLDOWN_TICKS = 240;
    private static final int WINDUP_TICKS = 25;
    private static final int RECOVERY_TICKS = 20;

    private static final class Rock {
        private final FallingBlock block;
        private Location lastLocation;
        private int flightTicks;

        private Rock(FallingBlock block) {
            this.block = block;
            this.lastLocation = block.getLocation();
        }
    }

    private static final class LifeStone {
        private final Interaction hitbox;
        private final BlockDisplay display;
        private final long healAt;
        private int hitsLeft = LIFE_STONE_HITS;
        private long lastAttack;

        private LifeStone(Interaction hitbox, BlockDisplay display, long healAt) {
            this.hitbox = hitbox;
            this.display = display;
            this.healAt = healAt;
        }
    }

    private final LivingEntity owner;
    private final List<Rock> rocks = new ArrayList<>();
    private final List<LifeStone> lifeStones = new ArrayList<>();

    public OrequakeSpell(LivingEntity owner) {
        super(COOLDOWN_TICKS, WINDUP_TICKS, RECOVERY_TICKS);
        this.owner = owner;
    }

    @Override
    public boolean canCast(SpellContext context) {
        return context.target().getLocation().distanceSquared(owner.getLocation()) <= TRIGGER_RANGE * TRIGGER_RANGE;
    }

    @Override
    public void onWindupStart(SpellContext context) {
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, WINDUP_TICKS, 9, false, false));
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1.5f, 0.5f);
    }

    @Override
    public void onWindupTick(SpellContext context, int elapsed) {
        owner.getWorld().spawnParticle(Particle.BLOCK, owner.getLocation(), 12, 1.2, 0.1, 1.2, 0, ROCK);
    }

    @Override
    public void onInterrupted(SpellContext context) {
        owner.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    @Override
    public void onCast(SpellContext context) {
        owner.swingMainHand();
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2f, 0.5f);
        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_DEEPSLATE_BREAK, 2f, 0.6f);

        Location from = owner.getLocation().add(0, owner.getHeight() * 0.8, 0);
        double baseAngle = Utils.RANDOM.nextDouble() * Math.PI * 2;
        for (int i = 0; i < ROCK_COUNT; i++) {
            Location landing = i == 0 && context.target() != null
                    ? context.target().getLocation()
                    : ringPoint(baseAngle + Math.PI * 2 * i / ROCK_COUNT);
            throwRock(from, landing);
        }
    }

    private Location ringPoint(double angle) {
        double distance = MIN_THROW_DISTANCE + Utils.RANDOM.nextDouble() * (MAX_THROW_DISTANCE - MIN_THROW_DISTANCE);
        return owner.getLocation().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    private void throwRock(Location from, Location landing) {
        FallingBlock block = owner.getWorld().spawn(from, FallingBlock.class, falling -> {
            falling.setBlockData(ROCK);
            falling.setDropItem(false);
            falling.setCancelDrop(true);
            falling.setHurtEntities(false);
            falling.setPersistent(false);
            falling.setVelocity(SpellTargets.ballisticVelocity(from, landing, FLIGHT_TICKS, ROCK_GRAVITY, ROCK_DRAG));
        });
        rocks.add(new Rock(block));
    }

    @Override
    public void tick(SpellContext context, int interval) {
        rocks.removeIf(rock -> {
            rock.flightTicks += interval;
            boolean landed = !rock.block.isValid() || rock.block.isOnGround() || rock.flightTicks > MAX_FLIGHT_TICKS;
            if (!landed) {
                rock.lastLocation = rock.block.getLocation();
                return false;
            }
            remove(rock.block);
            impact(rock.lastLocation, context.tick());
            return true;
        });
        lifeStones.removeIf(stone -> tickLifeStone(stone, context.tick()));
    }

    private void impact(Location location, long now) {
        location.getWorld().spawnParticle(Particle.BLOCK, location, 40, 0.8, 0.3, 0.8, 0, ROCK);
        location.getWorld().playSound(location, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.7f);

        double damage = EntityUtils.getAttributeOrDefault(owner, Attribute.ATTACK_DAMAGE, 0) * IMPACT_DAMAGE_MULTIPLIER;
        for (Player player : location.getNearbyPlayers(IMPACT_RADIUS)) {
            if (!player.isValid()) continue;
            DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.BLAST, ID);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(player, owner, damage, metadata);
            Vector push = SpellTargets.horizontalDirection(location, player.getLocation()).multiply(0.4).setY(0.4);
            player.setVelocity(player.getVelocity().add(push));
        }

        if (lifeStones.size() < MAX_LIFE_STONES) spawnLifeStone(location, now);
    }

    private void spawnLifeStone(Location location, long now) {
        Location base = location.getBlock().getLocation().add(0.5, 0, 0.5);
        while (!base.getBlock().isPassable() && base.getY() < location.getY() + 2) base.add(0, 1, 0);

        Interaction hitbox = base.getWorld().spawn(base, Interaction.class, interaction -> {
            interaction.setInteractionWidth(1f);
            interaction.setInteractionHeight(1f);
            interaction.setResponsive(true);
            interaction.setPersistent(false);
        });
        BlockDisplay display = base.getWorld().spawn(base, BlockDisplay.class, blockDisplay -> {
            blockDisplay.setBlock(LIFE_STONE);
            blockDisplay.setPersistent(false);
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(-0.4f, 0, -0.4f), new AxisAngle4f(), new Vector3f(0.8f, 0.8f, 0.8f), new AxisAngle4f()));
        });
        lifeStones.add(new LifeStone(hitbox, display, now + LIFE_STONE_HEAL_DELAY));
    }

    private boolean tickLifeStone(LifeStone stone, long now) {
        if (!stone.hitbox.isValid()) {
            remove(stone.display);
            return true;
        }

        Location location = stone.hitbox.getLocation();
        Interaction.PreviousInteraction attack = stone.hitbox.getLastAttack();
        if (attack != null && attack.getTimestamp() != stone.lastAttack) {
            stone.lastAttack = attack.getTimestamp();
            stone.hitsLeft--;
            location.getWorld().spawnParticle(Particle.BLOCK, location.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0, LIFE_STONE);
            location.getWorld().playSound(location, Sound.BLOCK_STONE_HIT, 1f, 0.8f);
            if (stone.hitsLeft <= 0) {
                location.getWorld().playSound(location, Sound.BLOCK_STONE_BREAK, 1.5f, 0.8f);
                removeLifeStone(stone);
                return true;
            }
        }

        if (now < stone.healAt) return false;

        healOwner(location);
        removeLifeStone(stone);
        return true;
    }

    private void healOwner(Location from) {
        double maxHealth = EntityUtils.getMaxHealth(owner);
        owner.setHealth(Math.min(maxHealth, owner.getHealth() + maxHealth * LIFE_STONE_HEAL_PERCENT));

        from.getWorld().spawnParticle(Particle.BLOCK, from.clone().add(0, 0.5, 0), 30, 0.4, 0.4, 0.4, 0, LIFE_STONE);
        from.getWorld().playSound(from, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.8f);
        Vector step = SpellTargets.centerOf(owner).toVector().subtract(from.toVector()).multiply(0.1);
        Location point = from.clone().add(0, 0.5, 0);
        for (int i = 0; i < 10; i++) {
            point.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, point.add(step), 1, 0, 0, 0, 0);
        }
        owner.getWorld().spawnParticle(Particle.HEART, owner.getEyeLocation(), 3, 0.4, 0.3, 0.4);
    }

    private void removeLifeStone(LifeStone stone) {
        remove(stone.hitbox);
        remove(stone.display);
    }

    private static void remove(Entity entity) {
        entity.getScheduler().run(RogueSmpCore.getInstance(), task -> entity.remove(), null);
    }

    @Override
    public void onRemoved() {
        rocks.forEach(rock -> remove(rock.block));
        lifeStones.forEach(this::removeLifeStone);
        rocks.clear();
        lifeStones.clear();
    }
}
