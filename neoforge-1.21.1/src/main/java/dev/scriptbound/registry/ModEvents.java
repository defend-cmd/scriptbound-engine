package dev.scriptbound.registry;

import net.neoforged.fml.common.EventBusSubscriber;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.entity.NpcEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

public final class ModEvents
{
    private ModEvents() {}

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
        event.put(ModEntities.NPC.get(), NpcEntity.createAttributes().build());
    }
}
