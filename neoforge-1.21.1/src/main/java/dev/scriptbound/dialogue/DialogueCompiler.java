package dev.scriptbound.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class DialogueCompiler
{
    private DialogueCompiler() {}

    public static void compileAndSave(MinecraftServer server, String dialogueId, DialogueGraph graph) throws IOException
    {
        cleanupSubTriggers(server, dialogueId);

        JsonObject file = new JsonObject();
        file.add("graph", JsonParserHelper.parseObject(graph.toJson()));
        file.add("lines", compileLines(server, dialogueId, graph));
        JsonFiles.writeObject(ScriptBoundPaths.dialogueFile(server, dialogueId), file);
    }

    public static JsonArray compileLines(MinecraftServer server, String dialogueId, DialogueGraph graph)
    {
        DialogueGraph.Node start = findStart(graph);

        if (start == null)
        {
            ScriptBoundMod.LOGGER.warn("Dialogue {}: no start node", dialogueId);
            return new JsonArray();
        }

        List<DialogueGraph.Node> choices = new ArrayList<>();

        for (String targetId : graph.outgoing(start.id))
        {
            DialogueGraph.Node node = graph.findNode(targetId);

            if (node != null && "choice".equals(node.type))
            {
                choices.add(node);
            }
        }

        if (choices.size() == 1)
        {
            ScriptBoundMod.LOGGER.warn("Dialogue {}: only one choice node — need at least 2 for a real choice", dialogueId);
        }

        JsonObject line = new JsonObject();
        line.addProperty("speaker", start.props.getOrDefault("speaker", "NPC"));
        line.addProperty("text", start.props.getOrDefault("text", ""));

        String npcId = start.props.getOrDefault("npcId", "").trim();

        if (!npcId.isBlank())
        {
            line.addProperty("npcId", npcId);
        }

        JsonArray choiceArray = new JsonArray();

        for (DialogueGraph.Node choice : choices)
        {
            JsonObject choiceObj = new JsonObject();
            choiceObj.addProperty("text", choice.props.getOrDefault("text", "..."));
            String trigger = resolveActionTrigger(server, dialogueId, graph, choice);

            if (!trigger.isBlank())
            {
                choiceObj.addProperty("trigger", trigger);
            }

            choiceArray.add(choiceObj);
        }

        if (!choiceArray.isEmpty())
        {
            line.add("choices", choiceArray);
        }

        JsonArray lines = new JsonArray();
        lines.add(line);
        return lines;
    }

    private static DialogueGraph.Node findStart(DialogueGraph graph)
    {
        for (DialogueGraph.Node node : graph.nodes())
        {
            if ("start".equals(node.type))
            {
                return node;
            }
        }

        return null;
    }

    private static String resolveActionTrigger(MinecraftServer server, String dialogueId, DialogueGraph graph, DialogueGraph.Node choice)
    {
        DialogueGraph.Node first = firstOutgoing(graph, choice);

        if (first == null || "comment".equals(first.type))
        {
            return "";
        }

        if ("run_trigger".equals(first.type) && firstOutgoing(graph, first) == null)
        {
            return first.props.getOrDefault("trigger", "");
        }

        String rootId = dialogueId + "__c_" + choice.id;
        compileChain(server, dialogueId, choice.id, graph, first, rootId, 0);
        return rootId;
    }

    private static DialogueGraph.Node firstOutgoing(DialogueGraph graph, DialogueGraph.Node node)
    {
        List<String> outgoing = graph.outgoing(node.id);

        if (outgoing.isEmpty())
        {
            return null;
        }

        return graph.findNode(outgoing.get(0));
    }

    private static void compileChain(
        MinecraftServer server,
        String dialogueId,
        String choiceId,
        DialogueGraph graph,
        DialogueGraph.Node node,
        String writeToId,
        int waitIndex
    )
    {
        if (node == null || "comment".equals(node.type))
        {
            return;
        }

        if ("wait".equals(node.type))
        {
            int ticks = parseInt(node.props.getOrDefault("ticks", "20"), 20);
            String afterId = dialogueId + "__c_" + choiceId + "_w" + waitIndex;
            compileChain(server, dialogueId, choiceId, graph, firstOutgoing(graph, node), afterId, waitIndex + 1);

            JsonArray root = new JsonArray();
            JsonObject delay = new JsonObject();
            delay.addProperty("type", "delay");
            delay.addProperty("ticks", ticks);
            delay.addProperty("trigger", afterId);
            root.add(delay);
            writeTrigger(server, writeToId, root);
            return;
        }

        JsonArray actions = new JsonArray();
        actions.add(actionToJson(node));
        writeTrigger(server, writeToId, actions);
    }

    private static void writeTrigger(MinecraftServer server, String triggerId, JsonArray actions)
    {
        JsonObject trigger = new JsonObject();
        trigger.add("actions", actions);
        JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, triggerId), trigger);
    }

    private static JsonObject actionToJson(DialogueGraph.Node node)
    {
        JsonObject action = new JsonObject();

        switch (node.type)
        {
            case "message" -> {
                action.addProperty("type", "message");
                action.addProperty("text", node.props.getOrDefault("text", ""));
                action.addProperty("mode", node.props.getOrDefault("mode", "chat"));
            }
            case "command" -> {
                action.addProperty("type", "command");
                action.addProperty("command", node.props.getOrDefault("command", ""));
            }
            case "state" -> {
                action.addProperty("type", "state");
                action.addProperty("scope", node.props.getOrDefault("scope", "player"));
                action.addProperty("key", node.props.getOrDefault("key", ""));
                action.addProperty("value", node.props.getOrDefault("value", "1"));
            }
            case "script" -> {
                action.addProperty("type", "script");
                action.addProperty("script", node.props.getOrDefault("script", ""));
            }
            case "dialogue" -> {
                action.addProperty("type", "dialogue");
                action.addProperty("dialogue", node.props.getOrDefault("dialogue", ""));
            }
            case "run_trigger" -> {
                action.addProperty("type", "trigger");
                action.addProperty("trigger", node.props.getOrDefault("trigger", ""));
            }
            default -> action.addProperty("type", node.type);
        }

        return action;
    }

    private static int parseInt(String value, int fallback)
    {
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private static void cleanupSubTriggers(MinecraftServer server, String dialogueId)
    {
        Path dir = ScriptBoundPaths.triggersDir(server);

        if (!Files.isDirectory(dir))
        {
            return;
        }

        try (Stream<Path> stream = Files.list(dir))
        {
            for (Path path : stream.toList())
            {
                String name = path.getFileName().toString();

                if (name.startsWith(dialogueId + "__c_") && name.endsWith(".json"))
                {
                    Files.deleteIfExists(path);
                }
            }
        }
        catch (IOException e)
        {
            ScriptBoundMod.LOGGER.warn("Failed to cleanup dialogue sub-triggers for {}", dialogueId, e);
        }
    }

    private static final class JsonParserHelper
    {
        private static JsonObject parseObject(String json)
        {
            return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        }
    }

    private static final class JsonArrayHelper
    {
        private static JsonArray single(JsonObject obj)
        {
            JsonArray array = new JsonArray();
            array.add(obj);
            return array;
        }
    }
}
