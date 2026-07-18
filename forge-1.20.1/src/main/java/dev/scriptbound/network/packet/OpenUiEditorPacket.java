package dev.scriptbound.network.packet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.ui.UiManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record OpenUiEditorPacket(String id)
{
    public static void encode(OpenUiEditorPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.id);
    }

    public static OpenUiEditorPacket decode(FriendlyByteBuf buf)
    {
        return new OpenUiEditorPacket(buf.readUtf());
    }

    public static void handle(OpenUiEditorPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();

            if (sender == null || !sender.hasPermissions(2))
            {
                return;
            }

            JsonObject json = UiManager.getJson(packet.id);

            if (json == null)
            {
                json = new JsonObject();
                json.addProperty("mode", "hud");
                json.add("elements", new JsonArray());
            }

            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), new EditUiPacket(packet.id, json.toString()));
        });

        context.setPacketHandled(true);
    }
}
