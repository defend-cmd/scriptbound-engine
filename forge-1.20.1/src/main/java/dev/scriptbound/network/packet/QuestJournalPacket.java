package dev.scriptbound.network.packet;

import dev.scriptbound.client.screen.QuestJournalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record QuestJournalPacket(List<String> lines)
{
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

    public static void handle(QuestJournalPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(new QuestJournalScreen(packet.lines))
            )
        );
        supplier.get().setPacketHandled(true);
    }
}
