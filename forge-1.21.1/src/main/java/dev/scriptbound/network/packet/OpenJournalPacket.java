package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import dev.scriptbound.quest.QuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record OpenJournalPacket() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenJournalPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "open_journal"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenJournalPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> OpenJournalPacket.encode(pkt, buf), OpenJournalPacket::decode);

    public static void encode(OpenJournalPacket packet, FriendlyByteBuf buf)
    {
    }

    public static OpenJournalPacket decode(FriendlyByteBuf buf)
    {
        return new OpenJournalPacket();
    }

    public static void handle(OpenJournalPacket packet, CustomPayloadEvent.Context supplier)
    {
        ServerPlayer player = supplier.getSender();

        if (player != null)
        {
            supplier.enqueueWork(() -> QuestManager.openJournal(player));
        }

        supplier.setPacketHandled(true);
    }
}
