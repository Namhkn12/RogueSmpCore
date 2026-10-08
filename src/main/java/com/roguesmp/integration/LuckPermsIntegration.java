package com.roguesmp.integration;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.permission.SmpContextCalculator;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LuckPermsIntegration {

    private static LuckPermsIntegration INSTANCE;

    private final LuckPerms luckPerms;
    private final PlayerAdapter<Player> playerAdapter;
    private final SmpContextCalculator contextCalculator = new SmpContextCalculator();

    private LuckPermsIntegration(RogueSmpCore plugin) {
        this.luckPerms = LuckPermsProvider.get();
        this.playerAdapter = luckPerms.getPlayerAdapter(Player.class);
        luckPerms.getContextManager().registerCalculator(contextCalculator);
        luckPerms.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, this::refreshCommandRequirements);
    }

    public static void init(RogueSmpCore plugin) {
        if (INSTANCE == null) {
            INSTANCE = new LuckPermsIntegration(plugin);
        }
    }

    public static LuckPermsIntegration getInstance() {
        return INSTANCE;
    }

    public void shutdown() {
        luckPerms.getContextManager().unregisterCalculator(contextCalculator);
    }

    public String prefix(Player player) {
        String prefix = playerAdapter.getMetaData(player).getPrefix();
        return prefix == null ? "" : prefix;
    }

    public String suffix(Player player) {
        String suffix = playerAdapter.getMetaData(player).getSuffix();
        return suffix == null ? "" : suffix;
    }

    public String groupDisplayName(Player player) {
        String groupName = playerAdapter.getMetaData(player).getPrimaryGroup();
        if (groupName == null) return "";

        Group group = luckPerms.getGroupManager().getGroup(groupName);
        String displayName = group == null ? null : group.getDisplayName();
        return displayName != null ? displayName : groupName;
    }

    private void refreshCommandRequirements(UserDataRecalculateEvent event) {
        Utils.runLater(() -> {
            Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
            if (player != null) CommandAPI.updateRequirements(player);
        });
    }
}
