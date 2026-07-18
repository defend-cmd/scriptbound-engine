package dev.scriptbound.trigger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.trigger.actions.CommandAction;
import dev.scriptbound.trigger.actions.ConditionalAction;
import dev.scriptbound.trigger.actions.DelayAction;
import dev.scriptbound.trigger.actions.DialogueAction;
import dev.scriptbound.trigger.actions.FactionAction;
import dev.scriptbound.trigger.actions.GiveItemAction;
import dev.scriptbound.trigger.actions.GiveXpAction;
import dev.scriptbound.trigger.actions.DamageAction;
import dev.scriptbound.trigger.actions.EffectAction;
import dev.scriptbound.trigger.actions.MessageAction;
import dev.scriptbound.trigger.actions.MoveBlockAction;
import dev.scriptbound.trigger.actions.PlayFilmAction;
import dev.scriptbound.trigger.actions.QuestAction;
import dev.scriptbound.trigger.actions.RunTriggerAction;
import dev.scriptbound.trigger.actions.ScriptAction;
import dev.scriptbound.trigger.actions.SoundAction;
import dev.scriptbound.trigger.actions.StateAction;
import dev.scriptbound.trigger.actions.TeleportAction;
import dev.scriptbound.trigger.actions.TriggerAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TriggerChain
{
    private final List<TriggerAction> actions;

    public TriggerChain(List<TriggerAction> actions)
    {
        this.actions = List.copyOf(actions);
    }

    public List<TriggerAction> actions()
    {
        return this.actions;
    }

    public static TriggerChain fromJson(JsonObject root)
    {
        List<TriggerAction> actions = new ArrayList<>();

        if (!root.has("actions") || !root.get("actions").isJsonArray())
        {
            return new TriggerChain(actions);
        }

        JsonArray array = root.getAsJsonArray("actions");

        for (int i = 0; i < array.size(); i++)
        {
            if (!array.get(i).isJsonObject())
            {
                continue;
            }

            JsonObject entry = array.get(i).getAsJsonObject();

            if (!entry.has("type"))
            {
                continue;
            }

            TriggerAction action = ConditionalAction.fromJson(entry);

            if (action != null)
            {
                actions.add(action);
            }
        }

        return new TriggerChain(actions);
    }

    public static TriggerAction parseActionBody(JsonObject entry)
    {
        if (!entry.has("type"))
        {
            return null;
        }

        return switch (entry.get("type").getAsString().toLowerCase())
        {
            case "message" -> MessageAction.fromJson(entry);
            case "command" -> CommandAction.fromJson(entry);
            case "state" -> StateAction.fromJson(entry);
            case "trigger" -> RunTriggerAction.fromJson(entry);
            case "script" -> ScriptAction.fromJson(entry);
            case "dialogue" -> DialogueAction.fromJson(entry);
            case "play_film" -> PlayFilmAction.fromJson(entry);
            case "quest" -> QuestAction.fromJson(entry);
            case "delay" -> DelayAction.fromJson(entry);
            case "teleport" -> TeleportAction.fromJson(entry);
            case "sound" -> SoundAction.fromJson(entry);
            case "give_item" -> GiveItemAction.fromJson(entry);
            case "faction" -> FactionAction.fromJson(entry);
            case "move_block" -> MoveBlockAction.fromJson(entry);
            case "wait" -> DelayAction.fromJson(entry);
            case "give_xp" -> GiveXpAction.fromJson(entry);
            case "damage" -> DamageAction.fromJson(entry);
            case "effect" -> EffectAction.fromJson(entry);
            default -> {
                ScriptBoundMod.LOGGER.warn("Unknown trigger action type: {}", entry.get("type").getAsString());
                yield null;
            }
        };
    }

    public static TriggerChain empty()
    {
        return new TriggerChain(Collections.emptyList());
    }
}
