package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.trigger.TriggerContext;

public record UiSetAction(String ui, String element, String field, String value) implements TriggerAction
{
    public static UiSetAction fromJson(JsonObject json)
    {
        return new UiSetAction(
            json.has("ui") ? json.get("ui").getAsString() : "",
            json.has("element") ? json.get("element").getAsString() : "",
            json.has("field") ? json.get("field").getAsString() : "text",
            json.has("value") ? json.get("value").getAsString() : ""
        );
    }

    @Override
    public String type()
    {
        return "ui_set";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (context.player() == null || this.ui.isBlank() || this.element.isBlank())
        {
            return;
        }

        NetworkHandler.sendUiSet(context.player(), this.ui, this.element, this.field, this.value);
    }
}
