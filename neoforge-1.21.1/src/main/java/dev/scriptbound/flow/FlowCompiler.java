package dev.scriptbound.flow;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.block.entity.RegionBlockEntity;
import dev.scriptbound.block.entity.TriggerBlockEntity;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class FlowCompiler
{
    private FlowCompiler() {}

    public static void compileAndSave(MinecraftServer server, String flowId, FlowGraph graph) throws IOException
    {
        Files.createDirectories(ScriptBoundPaths.flowsDir(server));
        Files.writeString(ScriptBoundPaths.flowFile(server, flowId), graph.toJson(), StandardCharsets.UTF_8);

        cleanupSubTriggers(server, flowId);

        Compilation compilation = new Compilation(graph, flowId);
        JsonArray mainActions = compilation.compile();

        JsonObject trigger = new JsonObject();
        trigger.add("actions", mainActions);
        JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, flowId), trigger);

        for (Map.Entry<String, JsonArray> entry : compilation.subTriggers.entrySet())
        {
            writeTriggerActions(server, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, JsonArray> entry : compilation.eventTriggers.entrySet())
        {
            writeTriggerActions(server, entry.getKey(), entry.getValue());
        }

        applyBindings(server, flowId, graph);
    }

    private static void cleanupSubTriggers(MinecraftServer server, String flowId)
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

                if ((name.startsWith(flowId + "__t") || name.startsWith(flowId + "__e_")) && name.endsWith(".json"))
                {
                    Files.deleteIfExists(path);
                }
            }
        }
        catch (IOException e)
        {
            ScriptBoundMod.LOGGER.warn("Failed to cleanup sub-triggers for flow {}", flowId, e);
        }
    }

    private static void writeTriggerActions(MinecraftServer server, String triggerId, JsonArray actions)
    {
        JsonObject trigger = new JsonObject();
        trigger.add("actions", actions);
        JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, triggerId), trigger);
    }

    private static String eventTriggerId(String flowId, FlowGraph.Node event)
    {
        return "manual".equals(event.type) ? flowId : flowId + "__e_" + event.id;
    }

    private static final class Compilation
    {
        private final FlowGraph graph;
        private final String flowId;
        private final Map<String, List<String>> outgoing = new LinkedHashMap<>();
        private final Map<String, JsonArray> subTriggers = new LinkedHashMap<>();
        private final Map<String, JsonArray> eventTriggers = new LinkedHashMap<>();
        private final Set<String> seen = new HashSet<>();
        private int delayCounter;
        private String currentEventTriggerId = "";

        Compilation(FlowGraph graph, String flowId)
        {
            this.graph = graph;
            this.flowId = flowId;

            for (FlowGraph.Link link : graph.links())
            {
                this.outgoing.computeIfAbsent(link.from, key -> new ArrayList<>()).add(link.to);
            }
        }

        JsonArray compile()
        {
            JsonArray main = new JsonArray();
            Set<String> reachableFromEvents = new HashSet<>();

            for (FlowGraph.Node event : this.graph.nodes())
            {
                if (!FlowGraph.isEvent(event.type))
                {
                    continue;
                }

                this.currentEventTriggerId = eventTriggerId(this.flowId, event);
                this.seen.clear();
                this.delayCounter = 0;

                JsonArray eventActions = new JsonArray();

                for (String next : this.outgoing.getOrDefault(event.id, List.of()))
                {
                    emit(this.graph.findNode(next), eventActions, List.of());
                }

                reachableFromEvents.addAll(this.seen);

                if ("manual".equals(event.type))
                {
                    for (int i = 0; i < eventActions.size(); i++)
                    {
                        main.add(eventActions.get(i));
                    }
                }
                else
                {
                    this.eventTriggers.put(this.currentEventTriggerId, eventActions);
                }
            }

            this.currentEventTriggerId = this.flowId;
            this.seen.clear();
            this.seen.addAll(reachableFromEvents);
            this.delayCounter = 0;

            for (FlowGraph.Node node : this.graph.nodes())
            {
                if (!FlowGraph.isEvent(node.type) && !this.seen.contains(node.id))
                {
                    emit(node, main, List.of());
                }
            }

            return main;
        }

        private void emit(FlowGraph.Node node, JsonArray list, List<JsonObject> conditions)
        {
            if (node == null || this.seen.contains(node.id) || FlowGraph.isEvent(node.type))
            {
                return;
            }

            this.seen.add(node.id);

            switch (node.type)
            {
                case "comment" -> {
                }
                case "condition" -> {
                    List<JsonObject> extended = new ArrayList<>(conditions);
                    extended.add(conditionFromNode(node));

                    for (String nextId : this.outgoing.getOrDefault(node.id, List.of()))
                    {
                        emit(this.graph.findNode(nextId), list, extended);
                    }
                }
                case "block" -> {
                    for (String nextId : this.outgoing.getOrDefault(node.id, List.of()))
                    {
                        emit(this.graph.findNode(nextId), list, conditions);
                    }
                }
                case "delay", "wait" -> {
                    this.delayCounter++;
                    String subId = this.currentEventTriggerId + "__t" + this.delayCounter;
                    JsonArray subList = new JsonArray();

                    for (String nextId : this.outgoing.getOrDefault(node.id, List.of()))
                    {
                        emit(this.graph.findNode(nextId), subList, conditions);
                    }

                    this.subTriggers.put(subId, subList);

                    JsonObject action = new JsonObject();
                    action.addProperty("type", "delay");
                    action.addProperty("ticks", parseInt(node.props.getOrDefault("ticks", "20"), 20));
                    action.addProperty("trigger", subId);
                    applyIf(action, conditions);
                    list.add(action);
                }
                default -> {
                    JsonObject action = actionFromNode(this.graph, node);

                    if (action != null)
                    {
                        applyIf(action, conditions);
                        list.add(action);
                    }

                    for (String nextId : this.outgoing.getOrDefault(node.id, List.of()))
                    {
                        emit(this.graph.findNode(nextId), list, conditions);
                    }
                }
            }
        }
    }

    private static void applyIf(JsonObject action, List<JsonObject> conditions)
    {
        if (conditions.isEmpty())
        {
            return;
        }

        if (conditions.size() == 1)
        {
            action.add("if", conditions.get(0));
            return;
        }

        JsonObject and = new JsonObject();
        and.addProperty("type", "and");
        JsonArray checks = new JsonArray();

        for (JsonObject condition : conditions)
        {
            checks.add(condition);
        }

        and.add("checks", checks);
        action.add("if", and);
    }

    private static JsonObject conditionFromNode(FlowGraph.Node node)
    {
        JsonObject condition = new JsonObject();
        String source = node.props.getOrDefault("source", "state").trim().toLowerCase();
        String key = node.props.getOrDefault("key", "");

        switch (source)
        {
            case "quest" -> {
                condition.addProperty("type", "quest");
                condition.addProperty("quest", key);
                condition.addProperty("status", node.props.getOrDefault("op", "active"));
            }
            case "talked", "talk" -> {
                condition.addProperty("type", "talk");
                condition.addProperty("npc", key);
            }
            case "faction" -> {
                condition.addProperty("type", "faction");
                condition.addProperty("faction", key);
                condition.addProperty("attitude", node.props.getOrDefault("op", "friendly"));
            }
            default -> {
                condition.addProperty("type", "state");
                condition.addProperty("scope", node.props.getOrDefault("scope", "player"));
                condition.addProperty("key", key);
                condition.addProperty("op", normalizeOp(node.props.getOrDefault("op", ">=")));
                condition.addProperty("value", parseDouble(node.props.getOrDefault("value", "1")));
            }
        }

        return condition;
    }

    private static String normalizeOp(String op)
    {
        return switch (op.trim())
        {
            case "==", "=", "eq" -> "eq";
            case "<", "lt" -> "lt";
            case "<=", "lte" -> "lte";
            case ">", "gt" -> "gt";
            case "absent", "missing", "!" -> "absent";
            case "present", "exists", "?" -> "present";
            default -> "gte";
        };
    }

    private static JsonObject actionFromNode(FlowGraph graph, FlowGraph.Node node)
    {
        return switch (node.type)
        {
            case "message" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "message");
                action.addProperty("text", node.props.getOrDefault("text", ""));
                action.addProperty("subtitle", node.props.getOrDefault("subtitle", ""));
                action.addProperty("mode", node.props.getOrDefault("mode", "chat"));
                yield action;
            }
            case "dialogue" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "dialogue");
                action.addProperty("dialogue", node.props.getOrDefault("dialogue", ""));
                yield action;
            }
            case "state" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "state");
                action.addProperty("scope", node.props.getOrDefault("scope", "player"));
                action.addProperty("key", node.props.getOrDefault("key", ""));
                action.addProperty("value", parseDouble(node.props.getOrDefault("value", "1")));
                yield action;
            }
            case "script" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "script");
                action.addProperty("script", node.props.getOrDefault("script", ""));
                yield action;
            }
            case "quest" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "quest");
                action.addProperty("op", node.props.getOrDefault("op", "give"));
                action.addProperty("quest", node.props.getOrDefault("quest", ""));
                yield action;
            }
            case "command" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "command");
                action.addProperty("command", node.props.getOrDefault("command", ""));
                yield action;
            }
            case "run_trigger" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "trigger");
                action.addProperty("trigger", node.props.getOrDefault("trigger", ""));
                yield action;
            }
            case "teleport" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "teleport");
                action.addProperty("x", parseDouble(node.props.getOrDefault("x", "0")));
                action.addProperty("y", parseDouble(node.props.getOrDefault("y", "64")));
                action.addProperty("z", parseDouble(node.props.getOrDefault("z", "0")));
                yield action;
            }
            case "sound" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "sound");
                action.addProperty("sound", node.props.getOrDefault("sound", ""));
                action.addProperty("volume", parseDouble(node.props.getOrDefault("volume", "1.0")));
                yield action;
            }
            case "give_item" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "give_item");
                action.addProperty("item", node.props.getOrDefault("item", ""));
                action.addProperty("count", parseInt(node.props.getOrDefault("count", "1"), 1));
                yield action;
            }
            case "play_film" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "play_film");
                action.addProperty("film", node.props.getOrDefault("film", ""));
                yield action;
            }
            case "faction" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "faction");
                action.addProperty("faction", node.props.getOrDefault("faction", ""));
                action.addProperty("op", node.props.getOrDefault("op", "add"));
                action.addProperty("value", parseDouble(node.props.getOrDefault("value", "1")));
                yield action;
            }
            case "give_xp" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "give_xp");
                action.addProperty("amount", parseInt(node.props.getOrDefault("amount", "1"), 1));
                action.addProperty("levels", "true".equals(node.props.getOrDefault("levels", "false")));
                yield action;
            }
            case "damage" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "damage");
                action.addProperty("amount", parseDouble(node.props.getOrDefault("amount", "1.0")));
                yield action;
            }
            case "effect" -> {
                JsonObject action = new JsonObject();
                action.addProperty("type", "effect");
                action.addProperty("effect", node.props.getOrDefault("effect", "minecraft:glowing"));
                action.addProperty("duration", parseInt(node.props.getOrDefault("duration", "200"), 200));
                action.addProperty("amplifier", parseInt(node.props.getOrDefault("amplifier", "0"), 0));
                action.addProperty("particles", "true".equals(node.props.getOrDefault("particles", "true")));
                yield action;
            }
            case "move_block" -> {
                BlockPos from = resolveLinkedBlock(graph, node);
                JsonObject action = new JsonObject();
                action.addProperty("type", "move_block");

                if (from != null)
                {
                    action.addProperty("fromX", from.getX());
                    action.addProperty("fromY", from.getY());
                    action.addProperty("fromZ", from.getZ());
                }

                action.addProperty("toX", parseInt(node.props.getOrDefault("toX", "0"), 0));
                action.addProperty("toY", parseInt(node.props.getOrDefault("toY", "64"), 64));
                action.addProperty("toZ", parseInt(node.props.getOrDefault("toZ", "0"), 0));

                String ticksRaw = node.props.getOrDefault("ticks", "").trim();

                if (ticksRaw.isEmpty())
                {
                    action.addProperty("ticks", speedToTicks(node.props.getOrDefault("speed", "normal")));
                }
                else
                {
                    action.addProperty("ticks", parseInt(ticksRaw, 40));
                }

                action.addProperty("easing", node.props.getOrDefault("easing", "smooth"));
                yield action;
            }
            default -> null;
        };
    }

    private static void applyBindings(MinecraftServer server, String flowId, FlowGraph graph) throws IOException
    {
        for (FlowGraph.Node node : graph.nodes())
        {
            if (!FlowGraph.isEvent(node.type))
            {
                continue;
            }

            String eventTrigger = eventTriggerId(flowId, node);

            switch (node.type)
            {
                case "npc" -> writeNpc(server, eventTrigger, node);
                case "player_join" -> DashboardSettingsHelper.setGlobalTrigger(server, "player_join", eventTrigger);
                case "player_chat" -> DashboardSettingsHelper.setGlobalTrigger(server, "player_chat", eventTrigger);
                case "player_death" -> DashboardSettingsHelper.setGlobalTrigger(server, "player_death", eventTrigger);
                case "player_respawn" -> DashboardSettingsHelper.setGlobalTrigger(server, "player_respawn", eventTrigger);
                case "block_left" -> bindBlock(server, eventTrigger, node, true);
                case "block_right" -> bindBlock(server, eventTrigger, node, false);
                case "region_enter" -> bindRegion(server, eventTrigger, node, true);
                case "region_exit" -> bindRegion(server, eventTrigger, node, false);
                default -> {
                }
            }
        }
    }

    private static void bindBlock(MinecraftServer server, String flowId, FlowGraph.Node node, boolean left)
    {
        BlockPos pos = parsePos(node);

        if (pos == null)
        {
            return;
        }

        for (ServerLevel level : server.getAllLevels())
        {
            BlockEntity entity = level.getBlockEntity(pos);

            if (entity instanceof TriggerBlockEntity trigger)
            {
                if (left)
                {
                    trigger.setLeftTrigger(flowId);
                }
                else
                {
                    trigger.setRightTrigger(flowId);
                }

                return;
            }
        }

        ScriptBoundMod.LOGGER.warn("Flow {}: no trigger block found at {} to bind", flowId, pos);
    }

    private static void bindRegion(MinecraftServer server, String flowId, FlowGraph.Node node, boolean enter)
    {
        BlockPos pos = parsePos(node);

        if (pos == null)
        {
            return;
        }

        for (ServerLevel level : server.getAllLevels())
        {
            BlockEntity entity = level.getBlockEntity(pos);

            if (entity instanceof RegionBlockEntity region)
            {
                if (enter)
                {
                    region.setOnEnter(flowId);
                }
                else
                {
                    region.setOnExit(flowId);
                }

                int radius = parseInt(node.props.getOrDefault("radius", ""), 0);

                if (radius > 0)
                {
                    region.setRadius(radius, Math.max(2, radius / 2), radius);
                }

                return;
            }
        }

        ScriptBoundMod.LOGGER.warn("Flow {}: no region block found at {} to bind", flowId, pos);
    }

    private static BlockPos parsePos(FlowGraph.Node node)
    {
        try
        {
            int x = Integer.parseInt(node.props.getOrDefault("x", "").trim());
            int y = Integer.parseInt(node.props.getOrDefault("y", "").trim());
            int z = Integer.parseInt(node.props.getOrDefault("z", "").trim());
            return new BlockPos(x, y, z);
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }

    private static void writeNpc(MinecraftServer server, String flowId, FlowGraph.Node node)
    {
        String npcId = node.props.getOrDefault("npcId", flowId);
        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.npcFile(server, npcId));

        root.addProperty("displayName", node.props.getOrDefault("displayName", root.has("displayName") ? root.get("displayName").getAsString() : npcId));
        root.addProperty("onInteract", flowId);

        if (!root.has("health"))
        {
            root.addProperty("health", 20);
        }

        if (!root.has("invulnerable"))
        {
            root.addProperty("invulnerable", true);
        }

        JsonObject form = root.has("form") && root.get("form").isJsonObject() ? root.getAsJsonObject("form") : new JsonObject();

        if (!form.has("id"))
        {
            form.addProperty("id", "bbs:mob");
        }

        form.addProperty("mobId", node.props.getOrDefault("mobId", form.has("mobId") ? form.get("mobId").getAsString() : "minecraft:villager"));
        root.add("form", form);

        JsonFiles.writeObject(ScriptBoundPaths.npcFile(server, npcId), root);
    }

    private static double parseDouble(String value)
    {
        try
        {
            return Double.parseDouble(value.trim());
        }
        catch (NumberFormatException ignored)
        {
            return 1D;
        }
    }

    public static int speedToTicks(String speed)
    {
        return switch (speed == null ? "" : speed.trim().toLowerCase())
        {
            case "slow" -> 80;
            case "fast" -> 15;
            case "instant", "sharp" -> 1;
            default -> 40;
        };
    }

    private static BlockPos resolveLinkedBlock(FlowGraph graph, FlowGraph.Node fromNode)
    {
        java.util.Set<String> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();

        for (FlowGraph.Link link : graph.links())
        {
            if (link.to.equals(fromNode.id))
            {
                queue.add(link.from);
            }
        }

        while (!queue.isEmpty())
        {
            String id = queue.poll();

            if (!visited.add(id))
            {
                continue;
            }

            FlowGraph.Node node = graph.findNode(id);

            if (node == null)
            {
                continue;
            }

            if ("block".equals(node.type))
            {
                BlockPos pos = parsePos(node);

                if (pos != null)
                {
                    return pos;
                }
            }

            for (FlowGraph.Link link : graph.links())
            {
                if (link.to.equals(id))
                {
                    queue.add(link.from);
                }
            }
        }

        return null;
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
}
