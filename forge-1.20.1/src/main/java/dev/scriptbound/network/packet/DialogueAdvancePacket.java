package dev.scriptbound.network.packet;

import dev.scriptbound.dialogue.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DialogueAdvancePacket(String dialogueId, int lineIndex)
{
    public static void encode(DialogueAdvancePacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.dialogueId);
        buf.writeVarInt(packet.lineIndex);
    }

    public static DialogueAdvancePacket decode(FriendlyByteBuf buf)
    {
        return new DialogueAdvancePacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(DialogueAdvancePacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        ServerPlayer player = supplier.get().getSender();

        if (player != null)
        {
            supplier.get().enqueueWork(() -> DialogueManager.advance(player, packet.dialogueId, packet.lineIndex));
        }

        supplier.get().setPacketHandled(true);
    }
}
