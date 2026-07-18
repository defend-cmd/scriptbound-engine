package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.scriptbound.client.ui.UiNotificationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record DashboardNotificationPacket(int notificationType, String message) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DashboardNotificationPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "dashboard_notification"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DashboardNotificationPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> DashboardNotificationPacket.encode(pkt, buf), DashboardNotificationPacket::decode);

    public static void encode(DashboardNotificationPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.notificationType());
        buffer.writeUtf(packet.message());
    }

    public static DashboardNotificationPacket decode(FriendlyByteBuf buffer)
    {
        return new DashboardNotificationPacket(buffer.readInt(), buffer.readUtf());
    }

    public static void handle(DashboardNotificationPacket packet, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            UiNotificationManager.push(packet.notificationType(), packet.message());
        });
    }
}
