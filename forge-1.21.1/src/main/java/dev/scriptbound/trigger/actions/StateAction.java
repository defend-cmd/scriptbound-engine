package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.trigger.TriggerContext;

public record StateAction(String scope, String key, String op, StateValue value) implements TriggerAction
{
    public static StateAction fromJson(JsonObject json)
    {
        String scope = json.has("scope") ? json.get("scope").getAsString() : "player";
        String key = json.has("key") ? json.get("key").getAsString() : "";
        String op = json.has("op") ? json.get("op").getAsString() : "set";
        StateValue value = json.has("value") ? StateValue.fromJson(json.get("value")) : new StateValue.NumberValue(1);

        return new StateAction(scope, key, op, value);
    }

    @Override
    public String type()
    {
        return "state";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (this.key.isBlank())
        {
            return;
        }

        String target = "global".equalsIgnoreCase(this.scope) || "~".equals(this.scope) ? "~" : "@p";
        StateService.Target resolved = StateService.resolveTarget(context.asSource(), target);

        switch (this.op.toLowerCase())
        {
            case "add" -> StateService.add(resolved, this.key, this.value.asNumber());
            case "remove" -> StateService.remove(resolved, this.key);
            default -> StateService.set(resolved, this.key, this.value);
        }
    }
}
