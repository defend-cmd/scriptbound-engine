package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;

public record GiveXpAction(int amount, boolean levels) implements TriggerAction
{
    public static GiveXpAction fromJson(JsonObject json)
    {
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
        boolean levels = json.has("levels") && json.get("levels").getAsBoolean();
        return new GiveXpAction(amount, levels);
    }

    @Override
    public String type()
    {
        return "give_xp";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (context.player() == null) return;

        if (this.levels)
        {
            context.player().giveExperienceLevels(this.amount);
        }
        else
        {
            context.player().giveExperiencePoints(this.amount);
        }
    }
}
