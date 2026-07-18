package dev.scriptbound.network.packet;

import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.ui.UiManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record RequestUiListPacket()
{
    public static void encode(RequestUiListPacket packet, FriendlyByteBuf buf) {}

    public static RequestUiListPacket decode(FriendlyByteBuf buf)
    {
        return new RequestUiListPacket();
    }

    public static void handle(RequestUiListPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();

            if (sender != null)
            {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), new UiListPacket(UiManager.list()));
            }
        });

        context.setPacketHandled(true);
    }
}
