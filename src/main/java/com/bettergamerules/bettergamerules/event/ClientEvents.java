package com.bettergamerules.bettergamerules.event;

import com.bettergamerules.bettergamerules.BetterGamerules;
import com.bettergamerules.bettergamerules.screen.GameruleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event handlers for BetterGamerules.
 *
 * Handles the Ctrl+G key combination to open the gamerule editor screen.
 */
@Mod.EventBusSubscriber(modid = BetterGamerules.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Only process at the END phase to avoid double-firing
        if (event.phase != TickEvent.Phase.END) return;

        // Ctrl+G → open gamerule screen
        // && short-circuits: consumeClick() is only called when Ctrl is held,
        // so plain G presses are NOT consumed and won't interfere with typing
        if (Screen.hasControlDown() && KeyBindings.OPEN_GAMERULE_GUI.consumeClick()) {
            Minecraft.getInstance().setScreen(new GameruleScreen());
        }
    }
}
