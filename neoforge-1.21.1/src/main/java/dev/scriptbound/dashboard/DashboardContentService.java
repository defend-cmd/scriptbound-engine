package dev.scriptbound.dashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.data.ServerSettings;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DashboardContentService
{
    public enum Category
    {
        TRIGGER("trigger", ".json"),
        SCRIPT("script", ".js"),
        DIALOGUE("dialogue", ".json"),
        QUEST("quest", ".json"),
        NPC("npc", ".json"),
        FLOW("flow", ".json");

        private final String token;
        private final String suffix;

        Category(String token, String suffix)
        {
            this.token = token;
            this.suffix = suffix;
        }

        public String token()
        {
            return this.token;
        }

        public static Category fromToken(String token)
        {
            if (token == null)
            {
                return null;
            }

            for (Category category : values())
            {
                if (category.token.equalsIgnoreCase(token))
                {
                    return category;
                }
            }

            return null;
        }
    }

    private DashboardContentService() {}

    public static Path fileFor(MinecraftServer server, Category category, String id)
    {
        return switch (category)
        {
            case TRIGGER -> ScriptBoundPaths.triggerFile(server, id);
            case SCRIPT -> ScriptBoundPaths.scriptFile(server, id);
            case DIALOGUE -> ScriptBoundPaths.dialogueFile(server, id);
            case QUEST -> ScriptBoundPaths.questFile(server, id);
            case NPC -> ScriptBoundPaths.npcFile(server, id);
            case FLOW -> ScriptBoundPaths.flowFile(server, id);
        };
    }

    public static String load(MinecraftServer server, Category category, String id) throws IOException
    {
        Path file = fileFor(server, category, id);

        if (!Files.exists(file))
        {
            return template(category, id);
        }

        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public static void save(MinecraftServer server, Category category, String id, String content) throws IOException
    {
        Path file = fileFor(server, category, id);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    public static void create(MinecraftServer server, Category category, String id) throws IOException
    {
        Path file = fileFor(server, category, id);

        if (Files.exists(file))
        {
            throw new IOException("Already exists: " + id);
        }

        save(server, category, id, template(category, id));
    }

    public static void delete(MinecraftServer server, Category category, String id) throws IOException
    {
        Files.deleteIfExists(fileFor(server, category, id));
    }

    public static void setGlobalTrigger(MinecraftServer server, String eventId, String triggerId) throws IOException
    {
        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.settingsFile(server));
        JsonObject global = root.has("global_triggers") && root.get("global_triggers").isJsonObject()
            ? root.getAsJsonObject("global_triggers")
            : new JsonObject();

        if (triggerId == null || triggerId.isBlank())
        {
            global.remove(eventId);
        }
        else
        {
            global.addProperty(eventId, triggerId);
        }

        root.add("global_triggers", global);
        JsonFiles.writeObject(ScriptBoundPaths.settingsFile(server), root);
        ServerSettings.load(server);
    }

    public static String template(Category category, String id)
    {
        return switch (category)
        {
            case TRIGGER -> """
                {
                  "actions": [
                    {
                      "type": "message",
                      "text": "New trigger",
                      "mode": "chat"
                    }
                  ]
                }
                """.stripIndent();
            case SCRIPT -> """
                function main() {
                    c.send("Hello from script!");
                }
                """.stripIndent();
            case DIALOGUE -> """
                {
                  "graph": {
                    "nodes": [
                      {
                        "id": "start1",
                        "type": "start",
                        "x": 160,
                        "y": 60,
                        "props": {
                          "npcId": "",
                          "speaker": "NPC",
                          "text": "Hello, traveler."
                        }
                      }
                    ],
                    "links": []
                  },
                  "lines": [
                    {
                      "speaker": "NPC",
                      "text": "Hello, traveler."
                    }
                  ]
                }
                """.stripIndent();
            case QUEST -> """
                {
                  "title": "%s",
                  "description": "Quest description",
                  "objectives": [
                    {
                      "type": "talk",
                      "npc": "guard"
                    }
                  ],
                  "reward_trigger": ""
                }
                """.formatted(titleCase(id)).stripIndent();
            case NPC -> """
                {
                  "displayName": "%s",
                  "health": 20,
                  "invulnerable": true,
                  "onInteract": "",
                  "form": {
                    "id": "bbs:mob",
                    "mobId": "minecraft:villager"
                  }
                }
                """.formatted(titleCase(id)).stripIndent();
            case FLOW -> """
                {
                  "nodes": [
                    {
                      "id": "start",
                      "type": "manual",
                      "x": 60,
                      "y": 80,
                      "props": {}
                    },
                    {
                      "id": "msg1",
                      "type": "message",
                      "x": 240,
                      "y": 80,
                      "props": {
                        "text": "Hello from flow!",
                        "mode": "chat"
                      }
                    }
                  ],
                  "links": [
                    {
                      "from": "start",
                      "to": "msg1"
                    }
                  ]
                }
                """.stripIndent();
        };
    }

    private static String titleCase(String id)
    {
        if (id == null || id.isBlank())
        {
            return "New Entry";
        }

        String[] parts = id.replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();

        for (String part : parts)
        {
            if (part.isBlank())
            {
                continue;
            }

            if (!builder.isEmpty())
            {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1)
            {
                builder.append(part.substring(1));
            }
        }

        return builder.isEmpty() ? id : builder.toString();
    }
}
