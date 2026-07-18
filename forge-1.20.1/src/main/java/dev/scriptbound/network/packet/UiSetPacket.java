package dev.scriptbound.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UiSetPacket(String uiId, String elementId, String field, String value)
{
    public static void encode(UiSetPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.uiId);
        buf.writeUtf(packet.elementId);
        buf.writeUtf(packet.field);
        buf.writeUtf(packet.value);
    }

    public static UiSetPacket decode(FriendlyByteBuf buf)
    {
        return new UiSetPacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(UiSetPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                dev.scriptbound.client.ui.ClientUiManager.applySet(packet.uiId, packet.elementId, packet.field, packet.value)
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
