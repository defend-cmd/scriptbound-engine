package dev.scriptbound.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.scriptbound.client.screen.QuestJournalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import dev.scriptbound.util.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public record QuestJournalPacket(List<String> lines) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<QuestJournalPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("scriptbound", "quest_journal"));
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, QuestJournalPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> QuestJournalPacket.encode(pkt, buf), QuestJournalPacket::decode);

    public static void encode(QuestJournalPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.lines.size());

        for (String line : packet.lines)
        {
            buf.writeUtf(line);
        }
    }

    public static QuestJournalPacket decode(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();
        List<String> lines = new ArrayList<>();

        for (int i = 0; i < count; i++)
        {
            lines.add(buf.readUtf());
        }

        return new QuestJournalPacket(lines);
    }

    public static void handle(QuestJournalPacket packet, IPayloadContext supplier)
    {
        supplier.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(new QuestJournalScreen(packet.lines))
            )
        );

    }
}
