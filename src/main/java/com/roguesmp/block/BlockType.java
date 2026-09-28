package com.roguesmp.block;

import com.roguesmp.constant.Keys;
import com.roguesmp.item.ItemType;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.key.Key;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A custom block's compile-time identity: its id and the factory for its {@link SmpBlock} subclass.
 * The id is independent of any item; items place a block through {@code BlockPlaceComponent}. Its tunable data is not stored here - it is loaded
 * into {@link Registries#BLOCK_PROPERTIES} and read through a {@link Holder}, so a data reload takes
 * effect immediately. A declared type with no {@code blocks/<id>.json} uses {@link BlockProperties#DEFAULT}
 * and is reported by {@code Registry.validateAllHolders()}. Declare instances as constants in {@link BlockTypes}.
 */
public final class BlockType<T extends SmpBlock> {

    private final String id;
    private final Supplier<T> factory;
    private final Holder<BlockProperties> properties;

    BlockType(String id, Supplier<T> factory) {
        this.id = id;
        this.factory = factory;
        this.properties = Registries.BLOCK_PROPERTIES.getHolder(id);
    }

    public T create() {
        T block = factory.get();
        block.setType(this);
        return block;
    }

    public String id() {
        return id;
    }

    public BlockProperties properties() {
        return properties.isBound() ? properties.value() : BlockProperties.DEFAULT;
    }

    public int hardness() {
        return properties().hardness();
    }

    public Set<Holder<ItemType>> tools() {
        return properties().tools();
    }

    public int breakStrength() {
        return properties().breakStrength();
    }

    public Key model() {
        return properties().model();
    }

    public List<BlockDrop> drops() {
        return properties().drops();
    }

    public BlockExperience experience() {
        return properties().experience();
    }

    public Key placeSound() {
        return properties().placeSound();
    }

    public Key breakSound() {
        return properties().breakSound();
    }
}
