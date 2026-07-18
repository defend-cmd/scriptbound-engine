package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.scriptbound.client.ClientDashboardHandler;
import dev.scriptbound.dashboard.DashboardSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import dev.scriptbound.util.DistExecutor;

public record DashboardDataPacket(DashboardSnapshot snapshot) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DashboardDataPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "dashboard_data"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DashboardDataPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> DashboardDataPacket.encode(pkt, buf), DashboardDataPacket::decode);

    public static void encode(DashboardDataPacket packet, FriendlyByteBuf buf)
    {
        DashboardSnapshot.encode(packet.snapshot, buf);
    }

    public static DashboardDataPacket decode(FriendlyByteBuf buf)
    {
        return new DashboardDataPacket(DashboardSnapshot.decode(buf));
    }

    public static void handle(DashboardDataPacket packet, IPayloadContext supplier)
    {
        dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] open dashboard packet received on client!");
        supplier.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] DashboardDataPacket DistExecutor ran!");
                ClientDashboardHandler.open(packet.snapshot);
            })
        );

    }
}
