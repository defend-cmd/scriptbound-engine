package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.trigger.TriggerContext;
import dev.scriptbound.ui.UiManager;

public record UiOpenAction(String ui) implements TriggerAction
{
    public static UiOpenAction fromJson(JsonObject json)
    {
        return new UiOpenAction(json.has("ui") ? json.get("ui").getAsString() : "");
    }

    @Override
    public String type()
    {
        return "ui_open";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (context.player() == null || this.ui.isBlank())
        {
            return;
        }

        JsonObject json = UiManager.getJson(this.ui);

        if (json == null)
        {
            return;
        }

        NetworkHandler.sendOpenUi(context.player(), this.ui, json.toString());
    }
}
