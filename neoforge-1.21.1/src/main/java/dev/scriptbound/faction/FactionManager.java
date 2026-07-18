package dev.scriptbound.faction;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public final class FactionManager
{
    public record Faction(String id, String title, double defaultScore, double friendlyAt, double hostileAt)
    {
        public static Faction fromJson(String id, JsonObject root)
        {
            return new Faction(
                id,
                root.has("title") ? root.get("title").getAsString() : id,
                root.has("defaultScore") ? root.get("defaultScore").getAsDouble() : 0D,
                root.has("friendly") ? root.get("friendly").getAsDouble() : 20D,
                root.has("hostile") ? root.get("hostile").getAsDouble() : -20D
            );
        }
    }

    public enum Attitude
    {
        FRIENDLY,
        NEUTRAL,
        HOSTILE
    }

    private static final Map<String, Faction> CACHE = new HashMap<>();

    private FactionManager() {}

    public static Faction get(MinecraftServer server, String id)
    {
        String key = id.trim();

        if (CACHE.containsKey(key))
        {
            return CACHE.get(key);
        }

        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.factionFile(server, key));
        Faction faction = Faction.fromJson(key, root);

        if (!Files.isRegularFile(ScriptBoundPaths.factionFile(server, key)))
        {
            JsonObject template = new JsonObject();
            template.addProperty("title", faction.title());
            template.addProperty("defaultScore", faction.defaultScore());
            template.addProperty("friendly", faction.friendlyAt());
            template.addProperty("hostile", faction.hostileAt());
            JsonFiles.writeObject(ScriptBoundPaths.factionFile(server, key), template);
        }

        CACHE.put(key, faction);
        return faction;
    }

    public static void invalidateAll()
    {
        CACHE.clear();
    }

    public static double score(ServerPlayer player, String factionId)
    {
        StateValue value = StateService.playerTarget(player).store().get("faction." + factionId);

        if (value != null && value.isNumber())
        {
            return value.asNumber();
        }

        return get(player.server, factionId).defaultScore();
    }

    public static void setScore(ServerPlayer player, String factionId, double score)
    {
        StateService.set(StateService.playerTarget(player), "faction." + factionId, new StateValue.NumberValue(score));
    }

    public static void addScore(ServerPlayer player, String factionId, double delta)
    {
        setScore(player, factionId, score(player, factionId) + delta);
    }

    public static Attitude attitude(ServerPlayer player, String factionId)
    {
        if (factionId == null || factionId.isBlank())
        {
            return Attitude.NEUTRAL;
        }

        Faction faction = get(player.server, factionId);
        double score = score(player, factionId);

        if (score >= faction.friendlyAt())
        {
            return Attitude.FRIENDLY;
        }

        if (score <= faction.hostileAt())
        {
            return Attitude.HOSTILE;
        }

        return Attitude.NEUTRAL;
    }
}
