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

public record CloseDialoguePacket() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CloseDialoguePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "close_dialogue"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, CloseDialoguePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> CloseDialoguePacket.encode(pkt, buf), CloseDialoguePacket::decode);

    public static void encode(CloseDialoguePacket packet, FriendlyByteBuf buf)
    {
    }

    public static CloseDialoguePacket decode(FriendlyByteBuf buf)
    {
        return new CloseDialoguePacket();
    }

    public static void handle(CloseDialoguePacket packet, CustomPayloadEvent.Context supplier)
    {
        supplier.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientDialogueHandler::close)
        );
        supplier.setPacketHandled(true);
    }
}
