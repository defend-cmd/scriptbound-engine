package dev.scriptbound.network.packet;

import dev.scriptbound.dashboard.DashboardService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DashboardActionPacket(String action, String id, String payload)
{
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

    public static void handle(DashboardActionPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        ServerPlayer player = supplier.get().getSender();

        if (player != null)
        {
            supplier.get().enqueueWork(() -> DashboardService.handleAction(player, packet.action(), packet.id(), packet.payload()));
        }

        supplier.get().setPacketHandled(true);
    }
}
