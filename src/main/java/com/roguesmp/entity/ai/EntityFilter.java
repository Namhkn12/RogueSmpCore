package com.roguesmp.entity.ai;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.registry.Registries;
import com.roguesmp.tag.SmpTag;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/**
 * Matches entities by {@code minecraft:<type>}, {@code #<custom entity tag>} or a custom entity id.
 */
public record EntityFilter(String raw) {

    private static final String VANILLA_PREFIX = "minecraft:";

    public static final Codec<EntityFilter> CODEC = Codec.STRING.xmap(EntityFilter::new, EntityFilter::raw);
    public static final EntityFilter PLAYER = new EntityFilter(VANILLA_PREFIX + "player");

    public boolean test(Entity entity) {
        if (raw.startsWith("#")) {
            SmpTag<BaseEntity> tag = Registries.ENTITY.getTag(raw.substring(1));
            BaseEntity base = EntityUtils.getBaseEntity(entity);
            return tag != null && base != null && tag.contains(base.getId());
        }
        if (raw.startsWith(VANILLA_PREFIX)) {
            NamespacedKey key = NamespacedKey.fromString(raw);
            EntityType type = key == null ? null : Registry.ENTITY_TYPE.get(key);
            return type != null && entity.getType() == type;
        }
        return raw.equals(EntityUtils.getBaseEntityId(entity));
    }

    public boolean test(net.minecraft.world.entity.Entity entity) {
        return test(entity.getBukkitEntity());
    }
}
