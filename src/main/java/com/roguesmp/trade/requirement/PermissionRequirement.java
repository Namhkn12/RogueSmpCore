package com.roguesmp.trade.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PermissionRequirement implements TradeRequirement {

    public static final String TYPE_KEY = "permission";
    public static final Codec<PermissionRequirement> CODEC = Codec.composite(
            Codec.STRING.fieldOf("permission").forGetter(PermissionRequirement::getPermission),
            Codec.STRING.optionalFieldOf("display", "Không có quyền").forGetter(PermissionRequirement::getDisplayText),
            PermissionRequirement::new
    );

    private final String permission;
    private final String displayText;

    public PermissionRequirement(String permission, String displayText) {
        this.permission = permission;
        this.displayText = displayText;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getPermission() {
        return permission;
    }

    public String getDisplayText() {
        return displayText;
    }

    @Override
    public boolean test(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        return bukkitPlayer != null && bukkitPlayer.hasPermission(permission);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Utils.fromString("<gray>" + displayText);
    }
}
