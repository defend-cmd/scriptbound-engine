package dev.scriptbound.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DashboardDocumentPacket(String category, String id, String content, boolean exists)
{
    public static void encode(DashboardDocumentPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.category, 32);
        buf.writeUtf(packet.id, 256);
        buf.writeBoolean(packet.exists);
        buf.writeUtf(packet.content == null ? "" : packet.content, 262144);
    }

    public static DashboardDocumentPacket decode(FriendlyByteBuf buf)
    {
        String category = buf.readUtf();
        String id = buf.readUtf();
        boolean exists = buf.readBoolean();
        String content = buf.readUtf();
        return new DashboardDocumentPacket(category, id, content, exists);
    }

    public static void handle(DashboardDocumentPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            dev.scriptbound.client.ClientDashboardHandler.onDocument(packet)
        ));
        supplier.get().setPacketHandled(true);
    }
}
