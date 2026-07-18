package dev.scriptbound.network.packet;

import dev.scriptbound.quest.QuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenJournalPacket()
{
    public static void encode(OpenJournalPacket packet, FriendlyByteBuf buf)
    {
    }

    public static OpenJournalPacket decode(FriendlyByteBuf buf)
    {
        return new OpenJournalPacket();
    }

    public static void handle(OpenJournalPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        ServerPlayer player = supplier.get().getSender();

        if (player != null)
        {
            supplier.get().enqueueWork(() -> QuestManager.openJournal(player));
        }

        supplier.get().setPacketHandled(true);
    }
}
