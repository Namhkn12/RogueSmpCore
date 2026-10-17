package com.roguesmp;

import com.roguesmp.constant.DamageFlag;
import com.roguesmp.constant.DamageType;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DamageTypeKeys;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.damage.DamageScaling;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
public class RogueSmpBootstrap implements PluginBootstrap {
    private static final List<TagKey<org.bukkit.damage.@NotNull DamageType>> CUSTOM_MITIGATION_TAGS = List.of(
            DamageTypeTagKeys.BYPASSES_ARMOR,
            DamageTypeTagKeys.BYPASSES_ENCHANTMENTS,
            DamageTypeTagKeys.BYPASSES_EFFECTS,
            DamageTypeTagKeys.BYPASSES_RESISTANCE,
            DamageTypeTagKeys.BYPASSES_WOLF_ARMOR,
            DamageFlag.BYPASS_COOLDOWN.getTag()
    );

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DAMAGE_TYPE.compose().newHandler(event -> {
            for (DamageType type : DamageType.values()) {
                type.getVariantKeys().forEach(key -> event.registry().register(key, builder -> builder
                        .messageId(type.getMessageId())
                        .exhaustion(0.1f)
                        .damageScaling(DamageScaling.NEVER)));
            }
        }));

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.DAMAGE_TYPE), event -> {
            for (DamageType type : DamageType.values()) {
                for (int flags = 0; flags < DamageFlag.combinationCount(); flags++) {
                    List<TypedKey<org.bukkit.damage.@NotNull DamageType>> variant = List.of(type.getVariantKeys().get(flags));
                    for (DamageFlag flag : DamageFlag.values()) {
                        if ((flags & flag.mask()) != 0) event.registrar().addToTag(flag.getTag(), variant);
                    }
                }
            }

            Set<TypedKey<org.bukkit.damage.@NotNull DamageType>> allTypes = collectAllDamageTypes(event.registrar().getAllTags().values());
            CUSTOM_MITIGATION_TAGS.forEach(tag -> event.registrar().addToTag(tag, allTypes));
        });
    }

    private static Set<TypedKey<org.bukkit.damage.@NotNull DamageType>> collectAllDamageTypes(Collection<Collection<TypedKey<org.bukkit.damage.@NotNull DamageType>>> taggedTypes) {
        Set<TypedKey<org.bukkit.damage.@NotNull DamageType>> types = new HashSet<>();
        taggedTypes.forEach(types::addAll);
        for (DamageType type : DamageType.values()) {
            types.addAll(type.getVariantKeys());
        }

        for (Field field : DamageTypeKeys.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != TypedKey.class) continue;
            try {
                types.add((TypedKey<org.bukkit.damage.@NotNull DamageType>) field.get(null));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return types;
    }
}
