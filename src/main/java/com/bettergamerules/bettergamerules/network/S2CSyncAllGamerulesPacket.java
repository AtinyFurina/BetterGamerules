package com.bettergamerules.bettergamerules.network;

import com.bettergamerules.bettergamerules.screen.GameruleScreen;
import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * S2C Packet: Server sends all current game rule values to the client.
 * Sent in response to C2SRequestGamerulesPacket.
 * The client updates the GameruleScreen with the received data.
 */
public class S2CSyncAllGamerulesPacket {

    private final Map<String, GameruleHelper.RuleData> rules;

    public S2CSyncAllGamerulesPacket(Map<String, GameruleHelper.RuleData> rules) {
        this.rules = rules;
    }

    public Map<String, GameruleHelper.RuleData> getRules() {
        return rules;
    }

    public static void encode(S2CSyncAllGamerulesPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.rules.size());
        for (Map.Entry<String, GameruleHelper.RuleData> entry : packet.rules.entrySet()) {
            GameruleHelper.RuleData data = entry.getValue();
            buf.writeUtf(data.id());
            buf.writeUtf(data.value());
            buf.writeUtf(data.type());
        }
    }

    public static S2CSyncAllGamerulesPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<String, GameruleHelper.RuleData> rules = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf();
            String value = buf.readUtf();
            String type = buf.readUtf();
            rules.put(id, new GameruleHelper.RuleData(id, value, type));
        }
        return new S2CSyncAllGamerulesPacket(rules);
    }

    public static void handle(S2CSyncAllGamerulesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side: update the GameruleScreen with received data
            // Must run on the main (render) thread
            if (Minecraft.getInstance().screen instanceof GameruleScreen screen) {
                screen.updateGamerules(packet.rules);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
