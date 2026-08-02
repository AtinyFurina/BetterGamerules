package com.bettergamerules.bettergamerules.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import java.util.*;

/**
 * Helper class for working with Minecraft GameRules.
 * Provides methods to collect, categorize, translate, and modify game rules.
 * Supports both vanilla and modded game rules.
 *
 * NOTE: Forge 1.20.1 uses Mojang official mappings.
 * The GameRules inner visitor class is GameRules.GameRuleTypeVisitor.
 * GameRules.Key does NOT expose getDefaultValue() — use instanceof on the value instead.
 */
public class GameruleHelper {

    /** RuleData: a client-friendly representation of a game rule entry */
    public record RuleData(String id, String value, String type) {}

    // ===== Java 17 compatible clamp helpers (consolidated from across the project) =====

    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    // ===== Shared UI color constants =====

    public static final int COLOR_GREEN = 0xFF5B8731;
    public static final int COLOR_GREEN_HOVER = 0xFF6B9F3F;
    public static final int COLOR_GRAY = 0xFF555555;
    public static final int COLOR_GRAY_HOVER = 0xFF777777;
    public static final int COLOR_TEXT_DIM = 0xE0E0E0;
    public static final int COLOR_TEXT_WHITE = 0xFFFFFF;
    public static final int COLOR_PANEL_BG = 0xCC000000;
    public static final int COLOR_PANEL_BORDER = 0xFF444444;

    // ===== Rule Collection =====

    /**
     * Collect all game rules and their current values from the server.
     * Rebuilds the key cache each call to support switching servers with different mod sets.
     */
    public static Map<String, RuleData> collectAllGamerules(MinecraftServer server) {
        Map<String, RuleData> result = new LinkedHashMap<>();
        GameRules gameRules = server.getGameRules();

        // Build key cache fresh each call — cheap (~50-100 entries) and avoids
        // stale cache when switching servers with different mod sets
        Map<String, GameRules.Key<?>> keyCache = buildKeyCache(gameRules);

        for (Map.Entry<String, GameRules.Key<?>> entry : keyCache.entrySet()) {
            String id = entry.getKey();
            GameRules.Key<?> key = entry.getValue();
            GameRules.Value<?> value = gameRules.getRule(key);
            if (value != null) {
                String valueStr = value.serialize(); // use serialize(), NOT toString()!
                String ruleType = detectType(value);
                result.put(id, new RuleData(id, valueStr, ruleType));
            }
        }

        return result;
    }

    /**
     * Build the key cache by visiting all game rule types.
     * Overrides ALL three visit methods for maximum compatibility:
     * - visitBoolean / visitInteger: called by vanilla BooleanType/IntegerType
     * - visit (generic): called by custom/modded GameRules.Type implementations
     */
    private static Map<String, GameRules.Key<?>> buildKeyCache(GameRules gameRules) {
        Map<String, GameRules.Key<?>> cache = new LinkedHashMap<>();

        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key,
                                      GameRules.Type<GameRules.BooleanValue> type) {
                cache.put(key.getId(), key);
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key,
                                      GameRules.Type<GameRules.IntegerValue> type) {
                cache.put(key.getId(), key);
            }

