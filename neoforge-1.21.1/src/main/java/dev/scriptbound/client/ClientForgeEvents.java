package dev.scriptbound.client;

import net.neoforged.fml.common.EventBusSubscriber;

import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.network.packet.OpenDashboardPacket;
import dev.scriptbound.network.packet.OpenJournalPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import dev.scriptbound.ScriptBoundMod;

public final class ClientForgeEvents
{
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return;
        }

        if (minecraft.level != null && minecraft.level.getGameTime() % 100 == 0) {

        }

        while (ClientKeybinds.DASHBOARD.consumeClick())
        {
            ScriptBoundMod.LOGGER.info("[DEBUG] dashboard key pressed");
            ScriptBoundMod.LOGGER.info("[DEBUG] open dashboard packet sent");
            dev.scriptbound.network.NetworkHandler.sendToServer(new OpenDashboardPacket());
        }

        while (ClientKeybinds.JOURNAL.consumeClick())
        {
            dev.scriptbound.network.NetworkHandler.sendToServer(new OpenJournalPacket());
        }
    }
}
