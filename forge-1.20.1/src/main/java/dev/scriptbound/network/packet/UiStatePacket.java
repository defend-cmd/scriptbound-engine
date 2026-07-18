package dev.scriptbound.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public record UiStatePacket(Map<String, String> values)
{
    public static void encode(UiStatePacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.values.size());

        for (Map.Entry<String, String> entry : packet.values.entrySet())
        {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static UiStatePacket decode(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();
        Map<String, String> values = new LinkedHashMap<>();

        for (int i = 0; i < count; i++)
        {
            values.put(buf.readUtf(), buf.readUtf());
        }

        return new UiStatePacket(values);
    }

    public static void handle(UiStatePacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                dev.scriptbound.client.ui.ClientUiManager.updateStates(packet.values)
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
