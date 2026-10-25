package com.roguesmp.entity.boss.veinstalker;

import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class WebSnareSpell extends ActiveSpell {

    private static final double RANGE = 12.0;
    private static final double EXTRA_WEB_CHANCE = 0.5;
    private static final int WEB_LIFETIME_TICKS = 100;
    private static final int COOLDOWN_TICKS = 160;
    private static final int WINDUP_TICKS = 10;
    private static final int RECOVERY_TICKS = 15;

    private record PlacedWeb(Block block, long removeAt) {
    }

    private final LivingEntity owner;
    private final List<PlacedWeb> webs = new ArrayList<>();

    public WebSnareSpell(LivingEntity owner) {
        super(COOLDOWN_TICKS, WINDUP_TICKS, RECOVERY_TICKS);
        this.owner = owner;
    }

    @Override
    public boolean canCast(SpellContext context) {
        Player target = context.target();
        return target.getLocation().distanceSquared(owner.getLocation()) <= RANGE * RANGE && owner.hasLineOfSight(target);
    }

    @Override
    public void onWindupStart(SpellContext context) {
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.6f);
    }

    @Override
    public void onCast(SpellContext context) {
        Player target = context.target();
        if (target == null) return;

        drawWebLine(owner.getEyeLocation(), target.getLocation().add(0, 0.5, 0));
        owner.getWorld().playSound(target.getLocation(), Sound.BLOCK_COBWEB_PLACE, 1.5f, 0.8f);

        Block center = target.getLocation().getBlock();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                boolean isCenter = x == 0 && z == 0;
                if (!isCenter && Utils.RANDOM.nextDouble() > EXTRA_WEB_CHANCE) continue;
                placeWeb(center.getRelative(x, 0, z), context.tick());
            }
        }
    }

    private void placeWeb(Block block, long now) {
        if (!block.getType().isAir()) return;
        if (!new EntityChangeBlockEvent(owner, block, Material.COBWEB.createBlockData()).callEvent()) return;
        block.setType(Material.COBWEB);
        webs.add(new PlacedWeb(block, now + WEB_LIFETIME_TICKS));
    }

    private void drawWebLine(Location from, Location to) {
        Vector step = to.toVector().subtract(from.toVector());
        int points = (int) Math.ceil(step.length() * 2);
        step.multiply(1.0 / Math.max(points, 1));
        Location point = from.clone();
        for (int i = 0; i < points; i++) {
            point.add(step);
            point.getWorld().spawnParticle(Particle.BLOCK, point, 2, 0.05, 0.05, 0.05, 0, Material.COBWEB.createBlockData());
        }
    }

    @Override
    public void tick(SpellContext context, int interval) {
        webs.removeIf(web -> {
            if (context.tick() < web.removeAt()) return false;
            clear(web);
            return true;
        });
    }

    @Override
    public void onRemoved() {
        webs.forEach(this::clear);
        webs.clear();
    }

    private void clear(PlacedWeb web) {
        if (web.block().getType() == Material.COBWEB) web.block().setType(Material.AIR);
    }
}
