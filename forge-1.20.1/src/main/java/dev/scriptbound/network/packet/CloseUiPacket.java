package dev.scriptbound.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CloseUiPacket(String id)
{
    public static void encode(CloseUiPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.id);
    }

    public static CloseUiPacket decode(FriendlyByteBuf buf)
    {
        return new CloseUiPacket(buf.readUtf());
    }

    public static void handle(CloseUiPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                dev.scriptbound.client.ui.ClientUiManager.close(packet.id)
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
