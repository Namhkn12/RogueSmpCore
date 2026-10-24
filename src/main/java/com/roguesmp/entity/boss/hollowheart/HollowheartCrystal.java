package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.PhaseComponent;
import com.roguesmp.event.DamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class HollowheartCrystal extends SmpEntity {

    public static final String ID = "hollowheart_crystal";
    public static final String ARENA_REGION = "hollowheart_arena";

    private static final int BLOCKED_FEEDBACK_COOLDOWN = 10;
    private static final Color CORE_GLOW = Color.fromRGB(0xE0A8FF);

    private final CrystalBody body;
    private final CrystalCycleComponent cycle;
    private final CrystalEffects effects;
    private int nextBlockedFeedbackTick;

    public HollowheartCrystal(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        this.body = new CrystalBody(entity, Material.AMETHYST_BLOCK.createBlockData(), 1.0f);

        Arena arena = Arena.resolve(entity.getLocation(), ARENA_REGION);
        this.effects = new CrystalEffects(entity, arena);
        this.cycle = new CrystalCycleComponent(this, arena, effects, List.of(
                type(CrystalPhase.SHARDS, "Mảnh Tinh Thể", NamedTextColor.LIGHT_PURPLE, CrystalShardsChallenge::new),
                type(CrystalPhase.FLOOR, "Mặt Đất Tha Hóa", NamedTextColor.DARK_PURPLE, CorruptedFloorChallenge::new),
                type(CrystalPhase.GUARDIANS, "Hộ Vệ Tinh Thể", NamedTextColor.AQUA, CrystalGuardiansChallenge::new),
                type(CrystalPhase.ORBS, "Quả Cầu Năng Lượng", NamedTextColor.BLUE, EnergyOrbsChallenge::new),
                type(CrystalPhase.PULSE, "Xung Chấn Tinh Thể", NamedTextColor.RED, CrystalPulseChallenge::new),
                type(CrystalPhase.OVERLOAD, "Quá Tải Tinh Thể", NamedTextColor.GOLD, OverloadChallenge::new)
        ));
        setComponent(CrystalCycleComponent.KEY, cycle);
        setComponent(EntityComponentKeys.PHASE, new PhaseComponent(Map.of(
                80, boss -> cycle.enterPhase(1),
                60, boss -> cycle.enterPhase(2),
                40, boss -> cycle.enterPhase(3),
                20, boss -> cycle.enterPhase(4)
        ), true));
    }

    private static CrystalCycleComponent.ChallengeType type(String id, String name, NamedTextColor color,
                                                            Function<CrystalContext, CrystalChallenge> factory) {
        return new CrystalCycleComponent.ChallengeType(id, Component.text(name, color), factory);
    }

    @Override
    protected void onInitialized() {
        body.setup();
    }

    public void setCoreOpen(boolean open) {
        body.setGlow(open, CORE_GLOW);
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (!cycle.isExposed()) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player && Bukkit.getCurrentTick() >= nextBlockedFeedbackTick) {
                nextBlockedFeedbackTick = Bukkit.getCurrentTick() + BLOCKED_FEEDBACK_COOLDOWN;
                player.sendActionBar(Component.text("Lõi đang được bảo vệ!", NamedTextColor.GRAY));
                player.playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.5f);
            }
            return;
        }
        super.onHurt(event);
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        super.onDeath(event);
        effects.death();
        body.remove();
    }

    @Override
    public void unload() {
        super.unload();
        body.remove();
    }
}
