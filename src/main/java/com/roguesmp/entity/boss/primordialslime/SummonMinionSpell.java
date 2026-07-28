package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.spell.Spell;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SummonMinionSpell extends Spell {
    private final LivingEntity owner;
    private final PrimordialSlime boss;
    private final Location altarLocation;
    private final List<String> minionIds;
    private final int minAmount;
    private final int maxAmount;
    private final double spawnRadius;
    private final Random random = new Random();

    // Danh sách câu thoại ngẫu nhiên
    private final List<Component> spawnMessages = Arrays.asList(
            Component.text("Trỗi dậy đi, những đứa con của ta!", NamedTextColor.DARK_GREEN),
            Component.text("Các ngươi nghĩ mình đủ đông sao? Hãy nếm thử cái này!", NamedTextColor.GREEN),
            Component.text("Hòa quyện và sinh sôi... tiêu diệt kẻ thù cho ta!", NamedTextColor.DARK_RED, TextDecoration.ITALIC)
    );

    private int currentCooldown = 0;

    public SummonMinionSpell(LivingEntity owner, PrimordialSlime boss, Location altarLocation, List<String> minionIds, int minAmount, int maxAmount, double spawnRadius) {
        this.owner = owner;
        this.boss = boss;
        this.altarLocation = altarLocation;
        this.minionIds = minionIds;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.spawnRadius = spawnRadius;
    }

    @Override
    public void run(int interval) {
        currentCooldown = currentCooldown + interval;
        if (currentCooldown < cooldownTicks()) return;
        currentCooldown = 0;

        if (owner == null || !owner.isValid() || minionIds.isEmpty()) return;

        // 1. Gửi lời thoại ngẫu nhiên
        sendRandomDialogue();

        // Tính toán số lượng spawn
        int amountToSpawn = random.nextInt((maxAmount - minAmount) + 1) + minAmount;

        // Hiệu ứng bắt đầu triệu hồi
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.5f);
        owner.getWorld().spawnParticle(Particle.SOUL, owner.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        for (int i = 0; i < amountToSpawn; i++) {
            Location spawnLoc = getRandomLocationAroundAltar();
            String selectedId = minionIds.get(random.nextInt(minionIds.size()));

            spawnLoc.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 15, 0.1, 0.1, 0.1, 0.05);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (owner.isDead() || !owner.isValid()) return;

                    spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 10, 0.2, 0.5, 0.2, 0.05);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_SLIME_JUMP, 1f, 1.5f);

                    EntityManager.getInstance().spawnEntity(selectedId, spawnLoc);
                }
            }.runTaskLater(RogueSmpCore.getInstance(), 15L);
        }
    }

    private void sendRandomDialogue() {
        Component message = spawnMessages.get(random.nextInt(spawnMessages.size()));
        Component formattedMessage = Component.text("[" + owner.getName() + "] ", NamedTextColor.DARK_GRAY)
                .append(message);

        // Gửi cho tất cả người chơi trong khu vực (Dựa trên hàm getParticipants của Boss)
        // Nếu bạn không có hàm PlayerUtils, bạn có thể loop qua người chơi trong world và check distance
        for (Player p : boss.getParticipants()) {
            p.sendMessage(formattedMessage);

        }
    }

    private Location getRandomLocationAroundAltar() {
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = random.nextDouble() * spawnRadius;
            double x = altarLocation.getX() + r * Math.cos(angle);
            double z = altarLocation.getZ() + r * Math.sin(angle);

            Location loc = new Location(altarLocation.getWorld(), x, altarLocation.getY(), z);
            Block block = loc.getWorld().getHighestBlockAt(loc);
            Location finalLoc = block.getLocation().add(0.5, 1, 0.5);

            if (finalLoc.getBlock().getType().isAir()) {
                return finalLoc;
            }
        }
        return altarLocation.clone().add(1, 1, 0);
    }

    @Override
    public int cooldownTicks() {
        return 240; // Tăng lên một chút để lời thoại không bị spam quá nhiều
    }
}
