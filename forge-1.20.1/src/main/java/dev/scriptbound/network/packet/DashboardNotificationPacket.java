package dev.scriptbound.network.packet;

import dev.scriptbound.client.ui.UiNotificationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DashboardNotificationPacket(int type, String message)
{
    public static void encode(DashboardNotificationPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.type());
        buffer.writeUtf(packet.message());
    }

    public static DashboardNotificationPacket decode(FriendlyByteBuf buffer)
    {
        return new DashboardNotificationPacket(buffer.readInt(), buffer.readUtf());
    }

    public static void handle(DashboardNotificationPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            UiNotificationManager.push(packet.type(), packet.message());
        });
        context.setPacketHandled(true);
    }
}
