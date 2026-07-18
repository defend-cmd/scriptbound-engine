package dev.scriptbound.network.packet;

import dev.scriptbound.client.ClientDialogueHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CloseDialoguePacket()
{
    public static void encode(CloseDialoguePacket packet, FriendlyByteBuf buf)
    {
    }

    public static CloseDialoguePacket decode(FriendlyByteBuf buf)
    {
        return new CloseDialoguePacket();
    }

    public static void handle(CloseDialoguePacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientDialogueHandler::close)
        );
        supplier.get().setPacketHandled(true);
    }
}
