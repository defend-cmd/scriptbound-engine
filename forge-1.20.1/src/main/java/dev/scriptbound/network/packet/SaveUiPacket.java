package dev.scriptbound.network.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ui.UiManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SaveUiPacket(String id, String json)
{
    public static void encode(SaveUiPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.id);
        buf.writeUtf(packet.json, 262144);
    }

    public static SaveUiPacket decode(FriendlyByteBuf buf)
    {
        return new SaveUiPacket(buf.readUtf(), buf.readUtf(262144));
    }

    public static void handle(SaveUiPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();

            if (sender == null || !sender.hasPermissions(2))
            {
                return;
            }

            try
            {
                JsonObject root = JsonParser.parseString(packet.json).getAsJsonObject();
                UiManager.save(packet.id, root);
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Saved UI '" + packet.id + "'."));
            }
            catch (Exception e)
            {
                ScriptBoundMod.LOGGER.error("Failed to save UI '{}'", packet.id, e);
            }
        });

        context.setPacketHandled(true);
    }
}
