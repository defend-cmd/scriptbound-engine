package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import dev.scriptbound.dialogue.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record DialogueAdvancePacket(String dialogueId, int lineIndex) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DialogueAdvancePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "dialogue_advance"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DialogueAdvancePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> DialogueAdvancePacket.encode(pkt, buf), DialogueAdvancePacket::decode);

    public static void encode(DialogueAdvancePacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.dialogueId);
        buf.writeVarInt(packet.lineIndex);
    }

    public static DialogueAdvancePacket decode(FriendlyByteBuf buf)
    {
        return new DialogueAdvancePacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(DialogueAdvancePacket packet, CustomPayloadEvent.Context supplier)
    {
        ServerPlayer player = supplier.getSender();

        if (player != null)
        {
            supplier.enqueueWork(() -> DialogueManager.advance(player, packet.dialogueId, packet.lineIndex));
        }

        supplier.setPacketHandled(true);
    }
}
