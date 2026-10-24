package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class CrystalMinion extends SmpEntity {

    public static final String SHARD_ID = "crystal_shard";
    public static final String ORB_ID = "crystal_orb";
    public static final String CONDUIT_ID = "crystal_conduit";

    private final CrystalBody body;

    public CrystalMinion(BaseEntity base, LivingEntity entity, BlockData look, float scale) {
        super(base, entity);
        this.body = new CrystalBody(entity, look, scale);
    }

    public static CrystalMinion shard(BaseEntity base, LivingEntity entity) {
        return new CrystalMinion(base, entity, Material.AMETHYST_CLUSTER.createBlockData(), 0.8f);
    }

    public static CrystalMinion orb(BaseEntity base, LivingEntity entity) {
        return new CrystalMinion(base, entity, Material.LARGE_AMETHYST_BUD.createBlockData(), 0.6f);
    }

    public static CrystalMinion conduit(BaseEntity base, LivingEntity entity) {
        return new CrystalMinion(base, entity, Material.BUDDING_AMETHYST.createBlockData(), 0.9f);
    }

    public CrystalBody body() {
        return body;
    }

    @Override
    protected void onInitialized() {
        body.setup();
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        super.onDeath(event);
        event.setDroppedExp(0);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.8f);
        body.remove();
    }

    @Override
    public void unload() {
        super.unload();
        body.remove();
    }
}
