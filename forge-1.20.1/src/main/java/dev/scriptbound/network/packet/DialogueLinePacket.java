package dev.scriptbound.network.packet;

import dev.scriptbound.client.ClientDialogueHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record DialogueLinePacket(String dialogueId, int lineIndex, String speaker, String text, List<String> choices)
{
    public static void encode(DialogueLinePacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.dialogueId, 32767);
        buf.writeVarInt(packet.lineIndex);
        buf.writeUtf(packet.speaker, 32767);
        buf.writeUtf(packet.text, 32767);
        buf.writeVarInt(packet.choices.size());

        for (String choice : packet.choices)
        {
            buf.writeUtf(choice, 32767);
        }
    }

    public static DialogueLinePacket decode(FriendlyByteBuf buf)
    {
        String dialogueId = buf.readUtf();
        int lineIndex = buf.readVarInt();
        String speaker = buf.readUtf();
        String text = buf.readUtf();
        int count = buf.readVarInt();
        List<String> choices = new ArrayList<>();

        for (int i = 0; i < count; i++)
        {
            choices.add(buf.readUtf());
        }

        return new DialogueLinePacket(dialogueId, lineIndex, speaker, text, choices);
    }

    public static void handle(DialogueLinePacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientDialogueHandler.open(
                    packet.dialogueId,
                    packet.lineIndex,
                    packet.speaker,
                    packet.text,
                    packet.choices
                )
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
