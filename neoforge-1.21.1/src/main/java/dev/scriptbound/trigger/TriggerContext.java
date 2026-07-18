package dev.scriptbound.trigger;

import dev.scriptbound.entity.NpcEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record TriggerContext(
    ServerPlayer player,
    ServerLevel level,
    @Nullable BlockPos blockPos,
    @Nullable String chatMessage,
    @Nullable NpcEntity npc,
    int depth
)
{
    public TriggerContext(ServerPlayer player, ServerLevel level, @Nullable BlockPos blockPos, @Nullable String chatMessage)
    {
        this(player, level, blockPos, chatMessage, null, 0);
    }

    public CommandSourceStack asSource()
    {
        if (this.blockPos != null)
        {
            Vec3 center = Vec3.atCenterOf(this.blockPos);
            return this.level.getServer().createCommandSourceStack()
                .withPermission(2)
                .withLevel(this.level)
                .withPosition(center)
                .withEntity(this.player);
        }

        return this.player.createCommandSourceStack().withPermission(2);
    }
}
