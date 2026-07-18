package dev.scriptbound.network.packet;

import dev.scriptbound.dialogue.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DialogueChoicePacket(String dialogueId, int lineIndex, int choiceIndex)
{
    public static void encode(DialogueChoicePacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.dialogueId);
        buf.writeVarInt(packet.lineIndex);
        buf.writeVarInt(packet.choiceIndex);
    }

    public static DialogueChoicePacket decode(FriendlyByteBuf buf)
    {
        return new DialogueChoicePacket(buf.readUtf(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(DialogueChoicePacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        ServerPlayer player = supplier.get().getSender();

        if (player != null)
        {
            supplier.get().enqueueWork(() -> DialogueManager.choose(player, packet.dialogueId, packet.lineIndex, packet.choiceIndex));
        }

        supplier.get().setPacketHandled(true);
    }
}
