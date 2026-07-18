package dev.scriptbound.client.bbs;

import dev.scriptbound.dashboard.DashboardSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public final class BBSUiBridge
{
    public record IconUv(int u, int v, int size)
    {
    }

    private BBSUiBridge() {}

    public static boolean isAvailable()
    {
        try
        {
            Class.forName("mchorse.bbs_mod.ui.utils.icons.Icons");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    private static final java.util.Map<String, IconUv> FALLBACK = java.util.Map.ofEntries(
        java.util.Map.entry("GEAR", new IconUv(0, 0, 16)),
        java.util.Map.entry("SAVED", new IconUv(32, 0, 16)),
        java.util.Map.entry("SAVE", new IconUv(48, 0, 16)),
        java.util.Map.entry("ADD", new IconUv(64, 0, 16)),
        java.util.Map.entry("REMOVE", new IconUv(96, 0, 16)),
        java.util.Map.entry("POSE", new IconUv(112, 0, 16)),
        java.util.Map.entry("FILTER", new IconUv(128, 0, 16)),
        java.util.Map.entry("SEARCH", new IconUv(160, 48, 16)),
        java.util.Map.entry("REFRESH", new IconUv(240, 0, 16)),
        java.util.Map.entry("SERVER", new IconUv(32, 16, 16)),
        java.util.Map.entry("EDIT", new IconUv(80, 16, 16)),
        java.util.Map.entry("CLOSE", new IconUv(112, 16, 16)),
        java.util.Map.entry("CODE", new IconUv(144, 16, 16)),
        java.util.Map.entry("BLOCK", new IconUv(240, 16, 16)),
        java.util.Map.entry("FAVORITE", new IconUv(0, 32, 16)),
        java.util.Map.entry("PLAY", new IconUv(48, 32, 16)),
        java.util.Map.entry("PLAYER", new IconUv(224, 64, 16)),
        java.util.Map.entry("EDITOR", new IconUv(208, 80, 16)),
        java.util.Map.entry("UP", new IconUv(128, 0, 16)),
        java.util.Map.entry("DOWN", new IconUv(144, 0, 16)),
        java.util.Map.entry("LEFT", new IconUv(80, 0, 16)),
        java.util.Map.entry("RIGHT", new IconUv(112, 0, 16))
    );

    public static IconUv icon(String fieldName)
    {
        if (!isAvailable())
        {
            return FALLBACK.getOrDefault(fieldName, new IconUv(0, 0, 16));
        }

        try
        {
            Class<?> iconsClass = Class.forName("mchorse.bbs_mod.ui.utils.icons.Icons");
            Field field = iconsClass.getField(fieldName);
            Object icon = field.get(null);
            Class<?> iconClass = Class.forName("mchorse.bbs_mod.ui.utils.icons.Icon");
            int x = iconClass.getField("x").getInt(icon);
            int y = iconClass.getField("y").getInt(icon);
            int w = iconClass.getField("w").getInt(icon);
            return new IconUv(x, y, w > 0 ? w : 16);
        }
        catch (ReflectiveOperationException e)
        {
            return FALLBACK.getOrDefault(fieldName, new IconUv(0, 0, 16));
        }
    }

    public static void blitIcon(GuiGraphics graphics, int x, int y, IconUv icon, int tint, boolean selected)
    {
        int size = icon.size();
        float r = ((tint >> 16) & 0xFF) / 255F;
        float g = ((tint >> 8) & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        float a = selected ? 1F : 0.85F;
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
        graphics.blit(BBSUiAssets.ICONS, x, y, icon.u(), icon.v(), size, size, 256, 256);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
}
