package com.roguesmp.quest.board;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.quest.data.QuestDataManager;
import com.roguesmp.quest.data.QuestDataManager.RankEntry;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Daily ranking by quest exp of every player at {@link #MIN_RANKED_LEVEL} or above, frozen in the quest database so a
 * restart never reshuffles it. The top rank goes to the first {@link TopRankSettings#slots()} players at its minimum level.
 */
public class AdventureLeaderboard {

    private static final int MIN_RANKED_LEVEL = 10;

    private final QuestDataManager dataManager;
    private final @Nullable TopRankSettings settings;
    private final long topRankMinExp;
    private final long rankedMinExp;
    private final Executor mainThread;

    public AdventureLeaderboard(RogueSmpCore plugin, QuestDataManager dataManager, QuestBoardSettings settings) {
        this.dataManager = dataManager;
        this.settings = settings.topRank();
        this.topRankMinExp = this.settings == null ? 0 : settings.level().totalExpFor(this.settings.minLevel());
        this.rankedMinExp = settings.level().totalExpFor(MIN_RANKED_LEVEL);
        this.mainThread = Bukkit.getScheduler().getMainThreadExecutor(plugin);
    }

    public @Nullable TopRankSettings getSettings() {
        return settings;
    }

    public boolean holdsTopRank(@Nullable RankEntry entry) {
        return settings != null && entry != null && entry.position() <= settings.slots() && entry.exp() >= topRankMinExp;
    }

    public CompletableFuture<List<RankEntry>> top(int limit) {
        return dataManager.topRanked(limit);
    }

    public CompletableFuture<@Nullable RankEntry> rankingOf(UUID uuid) {
        return dataManager.rankingOf(uuid);
    }

    public CompletableFuture<Void> refreshIfEmpty() {
        return dataManager.isRankingEmpty().thenCompose(empty -> empty ? refresh() : CompletableFuture.completedFuture(null));
    }

    public static int minRankedLevel() {
        return MIN_RANKED_LEVEL;
    }

    /**
     * Re-ranks from stored exp and announces who entered the top rank. Completes on the main thread.
     */
    public CompletableFuture<Void> refresh() {
        int slots = settings == null ? 0 : settings.slots();
        return topRankHolders(slots).thenCompose(previous -> dataManager.rebuildRanking(rankedMinExp)
                .thenCompose(rebuilt -> topRankHolders(slots))
                .thenAcceptAsync(next -> announce(previous, next), mainThread));
    }

    private CompletableFuture<List<UUID>> topRankHolders(int slots) {
        if (slots <= 0) return CompletableFuture.completedFuture(List.of());
        return dataManager.topRanked(slots).thenApply(entries -> entries.stream().filter(this::holdsTopRank).map(RankEntry::uuid).toList());
    }

    private void announce(List<UUID> previous, List<UUID> next) {
        if (settings == null) return;
        Component rankName = Utils.fromString(settings.rank().name());
        for (UUID uuid : next) {
            if (previous.contains(uuid)) continue;
            Bukkit.broadcast(Utils.fromString("<gold><bold>[!]</bold> <yellow>" + nameOf(uuid) + " đã đạt hạng ").append(rankName).append(settings.rank().getDisplayIcon()));
        }
    }

    public static String nameOf(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString();
    }
}
