package dev.scriptbound.client;

import dev.scriptbound.client.screen.DashboardScreen;
import dev.scriptbound.dashboard.DashboardSnapshot;
import dev.scriptbound.network.packet.DashboardDocumentPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientDashboardHandler
{
    private ClientDashboardHandler() {}

    public static void open(DashboardSnapshot snapshot)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof DashboardScreen dashboardScreen)
        {
            dashboardScreen.updateData(snapshot);
            return;
        }

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
