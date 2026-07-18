package dev.scriptbound.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class UiElement
{
    public String id = "";
    public String type = "panel";
    public UiAnchor anchor = UiAnchor.TOP_LEFT;
    public int x;
    public int y;
    public int w = 40;
    public int h = 20;
    public boolean visible = true;

    public int color = 0xAA000000;
    public int border = 0;

    public String text = "";
    public float scale = 1F;
    public int textColor = 0xFFFFFFFF;
    public String align = "left";
    public boolean barText = false;

    public transient Double runtimeValue = null;
    public transient String runtimeText = null;

    public String image = "";

    public String onClick = "";

    public String bindScope = "";
    public String bindKey = "";
    public double max = 1D;

    public final List<UiElement> children = new ArrayList<>();

    public static UiElement fromJson(JsonObject root)
    {
        UiElement e = new UiElement();
        e.id = str(root, "id", "");
        e.type = str(root, "type", "panel").toLowerCase();
        e.anchor = UiAnchor.fromId(str(root, "anchor", "top_left"));
        e.x = intval(root, "x", 0);
        e.y = intval(root, "y", 0);
        e.w = intval(root, "w", 40);
        e.h = intval(root, "h", 20);
        e.visible = !root.has("visible") || root.get("visible").getAsBoolean();

        e.color = colorVal(root, "color", 0xAA000000);
        e.border = colorVal(root, "border", 0);
        e.text = str(root, "text", "");
        e.scale = (float) dbl(root, "scale", 1D);
        e.textColor = colorVal(root, "textColor", 0xFFFFFFFF);
        e.align = str(root, "align", "left").toLowerCase();
        e.barText = root.has("barText") && root.get("barText").getAsBoolean();
        e.image = str(root, "image", "");
        e.onClick = str(root, "onClick", "");
        e.bindScope = str(root, "bindScope", "");
        e.bindKey = str(root, "bindKey", "");
        e.max = dbl(root, "max", 1D);

        if (root.has("children") && root.get("children").isJsonArray())
        {
            for (var child : root.getAsJsonArray("children"))
            {
                if (child.isJsonObject())
                {
                    e.children.add(fromJson(child.getAsJsonObject()));
                }
            }
        }

        return e;
    }

    public JsonObject toJson()
    {
        JsonObject root = new JsonObject();
        root.addProperty("id", this.id);
        root.addProperty("type", this.type);
        root.addProperty("anchor", this.anchor.id());
        root.addProperty("x", this.x);
        root.addProperty("y", this.y);
        root.addProperty("w", this.w);
        root.addProperty("h", this.h);
        root.addProperty("visible", this.visible);
        root.addProperty("color", colorString(this.color));
        root.addProperty("border", colorString(this.border));
        root.addProperty("text", this.text);
        root.addProperty("scale", this.scale);
        root.addProperty("textColor", colorString(this.textColor));
        root.addProperty("align", this.align);
        root.addProperty("barText", this.barText);
        root.addProperty("image", this.image);
        root.addProperty("onClick", this.onClick);
        root.addProperty("bindScope", this.bindScope);
        root.addProperty("bindKey", this.bindKey);
        root.addProperty("max", this.max);

        if (!this.children.isEmpty())
        {
            JsonArray array = new JsonArray();

            for (UiElement child : this.children)
            {
                array.add(child.toJson());
            }

            root.add("children", array);
        }

        return root;
    }

    public int resolveX(int parentX, int parentW)
    {
        return this.anchor.resolveX(parentX, parentW, this.w, this.x);
    }

    public int resolveY(int parentY, int parentH)
    {
        return this.anchor.resolveY(parentY, parentH, this.h, this.y);
    }

    public static int parseColor(String value, int fallback)
    {
        if (value == null || value.isBlank())
        {
            return fallback;
        }

        String hex = value.startsWith("#") ? value.substring(1) : value;

        try
        {
            long parsed = Long.parseLong(hex, 16);

            if (hex.length() <= 6)
            {
                return (int) (0xFF000000L | parsed);
            }

            return (int) parsed;
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }

    public static String colorString(int argb)
    {
        return String.format("#%08X", argb);
    }

    private static int colorVal(JsonObject root, String key, int fallback)
    {
        if (!root.has(key))
        {
            return fallback;
        }

        if (root.get(key).getAsJsonPrimitive().isNumber())
        {
            return root.get(key).getAsInt();
        }

        return parseColor(root.get(key).getAsString(), fallback);
    }

    private static String str(JsonObject root, String key, String fallback)
    {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : fallback;
    }

    private static int intval(JsonObject root, String key, int fallback)
    {
        return root.has(key) && root.get(key).getAsJsonPrimitive().isNumber() ? root.get(key).getAsInt() : fallback;
    }

    private static double dbl(JsonObject root, String key, double fallback)
    {
        return root.has(key) && root.get(key).getAsJsonPrimitive().isNumber() ? root.get(key).getAsDouble() : fallback;
    }
}
