package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import dev.scriptbound.dashboard.DashboardService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record OpenDashboardPacket() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenDashboardPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "open_dashboard"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenDashboardPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> OpenDashboardPacket.encode(pkt, buf), OpenDashboardPacket::decode);

    public static void encode(OpenDashboardPacket packet, FriendlyByteBuf buf)
    {
    }

    public static OpenDashboardPacket decode(FriendlyByteBuf buf)
    {
        return new OpenDashboardPacket();
    }

    public static void handle(OpenDashboardPacket packet, CustomPayloadEvent.Context supplier)
    {
        ServerPlayer player = supplier.getSender();

        if (player != null)
        {
            supplier.enqueueWork(() -> DashboardService.sendDashboard(player));
        }

        supplier.setPacketHandled(true);
    }
}
