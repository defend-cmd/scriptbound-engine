package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.entity.MovingBlockEntity;
import dev.scriptbound.flow.FlowCompiler;
import dev.scriptbound.trigger.TriggerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record MoveBlockAction(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, int ticks, String easing)
    implements TriggerAction
{
    public static MoveBlockAction fromJson(JsonObject json)
    {
        int fromX = json.has("fromX") ? json.get("fromX").getAsInt() : 0;
        int fromY = json.has("fromY") ? json.get("fromY").getAsInt() : 64;
        int fromZ = json.has("fromZ") ? json.get("fromZ").getAsInt() : 0;
        int toX = json.has("toX") ? json.get("toX").getAsInt() : fromX;
        int toY = json.has("toY") ? json.get("toY").getAsInt() : fromY;
        int toZ = json.has("toZ") ? json.get("toZ").getAsInt() : fromZ;
        int ticks = json.has("ticks") ? json.get("ticks").getAsInt() : FlowCompiler.speedToTicks("normal");
        String easing = json.has("easing") ? json.get("easing").getAsString() : "smooth";
        return new MoveBlockAction(fromX, fromY, fromZ, toX, toY, toZ, ticks, easing);
    }

    @Override
    public String type()
    {
        return "move_block";
    }

    @Override
    public void execute(TriggerContext context)
    {
        ServerLevel level = context.level();
        BlockPos from = new BlockPos(this.fromX, this.fromY, this.fromZ);
        BlockPos to = new BlockPos(this.toX, this.toY, this.toZ);

        if (from.equals(to))
        {
            return;
        }

        MovingBlockEntity.start(level, from, to, this.ticks, this.easing);
    }
}
