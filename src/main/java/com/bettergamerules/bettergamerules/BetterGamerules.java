package com.bettergamerules.bettergamerules;

import com.bettergamerules.bettergamerules.config.ClientConfig;
import com.bettergamerules.bettergamerules.network.ModNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Main mod class for BetterGamerules.
 * Registers client config and network channel.
 */
@Mod(BetterGamerules.MOD_ID)
public class BetterGamerules {
    public static final String MOD_ID = "bettergamerules";

    public BetterGamerules() {
        final var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register client-side config
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ClientConfig.SPEC,
                MOD_ID + "-client.toml"
        );

        // Register network channel during common setup
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
