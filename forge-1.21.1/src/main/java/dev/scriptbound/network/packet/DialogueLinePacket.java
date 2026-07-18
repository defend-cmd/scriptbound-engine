package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import dev.scriptbound.client.ClientDialogueHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public record DialogueLinePacket(String dialogueId, int lineIndex, String speaker, String text, List<String> choices) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DialogueLinePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "dialogue_line"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DialogueLinePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> DialogueLinePacket.encode(pkt, buf), DialogueLinePacket::decode);

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

    public static void handle(DialogueLinePacket packet, CustomPayloadEvent.Context supplier)
    {
        supplier.enqueueWork(() ->
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
        supplier.setPacketHandled(true);
    }
}
