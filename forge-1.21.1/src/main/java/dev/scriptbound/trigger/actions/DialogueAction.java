package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.dialogue.DialogueManager;
import dev.scriptbound.trigger.TriggerContext;

public record DialogueAction(String dialogueId) implements TriggerAction
{
    public static DialogueAction fromJson(JsonObject json)
    {
        return new DialogueAction(json.has("dialogue") ? json.get("dialogue").getAsString() : "");
    }

    @Override
    public String type()
    {
        return "dialogue";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (!this.dialogueId.isBlank())
        {
            DialogueManager.open(context.player(), this.dialogueId);
        }
    }
}
