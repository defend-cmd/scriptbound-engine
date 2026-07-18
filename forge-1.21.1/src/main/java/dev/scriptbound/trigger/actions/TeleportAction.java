package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;

public record TeleportAction(double x, double y, double z) implements TriggerAction
{
    public static TeleportAction fromJson(JsonObject json)
    {
        double x = json.has("x") ? json.get("x").getAsDouble() : 0D;
        double y = json.has("y") ? json.get("y").getAsDouble() : 64D;
        double z = json.has("z") ? json.get("z").getAsDouble() : 0D;
        return new TeleportAction(x, y, z);
    }

    @Override
    public String type()
    {
        return "teleport";
    }

    @Override
    public void execute(TriggerContext context)
    {
        context.player().teleportTo(
            context.player().serverLevel(),
            this.x,
            this.y,
            this.z,
            context.player().getYRot(),
            context.player().getXRot()
        );
    }
}
