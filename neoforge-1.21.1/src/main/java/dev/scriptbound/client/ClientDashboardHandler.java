package dev.scriptbound.client;

import dev.scriptbound.client.screen.DashboardScreen;
import dev.scriptbound.dashboard.DashboardSnapshot;
import dev.scriptbound.network.packet.DashboardDocumentPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientDashboardHandler
{
    private ClientDashboardHandler() {}

    public static void open(DashboardSnapshot snapshot)
    {
        dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] ClientDashboardHandler.open called!");
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof DashboardScreen dashboardScreen)
        {
            dashboardScreen.updateData(snapshot);
            return;
        }

        dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] Minecraft.setScreen called!");
        minecraft.setScreen(new DashboardScreen(snapshot));
    }

    public static void onDocument(DashboardDocumentPacket packet)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof DashboardScreen dashboardScreen)
        {
            dashboardScreen.loadDocument(packet.category(), packet.id(), packet.content());
        }
    }
}
