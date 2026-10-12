package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.List;
import java.util.Set;

/**
 * Breaks the block a target is standing on when it's perched {@code minHeightDifference}+ blocks
 * above the owner. Never touches containers, spawners, bedrock & co, or any {@code protectedBlocks};
 * goes through {@link EntityChangeBlockEvent} so region protection can veto it.
 */
public class UndermineSpell extends Spell {

    public static final String TYPE_KEY = "undermine_spell";

    private static final Set<Material> ALWAYS_PROTECTED = Set.of(
            Material.BEDROCK, Material.BARRIER, Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT,
            Material.ENDER_CHEST, Material.END_PORTAL_FRAME, Material.REINFORCED_DEEPSLATE,
            Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.STRUCTURE_BLOCK
    );

    public record Params(double range, double minHeightDifference, int cooldownTicks, List<Material> protectedBlocks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("range", 6.0).forGetter(Params::range),
                Codec.DOUBLE.optionalFieldOf("minHeightDifference", 2.0).forGetter(Params::minHeightDifference),
                Codec.INT.optionalFieldOf("cooldownTicks", 60).forGetter(Params::cooldownTicks),
                Codec.listOf(Codec.MATERIAL).optionalFieldOf("protectedBlocks", List.of()).forGetter(Params::protectedBlocks),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;

    public UndermineSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public boolean canRun() {
        Player target = SpellTargets.playerTarget(owner);
        if (target == null) return false;
        if (target.getY() - owner.getY() < params.minHeightDifference()) return false;
        if (SpellTargets.horizontalDistance(owner.getLocation(), target.getLocation()) > params.range()) return false;
        return canBreak(supportingBlock(target));
    }

    @Override
    public void run(int interval) {
        Player target = SpellTargets.playerTarget(owner);
        if (target == null) return;
        Block block = supportingBlock(target);
        if (!canBreak(block)) return;

        EntityChangeBlockEvent event = new EntityChangeBlockEvent(owner, block, Material.AIR.createBlockData());
        if (!event.callEvent()) return;

        owner.swingMainHand();
        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().toCenterLocation(), 30, 0.3, 0.3, 0.3, block.getBlockData());
        block.getWorld().playSound(block.getLocation(), block.getBlockSoundGroup().getBreakSound(), 1f, 0.8f);
        block.getWorld().playSound(owner.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1f, 0.7f);
        block.setType(Material.AIR);
    }

    private static Block supportingBlock(Player target) {
        return target.getLocation().subtract(0, 0.2, 0).getBlock();
    }

    private boolean canBreak(Block block) {
        Material type = block.getType();
        if (type.isAir() || block.isLiquid() || type.getHardness() < 0) return false;
        if (ALWAYS_PROTECTED.contains(type) || params.protectedBlocks().contains(type)) return false;
        return !(block.getState(false) instanceof Container);
    }

    @Override
    public int cooldownTicks() {
        return params.cooldownTicks();
    }

    public static UndermineSpell create(Params params, LivingEntity owner) {
        return new UndermineSpell(owner, params);
    }
}
