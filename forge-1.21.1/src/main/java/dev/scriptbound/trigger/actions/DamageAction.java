package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;

public record DamageAction(float amount) implements TriggerAction
{
    public static DamageAction fromJson(JsonObject json)
    {
        float amount = json.has("amount") ? json.get("amount").getAsFloat() : 1.0f;
        return new DamageAction(amount);
    }

    @Override
    public String type()
    {
        return "damage";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (context.player() == null) return;

        if (this.amount < 0)
        {
            context.player().heal(-this.amount);
        }
        else if (this.amount > 0)
        {
            context.player().hurt(context.player().damageSources().generic(), this.amount);
        }
    }
}
