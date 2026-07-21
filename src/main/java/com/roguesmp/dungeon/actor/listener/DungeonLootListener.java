package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.event.LootRollEvent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Đóng góp modifier của hệ thống dungeon vào mọi lượt roll loot.
 *
 * <p>Nơi gọi roll (mở rương, mob chết...) không cần biết gì về dungeon —
 * listener này tự kiểm tra người roll có đang trong một dungeon instance hay không,
 * rồi cộng modifier theo điểm tiến trình.
 */
public class DungeonLootListener implements Listener {

    /** Mỗi điểm dungeon cộng bao nhiêu vào bonus roll modifier. */
    private static final double MODIFIER_PER_SCORE = 0.01;

    private static final String MODIFIER_SOURCE = "dungeon_score";

    private final IPartyService partyService;
    private final InstanceManager instanceManager;

    public DungeonLootListener(IPartyService partyService, InstanceManager instanceManager) {
        this.partyService = partyService;
        this.instanceManager = instanceManager;
    }

    @EventHandler
    public void onLootRoll(LootRollEvent event) {
        LootContext context = event.getContext();

        SmpPlayer smpPlayer = context.getPlayer();
        if (smpPlayer == null) return;

        Player player = smpPlayer.getBukkitPlayer();
        if (player == null) return;

        Party party = partyService.getPartyByPlayer(player);
        if (party == null) return;

        String instanceId = party.getInstanceId();
        if (instanceId == null || instanceId.isBlank()) return;

        DungeonInstance instance = instanceManager.get(instanceId);
        if (instance == null) return;

        int score = Math.max(0, instance.getProgress().getScore());
        if (score <= 0) return;

        context.addModifier(MODIFIER_SOURCE, score * MODIFIER_PER_SCORE);
    }
}
