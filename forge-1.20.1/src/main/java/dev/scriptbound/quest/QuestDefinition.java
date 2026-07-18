package dev.scriptbound.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record QuestDefinition(String id, String title, String description, List<Objective> objectives, String rewardTrigger)
{
    public record Objective(String type, String key, double value, String entity, int count, String npcId) {}

    public static QuestDefinition fromJson(String id, JsonObject root)
    {
        List<Objective> objectives = new ArrayList<>();

        if (root.has("objectives") && root.get("objectives").isJsonArray())
        {
            JsonArray array = root.getAsJsonArray("objectives");

            for (int i = 0; i < array.size(); i++)
            {
                if (!array.get(i).isJsonObject())
                {
                    continue;
                }

                JsonObject obj = array.get(i).getAsJsonObject();
                objectives.add(new Objective(
                    obj.has("type") ? obj.get("type").getAsString() : "state",
                    obj.has("key") ? obj.get("key").getAsString() : "",
                    obj.has("value") ? obj.get("value").getAsDouble() : 1D,
                    obj.has("entity") ? obj.get("entity").getAsString() : "",
                    obj.has("count") ? obj.get("count").getAsInt() : 1,
                    obj.has("npc") ? obj.get("npc").getAsString() : ""
                ));
            }
        }

        return new QuestDefinition(
            id,
            root.has("title") ? root.get("title").getAsString() : id,
            root.has("description") ? root.get("description").getAsString() : "",
            List.copyOf(objectives),
            root.has("reward_trigger") ? root.get("reward_trigger").getAsString() : ""
        );
    }
}
