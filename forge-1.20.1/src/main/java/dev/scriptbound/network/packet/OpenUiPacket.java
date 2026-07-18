package dev.scriptbound.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenUiPacket(String id, String json)
{
    public static void encode(OpenUiPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.id);
        buf.writeUtf(packet.json, 262144);
    }

    public static OpenUiPacket decode(FriendlyByteBuf buf)
    {
        return new OpenUiPacket(buf.readUtf(), buf.readUtf(262144));
    }

    public static void handle(OpenUiPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                dev.scriptbound.client.ui.ClientUiManager.open(packet.id, packet.json)
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
