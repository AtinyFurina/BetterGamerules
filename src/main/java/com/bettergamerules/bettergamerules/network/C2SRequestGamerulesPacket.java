package com.bettergamerules.bettergamerules.network;

import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.function.Supplier;

/**
 * C2S Packet: Client requests all current game rule values from the server.
 * Triggered when the player opens the GameruleScreen.
 * Server responds with S2CSyncAllGamerulesPacket.
 */
public class C2SRequestGamerulesPacket {

    /** Empty constructor - this packet carries no data, just a signal */
    public C2SRequestGamerulesPacket() {
    }

    public static void encode(C2SRequestGamerulesPacket packet, FriendlyByteBuf buf) {
        // No data to write
    }

    public static C2SRequestGamerulesPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestGamerulesPacket();
    }

    public static void handle(C2SRequestGamerulesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            // Collect all gamerule values from the server
            Map<String, GameruleHelper.RuleData> allRules =
                    GameruleHelper.collectAllGamerules(sender.server);

            // Send the collected data back to the requesting client
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new S2CSyncAllGamerulesPacket(allRules)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
