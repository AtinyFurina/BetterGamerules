package com.bettergamerules.bettergamerules.event;

import com.bettergamerules.bettergamerules.BetterGamerules;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Key binding registration for BetterGamerules.
 *
 * Registers the G key as the base keybinding.
 * The Ctrl modifier is checked at press time in ClientEvents
 * via Screen.hasControlDown(), so the effective shortcut is Ctrl+G.
 *
 * This separation means:
 *   - Controls menu shows "Open Game Rules — G"
 *   - Player can rebind the base key (e.g., to H → becomes Ctrl+H)
 *   - Ctrl is always required (prevents accidental triggers)
 */
public class KeyBindings {

    public static final String CATEGORY = "key.categories.bettergamerules";

    public static final KeyMapping OPEN_GAMERULE_GUI = new KeyMapping(
            "key.bettergamerules.open",         // Translation key
            InputConstants.Type.KEYSYM,         // Keyboard key type
            GLFW.GLFW_KEY_G,                    // Default base key: G (effective: Ctrl+G)
            CATEGORY                             // Category in Controls menu
    );

    /**
     * Register key bindings. Must be static, on MOD bus, CLIENT side.
     */
    @Mod.EventBusSubscriber(modid = BetterGamerules.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_GAMERULE_GUI);
        }
    }
}
