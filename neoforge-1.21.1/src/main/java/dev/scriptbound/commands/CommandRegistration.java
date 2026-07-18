package dev.scriptbound.commands;

import net.neoforged.fml.common.EventBusSubscriber;

import dev.scriptbound.ScriptBoundMod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class CommandRegistration
{
    private CommandRegistration() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        SBECommands.register(event.getDispatcher());
    }
}
