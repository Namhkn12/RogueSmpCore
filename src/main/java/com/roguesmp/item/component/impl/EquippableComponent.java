package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Makes the item equippable in a slot through vanilla's {@code minecraft:equippable} data
 * component (see the wiki's Data component format page for what each field does). Optional
 * fields left {@code null}/empty fall back to vanilla's own defaults - unset {@code equip_sound}
 * keeps {@code item.armor.equip_generic}, unset {@code shearing_sound} keeps {@code item.shears.snip},
 * an empty {@code allowed_entities} means every entity can equip it.
 */
public record EquippableComponent(
        EquipmentSlot slot,
        @Nullable Key equipSound,
        @Nullable Key assetId,
        List<Key> allowedEntities,
        boolean dispensable,
        boolean swappable,
        boolean damageOnHurt,
        boolean equipOnInteract,
        @Nullable Key cameraOverlay,
        boolean canBeSheared,
        @Nullable Key shearingSound
) implements ItemComponent {

    public static final Codec<EquippableComponent> CODEC = Codec.composite(
            Codec.enumOf(EquipmentSlot.class).fieldOf("slot").forGetter(EquippableComponent::slot),
            Codec.KEY.optionalFieldOf("equip_sound").forGetter(c -> Optional.ofNullable(c.equipSound)),
            Codec.KEY.optionalFieldOf("asset_id").forGetter(c -> Optional.ofNullable(c.assetId)),
            Codec.listOf(Codec.KEY).optionalFieldOf("allowed_entities", List.of()).forGetter(EquippableComponent::allowedEntities),
            Codec.BOOLEAN.optionalFieldOf("dispensable", true).forGetter(EquippableComponent::dispensable),
            Codec.BOOLEAN.optionalFieldOf("swappable", true).forGetter(EquippableComponent::swappable),
            Codec.BOOLEAN.optionalFieldOf("damage_on_hurt", true).forGetter(EquippableComponent::damageOnHurt),
            Codec.BOOLEAN.optionalFieldOf("equip_on_interact", false).forGetter(EquippableComponent::equipOnInteract),
            Codec.KEY.optionalFieldOf("camera_overlay").forGetter(c -> Optional.ofNullable(c.cameraOverlay)),
            Codec.BOOLEAN.optionalFieldOf("can_be_sheared", false).forGetter(EquippableComponent::canBeSheared),
            Codec.KEY.optionalFieldOf("shearing_sound").forGetter(c -> Optional.ofNullable(c.shearingSound)),
            (slot, equipSound, assetId, allowedEntities, dispensable, swappable, damageOnHurt, equipOnInteract, cameraOverlay, canBeSheared, shearingSound) ->
                    new EquippableComponent(slot, equipSound.orElse(null), assetId.orElse(null), allowedEntities, dispensable, swappable,
                            damageOnHurt, equipOnInteract, cameraOverlay.orElse(null), canBeSheared, shearingSound.orElse(null))
    );

    public EquippableComponent {
        allowedEntities = List.copyOf(allowedEntities);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        Equippable.Builder builder = Equippable.equippable(slot)
                .dispensable(dispensable)
                .swappable(swappable)
                .damageOnHurt(damageOnHurt)
                .equipOnInteract(equipOnInteract)
                .canBeSheared(canBeSheared);

        if (equipSound != null) builder.equipSound(equipSound);
        if (assetId != null) builder.assetId(assetId);
        if (cameraOverlay != null) builder.cameraOverlay(cameraOverlay);
        if (shearingSound != null) builder.shearSound(shearingSound);
        if (!allowedEntities.isEmpty()) {
            List<TypedKey<@NotNull EntityType>> entityKeys = allowedEntities.stream()
                    .map(key -> TypedKey.create(RegistryKey.ENTITY_TYPE, key))
                    .toList();
            builder.allowedEntities(RegistrySet.keySet(RegistryKey.ENTITY_TYPE, entityKeys));
        }

        context.newStack().setData(DataComponentTypes.EQUIPPABLE, builder.build());
    }
}
