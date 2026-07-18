package dev.scriptbound.trigger;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class TriggerManager
{
    private static final Map<String, TriggerChain> CACHE = new HashMap<>();
    private static MinecraftServer server;

    private TriggerManager() {}

    public static void onServerStarted(MinecraftServer minecraftServer)
    {
        server = minecraftServer;
        CACHE.clear();
    }

    public static void onServerStopping()
    {
        CACHE.clear();
        server = null;
    }

    public static TriggerChain get(String id)
    {
        if (server == null || id == null || id.isBlank())
        {
            return TriggerChain.empty();
        }

        String normalized = normalizeId(id);

        if (CACHE.containsKey(normalized))
        {
            return CACHE.get(normalized);
        }

        Path file = ScriptBoundPaths.triggerFile(server, normalized);

        if (!Files.isRegularFile(file))
        {
            ScriptBoundMod.LOGGER.warn("Trigger not found: {}", normalized);
            CACHE.put(normalized, TriggerChain.empty());
            return TriggerChain.empty();
        }

        TriggerChain chain = TriggerChain.fromJson(JsonFiles.readObject(file));
        CACHE.put(normalized, chain);
        return chain;
    }

    public static void invalidate(String id)
    {
        if (id != null)
        {
            CACHE.remove(normalizeId(id));
        }
    }

    public static void invalidateAll()
    {
        CACHE.clear();
    }

    private static String normalizeId(String id)
    {
        String trimmed = id.trim();

        if (trimmed.endsWith(".json"))
        {
            return trimmed.substring(0, trimmed.length() - 5);
        }

        if (trimmed.startsWith("triggers/"))
        {
            return trimmed.substring("triggers/".length());
        }

        return trimmed;
    }
}
