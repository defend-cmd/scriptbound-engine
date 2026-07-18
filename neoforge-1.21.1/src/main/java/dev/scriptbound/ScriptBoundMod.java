package dev.scriptbound;

import dev.scriptbound.data.GlobalStateManager;
import dev.scriptbound.data.ServerSettings;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.registry.ModBlockEntities;
import dev.scriptbound.registry.ModBlocks;
import dev.scriptbound.registry.ModCreativeTabs;
import dev.scriptbound.registry.ModEntities;
import dev.scriptbound.init.DemoContent;
import dev.scriptbound.trigger.TriggerManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ScriptBoundMod.MOD_ID)
public class ScriptBoundMod
{
    public static final String MOD_ID = "scriptbound";
    public static final Logger LOGGER = LogManager.getLogger("ScriptBound");

    public ScriptBoundMod(IEventBus modBus)
    {
        ScriptBoundAttachments.ATTACHMENTS.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModCreativeTabs.register(modBus);
        NetworkHandler.register(modBus);
        modBus.addListener(dev.scriptbound.registry.ModEvents::registerAttributes);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(GlobalStateManager.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(dev.scriptbound.trigger.GlobalTriggerHandler.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(dev.scriptbound.util.ServerScheduler.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(dev.scriptbound.region.RegionTracker.class);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT)
        {
            modBus.addListener(dev.scriptbound.client.ClientModEvents::registerRenderers);
            modBus.addListener(dev.scriptbound.client.ClientModEvents::registerKeys);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(dev.scriptbound.client.ClientForgeEvents.class);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event)
    {
        GlobalStateManager.onServerStarted(event.getServer());
        TriggerManager.onServerStarted(event.getServer());
        DemoContent.ensure(event.getServer());
        ServerSettings.load(event.getServer());
        LOGGER.info("ScriptBound Engine {} loaded.", ScriptBoundConstants.VERSION);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        GlobalStateManager.onServerStopping(event.getServer());
        TriggerManager.onServerStopping();
    }
}