            @Override
            public <T extends GameRules.Value<T>> void visit(
                    GameRules.Key<T> key, GameRules.Type<T> type) {
                cache.put(key.getId(), key);
            }
        });

        return cache;
    }

    // ===== Rule Type Detection =====

    /**
     * Detect the type of a GameRules value using instanceof.
     * Returns "boolean", "integer", or "unknown".
     */
    public static String detectType(GameRules.Value<?> value) {
        if (value instanceof GameRules.BooleanValue) return "boolean";
        if (value instanceof GameRules.IntegerValue) return "integer";
        return "unknown";
    }

    /**
     * Check if a RuleData entry represents a boolean rule.
     */
    public static boolean isBooleanRule(RuleData data) {
        return "boolean".equals(data.type());
    }

    // ===== Translation (with fallback for modded rules) =====

    private static final String TRANSLATION_PREFIX = "gamerule.bettergamerules.";

    public static MutableComponent getDisplayName(String ruleId) {
        return Component.translatable(TRANSLATION_PREFIX + ruleId);
    }

    public static MutableComponent getDescription(String ruleId) {
        return Component.translatable(TRANSLATION_PREFIX + ruleId + ".desc");
    }

    /**
     * Get the display name as a resolved string.
     * Falls back to a formatted version of the rule ID if no translation exists
     * (handles modded gamerules without translations).
     */
    public static String getDisplayNameString(String ruleId) {
        String translated = getDisplayName(ruleId).getString();
        // If the translation key itself is returned (missing translation),
        // format the rule ID as a readable fallback
        if (translated.equals(TRANSLATION_PREFIX + ruleId)) {
            return formatRuleId(ruleId);
        }
        return translated;
    }

    /**
     * Get the description as a resolved string.
     * Falls back to a generic message for modded rules without translations.
     */
    public static String getDescriptionString(String ruleId) {
        String translated = getDescription(ruleId).getString();
        if (translated.equals(TRANSLATION_PREFIX + ruleId + ".desc")) {
            return "No description available";
        }
        return translated;
    }

    /**
     * Format a rule ID into a human-readable name.
     * Converts camelCase/snake_case to Title Case.
     * Example: "doFireTick" -> "Do Fire Tick", "mod_custom_rule" -> "Mod Custom Rule"
     */
    private static String formatRuleId(String ruleId) {
        // Split on underscores and camelCase boundaries
        String withSpaces = ruleId
                .replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2");
        // Capitalize first letter of each word
        String[] words = withSpaces.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    // ===== Value Parsing =====

    public static int parseIntegerValue(String valueStr, int fallback) {
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ===== Server-Side Rule Modification =====

    /**
     * Apply a gamerule change on the server.
     * Uses instanceof on the actual value for type dispatch — does NOT trust
     * the client-supplied ruleType string, avoiding ClassCastException from forged packets.
     */
    public static boolean applyGamerule(MinecraftServer server, String ruleId,
                                         String newValue, String ruleType) {
        GameRules gameRules = server.getGameRules();
        Map<String, GameRules.Key<?>> keyCache = buildKeyCache(gameRules);
        GameRules.Key<?> key = keyCache.get(ruleId);
        if (key == null) return false;
        GameRules.Value<?> value = gameRules.getRule(key);
        if (value == null) return false;

        try {
            if (value instanceof GameRules.BooleanValue) {
                if (!"true".equalsIgnoreCase(newValue) && !"false".equalsIgnoreCase(newValue))
                    return false;
                @SuppressWarnings("unchecked")
                GameRules.Key<GameRules.BooleanValue> boolKey =
                        (GameRules.Key<GameRules.BooleanValue>) key;
                gameRules.getRule(boolKey).set(Boolean.parseBoolean(newValue), server);
                return true;
            } else if (value instanceof GameRules.IntegerValue) {
                int parsed = Integer.parseInt(newValue);
                @SuppressWarnings("unchecked")
                GameRules.Key<GameRules.IntegerValue> intKey =
                        (GameRules.Key<GameRules.IntegerValue>) key;
                gameRules.getRule(intKey).set(parsed, server);
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    // ===== Rule Range Mapping =====

    /**
     * Known sensible slider ranges for specific vanilla integer game rules.
     * Modded rules and rules not listed here get a dynamic range based on their current value.
     */
    private static final Map<String, int[]> KNOWN_RANGES = Map.ofEntries(
        Map.entry("randomTickSpeed",                new int[]{0, 10000}),
        Map.entry("playersSleepingPercentage",      new int[]{0, 100}),
        Map.entry("maxCommandChainLength",          new int[]{0, 1000000}),
        Map.entry("maxEntityCramming",              new int[]{0, 1000}),
        Map.entry("spawnRadius",                    new int[]{0, 1000}),
        Map.entry("commandModificationBlockLimit",  new int[]{0, 100000000}),
        Map.entry("maxCommandForkCount",            new int[]{0, 1000000}),
        Map.entry("snowAccumulationHeight",         new int[]{0, 64}),
        Map.entry("playersNetherPortalDefaultDelay",new int[]{0, 2000}),
        Map.entry("playersNetherPortalCreativeDelay",new int[]{0, 2000}),
        Map.entry("spawnChunkRadius",               new int[]{0, 32})
    );

    /**
     * Get the recommended slider range [min, max] for a given rule.
     * Uses known range for vanilla rules; dynamically computes range for modded rules
     * based on the current value to avoid clamping valid values.
     *
     * @param ruleId       the game rule ID
     * @param currentValue the current value of the rule (used for dynamic range calculation)
     */
    public static int[] getRuleRange(String ruleId, int currentValue) {
        int[] known = KNOWN_RANGES.get(ruleId);
        if (known != null) return known;

        // Dynamic range for unknown/modded rules:
        // [0, max(currentValue * 2, 1000)] ensures the current value is never clamped
        int max = Math.max(Math.abs(currentValue) * 2, 1000);
        return new int[]{0, max};
    }

    /**
     * Get the input box width for an integer rule, based on the current value's digit count.
     * Dynamically sizes for modded rules with large values.
     */
    public static int getInputWidth(String ruleId, int currentValue) {
        // Special case for commandModificationBlockLimit which can be very large
        if ("commandModificationBlockLimit".equals(ruleId)) return 56;

        // Dynamic width based on the number of digits (including minus sign)
        int digits = String.valueOf(currentValue).length();
        if (digits <= 4) return 32;
        if (digits <= 6) return 40;
        if (digits <= 8) return 48;
        return 56;
    }
}
