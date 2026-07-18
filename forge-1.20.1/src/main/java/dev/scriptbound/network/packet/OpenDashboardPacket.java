package dev.scriptbound.network.packet;

import dev.scriptbound.dashboard.DashboardService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenDashboardPacket()
{
    public static void encode(OpenDashboardPacket packet, FriendlyByteBuf buf)
    {
    }

    public static OpenDashboardPacket decode(FriendlyByteBuf buf)
    {
        return new OpenDashboardPacket();
    }

    public static void handle(OpenDashboardPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        ServerPlayer player = supplier.get().getSender();

        if (player != null)
        {
            supplier.get().enqueueWork(() -> DashboardService.sendDashboard(player));
        }

        supplier.get().setPacketHandled(true);
    }
}
