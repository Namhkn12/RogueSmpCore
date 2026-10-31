package com.roguesmp.text;

import com.roguesmp.constant.Keys;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.key.Key;

public final class Glyphs {

    private static final Key FONT = Keys.of("glyphs");

    private Glyphs() {
    }

    public static final Glyph SERVER_ICON = glyph("icon/server", '\uE000', 8, 7, 8);
    public static final Glyph COIN_ICON = glyph("icon/coin", '\uE001', 8, 7, 8);

    public static final Glyph FISHING_TRACK = glyph("fishing/track", '', 8, 7, 100);
    public static final Glyph FISHING_BAR_SEGMENT = glyph("fishing/bar_segment", '', 8, 7, 4);
    public static final Glyph FISHING_INDICATOR = glyph("fishing/indicator", '', 12, 9, 2);

    public static final Glyph GUILD_CARD_COPPER_1 = glyph("item/guild_card_copper_1", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_COPPER_2 = glyph("item/guild_card_copper_2", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_COPPER_3 = glyph("item/guild_card_copper_3", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_COPPER_4 = glyph("item/guild_card_copper_4", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_COPPER_5 = glyph("item/guild_card_copper_5", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_SILVER_1 = glyph("item/guild_card_silver_1", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_SILVER_2 = glyph("item/guild_card_silver_2", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_SILVER_3 = glyph("item/guild_card_silver_3", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_SILVER_4 = glyph("item/guild_card_silver_4", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_SILVER_5 = glyph("item/guild_card_silver_5", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_GOLD_1 = glyph("item/guild_card_gold_1", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_GOLD_2 = glyph("item/guild_card_gold_2", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_GOLD_3 = glyph("item/guild_card_gold_3", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_GOLD_4 = glyph("item/guild_card_gold_4", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_GOLD_5 = glyph("item/guild_card_gold_5", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_DIAMOND_1 = glyph("item/guild_card_diamond_1", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_DIAMOND_2 = glyph("item/guild_card_diamond_2", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_DIAMOND_3 = glyph("item/guild_card_diamond_3", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_DIAMOND_4 = glyph("item/guild_card_diamond_4", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_DIAMOND_5 = glyph("item/guild_card_diamond_5", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_NETHERITE_1 = glyph("item/guild_card_netherite_1", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_NETHERITE_2 = glyph("item/guild_card_netherite_2", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_NETHERITE_3 = glyph("item/guild_card_netherite_3", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_NETHERITE_4 = glyph("item/guild_card_netherite_4", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_NETHERITE_5 = glyph("item/guild_card_netherite_5", '', 8, 7, 8);
    public static final Glyph GUILD_CARD_S = glyph("item/guild_card_s", '', 8, 7, 8);

    private static Glyph glyph(String name, int symbol, int height, int ascent, int width) {
        return glyph(name, symbol, Keys.GLOBAL_NAMESPACE + ":" + name + ".png", height, ascent, width);
    }

    private static Glyph glyph(String name, int symbol, String texture, int height, int ascent, int width) {
        Glyph glyph = new Glyph(FONT, symbol, texture, ascent, height, width);
        Registries.GLYPH.register(name, glyph);
        return glyph;
    }

    public static void bootstrap() {

    }
}
