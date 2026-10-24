package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.utils.EntityUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public final class LastStand {

    private static final int PULSE_INTERVAL = 120;
    private static final int GUARDIAN_INTERVAL = 400;
    private static final int FIRST_GUARDIANS_DELAY = 60;
    private static final int MAX_GUARDIANS = 4;

    private final CrystalContext context;
    private final GuardianPack guardians;
    private @Nullable ShockwaveRing ring;
    private long nextPulseAt;
    private long nextGuardiansAt;
    private long chargeStartedAt = -1;
    private long chargeEndsAt = -1;

    public LastStand(CrystalContext context) {
        this.context = context;
        this.guardians = new GuardianPack(context.crystal(), context.arena());
    }

    public void start(long now) {
        nextPulseAt = now + PULSE_INTERVAL;
        nextGuardiansAt = now + FIRST_GUARDIANS_DELAY;
    }

    public void tick(long now, int interval) {
        guardians.tick(now, 1.0);

        if (now >= nextGuardiansAt) {
            int room = MAX_GUARDIANS - guardians.alive().size();
            if (room > 0) guardians.summon(Math.min(room, context.phase().guardianCount()));
            nextGuardiansAt = now + GUARDIAN_INTERVAL;
        }

        if (ring != null) {
            if (ring.tick(interval)) ring = null;
        } else if (chargeEndsAt >= 0) {
            ShockwaveRing.chargeEffect(context.crystal(), now - chargeStartedAt, chargeEndsAt - chargeStartedAt);
            if (now >= chargeEndsAt) {
                ring = new ShockwaveRing(context.crystal(), context.arena());
                chargeStartedAt = -1;
                chargeEndsAt = -1;
                nextPulseAt = now + PULSE_INTERVAL;
            }
        } else if (now >= nextPulseAt) {
            chargeStartedAt = now;
            chargeEndsAt = now + context.phase().pulseChargeTicks();
        }
    }

    public BarState status(long now) {
        if (chargeEndsAt >= 0) {
            return BarState.fill(Component.text("Xung Chấn Tinh Thể", NamedTextColor.RED),
                    now - chargeStartedAt, chargeEndsAt - chargeStartedAt, BossBar.Color.RED);
        }
        double health = context.crystal().getHealth() / EntityUtils.getMaxHealth(context.crystal());
        return new BarState(Component.text("Trái Tim Vỡ", NamedTextColor.GREEN), (float) health, BossBar.Color.GREEN);
    }

    public void cleanup() {
        guardians.removeAll();
        ring = null;
    }
}
