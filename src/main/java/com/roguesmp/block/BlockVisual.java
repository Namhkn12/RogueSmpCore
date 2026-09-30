package com.roguesmp.block;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class BlockVisual {

    /** Vanilla's own crack overlay is just these 10 shared textures laid over every block - same idea here. */
    public static final int CRACK_STAGES = 10;
    private static final float CRACK_OVERLAY_SCALE = 1.001f;

    public record Marker(String blockId, BlockPos pos) {}

    private BlockVisual() {}

    public static ItemDisplay spawn(BlockPos pos, BlockType<?> type) {
        World world = pos.getWorld();
        if (world == null) throw new IllegalStateException("World " + pos.world() + " is not loaded");

        ItemStack stack = displayStack(type);

        Location center = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, 0f, 0f);
        return world.spawn(center, ItemDisplay.class, display -> {
            display.setItemStack(stack);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            //Maybe in the future...
//            display.setTransformation(new Transformation(
//                    new Vector3f(),
//                    new AxisAngle4f(),
//                    new Vector3f(SCALE),
//                    new AxisAngle4f()
//            ));
            display.setPersistent(true);
            display.setInvulnerable(true);

            PersistentDataContainer data = display.getPersistentDataContainer();
            data.set(Keys.BLOCK_ID, PersistentDataType.STRING, type.id());
            data.set(Keys.BLOCK_POS, PersistentDataType.STRING, pos.serialize());
        });
    }

    public static ItemStack displayStack(BlockType<?> type) {
        ItemStack stack = ItemStack.of(Material.STONE);
        stack.setData(DataComponentTypes.ITEM_MODEL, type.model());
        return stack;
    }

    public static ItemStack displayStack(SmpBlock block) {
        Entity entity = Bukkit.getEntity(block.getDisplayId());
        ItemStack itemStack;
        if (entity instanceof ItemDisplay display) {
            itemStack = display.getItemStack();
        } else itemStack = displayStack(block.getType());
        return itemStack;
    }

    /**
     * A purely cosmetic entity co-located with the block's own display, showing one of the shared
     * {@code smp:destroy_stage_<0-9>} models. Never persisted - it must not survive a restart mid-mine.
     */
    public static Entity spawnCrackOverlay(BlockPos pos, int stage, Player player) {
        World world = pos.getWorld();
        if (world == null) throw new IllegalStateException("World " + pos.world() + " is not loaded");

        Location center = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        Entity entity = world.spawn(center, ItemDisplay.class, display -> {
            display.setItemStack(crackStageStack(stage));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(CRACK_OVERLAY_SCALE),
                    new AxisAngle4f()
            ));
            display.setPersistent(false);
            display.setInvulnerable(true);
        });
        entity.setVisibleByDefault(false);
        player.showEntity(RogueSmpCore.getInstance(), entity);
        return entity;
    }

    public static void updateCrackOverlay(Entity overlay, int stage) {
        if (!(overlay instanceof ItemDisplay display)) return;

        ItemStack stack = display.getItemStack();
        stack.setData(DataComponentTypes.ITEM_MODEL, crackStageKey(stage));
        display.setItemStack(stack);
    }

    private static ItemStack crackStageStack(int stage) {
        ItemStack stack = ItemStack.of(Material.STONE);
        stack.setData(DataComponentTypes.ITEM_MODEL, crackStageKey(stage));
        return stack;
    }

    private static Key crackStageKey(int stage) {
        return Keys.of("destroy_stage_" + stage);
    }

    public static @Nullable Marker read(Entity entity) {
        if (!(entity instanceof ItemDisplay)) return null;

        PersistentDataContainer data = entity.getPersistentDataContainer();
        String blockId = data.get(Keys.BLOCK_ID, PersistentDataType.STRING);
        String serializedPos = data.get(Keys.BLOCK_POS, PersistentDataType.STRING);
        if (blockId == null || serializedPos == null) return null;

        var pos = BlockPos.parse(serializedPos);
        if (!pos.isSuccess() || !pos.result().matches(entity.getLocation())) return null;
        return new Marker(blockId, pos.result());
    }
}
