package dev.scriptbound.data;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ServerSettings
{
    private static final Map<String, String> GLOBAL_TRIGGERS = new HashMap<>();

    private ServerSettings() {}

    public static void load(MinecraftServer server)
    {
        GLOBAL_TRIGGERS.clear();
        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.settingsFile(server));

        if (!root.has("global_triggers") || !root.get("global_triggers").isJsonObject())
        {
            return;
        }

        JsonObject triggers = root.getAsJsonObject("global_triggers");

        for (String key : triggers.keySet())
        {
            if (triggers.get(key).isJsonPrimitive())
            {
                GLOBAL_TRIGGERS.put(key, triggers.get(key).getAsString());
            }
        }
    }

    public static String globalTrigger(String eventId)
    {
        return GLOBAL_TRIGGERS.get(eventId);
    }

    public static Map<String, String> globalTriggers()
    {
        return Collections.unmodifiableMap(GLOBAL_TRIGGERS);
    }
}
