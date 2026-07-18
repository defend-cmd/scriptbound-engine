package dev.scriptbound.commands;

import dev.scriptbound.ScriptBoundMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class CommandRegistration
{
    private CommandRegistration() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        SBECommands.register(event.getDispatcher());
    }
}
