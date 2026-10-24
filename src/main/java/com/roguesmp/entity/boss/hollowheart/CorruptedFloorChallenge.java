package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorruptedFloorChallenge implements CrystalChallenge {

    public static final String ID = "crystal_corrupted_floor";

    private static final BlockData MARK = Material.PURPLE_STAINED_GLASS.createBlockData();
    private static final int RANDOM_PATCHES = 4;
    private static final double RANDOM_PATCH_RANGE = 10.0;
    private static final int WAVE_GAP_TICKS = 20;
    private static final double DAMAGE_MULTIPLIER = 0.5;

    private record Tile(int x, int y, int z) {
        static Tile of(Block floor) {
            return new Tile(floor.getX(), floor.getY(), floor.getZ());
        }
    }

    private final CrystalContext context;
    private final Map<Tile, BlockDisplay> marks = new HashMap<>();
    private int wave;
    private long nextWaveAt;
    private long burstAt;

    public CorruptedFloorChallenge(CrystalContext context) {
        this.context = context;
    }

    @Override
    public void start(long now) {
        nextWaveAt = now;
    }

    @Override
    public Outcome tick(long now, int interval) {
        if (marks.isEmpty()) {
            if (now < nextWaveAt) return Outcome.RUNNING;
            if (wave >= context.phase().floorWaves()) return Outcome.SUCCESS;
            markWave(now);
            return Outcome.RUNNING;
        }

        if (now % 4 == 0) {
            marks.keySet().forEach(tile -> context.crystal().getWorld().spawnParticle(Particle.WITCH,
                    tile.x() + 0.5, tile.y() + 1.1, tile.z() + 0.5, 2, 0.3, 0.05, 0.3, 0));
        }
        if (now >= burstAt) burst(now);
        return Outcome.RUNNING;
    }

    private void markWave(long now) {
        for (Player player : context.arena().players()) {
            Block standing = player.getLocation().getBlock();
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    Location floor = context.arena().floorAt(standing.getX() + dx + 0.5, standing.getZ() + dz + 0.5);
                    if (floor != null) mark(floor);
                }
            }
        }
        for (int i = 0; i < RANDOM_PATCHES; i++) {
            Location floor = context.arena().randomPoint(RANDOM_PATCH_RANGE);
            if (floor != null) mark(floor);
        }

        wave++;
        burstAt = now + context.warning(context.phase().floorWarnTicks());
        context.crystal().getWorld().playSound(context.arena().center(), Sound.BLOCK_SCULK_SPREAD, 2f, 0.6f);
    }

    private void mark(Location standing) {
        Block floor = standing.getBlock().getRelative(0, -1, 0);
        Tile tile = Tile.of(floor);
        if (marks.containsKey(tile)) return;

        BlockDisplay display = floor.getWorld().spawn(floor.getLocation(), BlockDisplay.class, blockDisplay -> {
            blockDisplay.setBlock(MARK);
            blockDisplay.setPersistent(false);
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(-0.005f, 1.002f, -0.005f), new AxisAngle4f(), new Vector3f(1.01f, 0.05f, 1.01f), new AxisAngle4f()));
        });
        marks.put(tile, display);
    }

    private void burst(long now) {
        double damage = EntityUtils.getAttributeOrDefault(context.crystal(), Attribute.ATTACK_DAMAGE, 0) * DAMAGE_MULTIPLIER;
        for (Player player : context.arena().players()) {
            if (!isOnMark(player)) continue;
            DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.MAGIC, ID);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(player, context.crystal(), damage, metadata);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
        }

        marks.keySet().forEach(tile -> context.crystal().getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                tile.x() + 0.5, tile.y() + 1.2, tile.z() + 0.5, 8, 0.3, 0.2, 0.3, 0.05));
        context.crystal().getWorld().playSound(context.arena().center(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 2f, 0.6f);
        clearMarks();
        nextWaveAt = now + WAVE_GAP_TICKS;
    }

    private boolean isOnMark(Player player) {
        Block feet = player.getLocation().getBlock();
        for (int dy = 1; dy <= 2; dy++) {
            if (marks.containsKey(Tile.of(feet.getRelative(0, -dy, 0)))) return true;
        }
        return false;
    }

    @Override
    public BarState status(long now) {
        int waves = context.phase().floorWaves();
        return BarState.fill(Component.text(Math.max(1, wave) + "/" + waves, NamedTextColor.GRAY), wave, waves, BossBar.Color.PURPLE);
    }

    private void clearMarks() {
        new ArrayList<>(marks.values()).forEach(Absorption::remove);
        marks.clear();
    }

    @Override
    public void cleanup() {
        clearMarks();
    }
}
