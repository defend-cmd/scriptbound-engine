package dev.scriptbound.network.packet;

import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.ui.UiManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record DeleteUiPacket(String id)
{
    public static void encode(DeleteUiPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.id);
    }

    public static DeleteUiPacket decode(FriendlyByteBuf buf)
    {
        return new DeleteUiPacket(buf.readUtf());
    }

    public static void handle(DeleteUiPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();

            if (sender == null || !sender.hasPermissions(2))
            {
                return;
            }

            UiManager.delete(packet.id);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), new UiListPacket(UiManager.list()));
        });

        context.setPacketHandled(true);
    }
}
