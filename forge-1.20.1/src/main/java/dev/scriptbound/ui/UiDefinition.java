package dev.scriptbound.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class UiDefinition
{
    public enum Mode { HUD, SCREEN }

    public String id = "";
    public Mode mode = Mode.HUD;
    public boolean pauseGame = false;
    public final List<UiElement> elements = new ArrayList<>();

    public static UiDefinition fromJson(String id, JsonObject root)
    {
        UiDefinition def = new UiDefinition();
        def.id = id;
        def.mode = "screen".equalsIgnoreCase(string(root, "mode")) ? Mode.SCREEN : Mode.HUD;
        def.pauseGame = root.has("pauseGame") && root.get("pauseGame").getAsBoolean();

        if (root.has("elements") && root.get("elements").isJsonArray())
        {
            for (var element : root.getAsJsonArray("elements"))
            {
                if (element.isJsonObject())
                {
                    def.elements.add(UiElement.fromJson(element.getAsJsonObject()));
                }
            }
        }

        return def;
    }

    public static UiDefinition fromJsonString(String id, String json)
    {
        return fromJson(id, JsonParser.parseString(json).getAsJsonObject());
    }

    public JsonObject toJson()
    {
        JsonObject root = new JsonObject();
        root.addProperty("id", this.id);
        root.addProperty("mode", this.mode == Mode.SCREEN ? "screen" : "hud");
        root.addProperty("pauseGame", this.pauseGame);

        JsonArray array = new JsonArray();

        for (UiElement element : this.elements)
        {
            array.add(element.toJson());
        }

        root.add("elements", array);
        return root;
    }

    public UiElement findElement(String elementId)
    {
        if (elementId == null || elementId.isBlank())
        {
            return null;
        }

        for (UiElement element : this.elements)
        {
            UiElement found = find(element, elementId);

            if (found != null)
            {
                return found;
            }
        }

        return null;
    }

    private static UiElement find(UiElement element, String id)
    {
        if (id.equals(element.id))
        {
            return element;
        }

        for (UiElement child : element.children)
        {
            UiElement found = find(child, id);

            if (found != null)
            {
                return found;
            }
        }

        return null;
    }

    private static String string(JsonObject root, String key)
    {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : "";
    }
}
