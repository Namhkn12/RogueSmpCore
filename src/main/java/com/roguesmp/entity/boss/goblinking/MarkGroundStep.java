package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MarkGroundStep extends ComboStep {

    private static final int FALLBACK_FUSE = 160;
    private static final double ARENA_SCATTER_RADIUS = 12;
    private static final double ARENA_SCATTER_CHANCE = 0.2;
    private static final double NEAR_MIN_OFFSET = 2.5;
    private static final double NEAR_MAX_OFFSET = 7;
    private static final double MIN_SPACING = 3.5;

    private final GroundRune.Style style;
    private final int scattered;
    private final boolean detonateOnExecute;
    private final String id;
    private final List<GroundRune> placed = new ArrayList<>();

    public MarkGroundStep(String title, StepKind kind, int windup, GroundRune.Style style, int scattered,
                          boolean detonateOnExecute, String id) {
        super(title, kind, windup);
        this.style = style;
        this.scattered = scattered;
        this.detonateOnExecute = detonateOnExecute;
        this.id = id;
    }

    @Override
    public void onWindupStart(KingContext ctx) {
        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.7f);
        if (detonateOnExecute) place(ctx);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        ctx.owner().getWorld().spawnParticle(Particle.ENCHANT, ctx.owner().getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0.5);
    }

    @Override
    public void onExecute(KingContext ctx) {
        ctx.owner().swingMainHand();
        if (detonateOnExecute) {
            placed.forEach(GroundRune::detonate);
        } else {
            place(ctx);
            ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.5f, 0.8f);
        }
    }

    private void place(KingContext ctx) {
        List<Player> players = ctx.players();
        List<Location> spots = new ArrayList<>();
        players.forEach(player -> spots.add(player.getLocation()));
        if (spots.isEmpty()) return;

        int target = spots.size() + scattered;
        int attempts = scattered * 6;
        while (spots.size() < target && attempts-- > 0) {
            Location spot = scatterSpot(ctx, players);
            if (spot.distanceSquared(ctx.home()) > KingContext.ARENA_RANGE * KingContext.ARENA_RANGE) continue;
            if (!spot.clone().add(0, 1, 0).getBlock().isPassable()) continue;
            if (spots.stream().anyMatch(other -> other.distanceSquared(spot) < MIN_SPACING * MIN_SPACING)) continue;
            spots.add(spot);
        }
        for (Location spot : spots) {
            GroundRune rune = new GroundRune(ctx, ctx.onFloor(spot), style, FALLBACK_FUSE, id);
            placed.add(rune);
            ctx.addRune(rune);
        }
    }

    private static Location scatterSpot(KingContext ctx, List<Player> players) {
        double angle = Utils.RANDOM.nextDouble() * Math.PI * 2;
        if (Utils.RANDOM.nextDouble() < ARENA_SCATTER_CHANCE) {
            double distance = Math.sqrt(Utils.RANDOM.nextDouble()) * ARENA_SCATTER_RADIUS;
            return ctx.home().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        }
        Location anchor = players.get(Utils.RANDOM.nextInt(players.size())).getLocation();
        double distance = NEAR_MIN_OFFSET + Utils.RANDOM.nextDouble() * (NEAR_MAX_OFFSET - NEAR_MIN_OFFSET);
        return anchor.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }
}
