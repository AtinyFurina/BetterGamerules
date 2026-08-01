package com.bettergamerules.bettergamerules.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side configuration for BetterGamerules mod.
 * Stores the player's customized simple mode rule list.
 * Config file is generated at: config/bettergamerules-client.toml
 */
public class ClientConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    /** List of rule IDs to display in Simple Mode.
     *  The player can customize this list through the UI.
     *  Default: 12 most commonly used game rules. */
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> SIMPLE_MODE_RULES;

    // Default simple mode rules
    private static final List<String> DEFAULT_RULES = List.of(
            "doFireTick",
            "keepInventory",
            "randomTickSpeed",
            "doDaylightCycle",
            "doMobSpawning",
            "doWeatherCycle",
            "mobGriefing",
            "commandBlockOutput",
            "doInsomnia",
            "disableRaids",
            "playersSleepingPercentage",
            "doTraderSpawning"
    );

    static {
        BUILDER.push("simple_mode");

        SIMPLE_MODE_RULES = BUILDER
                .comment(
                        "List of game rule IDs displayed in Simple Mode.",
                        "You can customize this list through the in-game UI (Simple Mode -> Customize List).",
                        "Default list: " + String.join(", ", DEFAULT_RULES)
                )
                .defineList(
                        "rule_ids",
                        () -> new ArrayList<>(DEFAULT_RULES),
                        obj -> obj instanceof List && !((List<?>) obj).isEmpty()
                );

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /**
     * Get the current simple mode rule list as a new mutable copy.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getSimpleModeRules() {
        List<? extends String> list = SIMPLE_MODE_RULES.get();
        return new ArrayList<>((List<String>) list);
    }

    /**
     * Update the simple mode rule list and save to config.
     * The list must contain at least one entry.
     */
    public static void setSimpleModeRules(List<String> newList) {
        if (newList == null || newList.isEmpty()) return;
        SIMPLE_MODE_RULES.set(new ArrayList<>(newList));
    }
}
