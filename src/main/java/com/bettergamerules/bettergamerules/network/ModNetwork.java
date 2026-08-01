package com.bettergamerules.bettergamerules.network;

import com.bettergamerules.bettergamerules.BetterGamerules;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network channel registration for BetterGamerules.
 * Manages the SimpleChannel used for client-server communication.
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterGamerules.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * Register all packet types on the channel.
     * Called during FMLCommonSetupEvent.
     */
    public static void register() {
        int id = 0;

        // Packet 1: Client requests all gamerule values from server
        CHANNEL.registerMessage(id++,
                C2SRequestGamerulesPacket.class,
                C2SRequestGamerulesPacket::encode,
                C2SRequestGamerulesPacket::decode,
                C2SRequestGamerulesPacket::handle
        );

        // Packet 2: Client sends a single gamerule modification to server
        CHANNEL.registerMessage(id++,
                C2SSyncGamerulePacket.class,
                C2SSyncGamerulePacket::encode,
                C2SSyncGamerulePacket::decode,
                C2SSyncGamerulePacket::handle
        );

        // Packet 3: Server sends all gamerule values back to client
        CHANNEL.registerMessage(id++,
                S2CSyncAllGamerulesPacket.class,
                S2CSyncAllGamerulesPacket::encode,
                S2CSyncAllGamerulesPacket::decode,
                S2CSyncAllGamerulesPacket::handle
        );
    }
}
