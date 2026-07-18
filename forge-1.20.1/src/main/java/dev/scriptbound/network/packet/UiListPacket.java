package dev.scriptbound.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record UiListPacket(List<String> ids)
{
    public static void encode(UiListPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.ids.size());

        for (String id : packet.ids)
        {
            buf.writeUtf(id);
        }
    }

    public static UiListPacket decode(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < count; i++)
        {
            ids.add(buf.readUtf());
        }

        return new UiListPacket(ids);
    }

    public static void handle(UiListPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (net.minecraft.client.Minecraft.getInstance().screen instanceof dev.scriptbound.client.screen.DashboardScreen dashboard)
                {
                    dashboard.setUiList(packet.ids);
                }
            })
        );
        supplier.get().setPacketHandled(true);
    }
}
