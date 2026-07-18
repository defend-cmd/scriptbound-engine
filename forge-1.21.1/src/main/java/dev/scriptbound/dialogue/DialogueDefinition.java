package dev.scriptbound.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DialogueDefinition
{
    public record Choice(String text, String trigger) {}

    public record Line(String speaker, String text, List<Choice> choices) {}

    private final String id;
    private final List<Line> lines;

    public DialogueDefinition(String id, List<Line> lines)
    {
        this.id = id;
        this.lines = List.copyOf(lines);
    }

    public String id()
    {
        return this.id;
    }

    public List<Line> lines()
    {
        return this.lines;
    }

    public static DialogueDefinition fromJson(String id, JsonObject root)
    {
        List<Line> lines = new ArrayList<>();

        if (!root.has("lines") || !root.get("lines").isJsonArray())
        {
            return new DialogueDefinition(id, lines);
        }

        JsonArray array = root.getAsJsonArray("lines");

        for (int i = 0; i < array.size(); i++)
        {
            if (!array.get(i).isJsonObject())
            {
                continue;
            }

            JsonObject line = array.get(i).getAsJsonObject();
            String speaker = line.has("speaker") ? line.get("speaker").getAsString() : "";
            String text = line.has("text") ? line.get("text").getAsString() : "";
            List<Choice> choices = new ArrayList<>();

            if (line.has("choices") && line.get("choices").isJsonArray())
            {
                JsonArray choiceArray = line.getAsJsonArray("choices");

                for (int j = 0; j < choiceArray.size(); j++)
                {
                    if (!choiceArray.get(j).isJsonObject())
                    {
                        continue;
                    }

                    JsonObject choice = choiceArray.get(j).getAsJsonObject();
                    choices.add(new Choice(
                        choice.has("text") ? choice.get("text").getAsString() : "...",
                        choice.has("trigger") ? choice.get("trigger").getAsString() : ""
                    ));
                }
            }

            lines.add(new Line(speaker, text, Collections.unmodifiableList(choices)));
        }

        return new DialogueDefinition(id, lines);
    }
}
