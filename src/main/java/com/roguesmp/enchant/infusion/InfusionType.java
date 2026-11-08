package com.roguesmp.enchant.infusion;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum InfusionType {
    BASIC("Ấn Ký Cơ Bản", "Tăng chỉ số cho trang bị.", Material.AMETHYST_SHARD, Material.PURPLE_STAINED_GLASS_PANE, NamedTextColor.LIGHT_PURPLE),
    ADVANCED("Ấn Ký Cao Cấp", "Ban cho trang bị hiệu ứng đặc biệt.", Material.ECHO_SHARD, Material.CYAN_STAINED_GLASS_PANE, NamedTextColor.AQUA);

    private final String displayName;
    private final String description;
    private final Material icon;
    private final Material framePane;
    private final NamedTextColor color;

    InfusionType(String displayName, String description, Material icon, Material framePane, NamedTextColor color) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.framePane = framePane;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public Material getFramePane() {
        return framePane;
    }

    public NamedTextColor getColor() {
        return color;
    }
}
