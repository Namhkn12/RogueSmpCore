package com.roguesmp.dungeon.ultis;

public final class MCStringBuilder {

    // ============================================================
    //  COLOR CONSTANTS
    // ============================================================
    public static final String BLACK        = "§0";
    public static final String DARK_BLUE    = "§1";
    public static final String DARK_GREEN   = "§2";
    public static final String DARK_AQUA    = "§3";
    public static final String DARK_RED     = "§4";
    public static final String DARK_PURPLE  = "§5";
    public static final String GOLD         = "§6";
    public static final String GRAY         = "§7";
    public static final String DARK_GRAY    = "§8";
    public static final String BLUE         = "§9";
    public static final String GREEN        = "§a";
    public static final String AQUA         = "§b";
    public static final String RED          = "§c";
    public static final String LIGHT_PURPLE = "§d";
    public static final String YELLOW       = "§e";
    public static final String WHITE        = "§f";

    // ============================================================
    //  FORMAT CONSTANTS
    // ============================================================
    public static final String BOLD          = "§l";
    public static final String ITALIC        = "§o";
    public static final String UNDERLINE     = "§n";
    public static final String STRIKETHROUGH = "§m";
    public static final String RESET         = "§r";

    // ============================================================
    //  PRESET — dùng thống nhất toàn plugin
    // ============================================================
    public static final String PREFIX        = GOLD + "[Party] " + RESET;
    public static final String PREFIX_ERROR  = RED;
    public static final String PREFIX_OK     = GREEN;
    public static final String PREFIX_INFO   = YELLOW;
    public static final String PREFIX_MUTED  = GRAY;

    // ============================================================
    //  CHAINING BUILDER
    // ============================================================
    private final StringBuilder sb = new StringBuilder();

    private MCStringBuilder() {}

    public static MCStringBuilder of() { return new MCStringBuilder(); }

    // -- màu --
    public MCStringBuilder black        (String text) { return append(BLACK,        text); }
    public MCStringBuilder darkBlue     (String text) { return append(DARK_BLUE,    text); }
    public MCStringBuilder darkGreen    (String text) { return append(DARK_GREEN,   text); }
    public MCStringBuilder darkAqua     (String text) { return append(DARK_AQUA,    text); }
    public MCStringBuilder darkRed      (String text) { return append(DARK_RED,     text); }
    public MCStringBuilder darkPurple   (String text) { return append(DARK_PURPLE,  text); }
    public MCStringBuilder gold         (String text) { return append(GOLD,         text); }
    public MCStringBuilder gray         (String text) { return append(GRAY,         text); }
    public MCStringBuilder darkGray     (String text) { return append(DARK_GRAY,    text); }
    public MCStringBuilder blue         (String text) { return append(BLUE,         text); }
    public MCStringBuilder green        (String text) { return append(GREEN,        text); }
    public MCStringBuilder aqua         (String text) { return append(AQUA,         text); }
    public MCStringBuilder red          (String text) { return append(RED,          text); }
    public MCStringBuilder lightPurple  (String text) { return append(LIGHT_PURPLE, text); }
    public MCStringBuilder yellow       (String text) { return append(YELLOW,       text); }
    public MCStringBuilder white        (String text) { return append(WHITE,        text); }

    // -- format --
    public MCStringBuilder bold         (String text) { return append(BOLD,         text, RESET); }
    public MCStringBuilder italic       (String text) { return append(ITALIC,       text, RESET); }
    public MCStringBuilder underline    (String text) { return append(UNDERLINE,    text, RESET); }
    public MCStringBuilder strikethrough(String text) { return append(STRIKETHROUGH,text, RESET); }

    // -- semantic shortcuts (dùng preset) --
    public MCStringBuilder error  (String text) { return append(PREFIX_ERROR, text); }
    public MCStringBuilder success (String text) { return append(PREFIX_OK,   text); }
    public MCStringBuilder info   (String text) { return append(PREFIX_INFO,  text); }
    public MCStringBuilder muted  (String text) { return append(PREFIX_MUTED, text); }
    public MCStringBuilder prefix ()            { sb.append(PREFIX); return this; }

    // -- layout --
    public MCStringBuilder newLine() { sb.append("\n"); return this; }
    public MCStringBuilder reset  () { sb.append(RESET); return this; }
    public MCStringBuilder space  () { sb.append(" "); return this; }

    public String build() { return sb.toString(); }

    // ============================================================
    //  PRIVATE HELPER
    // ============================================================
    private MCStringBuilder append(String color, String text) {
        sb.append(color).append(text);
        return this;
    }

    private MCStringBuilder append(String format, String text, String suffix) {
        sb.append(format).append(text).append(suffix);
        return this;
    }
}
