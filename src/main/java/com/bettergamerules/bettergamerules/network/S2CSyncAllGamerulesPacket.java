package com.bettergamerules.bettergamerules.network;

import com.bettergamerules.bettergamerules.screen.GameruleScreen;
import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * S2C Packet: Server sends all current game rule values to the client.
 * Sent in response to C2SRequestGamerulesPacket.
 * The client updates the GameruleScreen with the received data.
 */
public class S2CSyncAllGamerulesPacket {

    private static final int MAX_RULE_ID_LEN = 128;
    private static final int MAX_VALUE_LEN = 32;
    private static final int MAX_TYPE_LEN = 16;
    private static final int MAX_RULE_COUNT = 512;

    private final Map<String, GameruleHelper.RuleData> rules;

    public S2CSyncAllGamerulesPacket(Map<String, GameruleHelper.RuleData> rules) {
        this.rules = rules;
    }

    public Map<String, GameruleHelper.RuleData> getRules() {
        return Collections.unmodifiableMap(rules);
    }

    public static void encode(S2CSyncAllGamerulesPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.rules.size());
        for (GameruleHelper.RuleData data : packet.rules.values()) {
            buf.writeUtf(data.id(), MAX_RULE_ID_LEN);
            buf.writeUtf(data.value(), MAX_VALUE_LEN);
            buf.writeUtf(data.type(), MAX_TYPE_LEN);
        }
    }

    public static S2CSyncAllGamerulesPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        // Bounds check to prevent huge allocations from corrupted data
        if (count < 0 || count > MAX_RULE_COUNT) {
            return new S2CSyncAllGamerulesPacket(Map.of());
        }
        Map<String, GameruleHelper.RuleData> rules = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf(MAX_RULE_ID_LEN);
            String value = buf.readUtf(MAX_VALUE_LEN);
            String type = buf.readUtf(MAX_TYPE_LEN);
            rules.put(id, new GameruleHelper.RuleData(id, value, type));
        }
        return new S2CSyncAllGamerulesPacket(rules);
    }

    public static void handle(S2CSyncAllGamerulesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof GameruleScreen screen) {
                screen.updateGamerules(packet.rules);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
