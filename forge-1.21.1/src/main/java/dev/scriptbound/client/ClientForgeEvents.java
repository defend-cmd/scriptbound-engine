package dev.scriptbound.client;

import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.network.packet.OpenDashboardPacket;
import dev.scriptbound.network.packet.OpenJournalPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.scriptbound.ScriptBoundMod;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents
{
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return;
        }

        while (ClientKeybinds.DASHBOARD.consumeClick())
        {
            dev.scriptbound.network.NetworkHandler.sendToServer(new OpenDashboardPacket());
        }

        while (ClientKeybinds.JOURNAL.consumeClick())
        {
            dev.scriptbound.network.NetworkHandler.sendToServer(new OpenJournalPacket());
        }
    }
}
