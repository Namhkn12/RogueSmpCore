package com.roguesmp.fishing;

import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FishingHotspotMessages {

    private static final double EPSILON = 1e-9;
    private static final int LUCK_DISPLAY_SCALE = 100;
    private static final DecimalFormat NUMBER = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private FishingHotspotMessages() {
    }

    public static Component appeared(FishingHotspotManager.ActiveHotspot active) {
        ServerRegionManager regions = ServerRegionManager.getInstance();
        Component message = Utils.fromString("<aqua><bold>[Câu cá]</bold> <gray>Điểm câu cá đặc biệt đã xuất hiện: ")
                .append(Utils.fromString(active.hotspot().displayName()))
                .append(Utils.fromString(" <gray>tại <white>"))
                .append(Utils.fromString(regions.displayName(active.world(), active.regionId())));

        ServerRegionManager.Bounds bounds = regions.bounds(active.world(), active.regionId());
        if (bounds == null) return message;

        return message.append(Utils.fromString(" <dark_gray>(<white>X: " + (int) bounds.centerX() + ", Z: " + (int) bounds.centerZ() + "<dark_gray>)"));
    }

    public static void sendEntered(Player player, FishingHotspotManager.ActiveHotspot active) {
        FishingHotspot hotspot = active.hotspot();
        List<Component> lines = new ArrayList<>();
        lines.add(Utils.fromString("<aqua><bold>Điểm câu cá đặc biệt: </bold>").append(Utils.fromString(hotspot.displayName())));

        List<Component> buffs = buffs(hotspot);
        if (buffs.isEmpty()) lines.add(Component.text(" Không có hiệu ứng đặc biệt", NamedTextColor.GRAY));
        else lines.addAll(buffs);

        player.sendMessage(Component.join(JoinConfiguration.newlines(), lines));
    }

    private static List<Component> buffs(FishingHotspot hotspot) {
        List<Component> buffs = new ArrayList<>();

        long luck = Math.round(hotspot.luckBonus() * LUCK_DISPLAY_SCALE);
        if (luck != 0) buffs.add(buff((luck > 0 ? "+" : "") + luck + " May mắn", luck > 0 ? NamedTextColor.GREEN : NamedTextColor.RED));

        if (hotspot.barSegmentsBonus() > 0) buffs.add(buff("Thanh bắt cá rộng hơn (+" + hotspot.barSegmentsBonus() + ")", NamedTextColor.GREEN));
        if (hotspot.barSegmentsBonus() < 0) buffs.add(buff("Thanh bắt cá hẹp hơn (" + hotspot.barSegmentsBonus() + ")", NamedTextColor.RED));

        double turn = hotspot.barTurnChanceMultiplier();
        if (turn < 1.0 - EPSILON) buffs.add(buff("Thanh bắt cá ổn định hơn (x" + NUMBER.format(turn) + ")", NamedTextColor.GREEN));
        if (turn > 1.0 + EPSILON) buffs.add(buff("Thanh bắt cá hỗn loạn hơn (x" + NUMBER.format(turn) + ")", NamedTextColor.RED));

        for (FishTier tier : FishTier.values()) {
            double multiplier = hotspot.tierMultiplier(tier.id());
            if (Math.abs(multiplier - 1.0) < EPSILON) continue;

            buffs.add(prefix().append(Component.text("Cá " + tierName(tier) + " x" + NUMBER.format(multiplier), tier.color())));
        }

        if (!hotspot.lootTables().isEmpty()) {
            boolean exclusive = hotspot.lootMode() == FishingHotspot.LootMode.REPLACE;
            buffs.add(buff(exclusive ? "Phần thưởng độc quyền" : "Phần thưởng bổ sung", NamedTextColor.GOLD));
        }
        return buffs;
    }

    private static String tierName(FishTier tier) {
        return switch (tier) {
            case COMMON -> "COMMON";
            case UNCOMMON -> "UNCOMMON";
            case RARE -> "RARE";
            case EPIC -> "EPIC";
            case LEGENDARY -> "LEGENDARY";
        };
    }

    private static Component buff(String text, NamedTextColor color) {
        return prefix().append(Component.text(text, color));
    }

    private static Component prefix() {
        return Component.text(" • ", NamedTextColor.DARK_GRAY);
    }
}
