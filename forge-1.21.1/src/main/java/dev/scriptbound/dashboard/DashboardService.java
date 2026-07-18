package dev.scriptbound.dashboard;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.data.GlobalStateManager;
import dev.scriptbound.data.PlayerStateCapability;
import dev.scriptbound.data.ServerSettings;
import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.dialogue.DialogueCompiler;
import dev.scriptbound.dialogue.DialogueGraph;
import dev.scriptbound.dialogue.DialogueManager;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.network.packet.DashboardDocumentPacket;
import dev.scriptbound.npc.NpcManager;
import dev.scriptbound.quest.QuestManager;
import dev.scriptbound.script.ScriptManager;
import dev.scriptbound.trigger.TriggerExecutor;
import dev.scriptbound.trigger.TriggerManager;
import dev.scriptbound.flow.FlowCompiler;
import dev.scriptbound.flow.FlowGraph;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class DashboardService
{
    public static final List<String> GLOBAL_TRIGGER_EVENTS = List.of(
        "player_join",
        "player_chat",
        "player_death",
        "player_respawn"
    );

    private DashboardService() {}

    public static void sendDashboard(ServerPlayer player)
    {
        if (!player.hasPermissions(2))
        {
            player.sendSystemMessage(Component.literal("You need OP level 2 to open the ScriptBound dashboard."));
            return;
        }

        NetworkHandler.sendDashboard(player, collect(player));
    }

    public static DashboardSnapshot collect(ServerPlayer player)
    {
        Map<String, String> globalStates = formatStates(GlobalStateManager.global().snapshot());
        Map<String, String> playerStates = formatStates(PlayerStateCapability.get(player).snapshot());
        List<String> triggers = listIds(ScriptBoundPaths.triggersDir(player.server), ".json");
        Map<String, Integer> triggerActionCounts = new LinkedHashMap<>();

        for (String triggerId : triggers)
        {
            JsonObject root = JsonFiles.readObject(ScriptBoundPaths.triggerFile(player.server, triggerId));
            int count = 0;

            if (root.has("actions") && root.get("actions").isJsonArray())
            {
                count = root.getAsJsonArray("actions").size();
            }

            triggerActionCounts.put(triggerId, count);
        }

        List<String> npcFiles = listIds(ScriptBoundPaths.npcsDir(player.server), ".json");

        return new DashboardSnapshot(
            globalStates,
            playerStates,
            triggers,
            triggerActionCounts,
            listIds(ScriptBoundPaths.scriptsDir(player.server), ".js"),
            listIds(ScriptBoundPaths.dialoguesDir(player.server), ".json"),
            listIds(ScriptBoundPaths.questsDir(player.server), ".json"),
            npcFiles,
            listIds(ScriptBoundPaths.flowsDir(player.server), ".json"),
            collectNpcMeta(player.server, npcFiles),
            new LinkedHashMap<>(ServerSettings.globalTriggers())
        );
    }

    public static void handleAction(ServerPlayer player, String action, String id, String payload)
    {
        if (!player.hasPermissions(2))
        {
            return;
        }

        switch (action)
        {
            case "reload" -> {
                invalidate(player);
                NetworkHandler.sendNotification(player, 1, "Reloaded ScriptBound data.");
                sendDashboard(player);
            }
            case "refresh" -> sendDashboard(player);
            case "load_doc" -> loadDocument(player, id);
            case "save_doc" -> saveDocument(player, id, payload);
            case "create_doc" -> createDocument(player, id);
            case "duplicate_doc" -> duplicateDocument(player, id);
            case "rename_doc" -> renameDocument(player, id, payload);
            case "delete_doc" -> deleteDocument(player, id);
            case "run_trigger" -> {
                if (id == null || id.isBlank())
                {
                    return;
                }

                boolean ok = TriggerExecutor.run(player.server, id, player);
                if (ok) {
                    NetworkHandler.sendNotification(player, 1, "Ran trigger '" + id + "'");
                } else {
                    NetworkHandler.sendNotification(player, 2, "Trigger '" + id + "' not found or empty.");
                }
                sendDashboard(player);
            }
            case "run_script" -> {
                if (id == null || id.isBlank())
                {
                    return;
                }

                ScriptManager.Result result = ScriptManager.run(player.server, id, player, "main");
                NetworkHandler.sendNotification(player, 0, result.message());
                sendDashboard(player);
            }
            case "set_state" -> {
                if (id == null || !id.contains("|"))
                {
                    return;
                }

                String[] parts = id.split("\\|", 3);

                if (parts.length < 3)
                {
                    return;
                }

                String scopeToken = "player".equalsIgnoreCase(parts[0]) ? "@p" : "~";

                try
                {
                    StateService.Target target = StateService.resolveTarget(player.createCommandSourceStack().withPermission(2), scopeToken);
                    StateService.set(target, parts[1], StateValue.parse(parts[2]));
                    NetworkHandler.sendNotification(player, 1, "Set state " + parts[1]);
                    sendDashboard(player);
                }
                catch (IllegalArgumentException e)
                {
                    NetworkHandler.sendNotification(player, 3, e.getMessage());
                }
            }
            default -> {
            }
        }
    }

    private static void invalidate(ServerPlayer player)
    {
        TriggerManager.invalidateAll();
        ServerSettings.load(player.server);
        DialogueManager.invalidateAll();
        NpcManager.invalidateAll();
        QuestManager.invalidateAll();
        dev.scriptbound.faction.FactionManager.invalidateAll();
    }

    private static void loadDocument(ServerPlayer player, String token)
    {
        DocumentRef ref = DocumentRef.parse(token);

        if (ref == null)
        {
            return;
        }

        try
        {
            if ("global".equals(ref.category))
            {
                String assigned = ServerSettings.globalTriggers().getOrDefault(ref.id, "");
                NetworkHandler.sendDashboardDocument(player, new DashboardDocumentPacket("global", ref.id, assigned, true));
                return;
            }

            DashboardContentService.Category category = DashboardContentService.Category.fromToken(ref.category);

            if (category == null)
            {
                return;
            }

            String content = DashboardContentService.load(player.server, category, ref.id);
            boolean exists = Files.exists(DashboardContentService.fileFor(player.server, category, ref.id));
            NetworkHandler.sendDashboardDocument(player, new DashboardDocumentPacket(ref.category, ref.id, content, exists));
        }
        catch (IOException e)
        {
            NetworkHandler.sendNotification(player, 3, "Could not load document: " + e.getMessage());
        }
    }

    private static void saveDocument(ServerPlayer player, String token, String payload)
    {
        DocumentRef ref = DocumentRef.parse(token);

        if (ref == null)
        {
            return;
        }

        try
        {
            if ("global".equals(ref.category))
            {
                DashboardContentService.setGlobalTrigger(player.server, ref.id, payload);
                NetworkHandler.sendNotification(player, 1, "Saved global trigger for " + ref.id);
            }
            else
            {
                DashboardContentService.Category category = DashboardContentService.Category.fromToken(ref.category);

                if (category == null)
                {
                    return;
                }

                if (category == DashboardContentService.Category.FLOW)
                {
                    FlowGraph graph = FlowGraph.fromJson(payload);
                    FlowCompiler.compileAndSave(player.server, ref.id, graph);
                    NetworkHandler.sendNotification(player, 1, "Saved flow '" + ref.id + "'");
                }
                else if (category == DashboardContentService.Category.DIALOGUE)
                {
                    DialogueGraph graph = DialogueGraph.fromJson(payload);
                    DialogueCompiler.compileAndSave(player.server, ref.id, graph);
                    NetworkHandler.sendNotification(player, 1, "Saved dialogue '" + ref.id + "'");
                }
                else
                {
                    DashboardContentService.save(player.server, category, ref.id, payload);
                    NetworkHandler.sendNotification(player, 1, "Saved " + ref.category + "/" + ref.id);
                }
            }

            invalidate(player);
            sendDashboard(player);
        }
        catch (IOException e)
        {
            NetworkHandler.sendNotification(player, 3, "Could not save document: " + e.getMessage());
        }
    }

    private static void createDocument(ServerPlayer player, String token)
    {
        DocumentRef ref = DocumentRef.parse(token);

        if (ref == null || "global".equals(ref.category))
        {
            return;
        }

        DashboardContentService.Category category = DashboardContentService.Category.fromToken(ref.category);

        if (category == null)
        {
            return;
        }

        try
        {
            DashboardContentService.create(player.server, category, ref.id);
            NetworkHandler.sendNotification(player, 1, "Created " + ref.category + "/" + ref.id);
            invalidate(player);
            sendDashboard(player);
            loadDocument(player, token);
        }
        catch (IOException e)
        {
            NetworkHandler.sendNotification(player, 3, "Could not create document: " + e.getMessage());
        }
    }

    private static void duplicateDocument(ServerPlayer player, String token)
    {
        DocumentRef ref = DocumentRef.parse(token);

        if (ref == null || "global".equals(ref.category))
        {
            return;
        }

        DashboardContentService.Category category = DashboardContentService.Category.fromToken(ref.category);

        if (category == null)
        {
            return;
        }

        try
        {
            String content = DashboardContentService.load(player.server, category, ref.id);
            String newId = ref.id + "_copy";
            int copyCount = 2;

            while (Files.exists(DashboardContentService.fileFor(player.server, category, newId)))
            {
                newId = ref.id + "_copy_" + copyCount;
                copyCount++;
            }

            DashboardContentService.create(player.server, category, newId);
            DashboardContentService.save(player.server, category, newId, content);

            NetworkHandler.sendNotification(player, 1, "Duplicated " + ref.id + " as " + newId);
            invalidate(player);
            sendDashboard(player);
        }
        catch (IOException e)
        {
            NetworkHandler.sendNotification(player, 3, "Could not duplicate document: " + e.getMessage());
        }
    }

    private static void renameDocument(ServerPlayer player, String token, String payload)
    {
        DocumentRef ref = DocumentRef.parse(token);

        if (ref == null || "global".equals(ref.category) || payload == null || payload.isBlank())
        {
            return;
        }

        DashboardContentService.Category category = DashboardContentService.Category.fromToken(ref.category);

        if (category == null)
        {
            return;
        }

        try
        {
            String newId = payload.trim();
            if (newId.equals(ref.id)) return;

            if (Files.exists(DashboardContentService.fileFor(player.server, category, newId)))
            {
                NetworkHandler.sendNotification(player, 2, "Cannot rename: '" + newId + "' already exists.");
                return;
            }

            String content = DashboardContentService.load(player.server, category, ref.id);

            DashboardContentService.create(player.server, category, newId);
            DashboardContentService.save(player.server, category, newId, content);

            DashboardContentService.delete(player.server, category, ref.id);
            if (category == DashboardContentService.Category.FLOW)
            {
                Files.deleteIfExists(ScriptBoundPaths.triggerFile(player.server, ref.id));
                FlowGraph graph = FlowGraph.fromJson(content);
                FlowCompiler.compileAndSave(player.server, newId, graph);
            }

            NetworkHandler.sendNotification(player, 1, "Renamed " + ref.id + " to " + newId);
            invalidate(player);
            sendDashboard(player);
        }
        catch (IOException e)
        {
            NetworkHandler.sendNotification(player, 3, "Could not rename document: " + e.getMessage());
        }
    }

    private static void deleteDocument(ServerPlayer player, String token)
    {
        DocumentRef ref = DocumentRef.parse(token);

        if (ref == null || "global".equals(ref.category))
        {
            return;
        }

        DashboardContentService.Category category = DashboardContentService.Category.fromToken(ref.category);

        if (category == null)
        {
            return;
        }

        try
        {
            DashboardContentService.delete(player.server, category, ref.id);

            if (category == DashboardContentService.Category.FLOW)
            {
                Files.deleteIfExists(ScriptBoundPaths.triggerFile(player.server, ref.id));
            }

            NetworkHandler.sendNotification(player, 1, "Deleted " + ref.id);
            invalidate(player);
            sendDashboard(player);
        }
        catch (IOException e)
        {
            NetworkHandler.sendNotification(player, 3, "Could not delete document: " + e.getMessage());
        }
    }

    private static Map<String, String> collectNpcMeta(net.minecraft.server.MinecraftServer server, List<String> npcFiles)
    {
        Map<String, String> meta = new LinkedHashMap<>();

        for (String id : npcFiles)
        {
            JsonObject root = JsonFiles.readObject(ScriptBoundPaths.npcFile(server, id));
            String name = root.has("displayName") ? root.get("displayName").getAsString() : id;
            String mob = "minecraft:villager";

            if (root.has("form") && root.get("form").isJsonObject() && root.getAsJsonObject("form").has("mobId"))
            {
                mob = root.getAsJsonObject("form").get("mobId").getAsString();
            }

            meta.put(id, name + "\u0001" + mob);
        }

        try
        {
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels())
            {
                for (net.minecraft.world.entity.Entity entity : level.getEntities().getAll())
                {
                    if (!(entity instanceof dev.scriptbound.entity.NpcEntity npc))
                    {
                        continue;
                    }

                    String id = npc.getBlueprintId();

                    if (id == null || id.isBlank() || meta.containsKey(id))
                    {
                        continue;
                    }

                    String name = npc.hasCustomName() ? npc.getCustomName().getString() : id;
                    String mob = "minecraft:villager";

                    try
                    {
                        String form = npc.getFormJson();

                        if (form != null && !form.isBlank())
                        {
                            JsonObject formObj = com.google.gson.JsonParser.parseString(form).getAsJsonObject();

                            if (formObj.has("mobId"))
                            {
                                mob = formObj.get("mobId").getAsString();
                            }
                        }
                    }
                    catch (RuntimeException ignored)
                    {
                    }

                    meta.put(id, name + "\u0001" + mob);
                }
            }
        }
        catch (RuntimeException ignored)
        {
        }

        return meta;
    }

    private static Map<String, String> formatStates(Map<String, StateValue> states)
    {
        Map<String, String> formatted = new LinkedHashMap<>();

        for (Map.Entry<String, StateValue> entry : states.entrySet())
        {
            formatted.put(entry.getKey(), entry.getValue().isNumber()
                ? String.valueOf(entry.getValue().asNumber())
                : entry.getValue().asString());
        }

        return formatted;
    }

    private static List<String> listIds(java.nio.file.Path dir, String suffix)
    {
        List<String> ids = new ArrayList<>();

        if (!Files.isDirectory(dir))
        {
            return ids;
        }

        try (Stream<java.nio.file.Path> files = Files.list(dir))
        {
            files
                .filter(path -> path.getFileName().toString().endsWith(suffix))
                .map(path -> path.getFileName().toString().substring(0, path.getFileName().toString().length() - suffix.length()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(ids::add);
        }
        catch (IOException ignored)
        {
        }

        return ids;
    }

    private record DocumentRef(String category, String id)
    {
        static DocumentRef parse(String token)
        {
            if (token == null || !token.contains("|"))
            {
                return null;
            }

            String[] parts = token.split("\\|", 2);

            if (parts.length < 2 || parts[1].isBlank())
            {
                return null;
            }

            return new DocumentRef(parts[0], parts[1]);
        }
    }
}
