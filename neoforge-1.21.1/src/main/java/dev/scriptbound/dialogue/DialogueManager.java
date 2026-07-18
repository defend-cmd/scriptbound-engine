package dev.scriptbound.dialogue;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.trigger.TriggerExecutor;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public final class DialogueManager
{
    private static final Map<String, DialogueDefinition> CACHE = new HashMap<>();

    private DialogueManager() {}

    public static DialogueDefinition get(net.minecraft.server.MinecraftServer server, String id)
    {
        String key = normalize(id);

        if (CACHE.containsKey(key))
        {
            return CACHE.get(key);
        }

        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.dialogueFile(server, key));
        DialogueDefinition definition = DialogueDefinition.fromJson(key, root);
        CACHE.put(key, definition);
        return definition;
    }

    public static void invalidateAll()
    {
        CACHE.clear();
    }

    public static boolean open(ServerPlayer player, String dialogueId)
    {
        DialogueDefinition definition = get(player.server, dialogueId);

        if (definition.lines().isEmpty())
        {
            ScriptBoundMod.LOGGER.warn("Dialogue '{}' is empty or missing.", dialogueId);
            player.sendSystemMessage(Component.literal("Dialogue '" + dialogueId + "' is empty or missing. Check scriptbound/dialogues/" + normalize(dialogueId) + ".json"));
            return false;
        }

        showLine(player, definition, 0);
        return true;
    }

    public static void showLine(ServerPlayer player, DialogueDefinition definition, int index)
    {
        if (index >= definition.lines().size())
        {
            NetworkHandler.sendCloseDialogue(player);
            return;
        }

        DialogueDefinition.Line line = definition.lines().get(index);

        if (!line.speaker().isBlank())
        {
            player.sendSystemMessage(Component.literal("[" + line.speaker() + "] " + line.text()));
        }
        else
        {
            player.sendSystemMessage(Component.literal(line.text()));
        }

        NetworkHandler.sendDialogueLine(player, definition.id(), index, line.speaker(), line.text(), line.choices());
    }

    public static void advance(ServerPlayer player, String dialogueId, int lineIndex)
    {
        DialogueDefinition definition = get(player.server, dialogueId);
        int next = lineIndex + 1;

        if (next < definition.lines().size())
        {
            showLine(player, definition, next);
        }
        else
        {
            NetworkHandler.sendCloseDialogue(player);
        }
    }

    public static void choose(ServerPlayer player, String dialogueId, int lineIndex, int choiceIndex)
    {
        DialogueDefinition definition = get(player.server, dialogueId);

        if (lineIndex < 0 || lineIndex >= definition.lines().size())
        {
            return;
        }

        DialogueDefinition.Line line = definition.lines().get(lineIndex);

        if (choiceIndex < 0 || choiceIndex >= line.choices().size())
        {
            return;
        }

        String trigger = line.choices().get(choiceIndex).trigger();
        NetworkHandler.sendCloseDialogue(player);

        if (trigger != null && !trigger.isBlank())
        {
            TriggerExecutor.run(player.server, trigger, player);
        }
    }

    private static String normalize(String id)
    {
        String trimmed = id.trim();

        if (trimmed.endsWith(".json"))
        {
            return trimmed.substring(0, trimmed.length() - 5);
        }

        return trimmed;
    }
}
