package dev.scriptbound.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record NpcBlueprint(
    String id,
    String displayName,
    double health,
    boolean invulnerable,
    String onInteract,
    String onDeath,
    String dialogue,
    String formJson,
    String faction,
    String onHostileInteract,
    double speed,
    List<BlockPos> patrol
)
{
    public static NpcBlueprint fromJson(String id, JsonObject root)
    {
        List<BlockPos> patrol = new ArrayList<>();

        if (root.has("patrol") && root.get("patrol").isJsonArray())
        {
            for (var element : root.getAsJsonArray("patrol"))
            {
                if (!element.isJsonArray())
                {
                    continue;
                }

                JsonArray point = element.getAsJsonArray();

                if (point.size() >= 3)
                {
                    patrol.add(new BlockPos(point.get(0).getAsInt(), point.get(1).getAsInt(), point.get(2).getAsInt()));
                }
            }
        }

        return new NpcBlueprint(
            id,
            root.has("displayName") ? root.get("displayName").getAsString() : id,
            root.has("health") ? root.get("health").getAsDouble() : 20D,
            !root.has("invulnerable") || root.get("invulnerable").getAsBoolean(),
            root.has("onInteract") ? root.get("onInteract").getAsString() : "",
            root.has("onDeath") ? root.get("onDeath").getAsString() : "",
            root.has("dialogue") ? root.get("dialogue").getAsString() : "",
            root.has("form") ? root.get("form").toString() : "",
            root.has("faction") ? root.get("faction").getAsString() : "",
            root.has("onHostileInteract") ? root.get("onHostileInteract").getAsString() : "",
            root.has("speed") ? root.get("speed").getAsDouble() : 0D,
            List.copyOf(patrol)
        );
    }
}
