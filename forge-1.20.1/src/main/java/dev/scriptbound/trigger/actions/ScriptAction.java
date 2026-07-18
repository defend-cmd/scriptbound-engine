package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.script.ScriptManager;
import dev.scriptbound.trigger.TriggerContext;
import net.minecraft.network.chat.Component;

public record ScriptAction(String scriptId, String function) implements TriggerAction
{
    public static ScriptAction fromJson(JsonObject json)
    {
        return new ScriptAction(
            json.has("script") ? json.get("script").getAsString() : "",
            json.has("function") ? json.get("function").getAsString() : "main"
        );
    }

    @Override
    public String type()
    {
        return "script";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (!this.scriptId.isBlank())
        {
            ScriptManager.Result result = ScriptManager.run(context.player().server, this.scriptId, context.player(), this.function);

            if (!result.success())
            {
                context.player().sendSystemMessage(Component.literal(result.message()));
            }
        }
    }
}
