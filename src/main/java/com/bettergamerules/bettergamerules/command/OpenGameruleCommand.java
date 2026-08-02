package com.bettergamerules.bettergamerules.command;

import com.bettergamerules.bettergamerules.screen.GameruleScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side commands to open the gamerule editor.
 * Available even without server operator permissions.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class OpenGameruleCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("bettergamerules")
                .executes(ctx -> openScreen()));

        dispatcher.register(Commands.literal("bg")
                .executes(ctx -> openScreen()));
    }

    private static int openScreen() {
        Minecraft.getInstance().setScreen(new GameruleScreen());
        return 1;
    }
}
