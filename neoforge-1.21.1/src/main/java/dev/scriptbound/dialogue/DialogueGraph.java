package dev.scriptbound.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DialogueGraph
{
    public static final class Node
    {
        public String id;
        public String type;
        public int x;
        public int y;
        public final Map<String, String> props = new LinkedHashMap<>();
    }

    public static final class Link
    {
        public String from;
        public String to;
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();

    public List<Node> nodes()
    {
        return this.nodes;
    }

    public List<Link> links()
    {
        return this.links;
    }

    public Node findNode(String id)
    {
        for (Node node : this.nodes)
        {
            if (node.id.equals(id))
            {
                return node;
            }
        }

        return null;
    }

    public Node addNode(String type, int x, int y)
    {
        Node node = new Node();
        node.id = UUID.randomUUID().toString().substring(0, 8);
        node.type = type;
        node.x = x;
        node.y = y;
        applyDefaults(node);
        this.nodes.add(node);
        return node;
    }

    public void removeNode(String id)
    {
        this.nodes.removeIf(node -> node.id.equals(id));
        this.links.removeIf(link -> link.from.equals(id) || link.to.equals(id));
    }

    public Link connect(String from, String to)
    {
        if (from.equals(to))
        {
            return null;
        }

        for (Link link : this.links)
        {
            if (link.from.equals(from) && link.to.equals(to))
            {
                return link;
            }
        }

        Link link = new Link();
        link.from = from;
        link.to = to;
        this.links.add(link);
        return link;
    }

    public List<String> outgoing(String nodeId)
    {
        List<String> result = new ArrayList<>();

        for (Link link : this.links)
        {
            if (link.from.equals(nodeId))
            {
                result.add(link.to);
            }
        }

        return result;
    }

    public String incomingOfType(String nodeId, String type)
    {
        for (Link link : this.links)
        {
            if (link.to.equals(nodeId))
            {
                Node from = findNode(link.from);

                if (from != null && type.equals(from.type))
                {
                    return link.from;
                }
            }
        }

        return null;
    }

    public static DialogueGraph fromJson(String json)
    {
        JsonObject root = JsonParser.parseString(json == null || json.isBlank() ? "{}" : json).getAsJsonObject();

        if (root.has("graph") && root.get("graph").isJsonObject())
        {
            return fromGraphObject(root.getAsJsonObject("graph"));
        }

        if (root.has("nodes"))
        {
            return fromGraphObject(root);
        }

        return fromLegacyLines(root);
    }

    private static DialogueGraph fromGraphObject(JsonObject root)
    {
        DialogueGraph graph = new DialogueGraph();

        if (root.has("nodes") && root.get("nodes").isJsonArray())
        {
            for (var element : root.getAsJsonArray("nodes"))
            {
                if (!element.isJsonObject())
                {
                    continue;
                }

                JsonObject obj = element.getAsJsonObject();
                Node node = new Node();
                node.id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString().substring(0, 8);
                node.type = obj.has("type") ? obj.get("type").getAsString() : "start";
                node.x = obj.has("x") ? obj.get("x").getAsInt() : 120;
                node.y = obj.has("y") ? obj.get("y").getAsInt() : 80;

                if (obj.has("props") && obj.get("props").isJsonObject())
                {
                    for (String key : obj.getAsJsonObject("props").keySet())
                    {
                        node.props.put(key, obj.getAsJsonObject("props").get(key).getAsString());
                    }
                }

                graph.nodes.add(node);
            }
        }

        if (root.has("links") && root.get("links").isJsonArray())
        {
            for (var element : root.getAsJsonArray("links"))
            {
                if (!element.isJsonObject())
                {
                    continue;
                }

                JsonObject obj = element.getAsJsonObject();
                Link link = new Link();
                link.from = obj.get("from").getAsString();
                link.to = obj.get("to").getAsString();
                graph.links.add(link);
            }
        }

        if (graph.nodes.isEmpty())
        {
            graph.addNode("start", 160, 60);
        }

        return graph;
    }

    private static DialogueGraph fromLegacyLines(JsonObject root)
    {
        DialogueGraph graph = new DialogueGraph();
        Node start = graph.addNode("start", 160, 60);

        if (!root.has("lines") || !root.get("lines").isJsonArray() || root.getAsJsonArray("lines").isEmpty())
        {
            return graph;
        }

        JsonObject line = root.getAsJsonArray("lines").get(0).getAsJsonObject();
        start.props.put("speaker", line.has("speaker") ? line.get("speaker").getAsString() : "NPC");
        start.props.put("text", line.has("text") ? line.get("text").getAsString() : "");

        if (!line.has("choices") || !line.get("choices").isJsonArray())
        {
            return graph;
        }

        int cx = 80;
        int cy = 180;

        for (var element : line.getAsJsonArray("choices"))
        {
            if (!element.isJsonObject())
            {
                continue;
            }

            JsonObject choiceObj = element.getAsJsonObject();
            Node choice = graph.addNode("choice", cx, cy);
            choice.props.put("text", choiceObj.has("text") ? choiceObj.get("text").getAsString() : "...");
            graph.connect(start.id, choice.id);

            String trigger = choiceObj.has("trigger") ? choiceObj.get("trigger").getAsString() : "";

            if (!trigger.isBlank())
            {
                Node action = graph.addNode("run_trigger", cx, cy + 90);
                action.props.put("trigger", trigger);
                graph.connect(choice.id, action.id);
            }

            cx += 120;
        }

        return graph;
    }

    public String toJson()
    {
        JsonObject root = new JsonObject();
        JsonArray nodesArray = new JsonArray();

        for (Node node : this.nodes)
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", node.id);
            obj.addProperty("type", node.type);
            obj.addProperty("x", node.x);
            obj.addProperty("y", node.y);
            JsonObject props = new JsonObject();

            for (Map.Entry<String, String> entry : node.props.entrySet())
            {
                props.addProperty(entry.getKey(), entry.getValue());
            }

            obj.add("props", props);
            nodesArray.add(obj);
        }

        JsonArray linksArray = new JsonArray();

        for (Link link : this.links)
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("from", link.from);
            obj.addProperty("to", link.to);
            linksArray.add(obj);
        }

        root.add("nodes", nodesArray);
        root.add("links", linksArray);
        return root.toString();
    }

    public static void applyDefaults(Node node)
    {
        switch (node.type)
        {
            case "start" -> {
                node.props.put("npcId", "");
                node.props.put("speaker", "NPC");
                node.props.put("text", "Hello, traveler.");
            }
            case "choice" -> node.props.put("text", "Option");
            case "message" -> {
                node.props.put("text", "...");
                node.props.put("mode", "chat");
            }
            case "run_trigger" -> node.props.put("trigger", "");
            case "command" -> node.props.put("command", "");
            case "state" -> {
                node.props.put("scope", "player");
                node.props.put("key", "");
                node.props.put("value", "1");
            }
            case "script" -> node.props.put("script", "");
            case "dialogue" -> node.props.put("dialogue", "");
            case "wait" -> node.props.put("ticks", "20");
            case "comment" -> node.props.put("text", "Note");
            default -> node.props.put("text", "");
        }
    }

    public static boolean hasInput(String type)
    {
        return switch (type)
        {
            case "start", "comment" -> false;
            default -> true;
        };
    }

    public static boolean hasOutput(String type)
    {
        return switch (type)
        {
            case "message", "run_trigger", "command", "state", "script", "dialogue", "comment" -> false;
            default -> true;
        };
    }

    public static String labelFor(String type)
    {
        return switch (type)
        {
            case "start" -> "Dialogue Start";
            case "choice" -> "Choice";
            case "message" -> "Message";
            case "run_trigger" -> "Run Trigger";
            case "command" -> "Command";
            case "state" -> "Set State";
            case "script" -> "Run Script";
            case "dialogue" -> "Next Dialogue";
            case "wait" -> "Wait";
            case "comment" -> "Comment";
            default -> type;
        };
    }

    public static int colorFor(String type)
    {
        return switch (type)
        {
            case "start" -> 0xFF43A047;
            case "choice" -> 0xFFFFCA28;
            case "message" -> 0xFF42A5F5;
            case "run_trigger" -> 0xFFEF5350;
            case "command" -> 0xFFFF7043;
            case "state" -> 0xFF26C6DA;
            case "script" -> 0xFFAB47BC;
            case "dialogue" -> 0xFF29B6F6;
            case "wait" -> 0xFF8D6E63;
            case "comment" -> 0xFF90A4AE;
            default -> 0xFF42A5F5;
        };
    }

    public int countChoices()
    {
        int count = 0;

        for (Node node : this.nodes)
        {
            if ("choice".equals(node.type))
            {
                count++;
            }
        }

        return count;
    }
}
