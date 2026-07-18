package dev.scriptbound.client.ui;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ui.UiDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientUiManager
{
    private static final Map<String, UiDefinition> ACTIVE_HUDS = new LinkedHashMap<>();
    private static final Map<String, String> STATE_VALUES = new java.util.concurrent.ConcurrentHashMap<>();

    private ClientUiManager() {}

    public static void updateStates(Map<String, String> values)
    {
        STATE_VALUES.putAll(values);
    }

    public static void open(String id, String json)
    {
        try
        {
            UiDefinition def = UiDefinition.fromJsonString(id, json);

            if (def.mode == UiDefinition.Mode.SCREEN)
            {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                    new dev.scriptbound.client.screen.CustomUiScreen(id, def)
                );
            }
            else
            {
                ACTIVE_HUDS.put(id, def);
            }
        }
        catch (Exception e)
        {
            ScriptBoundMod.LOGGER.error("Failed to open UI '{}'", id, e);
        }
    }

    public static void close(String id)
    {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();

        if (minecraft.screen instanceof dev.scriptbound.client.screen.CustomUiScreen screen
            && ("*".equals(id) || screen.uiId().equals(id)))
        {
            minecraft.setScreen(null);
        }

        if ("*".equals(id))
        {
            ACTIVE_HUDS.clear();
            return;
        }

        ACTIVE_HUDS.remove(id);
    }

    public static void clear()
    {
        ACTIVE_HUDS.clear();
    }

    public static Iterable<UiDefinition> activeHuds()
    {
        return ACTIVE_HUDS.values();
    }

    public static String resolveText(dev.scriptbound.ui.UiElement element)
    {
        String base = element.runtimeText != null ? element.runtimeText : element.text;
        return dev.scriptbound.ui.UiBindings.apply(base, k -> STATE_VALUES.getOrDefault(k, ""));
    }

    public static double resolveBarValue(dev.scriptbound.ui.UiElement element)
    {
        if (element.runtimeValue != null)
        {
            return element.runtimeValue;
        }

        if (element.bindKey == null || element.bindKey.isBlank())
        {
            return Double.NaN;
        }

        String raw = STATE_VALUES.get(dev.scriptbound.ui.UiBindings.key(element.bindScope, element.bindKey));

        if (raw == null || raw.isBlank())
        {
            return 0D;
        }

        try
        {
            return Double.parseDouble(raw);
        }
        catch (NumberFormatException e)
        {
            return 0D;
        }
    }

    public static double resolveBarFraction(dev.scriptbound.ui.UiElement element)
    {
        double value = resolveBarValue(element);

        if (Double.isNaN(value))
        {
            return 1D;
        }

        return element.max > 0 ? value / element.max : 0D;
    }

    public static void applySet(String uiId, String elementId, String field, String value)
    {
        dev.scriptbound.ui.UiElement element = findActive(uiId, elementId);

        if (element == null)
        {
            return;
        }

        switch (field == null ? "" : field.toLowerCase())
        {
            case "text" -> element.runtimeText = value;
            case "visible" -> element.visible = "true".equalsIgnoreCase(value) || "1".equals(value);
            case "value" -> {
                try { element.runtimeValue = Double.parseDouble(value); } catch (NumberFormatException ignored) {}
            }
            default -> {}
        }
    }

    private static dev.scriptbound.ui.UiElement findActive(String uiId, String elementId)
    {
        UiDefinition def = ACTIVE_HUDS.get(uiId);

        if (def == null
            && net.minecraft.client.Minecraft.getInstance().screen instanceof dev.scriptbound.client.screen.CustomUiScreen screen
            && screen.uiId().equals(uiId))
        {
            def = screen.definition();
        }

        return def == null ? null : def.findElement(elementId);
    }
}
