package dev.scriptbound.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UiScale
{
    private UiScale() {}

    public static int guiScale()
    {
        return (int) Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static float factor()
    {
        int gs = guiScale();

        if (gs >= 4)
        {
            return 0.68F;
        }

        if (gs >= 3)
        {
            return 0.82F;
        }

        return 1.0F;
    }

    public static int px(int value)
    {
        return Math.max(1, Math.round(value * factor()));
    }

    public static int pad()
    {
        return px(UiTheme.PAD);
    }

    public static int headerH()
    {
        return px(UiTheme.HEADER_H);
    }

    public static int sidebarW()
    {
        return px(UiTheme.SIDEBAR_W);
    }

    public static int panelHeaderH()
    {
        return px(UiTheme.PANEL_HEADER_H);
    }

    public static int rowH()
    {
        return px(UiTheme.ROW_H);
    }

    public static int nodeW()
    {
        return px(104);
    }

    public static int nodeH()
    {
        return px(34);
    }

    public static int portR()
    {
        return Math.max(3, px(4));
    }
}
