package dev.scriptbound.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Files;

public final class UiConfig
{
    public static String theme = "dark";
    public static int panelAlpha = 255;
    public static int roundedStyle = 0;

    public static boolean showGrid = true;
    public static boolean compactMode = false;
    public static boolean roundedCorners = false;
    public static boolean visualEffects = true;

    static
    {
        load();
    }

    private static File getFile()
    {
        return new File(Minecraft.getInstance().gameDirectory, "config/scriptbound-client-ui.json");
    }

    public static void load()
    {
        try
        {
            File file = getFile();
            if (file.exists())
            {
                String json = Files.readString(file.toPath());
                JsonObject obj = new Gson().fromJson(json, JsonObject.class);
                if (obj.has("showGrid")) showGrid = obj.get("showGrid").getAsBoolean();
                if (obj.has("compactMode")) compactMode = obj.get("compactMode").getAsBoolean();
                if (obj.has("roundedCorners")) roundedCorners = obj.get("roundedCorners").getAsBoolean();
                if (obj.has("visualEffects")) visualEffects = obj.get("visualEffects").getAsBoolean();
                if (obj.has("theme")) theme = obj.get("theme").getAsString();
                if (obj.has("panelAlpha")) panelAlpha = obj.get("panelAlpha").getAsInt();
                if (obj.has("roundedStyle")) roundedStyle = obj.get("roundedStyle").getAsInt();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        UiTheme.applyTheme(theme, panelAlpha);
    }

    public static void save()
    {
        try
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("showGrid", showGrid);
            obj.addProperty("compactMode", compactMode);
            obj.addProperty("roundedCorners", roundedCorners);
            obj.addProperty("visualEffects", visualEffects);
            obj.addProperty("theme", theme);
            obj.addProperty("panelAlpha", panelAlpha);
            obj.addProperty("roundedStyle", roundedStyle);

            File file = getFile();
            file.getParentFile().mkdirs();
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(obj);
            Files.writeString(file.toPath(), json);

            UiTheme.applyTheme(theme, panelAlpha);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
