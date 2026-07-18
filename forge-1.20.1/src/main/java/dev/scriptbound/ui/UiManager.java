package dev.scriptbound.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class UiManager
{
    private static final java.util.Map<String, JsonObject> CACHE = new java.util.HashMap<>();
    private static MinecraftServer server;

    private UiManager() {}

    public static void onServerStarted(MinecraftServer minecraftServer)
    {
        server = minecraftServer;
        CACHE.clear();
        ensureDemo();
    }

    public static void onServerStopping()
    {
        CACHE.clear();
        server = null;
    }

    public static void invalidate(String id)
    {
        if (id != null)
        {
            CACHE.remove(normalize(id));
        }
    }

    public static void save(String id, JsonObject root)
    {
        if (server == null || id == null || id.isBlank())
        {
            return;
        }

        String normalized = normalize(id);
        JsonFiles.writeObject(ScriptBoundPaths.uiFile(server, normalized), root);
        CACHE.put(normalized, root);
    }

    public static boolean delete(String id)
    {
        if (server == null || id == null || id.isBlank())
        {
            return false;
        }

        String normalized = normalize(id);
        CACHE.remove(normalized);

        try
        {
            return Files.deleteIfExists(ScriptBoundPaths.uiFile(server, normalized));
        }
        catch (Exception e)
        {
            ScriptBoundMod.LOGGER.error("Failed to delete UI '{}'", normalized, e);
            return false;
        }
    }

    public static JsonObject getJson(String id)
    {
        if (server == null || id == null || id.isBlank())
        {
            return null;
        }

        String normalized = normalize(id);

        if (CACHE.containsKey(normalized))
        {
            return CACHE.get(normalized);
        }

        Path file = ScriptBoundPaths.uiFile(server, normalized);

        if (!Files.isRegularFile(file))
        {
            return null;
        }

        JsonObject root = JsonFiles.readObject(file);
        CACHE.put(normalized, root);
        return root;
    }

    public static UiDefinition get(String id)
    {
        JsonObject root = getJson(id);
        return root == null ? null : UiDefinition.fromJson(normalize(id), root);
    }

    public static List<String> list()
    {
        List<String> ids = new ArrayList<>();

        if (server == null)
        {
            return ids;
        }

        Path dir = ScriptBoundPaths.uiDir(server);

        if (!Files.isDirectory(dir))
        {
            return ids;
        }

        try (var stream = Files.list(dir))
        {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String name = p.getFileName().toString();
                ids.add(name.substring(0, name.length() - 5));
            });
        }
        catch (Exception e)
        {
            ScriptBoundMod.LOGGER.error("Failed to list UI documents", e);
        }

        return ids;
    }

    private static String normalize(String id)
    {
        String trimmed = id.trim();
        return trimmed.endsWith(".json") ? trimmed.substring(0, trimmed.length() - 5) : trimmed;
    }

    private static void ensureDemo()
    {
        if (server == null)
        {
            return;
        }

        Path file = ScriptBoundPaths.uiFile(server, "demo");

        if (Files.exists(file))
        {
            return;
        }

        JsonObject panel = new JsonObject();
        panel.addProperty("id", "panel");
        panel.addProperty("type", "panel");
        panel.addProperty("anchor", "top_left");
        panel.addProperty("x", 8);
        panel.addProperty("y", 8);
        panel.addProperty("w", 130);
        panel.addProperty("h", 46);
        panel.addProperty("color", "#B0101018");
        panel.addProperty("border", "#FF3A6EA5");

        JsonObject title = new JsonObject();
        title.addProperty("id", "title");
        title.addProperty("type", "label");
        title.addProperty("anchor", "top_left");
        title.addProperty("x", 8);
        title.addProperty("y", 7);
        title.addProperty("text", "ScriptBound HUD");
        title.addProperty("textColor", "#FF6EC1FF");

        JsonObject sub = new JsonObject();
        sub.addProperty("id", "sub");
        sub.addProperty("type", "label");
        sub.addProperty("anchor", "top_left");
        sub.addProperty("x", 8);
        sub.addProperty("y", 22);
        sub.addProperty("text", "Coins: %player.coins%");
        sub.addProperty("textColor", "#FFFFFFFF");

        JsonObject bar = new JsonObject();
        bar.addProperty("id", "hp");
        bar.addProperty("type", "bar");
        bar.addProperty("anchor", "top_left");
        bar.addProperty("x", 8);
        bar.addProperty("y", 34);
        bar.addProperty("w", 114);
        bar.addProperty("h", 6);
        bar.addProperty("color", "#FF33CC55");
        bar.addProperty("border", "#FF000000");
        bar.addProperty("bindScope", "player");
        bar.addProperty("bindKey", "coins");
        bar.addProperty("max", 100);

        JsonArray children = new JsonArray();
        children.add(title);
        children.add(sub);
        children.add(bar);
        panel.add("children", children);

        JsonArray elements = new JsonArray();
        elements.add(panel);

        JsonObject root = new JsonObject();
        root.addProperty("mode", "hud");
        root.add("elements", elements);

        JsonFiles.writeObject(file, root);
        ScriptBoundMod.LOGGER.info("Created demo UI: scriptbound/ui/demo.json");
    }
}
