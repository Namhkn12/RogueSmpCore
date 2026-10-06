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

    public static final int CRACK_STAGES = 10;
    private static final float CRACK_OVERLAY_SCALE = 1.001f;

    public record Marker(String blockId, BlockPos pos) {}

    private BlockVisual() {}

    public static ItemDisplay spawn(BlockPos pos, SmpBlock block) {
        World world = pos.getWorld();
        if (world == null) throw new IllegalStateException("World " + pos.world() + " is not loaded");

        ItemStack stack = displayStack(block.getDisplayModel());
        String blockId = block.getId();

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
            data.set(Keys.BLOCK_ID, PersistentDataType.STRING, blockId);
            data.set(Keys.BLOCK_POS, PersistentDataType.STRING, pos.serialize());
        });
    }

    public static ItemStack displayStack(Key model) {
        ItemStack stack = ItemStack.of(Material.STONE);
        stack.setData(DataComponentTypes.ITEM_MODEL, model);
        return stack;
    }

    public static void refreshModel(SmpBlock block) {
        Entity entity = Bukkit.getEntity(block.getDisplayId());
        if (entity instanceof ItemDisplay display) {
            display.setItemStack(displayStack(block.getDisplayModel()));
        }
    }

    public static ItemStack displayStack(SmpBlock block) {
        Entity entity = Bukkit.getEntity(block.getDisplayId());
        ItemStack itemStack;
        if (entity instanceof ItemDisplay display) {
            itemStack = display.getItemStack();
        } else itemStack = displayStack(block.getDisplayModel());
        return itemStack;
    }

    /**
     * A purely cosmetic entity co-located with a block's own display, showing one of the shared
     * {@code smp:destroy_stage_<0-9>} models. This entity is not persistent and is visible to everyone nearby.
     */
    public static Entity spawnCrackOverlay(BlockPos pos, int stage) {
        World world = pos.getWorld();
        if (world == null) throw new IllegalStateException("World " + pos.world() + " is not loaded");

        Location center = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        return world.spawn(center, ItemDisplay.class, display -> {
            display.setItemStack(displayStack(crackStageKey(stage)));
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
    }

    /** Same as {@link #spawnCrackOverlay(BlockPos, int)}, but visible only to {@code player}. */
    public static Entity spawnCrackOverlay(BlockPos pos, int stage, Player player) {
        Entity entity = spawnCrackOverlay(pos, stage);
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

    private static final double CRACK_DAMAGE_RANGE = 8;

    /**
     * Vanilla block-damage animation, for a block that isn't a custom. {@code stage} is 0-9 like {@link #CRACK_STAGES}, or
     * negative to clear it.
     */
    public static void sendBlockDamage(BlockPos pos, int stage) {
        World world = pos.getWorld();
        if (world == null) return;
        Location loc = new Location(world, pos.x(), pos.y(), pos.z());
        float progress = stage < 0 ? 0 : stage / (float) (CRACK_STAGES - 1);
        double rangeSquared = CRACK_DAMAGE_RANGE * CRACK_DAMAGE_RANGE;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= rangeSquared) {
                player.sendBlockDamage(loc, progress, pos.hashCode());
            }
        }
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
