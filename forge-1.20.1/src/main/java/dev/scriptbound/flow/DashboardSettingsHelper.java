package dev.scriptbound.flow;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.data.ServerSettings;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;

final class DashboardSettingsHelper
{
    private DashboardSettingsHelper() {}

    static void setGlobalTrigger(MinecraftServer server, String eventId, String triggerId) throws IOException
    {
        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.settingsFile(server));
        JsonObject global = root.has("global_triggers") && root.get("global_triggers").isJsonObject()
            ? root.getAsJsonObject("global_triggers")
            : new JsonObject();
        global.addProperty(eventId, triggerId);
        root.add("global_triggers", global);
        JsonFiles.writeObject(ScriptBoundPaths.settingsFile(server), root);
        ServerSettings.load(server);
    }
}
