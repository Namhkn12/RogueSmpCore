package com.roguesmp.player.trade;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.trade.gui.PlayerTradeGui;
import com.roguesmp.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class PlayerTradeSession {

    public static final int COUNTDOWN_SECONDS = 5;

    private final PlayerTradeManager manager;
    private final PlayerTradeGui first;
    private final PlayerTradeGui second;
    private @Nullable BukkitTask countdownTask;
    private int countdownRemaining;
    private boolean ended;

    PlayerTradeSession(PlayerTradeManager manager, Player firstPlayer, Player secondPlayer) {
        this.manager = manager;
        this.first = new PlayerTradeGui(this, firstPlayer, secondPlayer);
        this.second = new PlayerTradeGui(this, secondPlayer, firstPlayer);
    }

    void open() {
        first.showInventory(first.getOwner());
        second.showInventory(second.getOwner());
    }

    public PlayerTradeGui getPartnerGui(PlayerTradeGui gui) {
        return gui == first ? second : first;
    }

    public boolean isCountingDown() {
        return countdownTask != null;
    }

    public int getCountdownRemaining() {
        return countdownRemaining;
    }

    public boolean isEnded() {
        return ended;
    }

    public void onOfferChanged(PlayerTradeGui changed) {
        if (ended) return;
        if (isCountingDown()) {
            notifyBoth(Component.text("Giao dịch bị huỷ đếm ngược vì " + changed.getOwner().getName() + " đã thay đổi vật phẩm.", NamedTextColor.RED));
            playSound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
        }
        stopCountdown();
        first.setReady(false);
        second.setReady(false);
        refreshAll();
    }

    public void toggleReady(PlayerTradeGui gui) {
        if (ended) return;
        gui.setReady(!gui.isReady());
        if (first.isReady() && second.isReady()) {
            startCountdown();
        } else {
            stopCountdown();
        }
        refreshAll();
    }

    public void onClosed(PlayerTradeGui gui) {
        cancel(gui.getOwner().getName() + " đã đóng giao dịch.");
    }

    public void cancel(String reason) {
        if (ended) return;
        end();
        returnOffer(first);
        returnOffer(second);
        notifyBoth(Component.text("Giao dịch đã bị huỷ: " + reason, NamedTextColor.RED));
    }

    private void startCountdown() {
        stopCountdown();
        countdownRemaining = COUNTDOWN_SECONDS;
        countdownTask = Bukkit.getScheduler().runTaskTimer(RogueSmpCore.getInstance(), this::tickCountdown, 20L, 20L);
        playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f);
    }

    private void stopCountdown() {
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = null;
    }

    private void tickCountdown() {
        countdownRemaining--;
        if (countdownRemaining <= 0) {
            complete();
            return;
        }
        playSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1f);
        refreshAll();
    }

    private void complete() {
        boolean firstChanged = first.syncOfferSnapshot();
        boolean secondChanged = second.syncOfferSnapshot();
        if (firstChanged || secondChanged) {
            onOfferChanged(firstChanged ? first : second);
            return;
        }
        SmpPlayer firstSmp = PlayerManager.getInstance().getSmpPlayer(first.getOwner());
        SmpPlayer secondSmp = PlayerManager.getInstance().getSmpPlayer(second.getOwner());
        if (firstSmp == null || secondSmp == null) {
            cancel("Không tìm thấy dữ liệu người chơi.");
            return;
        }
        if (!firstSmp.hasMoney(first.getCoins()) || !secondSmp.hasMoney(second.getCoins())) {
            cancel("Một trong hai người không còn đủ xu.");
            return;
        }

        end();
        transferCoins(firstSmp, secondSmp, first.getCoins());
        transferCoins(secondSmp, firstSmp, second.getCoins());
        PlayerUtils.giveItem(second.getOwner(), first.takeOfferedItems());
        PlayerUtils.giveItem(first.getOwner(), second.takeOfferedItems());

        notifyBoth(Component.text("Giao dịch thành công!", NamedTextColor.GREEN));
        playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.5f);
    }

    private void end() {
        ended = true;
        stopCountdown();
        manager.endSession(this);
        first.closeIfViewing();
        second.closeIfViewing();
    }

    private void transferCoins(SmpPlayer from, SmpPlayer to, long amount) {
        if (amount <= 0) return;
        if (from.takeMoney(amount)) to.giveMoney(amount);
    }

    private void returnOffer(PlayerTradeGui gui) {
        PlayerUtils.giveItem(gui.getOwner(), gui.takeOfferedItems());
    }

    private void refreshAll() {
        first.render();
        second.render();
    }

    private void notifyBoth(Component message) {
        first.getOwner().sendMessage(message);
        second.getOwner().sendMessage(message);
    }

    private void playSound(Sound sound, float pitch) {
        first.getOwner().playSound(first.getOwner(), sound, 1f, pitch);
        second.getOwner().playSound(second.getOwner(), sound, 1f, pitch);
    }
}
