package dev.scriptbound.commands;

import dev.scriptbound.ScriptBoundMod;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
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
