package dev.scriptbound.init;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class DemoContent
{
    private DemoContent() {}

    public static void ensure(MinecraftServer server)
    {
        try
        {
            Files.createDirectories(ScriptBoundPaths.triggersDir(server));
            Files.createDirectories(ScriptBoundPaths.scriptsDir(server));
            Files.createDirectories(ScriptBoundPaths.npcsDir(server));
            Files.createDirectories(ScriptBoundPaths.dialoguesDir(server));
            Files.createDirectories(ScriptBoundPaths.questsDir(server));
            Files.createDirectories(ScriptBoundPaths.flowsDir(server));

            if (!Files.exists(ScriptBoundPaths.triggerFile(server, "welcome")))
            {
                JsonObject trigger = new JsonObject();
                JsonArray actions = new JsonArray();
                JsonObject message = new JsonObject();
                message.addProperty("type", "message");
                message.addProperty("text", "Welcome to ScriptBound Engine!");
                message.addProperty("mode", "title");
                actions.add(message);
                trigger.add("actions", actions);
                JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, "welcome"), trigger);
            }

            if (!Files.exists(ScriptBoundPaths.triggerFile(server, "player_start")))
            {
                JsonObject trigger = new JsonObject();
                JsonArray actions = new JsonArray();

                JsonObject welcome = new JsonObject();
                welcome.addProperty("type", "message");
                welcome.addProperty("text", "Welcome to ScriptBound Engine!");
                welcome.addProperty("mode", "title");
                actions.add(welcome);

                JsonObject quest = new JsonObject();
                quest.addProperty("type", "quest");
                quest.addProperty("op", "give");
                quest.addProperty("quest", "meet_guard");
                actions.add(quest);

                JsonObject hint = new JsonObject();
                hint.addProperty("type", "message");
                hint.addProperty("text", "Quest started: talk to the Gate Guard.");
                hint.addProperty("mode", "chat");
                actions.add(hint);

                trigger.add("actions", actions);
                JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, "player_start"), trigger);
            }

            if (!Files.exists(ScriptBoundPaths.settingsFile(server)))
            {
                JsonObject settings = new JsonObject();
                JsonObject global = new JsonObject();
                global.addProperty("player_join", "player_start");
                settings.add("global_triggers", global);
                JsonFiles.writeObject(ScriptBoundPaths.settingsFile(server), settings);
            }

            if (!Files.exists(ScriptBoundPaths.triggerFile(server, "test")))
            {
                JsonObject trigger = new JsonObject();
                JsonArray actions = new JsonArray();
                JsonObject message = new JsonObject();
                message.addProperty("type", "message");
                message.addProperty("text", "Trigger works!");
                message.addProperty("mode", "chat");
                actions.add(message);
                trigger.add("actions", actions);
                JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, "test"), trigger);
            }

            writeHelloScript(server);

            writeDialogue(server, "guard_first", "Gate Guard", "Halt! State your business, traveler.");
            writeDialogue(server, "guard_return", "Gate Guard", "Move along. The gate stays closed for now.");

            if (!Files.exists(ScriptBoundPaths.triggerFile(server, "guard_interact")))
            {
                JsonObject trigger = new JsonObject();
                JsonArray actions = new JsonArray();

                JsonObject firstTalk = new JsonObject();
                JsonObject firstIf = new JsonObject();
                firstIf.addProperty("type", "state");
                firstIf.addProperty("scope", "player");
                firstIf.addProperty("key", "talked.guard");
                firstIf.addProperty("op", "absent");
                firstTalk.add("if", firstIf);
                firstTalk.addProperty("type", "dialogue");
                firstTalk.addProperty("dialogue", "guard_first");
                actions.add(firstTalk);

                JsonObject returnTalk = new JsonObject();
                JsonObject returnIf = new JsonObject();
                returnIf.addProperty("type", "state");
                returnIf.addProperty("scope", "player");
                returnIf.addProperty("key", "talked.guard");
                returnIf.addProperty("op", "present");
                returnTalk.add("if", returnIf);
                returnTalk.addProperty("type", "dialogue");
                returnTalk.addProperty("dialogue", "guard_return");
                actions.add(returnTalk);

                JsonObject questHint = new JsonObject();
                JsonObject questIf = new JsonObject();
                questIf.addProperty("type", "quest");
                questIf.addProperty("quest", "meet_guard");
                questIf.addProperty("status", "ready");
                questHint.add("if", questIf);
                questHint.addProperty("type", "message");
                questHint.addProperty("text", "Quest ready: /sbe quest complete @p meet_guard");
                questHint.addProperty("mode", "actionbar");
                actions.add(questHint);

                trigger.add("actions", actions);
                JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, "guard_interact"), trigger);
            }

            if (!Files.exists(ScriptBoundPaths.questFile(server, "meet_guard")))
            {
                JsonObject quest = new JsonObject();
                quest.addProperty("title", "Meet the Guard");
                quest.addProperty("description", "Find the gate guard and talk to him.");
                JsonArray objectives = new JsonArray();
                JsonObject objective = new JsonObject();
                objective.addProperty("type", "talk");
                objective.addProperty("npc", "guard");
                objectives.add(objective);
                quest.add("objectives", objectives);
                quest.addProperty("reward_trigger", "meet_guard_reward");
                JsonFiles.writeObject(ScriptBoundPaths.questFile(server, "meet_guard"), quest);
            }

            if (!Files.exists(ScriptBoundPaths.triggerFile(server, "meet_guard_reward")))
            {
                JsonObject trigger = new JsonObject();
                JsonArray actions = new JsonArray();

                JsonObject message = new JsonObject();
                message.addProperty("type", "message");
                message.addProperty("text", "Quest Complete!");
                message.addProperty("mode", "title");
                actions.add(message);

                JsonObject state = new JsonObject();
                state.addProperty("type", "state");
                state.addProperty("scope", "player");
                state.addProperty("key", "guard.quest_done");
                state.addProperty("value", 1);
                actions.add(state);

                trigger.add("actions", actions);
                JsonFiles.writeObject(ScriptBoundPaths.triggerFile(server, "meet_guard_reward"), trigger);
            }

            if (!Files.exists(ScriptBoundPaths.flowFile(server, "player_start")))
            {
                Files.writeString(ScriptBoundPaths.flowFile(server, "player_start"), """
                    {
                      "nodes": [
                        {"id": "join", "type": "player_join", "x": 40, "y": 80, "props": {}},
                        {"id": "title", "type": "message", "x": 220, "y": 60, "props": {"text": "Welcome to ScriptBound Engine!", "mode": "title"}},
                        {"id": "quest", "type": "quest", "x": 220, "y": 120, "props": {"op": "give", "quest": "meet_guard"}},
                        {"id": "hint", "type": "message", "x": 400, "y": 120, "props": {"text": "Quest started: talk to the Gate Guard.", "mode": "chat"}}
                      ],
                      "links": [
                        {"from": "join", "to": "title"},
                        {"from": "join", "to": "quest"},
                        {"from": "quest", "to": "hint"}
                      ]
                    }
                    """.stripIndent(), StandardCharsets.UTF_8);
            }

            if (!Files.exists(ScriptBoundPaths.flowFile(server, "guard_interact")))
            {
                Files.writeString(ScriptBoundPaths.flowFile(server, "guard_interact"), """
                    {
                      "nodes": [
                        {"id": "npc", "type": "npc", "x": 40, "y": 80, "props": {"npcId": "guard", "displayName": "Gate Guard", "mobId": "minecraft:villager"}},
                        {"id": "dlg1", "type": "dialogue", "x": 220, "y": 80, "props": {"dialogue": "guard_first"}}
                      ],
                      "links": [
                        {"from": "npc", "to": "dlg1"}
                      ]
                    }
                    """.stripIndent(), StandardCharsets.UTF_8);
            }

            if (!Files.exists(ScriptBoundPaths.npcFile(server, "guard")))
            {
                JsonObject npc = new JsonObject();
                npc.addProperty("displayName", "Gate Guard");
                npc.addProperty("health", 20);
                npc.addProperty("invulnerable", true);
                npc.addProperty("onInteract", "guard_interact");

                JsonObject form = new JsonObject();
                form.addProperty("id", "bbs:mob");
                form.addProperty("mobId", "minecraft:villager");
                npc.add("form", form);

                JsonFiles.writeObject(ScriptBoundPaths.npcFile(server, "guard"), npc);
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static void writeDialogue(MinecraftServer server, String id, String speaker, String text) throws java.io.IOException
    {
        if (Files.exists(ScriptBoundPaths.dialogueFile(server, id)))
        {
            return;
        }

        JsonObject dialogue = new JsonObject();
        JsonArray lines = new JsonArray();
        JsonObject line = new JsonObject();
        line.addProperty("speaker", speaker);
        line.addProperty("text", text);
        lines.add(line);
        dialogue.add("lines", lines);
        JsonFiles.writeObject(ScriptBoundPaths.dialogueFile(server, id), dialogue);
    }

    private static void writeHelloScript(MinecraftServer server) throws java.io.IOException
    {
        var file = ScriptBoundPaths.scriptFile(server, "hello");
        String content = """
            function main() {
                c.setStateString("player", "script_ok", "1");
                c.send("Script hello ran!");
            }
            """.stripIndent();

        if (!Files.exists(file))
        {
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return;
        }

        String existing = Files.readString(file, StandardCharsets.UTF_8);

        if (existing.contains("\"p\", \"script_ok\""))
        {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
    }
}
