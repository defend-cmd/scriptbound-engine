package dev.scriptbound.flow;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FlowGraph
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

    public static FlowGraph fromJson(String json)
    {
        FlowGraph graph = new FlowGraph();
        JsonObject root = JsonParser.parseString(json == null || json.isBlank() ? "{}" : json).getAsJsonObject();

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
                node.type = obj.has("type") ? obj.get("type").getAsString() : "message";
                node.x = obj.has("x") ? obj.get("x").getAsInt() : 80;
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

    private static void applyDefaults(Node node)
    {
        switch (node.type)
        {
            case "npc" -> {
                node.props.put("npcId", "guard");
                node.props.put("displayName", "NPC");
                node.props.put("mobId", "minecraft:villager");
            }
            case "player_join", "player_chat", "player_death", "player_respawn", "manual" -> {
            }
            case "block_left", "block_right" -> {
                node.props.put("x", "");
                node.props.put("y", "");
                node.props.put("z", "");
            }
            case "region_enter", "region_exit" -> {
                node.props.put("x", "");
                node.props.put("y", "");
                node.props.put("z", "");
                node.props.put("radius", "");
            }
            case "message" -> {
                node.props.put("text", "Hello!");
                node.props.put("mode", "chat");
            }
            case "dialogue" -> node.props.put("dialogue", "");
            case "state" -> {
                node.props.put("scope", "player");
                node.props.put("key", "");
                node.props.put("value", "1");
            }
            case "script" -> node.props.put("script", "");
            case "quest" -> {
                node.props.put("op", "give");
                node.props.put("quest", "");
            }
            case "command" -> node.props.put("command", "");
            case "run_trigger" -> node.props.put("trigger", "");
            case "condition" -> {
                node.props.put("source", "state");
                node.props.put("key", "");
                node.props.put("op", ">=");
                node.props.put("value", "1");
            }
            case "delay", "wait" -> node.props.put("ticks", "20");
            case "block" -> {
                node.props.put("x", "");
                node.props.put("y", "");
                node.props.put("z", "");
            }
            case "move_block" -> {
                node.props.put("toX", "");
                node.props.put("toY", "");
                node.props.put("toZ", "");
                node.props.put("speed", "normal");
                node.props.put("easing", "smooth");
                node.props.put("ticks", "");
            }
            case "teleport" -> {
                node.props.put("x", "0");
                node.props.put("y", "64");
                node.props.put("z", "0");
            }
            case "sound" -> {
                node.props.put("sound", "minecraft:entity.experience_orb.pickup");
                node.props.put("volume", "1.0");
            }
            case "give_item" -> {
                node.props.put("item", "minecraft:bread");
                node.props.put("count", "1");
            }
            case "play_film" -> node.props.put("film", "");
            case "faction" -> {
                node.props.put("faction", "");
                node.props.put("op", "add");
                node.props.put("value", "1");
            }
            case "give_xp" -> {
                node.props.put("amount", "1");
                node.props.put("levels", "false");
            }
            case "damage" -> node.props.put("amount", "1.0");
            case "effect" -> {
                node.props.put("effect", "minecraft:glowing");
                node.props.put("duration", "200");
                node.props.put("amplifier", "0");
                node.props.put("particles", "true");
            }
            case "comment" -> node.props.put("text", "Note");
            default -> node.props.put("text", "");
        }
    }

    public static boolean isEvent(String type)
    {
        return switch (type)
        {
            case "npc", "player_join", "player_chat", "player_death", "player_respawn", "block_left", "block_right", "region_enter", "region_exit", "manual" -> true;
            default -> false;
        };
    }

    public static String labelFor(String type)
    {
        return switch (type)
        {
            case "npc" -> "NPC R-Click";
            case "player_join" -> "Player Join";
            case "player_chat" -> "Player Chat";
            case "player_death" -> "Player Death";
            case "player_respawn" -> "Player Respawn";
            case "block_left" -> "Block LMB";
            case "block_right" -> "Block RMB";
            case "region_enter" -> "Region Enter";
            case "region_exit" -> "Region Exit";
            case "manual" -> "Manual Run";
            case "message" -> "Message";
            case "dialogue" -> "Dialogue";
            case "state" -> "Set State";
            case "script" -> "Run Script";
            case "quest" -> "Quest";
            case "command" -> "Command";
            case "run_trigger" -> "Run Trigger";
            case "condition" -> "Condition";
            case "delay" -> "Delay";
            case "wait" -> "Wait";
            case "block" -> "Block";
            case "move_block" -> "Move Block";
            case "teleport" -> "Teleport";
            case "sound" -> "Play Sound";
            case "give_item" -> "Give Item";
            case "play_film" -> "Play Film";
            case "faction" -> "Faction Rep";
            case "give_xp" -> "Give XP";
            case "damage" -> "Damage/Heal";
            case "effect" -> "Potion Effect";
            case "comment" -> "Comment";
            default -> type;
        };
    }

    public static int colorFor(String type)
    {
        return switch (type)
        {
            case "npc" -> 0xFF43A047;
            case "player_join", "player_chat", "player_death", "player_respawn" -> 0xFF66BB6A;
            case "block_left", "block_right" -> 0xFF26A69A;
            case "region_enter", "region_exit" -> 0xFF26C6DA;
            case "manual" -> 0xFF9CCC65;
            case "message" -> 0xFF42A5F5;
            case "dialogue" -> 0xFF29B6F6;
            case "state" -> 0xFF26C6DA;
            case "script" -> 0xFFAB47BC;
            case "quest" -> 0xFFFFCA28;
            case "command" -> 0xFFFF7043;
            case "run_trigger" -> 0xFFEF5350;
            case "condition" -> 0xFFFFB300;
            case "delay", "wait" -> 0xFF8D6E63;
            case "block" -> 0xFF78909C;
            case "move_block" -> 0xFF5C6BC0;
            case "teleport" -> 0xFF7E57C2;
            case "sound" -> 0xFF5C6BC0;
            case "give_item" -> 0xFFD4E157;
            case "play_film" -> 0xFFEC407A;
            case "faction" -> 0xFFFF8A65;
            case "give_xp" -> 0xFF81C784;
            case "damage" -> 0xFFE57373;
            case "effect" -> 0xFFBA68C8;
            case "comment" -> 0xFF90A4AE;
            default -> 0xFF42A5F5;
        };
    }
}
