package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public record DashboardDocumentPacket(String category, String id, String content, boolean exists) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DashboardDocumentPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "dashboard_document"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DashboardDocumentPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> DashboardDocumentPacket.encode(pkt, buf), DashboardDocumentPacket::decode);

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

    public static void handle(DashboardDocumentPacket packet, CustomPayloadEvent.Context supplier)
    {
        supplier.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            dev.scriptbound.client.ClientDashboardHandler.onDocument(packet)
        ));
        supplier.setPacketHandled(true);
    }
}
