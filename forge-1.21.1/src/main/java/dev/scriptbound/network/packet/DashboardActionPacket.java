package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import dev.scriptbound.dashboard.DashboardService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record DashboardActionPacket(String action, String id, String payload) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DashboardActionPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "dashboard_action"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DashboardActionPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> DashboardActionPacket.encode(pkt, buf), DashboardActionPacket::decode);

    public DashboardActionPacket(String action, String id)
    {
        this(action, id, "");
    }

    public static void encode(DashboardActionPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.action, 64);
        buf.writeUtf(packet.id == null ? "" : packet.id, 32767);
        buf.writeUtf(packet.payload == null ? "" : packet.payload, 262144);
    }

    public static DashboardActionPacket decode(FriendlyByteBuf buf)
    {
        return new DashboardActionPacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(DashboardActionPacket packet, CustomPayloadEvent.Context supplier)
    {
        ServerPlayer player = supplier.getSender();

        if (player != null)
        {
            supplier.enqueueWork(() -> DashboardService.handleAction(player, packet.action(), packet.id(), packet.payload()));
        }

        supplier.setPacketHandled(true);
    }
}
