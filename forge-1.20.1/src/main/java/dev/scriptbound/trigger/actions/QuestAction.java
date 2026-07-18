package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.quest.QuestManager;
import dev.scriptbound.trigger.TriggerContext;

public record QuestAction(String op, String questId) implements TriggerAction
{
    public static QuestAction fromJson(JsonObject json)
    {
        String op = json.has("op") ? json.get("op").getAsString() : "give";
        String quest = json.has("quest") ? json.get("quest").getAsString()
            : (json.has("id") ? json.get("id").getAsString() : "");
        return new QuestAction(op, quest);
    }

    @Override
    public String type()
    {
        return "quest";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (this.questId.isBlank())
        {
            return;
        }

        switch (this.op.toLowerCase())
        {
            case "complete" -> QuestManager.complete(context.player(), this.questId);
            default -> QuestManager.give(context.player(), this.questId);
        }
    }
}
