package dev.scriptbound.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.scriptbound.ui.UiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UiElementRenderer
{
    private UiElementRenderer() {}

    public static void render(GuiGraphics graphics, UiElement element, int parentX, int parentY, int parentW, int parentH)
    {
        if (!element.visible)
        {
            return;
        }

        int x = element.resolveX(parentX, parentW);
        int y = element.resolveY(parentY, parentH);
        int w = element.w;
        int h = element.h;

        switch (element.type)
        {
            case "label" -> drawLabel(graphics, element, x, y);
            case "image" -> drawImage(graphics, element, x, y, w, h);
            case "bar" -> drawBar(graphics, element, x, y, w, h);
            case "button" -> { drawPanel(graphics, element, x, y, w, h); drawCenteredText(graphics, element.text, element.textColor, x, y, w, h); }
            default -> drawPanel(graphics, element, x, y, w, h);
        }

        for (UiElement child : element.children)
        {
            render(graphics, child, x, y, w, h);
        }
    }

    private static void drawPanel(GuiGraphics graphics, UiElement e, int x, int y, int w, int h)
    {
        if ((e.color >>> 24) != 0)
        {
            graphics.fill(x, y, x + w, y + h, e.color);
        }

        if ((e.border >>> 24) != 0)
        {
            graphics.fill(x, y, x + w, y + 1, e.border);
            graphics.fill(x, y + h - 1, x + w, y + h, e.border);
            graphics.fill(x, y, x + 1, y + h, e.border);
            graphics.fill(x + w - 1, y, x + w, y + h, e.border);
        }
    }

    private static void drawLabel(GuiGraphics graphics, UiElement e, int x, int y)
    {
        Font font = Minecraft.getInstance().font;
        String text = ClientUiManager.resolveText(e);
        int tw = (int) (font.width(text) * e.scale);
        int ax = switch (e.align == null ? "left" : e.align)
        {
            case "center" -> x + (e.w - tw) / 2;
            case "right" -> x + e.w - tw;
            default -> x;
        };

        if (e.scale == 1F)
        {
            graphics.drawString(font, text, ax, y, e.textColor, false);
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(ax, y, 0);
        graphics.pose().scale(e.scale, e.scale, 1F);
        graphics.drawString(font, text, 0, 0, e.textColor, false);
        graphics.pose().popPose();
    }

    private static void drawCenteredText(GuiGraphics graphics, String text, int color, int x, int y, int w, int h)
    {
        Font font = Minecraft.getInstance().font;
        int tx = x + (w - font.width(text)) / 2;
        int ty = y + (h - font.lineHeight) / 2 + 1;
        graphics.drawString(font, text, tx, ty, color, false);
    }

    private static void drawBar(GuiGraphics graphics, UiElement e, int x, int y, int w, int h)
    {
        graphics.fill(x, y, x + w, y + h, 0xC0000000);
        double fraction = ClientUiManager.resolveBarFraction(e);
        int fill = (int) Math.round((w - 2) * Math.max(0D, Math.min(1D, fraction)));
        graphics.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, e.color);

        if ((e.border >>> 24) != 0)
        {
            graphics.fill(x, y, x + w, y + 1, e.border);
            graphics.fill(x, y + h - 1, x + w, y + h, e.border);
            graphics.fill(x, y, x + 1, y + h, e.border);
            graphics.fill(x + w - 1, y, x + w, y + h, e.border);
        }

        if (e.barText)
        {
            double value = ClientUiManager.resolveBarValue(e);
            String text = (Double.isNaN(value) ? "" : trim(value)) + " / " + trim(e.max);
            drawCenteredText(graphics, text, e.textColor, x, y - 1, w, h);
        }
    }

    private static String trim(double v)
    {
        return v == Math.rint(v) && !Double.isInfinite(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static void drawImage(GuiGraphics graphics, UiElement e, int x, int y, int w, int h)
    {
        ResourceLocation texture = ResourceLocation.tryParse(e.image);

        if (texture == null)
        {
            return;
        }

        RenderSystem.enableBlend();
        graphics.blit(texture, x, y, 0F, 0F, w, h, w, h);
        RenderSystem.disableBlend();
    }
}
