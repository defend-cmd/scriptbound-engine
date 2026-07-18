package dev.scriptbound.npc;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.entity.NpcEntity;
import dev.scriptbound.registry.ModEntities;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public final class NpcManager
{
    private static final Map<String, NpcBlueprint> CACHE = new HashMap<>();

    private NpcManager() {}

    public static NpcBlueprint get(MinecraftServer server, String id)
    {
        String key = normalize(id);

        if (CACHE.containsKey(key))
        {
            return CACHE.get(key);
        }

        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.npcFile(server, key));
        NpcBlueprint blueprint = NpcBlueprint.fromJson(key, root);
        CACHE.put(key, blueprint);
        return blueprint;
    }

    public static void invalidateAll()
    {
        CACHE.clear();
    }

    public static NpcEntity spawn(ServerLevel level, String blueprintId, double x, double y, double z, float yaw)
    {
        NpcBlueprint blueprint = get(level.getServer(), blueprintId);
        NpcEntity npc = ModEntities.NPC.get().create(level);

        if (npc == null)
        {
            return null;
        }

        npc.moveTo(x, y, z, yaw, 0F);
        npc.applyBlueprint(blueprint);
        level.addFreshEntity(npc);
        dev.scriptbound.compat.BBSCompat.applyForm(npc, blueprint.formJson());
        return npc;
    }

    public static NpcEntity spawnNearPlayer(ServerPlayer player, String blueprintId)
    {
        return spawn(
            player.serverLevel(),
            blueprintId,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot()
        );
    }

    private static String normalize(String id)
    {
        String trimmed = id.trim();

        if (trimmed.endsWith(".json"))
        {
            return trimmed.substring(0, trimmed.length() - 5);
        }

        return trimmed;
    }
}
