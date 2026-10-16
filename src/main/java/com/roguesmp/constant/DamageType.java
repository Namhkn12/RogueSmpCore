package com.roguesmp.constant;

import com.roguesmp.RogueSmpCore;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.DamageTypeKeys;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.damage.DamageSource;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public enum DamageType {
    MELEE("mob"),
    MELEE_ABILITY("mob"),
    PROJECTILE("arrow"),
    PROJECTILE_ABILITY("arrow"),
    UNSCALEABLE("generic"),
    UNSCALABLE_ABILITY("generic"),
    UNSCALABLE_ENCHANT("generic"),
    MAGIC("magic"),
    THORNS("thorns", false, true),
    BLAST("explosion"),
    FIRE("onFire", false, true),
    FALL("fall", false, true),
    AILMENT("magic", false, false),
    TRUE("generic", false, true),
    OTHER("generic");

    public static final String NAMESPACE = "roguesmp";

    private final List<TypedKey<org.bukkit.damage.@NotNull DamageType>> variantKeys;
    private final String messageId;
    private final boolean blockable;
    private final boolean knockback;

    DamageType(String messageId) {
        this(messageId, true, true);
    }

    DamageType(String messageId, boolean blockable, boolean knockback) {
        this.variantKeys = IntStream.range(0, DamageFlag.combinationCount()).mapToObj(this::createVariantKey).toList();
        this.messageId = messageId;
        this.blockable = blockable;
        this.knockback = knockback;
    }

    private TypedKey<org.bukkit.damage.@NotNull DamageType> createVariantKey(int flags) {
        StringBuilder path = new StringBuilder(name().toLowerCase(Locale.ROOT));
        for (DamageFlag flag : DamageFlag.values()) {
            if ((flags & flag.mask()) != 0) path.append('/').append(flag.name().toLowerCase(Locale.ROOT));
        }
        return TypedKey.create(RegistryKey.DAMAGE_TYPE, Key.key(NAMESPACE, path.toString()));
    }

    public List<TypedKey<org.bukkit.damage.@NotNull DamageType>> getVariantKeys() {
        return variantKeys;
    }

    public boolean isBlockableByDefault() {
        return blockable;
    }

    public boolean knocksBackByDefault() {
        return knockback;
    }

    public String getMessageId() {
        return messageId;
    }

    public org.bukkit.damage.DamageType asVanilla(int flags) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).getOrThrow(variantKeys.get(flags));
    }

    private record Variant(DamageType type, int flags) {
    }

    private static final Map<Key, Variant> PLUGIN_VARIANTS = new HashMap<>();
    private static final Map<Key, DamageType> VANILLA_TYPES = new HashMap<>();
    private static final Set<Key> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    static {
        for (DamageType type : values()) {
            for (int flags = 0; flags < type.variantKeys.size(); flags++) {
                PLUGIN_VARIANTS.put(type.variantKeys.get(flags).key(), new Variant(type, flags));
            }
        }

        mapVanilla(MELEE, DamageTypeKeys.PLAYER_ATTACK, DamageTypeKeys.MOB_ATTACK, DamageTypeKeys.MOB_ATTACK_NO_AGGRO,
                DamageTypeKeys.STING, DamageTypeKeys.SPEAR, DamageTypeKeys.MACE_SMASH);
        mapVanilla(PROJECTILE, DamageTypeKeys.ARROW, DamageTypeKeys.TRIDENT, DamageTypeKeys.MOB_PROJECTILE,
                DamageTypeKeys.THROWN, DamageTypeKeys.SPIT, DamageTypeKeys.WIND_CHARGE, DamageTypeKeys.FIREBALL,
                DamageTypeKeys.UNATTRIBUTED_FIREBALL, DamageTypeKeys.WITHER_SKULL);
        mapVanilla(MAGIC, DamageTypeKeys.MAGIC, DamageTypeKeys.INDIRECT_MAGIC, DamageTypeKeys.SONIC_BOOM);
        mapVanilla(THORNS, DamageTypeKeys.THORNS);
        mapVanilla(BLAST, DamageTypeKeys.EXPLOSION, DamageTypeKeys.PLAYER_EXPLOSION, DamageTypeKeys.BAD_RESPAWN_POINT,
                DamageTypeKeys.FIREWORKS);
        mapVanilla(FIRE, DamageTypeKeys.IN_FIRE, DamageTypeKeys.ON_FIRE, DamageTypeKeys.LAVA, DamageTypeKeys.HOT_FLOOR,
                DamageTypeKeys.CAMPFIRE, DamageTypeKeys.SULFUR_CUBE_HOT);
        mapVanilla(FALL, DamageTypeKeys.FALL, DamageTypeKeys.FLY_INTO_WALL, DamageTypeKeys.STALAGMITE,
                DamageTypeKeys.ENDER_PEARL);
        mapVanilla(AILMENT, DamageTypeKeys.WITHER);
        mapVanilla(TRUE, DamageTypeKeys.OUT_OF_WORLD, DamageTypeKeys.GENERIC_KILL, DamageTypeKeys.OUTSIDE_BORDER);
        mapVanilla(OTHER, DamageTypeKeys.CACTUS, DamageTypeKeys.CRAMMING, DamageTypeKeys.DRAGON_BREATH,
                DamageTypeKeys.DROWN, DamageTypeKeys.DRY_OUT, DamageTypeKeys.FALLING_ANVIL, DamageTypeKeys.FALLING_BLOCK,
                DamageTypeKeys.FALLING_STALACTITE, DamageTypeKeys.FREEZE, DamageTypeKeys.GENERIC, DamageTypeKeys.IN_WALL,
                DamageTypeKeys.LIGHTNING_BOLT, DamageTypeKeys.STARVE, DamageTypeKeys.SWEET_BERRY_BUSH);
    }

    @SafeVarargs
    private static void mapVanilla(DamageType type, TypedKey<org.bukkit.damage.@NotNull DamageType>... keys) {
        for (TypedKey<org.bukkit.damage.@NotNull DamageType> key : keys) {
            VANILLA_TYPES.put(key.key(), type);
        }
    }

    public static boolean hasFlag(DamageSource source, DamageFlag flag) {
        Variant variant = PLUGIN_VARIANTS.get(source.getDamageType().key());
        return variant != null && (variant.flags() & flag.mask()) != 0;
    }

    public static boolean isPluginSource(DamageSource source) {
        return PLUGIN_VARIANTS.containsKey(source.getDamageType().key());
    }

    public static DamageType fromSource(DamageSource source) {
        Key key = source.getDamageType().key();
        Variant variant = PLUGIN_VARIANTS.get(key);
        if (variant != null) return variant.type();

        DamageType vanilla = VANILLA_TYPES.get(key);
        if (vanilla != null) return vanilla;

        DamageType fallback = fromVanillaTags(key);
        if (WARNED_KEYS.add(key)) {
            RogueSmpCore.LOGGER.warn("Unmapped damage type {}, classified as {}", key.asString(), fallback);
        }
        return fallback;
    }

    private static DamageType fromVanillaTags(Key key) {
        Registry<org.bukkit.damage.@NotNull DamageType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE);
        TypedKey<org.bukkit.damage.@NotNull DamageType> typedKey = TypedKey.create(RegistryKey.DAMAGE_TYPE, key);
        if (registry.getTag(DamageTypeTagKeys.IS_PROJECTILE).contains(typedKey)) return PROJECTILE;
        if (registry.getTag(DamageTypeTagKeys.IS_EXPLOSION).contains(typedKey)) return BLAST;
        if (registry.getTag(DamageTypeTagKeys.IS_FIRE).contains(typedKey)) return FIRE;
        if (registry.getTag(DamageTypeTagKeys.IS_FALL).contains(typedKey)) return FALL;
        return OTHER;
    }

    public static boolean isMeleeDamage(DamageType type) {
        return type == MELEE || type == MELEE_ABILITY;
    }

    public static boolean isProjectileDamage(DamageType type) {
        return type == PROJECTILE || type == PROJECTILE_ABILITY;
    }

    public static boolean isRegicideAffected(DamageType type) {
        return isMeleeDamage(type) || isProjectileDamage(type) || type == MAGIC;
    }
}
