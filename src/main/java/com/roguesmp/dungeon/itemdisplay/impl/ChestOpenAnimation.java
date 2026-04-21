package com.roguesmp.dungeon.itemdisplay.impl;

import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.itemdisplay.AnimationHandle;
import com.roguesmp.dungeon.itemdisplay.FloatingItemAnimation;
import com.roguesmp.dungeon.task.TaskScheduler;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChestOpenAnimation {

    private final Plugin plugin;
    private final TaskScheduler taskScheduler;

    // Giữ handle để cancel nếu player disconnect
    private final Map<UUID, AnimationHandle> activeAnimations = new HashMap<>();

    public ChestOpenAnimation(Plugin plugin, TaskScheduler taskScheduler) {
        this.plugin = plugin;
        this.taskScheduler = taskScheduler;
    }

    public void play(Player clicker, Block block, List<ItemStack> items) {

        Location center = getAnimCenter(block);
        clicker.playSound(center, Sound.BLOCK_CHEST_OPEN, 1f, 0.7f);

        AnimationHandle handle = FloatingItemAnimation
                .builder(plugin, center, taskScheduler)
                .duration(50)
                .spinSpeed(8f)
                .items(items)
                .iconCycleInterval(5)
                .displayScale(0.7f)

                .onStart(ctx ->
                        clicker.sendActionBar(Component.text("Opening...", NamedTextColor.GOLD))
                )

                .onTick(ctx -> {
                    // Particle dọn đường mỗi 10 tick, trước tick 40
                    if (ctx.getTick() % 10 == 0 && ctx.getTick() < 40) {
                        ctx.getLocation().getWorld().spawnParticle(
                                Particle.END_ROD,
                                ctx.getLocation().clone().add(0, 1.8, 0),
                                6, 0.3, 0.3, 0.3, 0.04
                        );
                    }

                    // Tick 40: bắn firework rồi cancel animation
                    if (ctx.getTick() == 40) {
                        spawnFirework(ctx.getLocation().clone().add(0, 1.0, 0));
                        clicker.playSound(ctx.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
                        clicker.playSound(ctx.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.3f);
                        ctx.cancel(); // → trigger onEnd
                    }
                })

                .onEnd(ctx -> {
                    if (block.getState() instanceof TileState freshState) {
                        freshState.getPersistentDataContainer().remove(NameSpaceKeys.REWARD_CID_KEY);
                        freshState.update();
                    }
                })

                .build()
                .play();

        activeAnimations.put(clicker.getUniqueId(), handle);
    }

    // Cancel nếu player disconnect giữa chừng
    public void cancelFor(Player player) {
        AnimationHandle handle = activeAnimations.remove(player.getUniqueId());
        if (handle != null) handle.cancel();
    }

    // ... getAnimCenter, spawnFirework giữ nguyên như cũ
    private Location getAnimCenter(Block block) {
        if (!(block.getState() instanceof Chest chest)) return block.getLocation().add(0.5, 0, 0.5);

        if (chest.getInventory() instanceof DoubleChestInventory dci) {
            DoubleChest dc = (DoubleChest) dci.getHolder();
            if (dc != null) {
                return dc.getLocation();
            }
        }
        return block.getLocation().add(0.5, 0, 0.5);
    }

    private ItemDisplay spawnSpinDisplay(Location center, List<ItemStack> items) {
        // Hiển thị item "xịn nhất" (cuối list nếu sort theo rarity)
        ItemStack showcase = items.isEmpty()
                ? new ItemStack(Material.CHEST)
                : items.get(items.size() - 1);

        Location spawnLoc = center.clone().add(0, 1.5, 0);

        return center.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(showcase);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            d.setBillboard(Display.Billboard.FIXED);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(0.7f, 0.7f, 0.7f),
                    new Quaternionf()
            ));
            d.setGlowing(true);
        });
    }

    private BukkitTask startSpinTask(ItemDisplay display) {
        return new BukkitRunnable() {
            float angle = 0f;

            @Override
            public void run() {
                if (display.isDead()) { cancel(); return; }
                angle = (angle + 8f) % 360f;

                Quaternionf rot = new Quaternionf()
                        .rotateY((float) Math.toRadians(angle));
                Transformation t = display.getTransformation();
                display.setTransformation(new Transformation(
                        t.getTranslation(), rot, t.getScale(), t.getRightRotation()
                ));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Firework BURST trắng + vàng, không fly (power 0), tự detonate ngay.
     */
    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.WHITE, Color.YELLOW)
                .withFade(Color.ORANGE)
                .trail(false)
                .flicker(true)
                .build();

        meta.addEffect(effect);
        meta.setPower(0);
        fw.setFireworkMeta(meta);

        // Detonate ngay tick tiếp theo (power=0 vẫn cần 1 tick)
        new BukkitRunnable() {
            @Override public void run() {
                if (!fw.isDead()) fw.detonate();
            }
        }.runTaskLater(plugin, 1L);
    }

    private List<Player> getPartyPlayers(Party party, Player fallback) {
        if (party == null) return List.of(fallback);
        return party.getMembers().stream()
                .map(uuid -> plugin.getServer().getPlayer(uuid))
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toList());
    }
}