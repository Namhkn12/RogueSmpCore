package com.roguesmp.block.impl.altar;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public abstract class AltarBlock extends SmpBlock implements Tickable {

    private static final MapCodec<Optional<ItemStack>> ITEM_CODEC = Codec.ITEM_STACK.optionalFieldOf("item");

    private final AltarItemDisplay display = new AltarItemDisplay();
    private @Nullable ItemStack item;
    private @Nullable Location hoverLocation;
    private boolean locked;

    protected AltarBlock(BlockProperties properties) {
        super(properties);
    }

    public @Nullable ItemStack getItem() {
        return item;
    }

    public void setItem(@Nullable ItemStack newItem) {
        item = newItem;
        markDirty();
        display.refresh(this);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        if (!locked) display.settle();
    }

    public Location getHoverLocation() {
        if (hoverLocation == null) hoverLocation = getLocation().clone().add(0.5, AltarItemDisplay.HOVER_HEIGHT, 0.5);
        return hoverLocation;
    }

    void animateDisplay(Vector3f translation, float angle, float scale, int duration) {
        display.animate(translation, angle, scale, duration);
    }

    @Override
    public void onHydrated() {
        display.refresh(this);
    }

    @Override
    public void tick() {
        if (!locked) display.tick(this);
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        if (locked) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) onEmptyHandClick(player);
        else swapWithHand(player, held);
    }

    protected void onEmptyHandClick(Player player) {
        takeItem(player);
    }

    protected void takeItem(Player player) {
        if (item == null) return;

        PlayerUtils.giveItem(player, item);
        setItem(null);
        playSound(Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM);
    }

    private void swapWithHand(Player player, ItemStack held) {
        ItemStack previous = item;
        setItem(held.clone());
        player.getInventory().setItemInMainHand(null);
        if (previous != null) PlayerUtils.giveItem(player, previous);
        playSound(Sound.ENTITY_ITEM_FRAME_ADD_ITEM);
    }

    private void playSound(Sound sound) {
        Location hover = getHoverLocation();
        hover.getWorld().playSound(hover, sound, 1f, 1f);
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (item == null) return;

        Location center = getLocation().toCenterLocation();
        center.getWorld().dropItemNaturally(center, item);
        item = null;
    }

    @Override
    public void onUnload() {
        display.remove();
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(ITEM_CODEC, () -> Optional.ofNullable(item), value -> item = value.orElse(null)));
    }
}
