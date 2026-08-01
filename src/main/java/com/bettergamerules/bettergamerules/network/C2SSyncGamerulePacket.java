package com.bettergamerules.bettergamerules.network;

import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S Packet: Client requests to modify a single game rule.
 * Sent when the player toggles a boolean rule or adjusts a numeric rule.
 * The server validates permissions before applying the change.
 */
public class C2SSyncGamerulePacket {

    private final String ruleId;
    private final String newValue;
    private final String ruleType;

    public C2SSyncGamerulePacket(String ruleId, String newValue, String ruleType) {
        this.ruleId = ruleId;
        this.newValue = newValue;
        this.ruleType = ruleType;
    }

    public static void encode(C2SSyncGamerulePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.ruleId);
        buf.writeUtf(packet.newValue);
        buf.writeUtf(packet.ruleType);
    }

    public static C2SSyncGamerulePacket decode(FriendlyByteBuf buf) {
        return new C2SSyncGamerulePacket(
                buf.readUtf(),   // ruleId
                buf.readUtf(),   // newValue
                buf.readUtf()    // ruleType
        );
    }

    public static void handle(C2SSyncGamerulePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            // Permission check: require operator level 2 (GAMEMASTERS)
            // In single player, the player always has full permissions
            if (!sender.hasPermissions(2)) {
                sender.sendSystemMessage(
                        Component.translatable("message.bettergamerules.no_permission")
                                .withStyle(ChatFormatting.RED)
                );
                return;
            }

            // Apply the game rule change on the server
            boolean success = GameruleHelper.applyGamerule(
                    sender.server,
                    packet.ruleId,
                    packet.newValue,
                    packet.ruleType
            );

            if (success) {
                // Send a confirmation message
                Component ruleName = GameruleHelper.getDisplayName(packet.ruleId);
                sender.sendSystemMessage(
                        Component.translatable("message.bettergamerules.gamerule_changed",
                                ruleName, packet.newValue)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
