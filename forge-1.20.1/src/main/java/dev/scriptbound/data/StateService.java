package dev.scriptbound.data;

import dev.scriptbound.ScriptBoundConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Optional;

public final class StateService
{
    private StateService() {}

    public static StateStore resolveStore(CommandSourceStack source, String targetToken)
    {
        Target target = resolveTarget(source, targetToken);
        return target.store();
    }

    public static Target resolveTarget(CommandSourceStack source, String targetToken)
    {
        if (ScriptBoundConstants.GLOBAL_SCOPE.equals(targetToken) || "global".equalsIgnoreCase(targetToken))
        {
            return new Target(StateScope.GLOBAL, ScriptBoundConstants.GLOBAL_SCOPE, GlobalStateManager.global());
        }

        Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();

        if ("@p".equals(targetToken) || "player".equalsIgnoreCase(targetToken))
        {
            ServerPlayer executor = source.getPlayer();

            if (executor == null)
            {
                throw new IllegalArgumentException("Command must be run by a player when using @p.");
            }

            return playerTarget(executor);
        }

        Optional<ServerPlayer> byName = players.stream()
            .filter(player -> player.getGameProfile().getName().equalsIgnoreCase(targetToken))
            .findFirst();

        if (byName.isPresent())
        {
            return playerTarget(byName.get());
        }

        throw new IllegalArgumentException("Unknown state target: " + targetToken);
    }

    public static Target playerTarget(Player player)
    {
        return new Target(
            StateScope.PLAYER,
            player.getGameProfile().getName(),
            PlayerStateCapability.get(player)
        );
    }

    public static boolean set(Target target, String key, StateValue value)
    {
        boolean changed = target.store().set(key, value, target.scope(), target.holderId());

        if (target.scope() == StateScope.GLOBAL && changed)
        {
            GlobalStateManager.markDirty();
        }

        return changed;
    }

    public static boolean add(Target target, String key, double delta)
    {
        boolean changed = target.store().add(key, delta, target.scope(), target.holderId());

        if (target.scope() == StateScope.GLOBAL && changed)
        {
            GlobalStateManager.markDirty();
        }

        return changed;
    }

    public static boolean remove(Target target, String key)
    {
        boolean changed = target.store().remove(key, target.scope(), target.holderId());

        if (target.scope() == StateScope.GLOBAL && changed)
        {
            GlobalStateManager.markDirty();
        }

        return changed;
    }

    public static int clear(Target target, String pattern)
    {
        int count = target.store().clear(pattern, target.scope(), target.holderId());

        if (target.scope() == StateScope.GLOBAL && count > 0)
        {
            GlobalStateManager.markDirty();
        }

        return count;
    }

    public record Target(StateScope scope, String holderId, StateStore store) {}
}
