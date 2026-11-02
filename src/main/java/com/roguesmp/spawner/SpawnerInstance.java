package com.roguesmp.spawner;

import com.roguesmp.constant.Keys;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.spawner.behavior.BaseBehavior;
import com.roguesmp.spawner.behavior.SpawnerFx;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnerInstance {

    private static final double BEHAVIOR_PLAYER_RANGE = 20;

    private final String iid;
    private final Spawner template;
    private Location location;
    private final List<BaseBehavior> behaviors;
    private int delay;

    public SpawnerInstance(String iid, Spawner template, Location location, List<BaseBehavior> behaviors) {
        this.iid = iid;
        this.template = template;
        this.location = location;
        this.behaviors = List.copyOf(behaviors);
        this.delay = template.getDelay();
        this.behaviors.forEach(behavior -> behavior.bind(this));
    }

    public boolean runBreak(Player player, Location location) {
        boolean allow = true;
        for (BaseBehavior behavior : behaviors) {
            if (!behavior.onBreak(player, location)) allow = false;
        }
        return allow;
    }

    public boolean runSpawn(LivingEntity entity, List<Player> players, Location location) {
        boolean allow = true;
        for (BaseBehavior behavior : behaviors) {
            if (!behavior.onSpawn(entity, players, location)) allow = false;
        }
        return allow;
    }

    public void tick(int period, List<Player> viewers) {
        for (BaseBehavior behavior : behaviors) behavior.tick(location);

        double activeRangeSquared = (double) template.getActiveRange() * template.getActiveRange();
        Location center = SpawnerFx.center(location);
        boolean b = true;
        for (Player player : viewers) {
            if (player.getLocation().distanceSquared(center) <= activeRangeSquared) {
                b = false;
                break;
            }
        }
        if (b) return;

        delay -= period;
        if (delay > 0) return;
        delay = runSpawnCycle() ? delay + nextDelay() : 0;
    }

    private int nextDelay() {
        int min = template.getMinDelay();
        int max = template.getMaxDelay();
        return max <= min ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private boolean runSpawnCycle() {
        if (!SpawnerSystem.isActive(location)) return true;

        World world = location.getWorld();
        Location center = SpawnerFx.center(location);
        int range = template.getSpawnRange();
        int nearby = world.getNearbyEntities(center, range, range, range, this::isOwned).size();
        if (nearby >= template.getMaxNearBy()) return true;

        List<Player> players = List.copyOf(location.getNearbyPlayers(BEHAVIOR_PLAYER_RANGE));
        boolean spawned = false;
        for (int attempt = 0; attempt < template.getSpawnCount() && nearby < template.getMaxNearBy(); attempt++) {
            String mobId = pickMob();
            if (mobId == null) break;
            BaseEntity base = EntityManager.getInstance().getBaseEntity(mobId);
            if (base == null) continue;
            Location at = randomFreeSpot(world, range, base);
            if (at == null) continue;

            LivingEntity living = base.spawn(at).getEntity();
            living.getPersistentDataContainer().set(Keys.SPAWNER_OWNER, PersistentDataType.STRING, iid);
            spawned = true;
            nearby++;

            world.playEffect(location, Effect.MOBSPAWNER_FLAMES, null);
            living.playEffect(EntityEffect.ENTITY_POOF);
            if (!runSpawn(living, players, location) && living.isValid()) living.remove();
        }
        return spawned;
    }

    private boolean isOwned(Entity entity) {
        return iid.equals(entity.getPersistentDataContainer().get(Keys.SPAWNER_OWNER, PersistentDataType.STRING));
    }

    private String pickMob() {
        Map<String, Integer> mobs = template.getMobs();
        if (mobs == null || mobs.isEmpty()) return null;
        int total = mobs.values().stream().mapToInt(weight -> Math.max(0, weight)).sum();
        if (total <= 0) return null;

        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<String, Integer> entry : mobs.entrySet()) {
            roll -= Math.max(0, entry.getValue());
            if (roll < 0) return entry.getKey();
        }
        return null;
    }

    private Location randomFreeSpot(World world, int range, BaseEntity base) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x = location.getBlockX() + 0.5 + (random.nextDouble() - random.nextDouble()) * range;
        double y = location.getBlockY() + random.nextInt(3) - 1;
        double z = location.getBlockZ() + 0.5 + (random.nextDouble() - random.nextDouble()) * range;

        Location spot = new Location(world, x, y, z, random.nextFloat() * 360f, 0f);
        return world.hasCollisionsIn(base.boundingBoxAt(spot)) ? null : spot;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getId() {
        return iid;
    }

    public String getTemplateId() {
        return template.getId();
    }

    public List<BaseBehavior> getBehaviors() {
        return behaviors;
    }

    public Location getLocation() {
        return location;
    }
}
