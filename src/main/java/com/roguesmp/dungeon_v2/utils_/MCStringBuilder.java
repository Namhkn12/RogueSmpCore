package com.roguesmp.dungeon_v2.utils_;

/**
 * Fluent Minecraft chat string builder with color/format constants and static shortcuts.
 *
 * Usage examples:
 *   // Chaining
 *   String msg = MCStringBuilder.of()
 *       .prefix()
 *       .appendGreen("Hello ").bold("World").build();
 *
 *   // Static one-liners
 *   player.sendMessage(MCStringBuilder.error("Something went wrong!"));
 *   player.sendMessage(MCStringBuilder.success("Operation complete."));
 */
public final class MCStringBuilder {

    // ── Colors ───────────────────────────────────────────────────────────────
    public static final String BLACK         = "§0";
    public static final String DARK_BLUE     = "§1";
    public static final String DARK_GREEN    = "§2";
    public static final String DARK_AQUA     = "§3";
    public static final String DARK_RED      = "§4";
    public static final String DARK_PURPLE   = "§5";
    public static final String GOLD          = "§6";
    public static final String GRAY          = "§7";
    public static final String DARK_GRAY     = "§8";
    public static final String BLUE          = "§9";
    public static final String GREEN         = "§a";
    public static final String AQUA          = "§b";
    public static final String RED           = "§c";
    public static final String LIGHT_PURPLE  = "§d";
    public static final String YELLOW        = "§e";
    public static final String WHITE         = "§f";

    // ── Formats ──────────────────────────────────────────────────────────────
    public static final String OBFUSCATED    = "§k";
    public static final String BOLD          = "§l";
    public static final String STRIKETHROUGH = "§m";
    public static final String UNDERLINE     = "§n";
    public static final String ITALIC        = "§o";
    public static final String RESET         = "§r";

    // ── Presets ──────────────────────────────────────────────────────────────
    public static final String PREFIX        = GOLD + "[Party] " + RESET;
    public static final String PREFIX_ERROR  = RED    + "✗ ";
    public static final String PREFIX_OK     = GREEN  + "✔ ";
    public static final String PREFIX_INFO   = YELLOW + "ℹ ";
    public static final String PREFIX_MUTED  = GRAY;

    // ── Internal builder ─────────────────────────────────────────────────────
    private final StringBuilder sb = new StringBuilder();

    private MCStringBuilder() {}

    /** Creates a new builder instance. */
    public static MCStringBuilder of() { return new MCStringBuilder(); }

    // ── Raw append ───────────────────────────────────────────────────────────
    public MCStringBuilder append(String text)               { sb.append(text);                          return this; }
    public MCStringBuilder append(String color, String text) { sb.append(color).append(text);            return this; }

    // ── Layout helpers ───────────────────────────────────────────────────────
    public MCStringBuilder newLine() { sb.append("\n");  return this; }
    public MCStringBuilder space  () { sb.append(" ");   return this; }
    public MCStringBuilder reset  () { sb.append(RESET); return this; }
    public MCStringBuilder prefix () { sb.append(PREFIX); return this; }

    // ── Format helpers ───────────────────────────────────────────────────────
    public MCStringBuilder appendBold          (String t) { return wrap(BOLD,          t); }
    public MCStringBuilder appendItalic        (String t) { return wrap(ITALIC,        t); }
    public MCStringBuilder appendUnderline     (String t) { return wrap(UNDERLINE,     t); }
    public MCStringBuilder appendStrikethrough (String t) { return wrap(STRIKETHROUGH, t); }
    public MCStringBuilder appendObfuscated    (String t) { return wrap(OBFUSCATED,    t); }

    // ── Color helpers (instance) ──────────────────────────────────────────────
    public MCStringBuilder appendBlack       (String t) { return append(BLACK,        t); }
    public MCStringBuilder appendDarkBlue    (String t) { return append(DARK_BLUE,    t); }
    public MCStringBuilder appendDarkGreen   (String t) { return append(DARK_GREEN,   t); }
    public MCStringBuilder appendDarkAqua    (String t) { return append(DARK_AQUA,    t); }
    public MCStringBuilder appendDarkRed     (String t) { return append(DARK_RED,     t); }
    public MCStringBuilder appendDarkPurple  (String t) { return append(DARK_PURPLE,  t); }
    public MCStringBuilder appendGold        (String t) { return append(GOLD,         t); }
    public MCStringBuilder appendGray        (String t) { return append(GRAY,         t); }
    public MCStringBuilder appendDarkGray    (String t) { return append(DARK_GRAY,    t); }
    public MCStringBuilder appendBlue        (String t) { return append(BLUE,         t); }
    public MCStringBuilder appendGreen       (String t) { return append(GREEN,        t); }
    public MCStringBuilder appendAqua        (String t) { return append(AQUA,         t); }
    public MCStringBuilder appendRed         (String t) { return append(RED,          t); }
    public MCStringBuilder appendLightPurple (String t) { return append(LIGHT_PURPLE, t); }
    public MCStringBuilder appendYellow      (String t) { return append(YELLOW,       t); }
    public MCStringBuilder appendWhite       (String t) { return append(WHITE,        t); }

    // ── Static one-liner shortcuts ────────────────────────────────────────────
    public static String black       (String t) { return BLACK        + t; }
    public static String darkBlue    (String t) { return DARK_BLUE    + t; }
    public static String darkGreen   (String t) { return DARK_GREEN   + t; }
    public static String darkAqua    (String t) { return DARK_AQUA    + t; }
    public static String darkRed     (String t) { return DARK_RED     + t; }
    public static String darkPurple  (String t) { return DARK_PURPLE  + t; }
    public static String gold        (String t) { return GOLD         + t; }
    public static String gray        (String t) { return GRAY         + t; }
    public static String darkGray    (String t) { return DARK_GRAY    + t; }
    public static String blue        (String t) { return BLUE         + t; }
    public static String green       (String t) { return GREEN        + t; }
    public static String aqua        (String t) { return AQUA         + t; }
    public static String red         (String t) { return RED          + t; }
    public static String lightPurple (String t) { return LIGHT_PURPLE + t; }
    public static String yellow      (String t) { return YELLOW       + t; }
    public static String white       (String t) { return WHITE        + t; }

    // ── Static semantic shortcuts ─────────────────────────────────────────────
    public static String error  (String t) { return PREFIX_ERROR + t; }
    public static String success(String t) { return PREFIX_OK    + t; }
    public static String info   (String t) { return PREFIX_INFO  + t; }
    public static String muted  (String t) { return PREFIX_MUTED + t; }

    /** Wraps text with a format code then resets after. */
    public static String bold          (String t) { return BOLD          + t + RESET; }
    public static String italic        (String t) { return ITALIC        + t + RESET; }
    public static String underline     (String t) { return UNDERLINE     + t + RESET; }
    public static String strikethrough (String t) { return STRIKETHROUGH + t + RESET; }
    public static String obfuscated    (String t) { return OBFUSCATED    + t + RESET; }

    /** Builds and returns the final string. */
    public String build() { return sb.toString(); }

    // ── Private ───────────────────────────────────────────────────────────────
    private MCStringBuilder wrap(String fmt, String text) {
        sb.append(fmt).append(text).append(RESET);
        return this;
    }
}