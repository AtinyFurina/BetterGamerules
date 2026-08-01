package com.bettergamerules.bettergamerules.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import java.util.*;

/**
 * Helper class for working with Minecraft GameRules.
 * Provides methods to collect, categorize, translate, and modify game rules.
 *
 * NOTE: Forge 1.20.1 uses Mojang official mappings.
 * The GameRules inner visitor class is GameRules.Visitor (not GameRuleVisitor).
 * GameRules.Key does NOT expose getDefaultValue() — use instanceof on the value instead.
 */
public class GameruleHelper {

    /** Cached mapping from rule ID string to GameRules.Key, built on first use */
    private static Map<String, GameRules.Key<?>> KEY_CACHE = null;

    /** RuleData: a client-friendly representation of a game rule entry */
    public record RuleData(String id, String value, String type) {}

    /**
     * Collect all game rules and their current values from the server.
     */
    public static Map<String, RuleData> collectAllGamerules(MinecraftServer server) {
        Map<String, RuleData> result = new LinkedHashMap<>();
        GameRules gameRules = server.getGameRules();

        ensureKeyCache(gameRules);

        // Use the cached keys to get current values
        // This avoids needing to use GameRules.Visitor API which may vary
        for (Map.Entry<String, GameRules.Key<?>> entry : KEY_CACHE.entrySet()) {
            String id = entry.getKey();
            GameRules.Key<?> key = entry.getValue();
            GameRules.Value<?> value = gameRules.getRule(key);
            if (value != null) {
                String valueStr = value.serialize(); // use serialize(), NOT toString()!
                String ruleType = detectType(value, valueStr);
                result.put(id, new RuleData(id, valueStr, ruleType));
            }
        }

        return result;
    }

    /**
     * Build the key cache by visiting all game rule types.
     * In Forge 1.20.1 (official Mojang mappings), the method is:
     *   GameRules.visitGameRuleTypes(GameRuleTypeVisitor)
     * and the inner interface is GameRules.GameRuleTypeVisitor.
     */
    private static void ensureKeyCache(GameRules gameRules) {
        if (KEY_CACHE == null) {
            Map<String, GameRules.Key<?>> cache = new LinkedHashMap<>();

            // Override the GENERIC visit() method — visitBoolean/visitInteger
            // may not be called if visitGameRuleTypes dispatches via Type.callVisitor
            // which calls the default (empty) visit(). The generic visit catches ALL types.
            gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends GameRules.Value<T>> void visit(
                        GameRules.Key<T> key, GameRules.Type<T> type) {
                    cache.put(key.getId(), key);
                }
            });

            KEY_CACHE = cache;
        }
    }

    // ===== Rule Type Detection =====

    /**
     * Detect the type of a GameRules value using instanceof + serialized value fallback.
     * Returns "boolean" or "integer".
     */
    private static String detectType(GameRules.Value<?> value, String serialized) {
        if (value instanceof GameRules.BooleanValue) return "boolean";
        if (value instanceof GameRules.IntegerValue) return "integer";
        // Fallback: check the serialized value
        if ("true".equalsIgnoreCase(serialized) || "false".equalsIgnoreCase(serialized))
            return "boolean";
        try { Integer.parseInt(serialized); return "integer"; }
        catch (NumberFormatException e) { return "unknown"; }
    }

    // ===== Translation =====

    public static MutableComponent getDisplayName(String ruleId) {
        return Component.translatable("gamerule.bettergamerules." + ruleId);
    }

    public static MutableComponent getDescription(String ruleId) {
        return Component.translatable("gamerule.bettergamerules." + ruleId + ".desc");
    }

    public static String getDisplayNameString(String ruleId) {
        return getDisplayName(ruleId).getString();
    }

    public static String getDescriptionString(String ruleId) {
        return getDescription(ruleId).getString();
    }

    public static boolean parseBooleanValue(String valueStr) {
        return Boolean.parseBoolean(valueStr);
    }

    public static int parseIntegerValue(String valueStr, int fallback) {
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ===== Server-Side Rule Modification =====

    @SuppressWarnings("unchecked")
    public static boolean applyGamerule(MinecraftServer server, String ruleId,
                                         String newValue, String ruleType) {
        GameRules gameRules = server.getGameRules();
        GameRules.Key<?> key = findKeyById(ruleId);
        if (key == null) return false;

        try {
            if ("boolean".equals(ruleType)) {
                GameRules.Key<GameRules.BooleanValue> boolKey =
                        (GameRules.Key<GameRules.BooleanValue>) key;
                gameRules.getRule(boolKey).set(Boolean.parseBoolean(newValue), server);
                return true;
            } else if ("integer".equals(ruleType)) {
                GameRules.Key<GameRules.IntegerValue> intKey =
                        (GameRules.Key<GameRules.IntegerValue>) key;
                gameRules.getRule(intKey).set(Integer.parseInt(newValue), server);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static GameRules.Key<?> findKeyById(String ruleId) {
        if (KEY_CACHE == null) return null;
        return KEY_CACHE.get(ruleId);
    }

    public static List<String> getAllRuleIds() {
        if (KEY_CACHE == null) return List.of();
        List<String> ids = new ArrayList<>(KEY_CACHE.keySet());
        Collections.sort(ids);
        return ids;
    }

    // ===== Rule Range Mapping =====

    /**
     * Sensible slider ranges for each integer game rule.
     * Avoids Integer.MIN..MAX overflow and gives usable slider resolution.
     * Rules not listed here default to [0, 1000].
     */
    private static final Map<String, int[]> RULE_RANGES = Map.ofEntries(
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
     * Get the recommended slider range [min, max] for a given rule ID.
     * Falls back to [0, 1000] if the rule is not in the mapping table.
     */
    public static int[] getRuleRange(String ruleId) {
        int[] range = RULE_RANGES.get(ruleId);
        if (range != null) return range;
        return new int[]{0, 1000}; // safe fallback
    }

    /**
     * Get the input box width for a given integer rule ID.
     * Wider for rules with large values (e.g. commandModificationBlockLimit).
     */
    public static int getInputWidth(String ruleId) {
        if ("commandModificationBlockLimit".equals(ruleId)) return 56;
        return 32; // default
    }
}
