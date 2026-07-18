package dev.scriptbound.network.packet;

import dev.scriptbound.client.ClientDashboardHandler;
import dev.scriptbound.dashboard.DashboardSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DashboardDataPacket(DashboardSnapshot snapshot)
{
    public static void encode(DashboardDataPacket packet, FriendlyByteBuf buf)
    {
        DashboardSnapshot.encode(packet.snapshot, buf);
    }

    public static DashboardDataPacket decode(FriendlyByteBuf buf)
    {
        return new DashboardDataPacket(DashboardSnapshot.decode(buf));
    }

    public static void handle(DashboardDataPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientDashboardHandler.open(packet.snapshot)
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
