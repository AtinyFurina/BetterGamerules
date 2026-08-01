package com.bettergamerules.bettergamerules.command;

import com.bettergamerules.bettergamerules.BetterGamerules;
import com.bettergamerules.bettergamerules.screen.GameruleScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side commands for opening the BetterGamerules screen.
 *
 * Commands:
 *   /bettergamerules  — Opens the gamerule editor
 *   /bg                — Shorthand alias
 *
 * These are client-side commands (processed by the client immediately),
 * so they work regardless of server permissions.
 */
@Mod.EventBusSubscriber(modid = BetterGamerules.MOD_ID, value = Dist.CLIENT)
public class OpenGameruleCommand {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();

        // /bettergamerules
        dispatcher.register(
                Commands.literal("bettergamerules")
                        .executes(ctx -> openScreen())
        );

        // /bg (shorthand alias)
        dispatcher.register(
                Commands.literal("bg")
                        .executes(ctx -> openScreen())
        );
    }

    private static int openScreen() {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new GameruleScreen());
        });
        return 1;
    }
}
