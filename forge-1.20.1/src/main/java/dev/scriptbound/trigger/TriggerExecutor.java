package dev.scriptbound.trigger;

import dev.scriptbound.ScriptBoundMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class TriggerExecutor
{
    private TriggerExecutor() {}

    public static boolean run(MinecraftServer server, String triggerId, TriggerContext context)
    {
        if (context.depth() > 16)
        {
            ScriptBoundMod.LOGGER.error("Trigger '{}' aborted: max recursion depth (16) reached.", triggerId);
            return false;
        }

        TriggerChain chain = TriggerManager.get(triggerId);

        if (chain.actions().isEmpty())
        {
            return false;
        }

        for (var action : chain.actions())
        {
            try
            {
                action.execute(context);
            }
            catch (Exception e)
            {
                ScriptBoundMod.LOGGER.error("Trigger '{}' action '{}' failed", triggerId, action.type(), e);
            }
        }

        return true;
    }

    public static boolean run(MinecraftServer server, String triggerId, ServerPlayer player)
    {
        return run(server, triggerId, new TriggerContext(player, player.serverLevel(), null, null, null, 0));
    }

    public static boolean runAtBlock(MinecraftServer server, String triggerId, ServerPlayer player, ServerLevel level, net.minecraft.core.BlockPos pos)
    {
        return run(server, triggerId, new TriggerContext(player, level, pos, null, null, 0));
    }
}
