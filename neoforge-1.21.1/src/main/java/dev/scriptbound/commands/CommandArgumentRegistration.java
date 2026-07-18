package dev.scriptbound.commands;

import net.neoforged.fml.common.EventBusSubscriber;

import dev.scriptbound.ScriptBoundMod;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class CommandArgumentRegistration
{
    private CommandArgumentRegistration() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        ArgumentTypeInfos.registerByClass(
            StateTargetArgument.class,
            SingletonArgumentInfo.contextFree(StateTargetArgument::stateTarget)
        );
        ArgumentTypeInfos.registerByClass(
            StateKeyArgument.class,
            SingletonArgumentInfo.contextFree(StateKeyArgument::stateKey)
        );
    }
}
