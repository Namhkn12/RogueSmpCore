package com.roguesmp.worldboss.gui;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gate.GateDisplay;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.cost.CostPayment;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.worldboss.WorldBossManager;
import com.roguesmp.worldboss.definition.SummonedSpawn;
import com.roguesmp.worldboss.definition.WorldBossDefinition;
import com.roguesmp.worldboss.runtime.ArenaState;
import com.roguesmp.worldboss.runtime.OpenResult;
import com.roguesmp.worldboss.runtime.WorldBossInstance;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WorldBossSummonGui extends BaseGui {

    private static final int ROWS = 5;
    private static final int INFO_SLOT = 4;
    private static final int SUMMON_SLOT = 22;
    private static final int[] SUMMON_FRAME_SLOTS = {12, 13, 14, 21, 23, 30, 31, 32};

    private final Player player;
    private final WorldBossDefinition definition;
    private final SummonedSpawn spawn;
    private @Nullable BukkitTask refreshTask;

    public WorldBossSummonGui(Player player, WorldBossDefinition definition, SummonedSpawn spawn) {
        super(Utils.fromString("Triệu hồi: " + definition.displayName()), ROWS);
        this.player = player;
        this.definition = definition;
        this.spawn = spawn;
    }

    public static boolean open(Player player, String bossId) {
        WorldBossDefinition definition = Registries.WORLD_BOSS.get(bossId);
        if (definition == null || !(definition.spawn() instanceof SummonedSpawn spawn)) {
            player.sendMessage(Utils.fromString("<red>Không tìm thấy boss có thể triệu hồi: " + Utils.MINI_MESSAGE.escapeTags(bossId)));
            RogueSmpCore.LOGGER.warn("No summonable world boss with id '{}'", bossId);
            return false;
        }
        new WorldBossSummonGui(player, definition, spawn).showInventory(player);
        return true;
    }

    @Override
    public void setup() {
        clearUi();
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        addButton(INFO_SLOT, infoItem(), ClickHandler.noAction());
        if (smpPlayer != null) addButton(SUMMON_SLOT, summonButton(smpPlayer), event -> onSummonClick(event, smpPlayer));
        ItemStack frame = createDecoration(Material.ORANGE_STAINED_GLASS_PANE);
        for (int slot : SUMMON_FRAME_SLOTS) {
            addButton(slot, frame, ClickHandler.noAction());
        }
        fillEmpty(ItemStackUtils.hideTooltip(ItemStack.of(Material.GRAY_STAINED_GLASS_PANE)));

        if (refreshTask == null) {
            refreshTask = Bukkit.getScheduler().runTaskTimer(RogueSmpCore.getInstance(), this::refresh, 20L, 20L);
        }
    }

    private void refresh() {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return;
        addButton(SUMMON_SLOT, summonButton(smpPlayer), event -> onSummonClick(event, smpPlayer));
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (refreshTask != null) refreshTask.cancel();
    }

    private ItemStack infoItem() {
        List<String> lore = new ArrayList<>();
        definition.description().forEach(line -> lore.add("<gray>" + line));
        if (!lore.isEmpty()) lore.add("");
        lore.add("<gray>Thời gian tập hợp: <white>" + spawn.joinWindowSeconds() + " giây");
        lore.add("<gray>Giới hạn thời gian: <white>" + DateUtils.formatRemaining(definition.fightTimeoutSeconds() * 1000L));
        lore.add("");
        lore.add("<yellow>Sống sót đến cuối trận để nhận thưởng.");
        lore.add("<red>Chết hoặc rời đấu trường sẽ bị loại.");
        return item(definition.icon(), Utils.fromString(definition.displayName()), Utils.fromStrings(lore));
    }

    private ItemStack summonButton(SmpPlayer smpPlayer) {
        List<Component> lore = new ArrayList<>();
        boolean requirementsMet = definition.meetsRequirements(smpPlayer);
        boolean canAfford = CostPayment.canAfford(smpPlayer, spawn.costs());

        if (!definition.requirements().isEmpty()) {
            lore.add(Utils.fromString("<gold>Yêu cầu"));
            for (Requirement requirement : definition.requirements()) {
                lore.add(GateDisplay.check(requirement.getDisplay(smpPlayer), requirement.test(smpPlayer)));
            }
            lore.add(Component.empty());
        }
        if (!spawn.costs().isEmpty()) {
            lore.add(Utils.fromString("<gold>Chi phí"));
            for (Cost cost : spawn.costs()) {
                lore.add(GateDisplay.check(cost.getDisplay(smpPlayer), cost.canAfford(smpPlayer)));
            }
            lore.add(Utils.fromString("<dark_gray>Không hoàn lại nếu trận đấu bị hủy."));
            lore.add(Component.empty());
        }

        WorldBossManager manager = WorldBossManager.getInstance();
        WorldBossInstance active = manager.getActive(definition.id());
        long cooldown = manager.getCooldownRemaining(definition.id());

        Material material;
        String status;
        if (active != null) {
            material = Material.BARRIER;
            status = active.getState() == ArenaState.OPEN
                    ? "<red>Đang tập hợp (" + active.getSecondsLeft() + " giây)"
                    : "<red>Trận đấu đang diễn ra";
        } else if (manager.isDisabled(definition.id())) {
            material = Material.BARRIER;
            status = "<red>Tạm thời bị khóa";
        } else if (cooldown > 0) {
            material = Material.CLOCK;
            status = "<yellow>Có thể triệu hồi sau " + DateUtils.formatRemaining(cooldown);
        } else if (definition.location().getWorld() == null) {
            material = Material.BARRIER;
            status = "<red>Không khả dụng";
        } else if (!requirementsMet) {
            material = Material.RED_CONCRETE;
            status = "<red>Bạn chưa đáp ứng yêu cầu";
        } else if (!canAfford) {
            material = Material.RED_CONCRETE;
            status = "<red>Bạn không đủ chi phí";
        } else {
            material = Material.LIME_CONCRETE;
            status = "<green>Nhấn để triệu hồi";
        }
        lore.add(Utils.fromString(status));
        return item(material, Utils.fromString("<gold>Triệu hồi " + definition.displayName()), lore);
    }

    private void onSummonClick(InventoryClickEvent event, SmpPlayer smpPlayer) {
        event.setCancelled(true);
        boolean requirementsMet = true;
        for (Requirement requirement : definition.requirements()) {
            if (!requirement.test(smpPlayer)) {
                requirementsMet = false;
                break;
            }
        }
        if (!requirementsMet || !CostPayment.canAfford(smpPlayer, spawn.costs())) {
            fail();
            return;
        }

        OpenResult result = WorldBossManager.getInstance().open(definition, player.getUniqueId(), false);
        if (result != OpenResult.OPENED) {
            fail();
            return;
        }
        CostPayment.pay(smpPlayer, spawn.costs());
        player.playSound(Sound.sound(SoundEventKeys.ENTITY_WITHER_SPAWN, Sound.Source.HOSTILE, 0.6f, 1f));
        player.closeInventory();
    }

    private void fail() {
        player.playSound(Sound.sound(SoundEventKeys.ENTITY_VILLAGER_NO, Sound.Source.PLAYER, 1f, 1f));
        refresh();
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = ItemStack.of(material);
        ItemStackUtils.setItemName(stack, name);
        List<Component> list = new ArrayList<>();
        for (Component line : lore) {
            Component component = line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            list.add(component);
        }
        ItemStackUtils.setLore(stack, list);
        return stack;
    }
}
