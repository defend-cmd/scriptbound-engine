package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.scriptbound.dialogue.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record DialogueChoicePacket(String dialogueId, int lineIndex, int choiceIndex) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DialogueChoicePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "dialogue_choice"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DialogueChoicePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> DialogueChoicePacket.encode(pkt, buf), DialogueChoicePacket::decode);

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

    public static void handle(DialogueChoicePacket packet, IPayloadContext supplier)
    {
        ServerPlayer player = (ServerPlayer) supplier.player();

        if (player != null)
        {
            supplier.enqueueWork(() -> DialogueManager.choose(player, packet.dialogueId, packet.lineIndex, packet.choiceIndex));
        }

    }
}
