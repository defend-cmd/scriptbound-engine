package dev.scriptbound.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

public class NpcPatrolGoal extends Goal
{
    private final NpcEntity npc;
    private int targetIndex;
    private int cooldown;

    public NpcPatrolGoal(NpcEntity npc)
    {
        this.npc = npc;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        return this.npc.getPatrolPoints().size() >= 2;
    }

    @Override
    public boolean canContinueToUse()
    {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (this.cooldown > 0)
        {
            this.cooldown--;
            return;
        }

        List<BlockPos> points = this.npc.getPatrolPoints();

        if (points.isEmpty())
        {
            return;
        }

        BlockPos target = points.get(this.targetIndex % points.size());

        if (this.npc.blockPosition().closerThan(target, 1.6D))
        {
            this.targetIndex = (this.targetIndex + 1) % points.size();
            this.cooldown = 40;
            this.npc.getNavigation().stop();
            return;
        }

        if (this.npc.getNavigation().isDone())
        {
            this.npc.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
        }
    }
}
