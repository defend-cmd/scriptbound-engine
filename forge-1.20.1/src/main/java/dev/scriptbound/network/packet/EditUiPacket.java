package dev.scriptbound.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EditUiPacket(String id, String json)
{
    public static void encode(EditUiPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.id);
        buf.writeUtf(packet.json, 262144);
    }

    public static EditUiPacket decode(FriendlyByteBuf buf)
    {
        return new EditUiPacket(buf.readUtf(), buf.readUtf(262144));
    }

    public static void handle(EditUiPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                    new dev.scriptbound.client.screen.UiBuilderScreen(packet.id, packet.json)
                )
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
