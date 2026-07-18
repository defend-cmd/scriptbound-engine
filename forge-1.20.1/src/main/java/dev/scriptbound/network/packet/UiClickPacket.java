package dev.scriptbound.network.packet;

import dev.scriptbound.trigger.TriggerExecutor;
import dev.scriptbound.ui.UiDefinition;
import dev.scriptbound.ui.UiElement;
import dev.scriptbound.ui.UiManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UiClickPacket(String uiId, String elementId)
{
    public static void encode(UiClickPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.uiId);
        buf.writeUtf(packet.elementId);
    }

    public static UiClickPacket decode(FriendlyByteBuf buf)
    {
        return new UiClickPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(UiClickPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null)
            {
                return;
            }

            UiDefinition def = UiManager.get(packet.uiId);

            if (def == null)
            {
                return;
            }

            UiElement element = def.findElement(packet.elementId);

            if (element == null || element.onClick == null || element.onClick.isBlank())
            {
                return;
            }

            TriggerExecutor.run(player.server, element.onClick, player);
        });

        context.setPacketHandled(true);
    }
}
