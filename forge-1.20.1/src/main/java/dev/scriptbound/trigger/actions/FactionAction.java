package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.faction.FactionManager;
import dev.scriptbound.trigger.TriggerContext;

public record FactionAction(String faction, String op, double value) implements TriggerAction
{
    public static FactionAction fromJson(JsonObject json)
    {
        String faction = json.has("faction") ? json.get("faction").getAsString() : "";
        String op = json.has("op") ? json.get("op").getAsString() : "add";
        double value = json.has("value") ? json.get("value").getAsDouble() : 1D;
        return new FactionAction(faction, op, value);
    }

    @Override
    public String type()
    {
        return "faction";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (this.faction.isBlank())
        {
            return;
        }

        if ("set".equalsIgnoreCase(this.op))
        {
            FactionManager.setScore(context.player(), this.faction, this.value);
        }
        else
        {
            FactionManager.addScore(context.player(), this.faction, this.value);
        }
    }
}
