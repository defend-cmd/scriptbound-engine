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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ScriptBoundMod.MOD_ID)
public class ScriptBoundMod
{
    public static final String MOD_ID = "scriptbound";
    public static final Logger LOGGER = LogManager.getLogger("ScriptBound");

    public ScriptBoundMod()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModCreativeTabs.register(modBus);
        NetworkHandler.register(modBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(GlobalStateManager.class);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event)
    {
        GlobalStateManager.onServerStarted(event.getServer());
        TriggerManager.onServerStarted(event.getServer());
        dev.scriptbound.ui.UiManager.onServerStarted(event.getServer());
        DemoContent.ensure(event.getServer());
        ServerSettings.load(event.getServer());
        LOGGER.info("ScriptBound Engine {} loaded.", ScriptBoundConstants.VERSION);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        GlobalStateManager.onServerStopping(event.getServer());
        TriggerManager.onServerStopping();
        dev.scriptbound.ui.UiManager.onServerStopping();
    }
}
