package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKey;
import com.roguesmp.entity.component.TickingComponent;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CrystalCycleComponent implements TickingComponent {

    public static final EntityComponentKey<CrystalCycleComponent> KEY = new EntityComponentKey<>("crystal_cycle");

    public record ChallengeType(String id, Component name, Function<CrystalContext, CrystalChallenge> factory) {
    }

    private enum State { WAITING, TRANSITION, REST, TELEGRAPH, CHALLENGE, EXPOSED, PUNISH, LAST_STAND }

    private static final int START_DELAY_TICKS = 60;
    private static final int TELEGRAPH_TICKS = 40;
    private static final int PUNISH_TICKS = 40;
    private static final int TRANSITION_TICKS = 60;
    private static final int INTRODUCED_HAZARD_DELAY = 100;

    private final HollowheartCrystal crystal;
    private final Arena arena;
    private final CrystalEffects effects;
    private final Map<String, ChallengeType> types = new LinkedHashMap<>();
    private final ChargeUpManager bar;
    private final ChargeUpManager overlayBar;

    private int phaseIndex;
    private int empower;
    private @Nullable String forcedNext;

    private State state = State.WAITING;
    private int stateTicks;
    private long tick;
    private @Nullable ChallengeType currentType;
    private @Nullable ChallengeType lastType;
    private @Nullable CrystalChallenge challenge;
    private @Nullable ChallengeType hazardType;
    private @Nullable CrystalChallenge hazard;
    private @Nullable String forcedHazard;
    private long nextHazardAt = -1;
    private int challengeEmpower;
    private @Nullable LastStand lastStand;

    public CrystalCycleComponent(HollowheartCrystal crystal, Arena arena, CrystalEffects effects, List<ChallengeType> types) {
        this.crystal = crystal;
        this.arena = arena;
        this.effects = effects;
        this.bar = new ChargeUpManager(arena::players, 100, Component.empty(), BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        this.overlayBar = new ChargeUpManager(arena::players, 100, Component.empty(), BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_10);
        types.forEach(type -> this.types.put(type.id(), type));
    }

    @Override
    public @NotNull EntityComponent copy() {
        throw new UnsupportedOperationException("Code-driven, attached per entity");
    }

    private CrystalPhase phase() {
        return CrystalPhase.PHASES.get(phaseIndex);
    }

    @Override
    public void tick(SmpEntity owner, int interval) {
        tick += interval;
        if (owner.dead) return;

        List<Player> players = arena.players();
        if (players.isEmpty()) {
            hideBars();
            if (state != State.WAITING && state != State.LAST_STAND) reset();
            return;
        }
        if (state == State.WAITING) {
            enter(State.REST);
            stateTicks = phase().restTicks() - START_DELAY_TICKS;
            scheduleHazard(START_DELAY_TICKS);
            effects.awaken();
        }

        stateTicks += interval;
        if (state != State.TRANSITION && state != State.LAST_STAND) tickHazard(interval);
        switch (state) {
            case TRANSITION -> {
                show(bar, BarState.fill(Component.text("Giai đoạn " + (phaseIndex + 1) + ": " + phase().name(), NamedTextColor.LIGHT_PURPLE),
                        stateTicks, TRANSITION_TICKS, BossBar.Color.PINK));
                if (stateTicks >= TRANSITION_TICKS) {
                    if (phase().lastStand()) {
                        startLastStand();
                    } else {
                        effects.phaseBegin();
                        select(players);
                    }
                }
            }
            case REST -> {
                hideBars();
                if (stateTicks >= phase().restTicks()) select(players);
            }
            case TELEGRAPH -> {
                show(bar, BarState.fill(withEmpower(currentType.name(), empower), stateTicks, TELEGRAPH_TICKS, BossBar.Color.YELLOW));
                if (stateTicks >= TELEGRAPH_TICKS) startChallenge();
            }
            case CHALLENGE -> tickChallenge(players, interval);
            case EXPOSED -> {
                show(bar, BarState.countdown(Component.text("✦ Lõi đang mở", NamedTextColor.GREEN),
                        phase().exposeTicks() - stateTicks, phase().exposeTicks(), BossBar.Color.GREEN));
                if (stateTicks >= phase().exposeTicks()) {
                    crystal.setCoreOpen(false);
                    effects.coreClose();
                    enter(State.REST);
                }
            }
            case PUNISH -> {
                show(bar, new BarState(Component.text("✖ Tinh thể hồi phục", NamedTextColor.RED), 1f, BossBar.Color.RED));
                if (stateTicks >= PUNISH_TICKS) enter(State.REST);
            }
            case LAST_STAND -> {
                lastStand.tick(tick, interval);
                show(bar, lastStand.status(tick));
            }
            case WAITING -> {
            }
        }
    }

    private void show(ChargeUpManager target, BarState state) {
        target.show(state.title(), state.progress(), state.color());
    }

    private void scheduleHazard(int extraDelay) {
        CrystalPhase phase = phase();
        if (phase.hazards().isEmpty()) {
            nextHazardAt = -1;
            return;
        }
        int spread = Math.max(1, phase.hazardMaxTicks() - phase.hazardMinTicks());
        nextHazardAt = tick + extraDelay + phase.hazardMinTicks() + Utils.RANDOM.nextInt(spread);
    }

    private void tickHazard(int interval) {
        if (hazard == null) {
            if (nextHazardAt < 0 || tick < nextHazardAt) return;
            startHazard();
            return;
        }
        if (hazard.tick(tick, interval) == CrystalChallenge.Outcome.RUNNING) {
            show(overlayBar, labelled(hazardType, hazard.status(tick), 0));
            return;
        }
        effects.hazardEnd();
        stopHazard();
        scheduleHazard(0);
    }

    private void startHazard() {
        List<String> hazards = phase().hazards();
        String id = forcedHazard != null && hazards.contains(forcedHazard)
                ? forcedHazard
                : hazards.get(Utils.RANDOM.nextInt(hazards.size()));
        forcedHazard = null;
        hazardType = types.get(id);
        hazard = hazardType.factory().apply(new CrystalContext(crystal.getEntity(), arena, phase(), 0, true));
        hazard.start(tick);
        effects.hazardStart();
    }

    private void stopHazard() {
        if (hazard != null) hazard.cleanup();
        hazard = null;
        hazardType = null;
        overlayBar.hide();
    }

    private void hideBars() {
        bar.hide();
        overlayBar.hide();
    }

    private void select(List<Player> players) {
        List<String> pool = phase().pool();
        String id = forcedNext != null && pool.contains(forcedNext) ? forcedNext : pickRandom(pool);
        forcedNext = null;
        currentType = types.get(id);
        lastType = currentType;

        effects.telegraph();
        enter(State.TELEGRAPH);
    }

    private String pickRandom(List<String> pool) {
        List<String> options = pool.size() > 1 && lastType != null
                ? pool.stream().filter(id -> !id.equals(lastType.id())).toList()
                : pool;
        return options.get(Utils.RANDOM.nextInt(options.size()));
    }

    private void startChallenge() {
        challengeEmpower = empower;
        empower = 0;
        challenge = currentType.factory().apply(new CrystalContext(crystal.getEntity(), arena, phase(), challengeEmpower, false));
        enter(State.CHALLENGE);
        challenge.start(tick);
        effects.challengeStart();
    }

    private void tickChallenge(List<Player> players, int interval) {
        CrystalChallenge.Outcome outcome = challenge.tick(tick, interval);
        if (outcome == CrystalChallenge.Outcome.RUNNING) {
            show(bar, labelled(currentType, challenge.status(tick), challengeEmpower));
            return;
        }

        if (outcome == CrystalChallenge.Outcome.SUCCESS) {
            finishChallenge();
            crystal.setCoreOpen(true);
            effects.coreOpen();
            enter(State.EXPOSED);
        } else {
            challenge.punish();
            empower += challenge.empowerOnFailure();
            finishChallenge();
            effects.failure();
            enter(State.PUNISH);
        }
    }

    private static BarState labelled(ChallengeType type, BarState status, int empowerStacks) {
        Component title = type.name();
        if (!Component.empty().equals(status.title())) title = title.append(Component.text("  ")).append(status.title());
        return status.withTitle(withEmpower(title, empowerStacks));
    }

    private static Component withEmpower(Component title, int stacks) {
        if (stacks <= 0) return title;
        return title.append(Component.text("  ⚡x" + stacks, NamedTextColor.GOLD));
    }

    public void enterPhase(int index) {
        if (index <= phaseIndex || index >= CrystalPhase.PHASES.size()) return;
        finishChallenge();
        stopHazard();
        crystal.setCoreOpen(false);
        phaseIndex = index;
        empower = 0;
        forcedNext = phase().introduced();
        forcedHazard = forcedNext != null && phase().hazards().contains(forcedNext) ? forcedNext : null;
        if (forcedHazard != null) nextHazardAt = tick + TRANSITION_TICKS + INTRODUCED_HAZARD_DELAY;
        else scheduleHazard(TRANSITION_TICKS);

        effects.phaseShatter();
        Component message = Component.text("Giai đoạn " + (index + 1) + ": " + phase().name(), NamedTextColor.LIGHT_PURPLE);
        arena.players().forEach(player -> player.sendMessage(message));
        enter(State.TRANSITION);
    }

    private void startLastStand() {
        lastStand = new LastStand(new CrystalContext(crystal.getEntity(), arena, phase(), 0, false));
        lastStand.start(tick);
        crystal.setCoreOpen(true);
        effects.lastStand();
        enter(State.LAST_STAND);
    }

    private void finishChallenge() {
        if (challenge != null) challenge.cleanup();
        challenge = null;
    }

    private void reset() {
        finishChallenge();
        stopHazard();
        crystal.setCoreOpen(false);
        enter(State.WAITING);
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
    }

    public boolean isExposed() {
        return state == State.EXPOSED || state == State.LAST_STAND;
    }

    private void shutdown() {
        finishChallenge();
        stopHazard();
        if (lastStand != null) lastStand.cleanup();
        hideBars();
    }

    @Override
    public void onDeath(EntityDeathEvent event, SmpEntity entity) {
        shutdown();
    }

    @Override
    public void onUnload(SmpEntity entity) {
        shutdown();
    }
}
