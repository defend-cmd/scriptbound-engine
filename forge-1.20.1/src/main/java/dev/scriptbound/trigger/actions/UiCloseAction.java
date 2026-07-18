package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.trigger.TriggerContext;

public record UiCloseAction(String ui) implements TriggerAction
{
    public static UiCloseAction fromJson(JsonObject json)
    {
        return new UiCloseAction(json.has("ui") ? json.get("ui").getAsString() : "*");
    }

    @Override
    public String type()
    {
        return "ui_close";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (context.player() == null)
        {
            return;
        }

        NetworkHandler.sendCloseUi(context.player(), this.ui.isBlank() ? "*" : this.ui);
    }
}
