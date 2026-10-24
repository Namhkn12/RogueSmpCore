package com.roguesmp.entity.boss.hollowheart;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class CrystalPulseChallenge implements CrystalChallenge {

    private final CrystalContext context;
    private @Nullable ShockwaveRing ring;
    private int fired;
    private long chargeStartedAt = -1;
    private long chargeEndsAt = -1;

    public CrystalPulseChallenge(CrystalContext context) {
        this.context = context;
    }

    @Override
    public void start(long now) {
        beginCharge(now);
    }

    private void beginCharge(long now) {
        chargeStartedAt = now;
        chargeEndsAt = now + context.warning(context.phase().pulseChargeTicks());
    }

    @Override
    public Outcome tick(long now, int interval) {
        if (chargeEndsAt >= 0) {
            ShockwaveRing.chargeEffect(context.crystal(), now - chargeStartedAt, chargeEndsAt - chargeStartedAt);
            if (now >= chargeEndsAt) {
                chargeStartedAt = -1;
                chargeEndsAt = -1;
                ring = new ShockwaveRing(context.crystal(), context.arena());
            }
            return Outcome.RUNNING;
        }

        if (ring != null && ring.tick(interval)) {
            ring = null;
            fired++;
            if (fired >= context.phase().pulseCount()) return Outcome.SUCCESS;
            beginCharge(now);
        }
        return Outcome.RUNNING;
    }

    @Override
    public BarState status(long now) {
        Component detail = Component.text((fired + 1) + "/" + context.phase().pulseCount(), NamedTextColor.GRAY);
        if (chargeEndsAt >= 0) {
            return BarState.fill(detail, now - chargeStartedAt, chargeEndsAt - chargeStartedAt, BossBar.Color.RED);
        }
        return new BarState(detail, 1f, BossBar.Color.RED);
    }

    @Override
    public void cleanup() {
        ring = null;
    }
}
