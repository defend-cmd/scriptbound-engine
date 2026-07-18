package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;
import dev.scriptbound.trigger.TriggerExecutor;

public record RunTriggerAction(String triggerId) implements TriggerAction
{
    public static RunTriggerAction fromJson(JsonObject json)
    {
        return new RunTriggerAction(json.has("trigger") ? json.get("trigger").getAsString() : "");
    }

    @Override
    public String type()
    {
        return "trigger";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (!this.triggerId.isBlank())
        {
            TriggerContext newContext = new TriggerContext(
                context.player(), context.level(), context.blockPos(), context.chatMessage(), context.npc(), context.depth() + 1
            );
            TriggerExecutor.run(context.level().getServer(), this.triggerId, newContext);
        }
    }
}
