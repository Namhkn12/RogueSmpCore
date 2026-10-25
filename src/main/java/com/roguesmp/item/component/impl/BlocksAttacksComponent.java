package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record BlocksAttacksComponent(
        float blockDelaySeconds,
        float horizontalBlockingAngle,
        Key blockSound,
        Key disabledSound
) implements ItemComponent {

    public static final Key DEFAULT_BLOCK_SOUND = Key.key("item.shield.block");
    public static final Key DEFAULT_DISABLED_SOUND = Key.key("item.shield.break");

    public static final Codec<BlocksAttacksComponent> CODEC = Codec.composite(
            Codec.FLOAT.optionalFieldOf("block_delay_seconds", 0f).forGetter(BlocksAttacksComponent::blockDelaySeconds),
            Codec.FLOAT.optionalFieldOf("horizontal_blocking_angle", 90f).forGetter(BlocksAttacksComponent::horizontalBlockingAngle),
            Codec.KEY.optionalFieldOf("block_sound", DEFAULT_BLOCK_SOUND).forGetter(BlocksAttacksComponent::blockSound),
            Codec.KEY.optionalFieldOf("disabled_sound", DEFAULT_DISABLED_SOUND).forGetter(BlocksAttacksComponent::disabledSound),
            BlocksAttacksComponent::new
    );

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.BLOCKS_ATTACKS, BlocksAttacks.blocksAttacks()
                .blockDelaySeconds(blockDelaySeconds)
                .damageReductions(List.of(DamageReduction.damageReduction()
                        .base(0f)
                        .factor(1f)
                        .horizontalBlockingAngle(horizontalBlockingAngle)
                        .build()))
                .blockSound(blockSound)
                .disableSound(disabledSound)
                .build());
    }
}
