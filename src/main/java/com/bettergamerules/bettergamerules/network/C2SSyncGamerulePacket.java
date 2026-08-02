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

    private static final int MAX_RULE_ID_LEN = 128;
    private static final int MAX_VALUE_LEN = 32;
    private static final int MAX_TYPE_LEN = 16;

    private final String ruleId;
    private final String newValue;
    private final String ruleType;

    public C2SSyncGamerulePacket(String ruleId, String newValue, String ruleType) {
        this.ruleId = ruleId;
        this.newValue = newValue;
        this.ruleType = ruleType;
    }

    public static void encode(C2SSyncGamerulePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.ruleId, MAX_RULE_ID_LEN);
        buf.writeUtf(packet.newValue, MAX_VALUE_LEN);
        buf.writeUtf(packet.ruleType, MAX_TYPE_LEN);
    }

    public static C2SSyncGamerulePacket decode(FriendlyByteBuf buf) {
        return new C2SSyncGamerulePacket(
                buf.readUtf(MAX_RULE_ID_LEN),
                buf.readUtf(MAX_VALUE_LEN),
                buf.readUtf(MAX_TYPE_LEN)
        );
    }

    public static void handle(C2SSyncGamerulePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            // Permission check: require operator level 2
            // In single player with cheats enabled, the player always has full permissions
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
                Component ruleName = GameruleHelper.getDisplayName(packet.ruleId);
                sender.sendSystemMessage(
                        Component.translatable("message.bettergamerules.gamerule_changed",
                                ruleName, packet.newValue)
                );
            }
        });
        context.setPacketHandled(true);
    }
}
