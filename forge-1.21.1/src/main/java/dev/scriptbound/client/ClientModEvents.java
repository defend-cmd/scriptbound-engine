package dev.scriptbound.client;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.client.render.MovingBlockRenderer;
import dev.scriptbound.client.render.NpcEntityRenderer;
import dev.scriptbound.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents
{
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.NPC.get(), NpcEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MOVING_BLOCK.get(), MovingBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(ClientKeybinds.DASHBOARD);
        event.register(ClientKeybinds.JOURNAL);
    }
}
