package dev.scriptbound.client;

import net.neoforged.fml.common.EventBusSubscriber;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.client.render.MovingBlockRenderer;
import dev.scriptbound.client.render.NpcEntityRenderer;
import dev.scriptbound.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

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
        ScriptBoundMod.LOGGER.info("[DEBUG] keybind registered!");
        event.register(ClientKeybinds.DASHBOARD);
        event.register(ClientKeybinds.JOURNAL);
    }
}
