package dev.scriptbound.client.ui;

import dev.scriptbound.client.bbs.BBSUiBridge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class UiDraw
{
    private UiDraw() {}

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h)
    {
        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        if (r > 0) fillRounded(graphics, x + 3, y + 3, w, h, 0x33000000, r);
        fillRounded(graphics, x, y, w, h, UiTheme.PANEL, r);
        outlineRounded(graphics, x, y, w, h, UiTheme.BORDER, r);
    }

    public static void panelHeader(GuiGraphics graphics, Font font, int x, int y, int w, String title)
    {
        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        fillRoundedTop(graphics, x, y, w, UiTheme.PANEL_HEADER_H, UiTheme.PANEL_HEADER, r);
        graphics.fill(x, y + UiTheme.PANEL_HEADER_H - 1, x + w, y + UiTheme.PANEL_HEADER_H, UiTheme.BORDER);
        graphics.drawString(font, title, x + 8, y + 7, UiTheme.TEXT_ACCENT);
    }

    public static void inset(GuiGraphics graphics, int x, int y, int w, int h)
    {
        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        fillRounded(graphics, x, y, w, h, UiTheme.PANEL_INSET, r);
        outlineRounded(graphics, x, y, w, h, UiTheme.BORDER, r);
        graphics.fill(x + r, y, x + w - r, y + 2, 0x33000000);
    }

    public static void button(GuiGraphics graphics, Font font, UiRect rect, String label, String icon, boolean hovered, boolean active, boolean enabled)
    {
        int bg = active ? UiTheme.BTN_ACTIVE : (hovered ? UiTheme.BTN_HOVER : UiTheme.BTN);
        int text = enabled ? (active ? 0xFFFFFFFF : UiTheme.TEXT) : UiTheme.TEXT_DIM;

        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        fillRounded(graphics, rect.x(), rect.y(), rect.w(), rect.h(), bg, r);

        if (hovered || active) {
            outlineRounded(graphics, rect.x(), rect.y(), rect.w(), rect.h(), active ? UiTheme.ACCENT : UiTheme.BORDER_LIGHT, r);
        } else {
            outlineRounded(graphics, rect.x(), rect.y(), rect.w(), rect.h(), UiTheme.BORDER, r);
        }

        int tx = rect.x() + 6;
        int maxW = rect.w() - 12;

        if (icon != null && !icon.isBlank())
        {
            BBSUiBridge.IconUv uv = BBSUiBridge.icon(icon);
            BBSUiBridge.blitIcon(graphics, rect.x() + 4, rect.y() + (rect.h() - 12) / 2, uv, 0xFFFFFF, active);
            tx = rect.x() + 20;
            maxW = rect.w() - 24;
        }

        String visibleLabel = font.plainSubstrByWidth(label, maxW);
        if (visibleLabel.length() < label.length()) {
            visibleLabel = font.plainSubstrByWidth(label, Math.max(0, maxW - font.width("..."))) + "...";
        }

        int textW = font.width(visibleLabel);
        if (icon == null || icon.isBlank()) {
            tx = rect.x() + (rect.w() - textW) / 2;
        }

        graphics.drawString(font, visibleLabel, tx, rect.y() + (rect.h() - 8) / 2, text);
    }

    public static void iconButton(GuiGraphics graphics, UiRect rect, String icon, boolean hovered, boolean selected)
    {
        if (selected)
        {

            graphics.fill(rect.x(), rect.y(), rect.x() + rect.w(), rect.y() + rect.h(), UiTheme.ACCENT);
        }
        else if (hovered)
        {
            graphics.fill(rect.x(), rect.y(), rect.x() + rect.w(), rect.y() + rect.h(), 0x33FFFFFF);
        }

        BBSUiBridge.IconUv uv = BBSUiBridge.icon(icon);
        int ix = rect.x() + (rect.w() - uv.size()) / 2;
        int iy = rect.y() + (rect.h() - uv.size()) / 2;
        int color = selected ? 0xFFFFFFFF : 0xBBBBBB;
        BBSUiBridge.blitIcon(graphics, ix, iy, uv, color, false);
    }

    public static void fillRoundedTop(GuiGraphics graphics, int x, int y, int w, int h, int color, int r)
    {
        if (r <= 1) {
            if (r == 1) {
                graphics.fill(x, y + r, x + w, y + h, color);
                graphics.fill(x + r, y, x + w - r, y + r, color);
            } else {
                graphics.fill(x, y, x + w, y + h, color);
            }
            return;
        }

        org.joml.Matrix4f matrix = graphics.pose().last().pose();
        float a = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_FAN, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);

        bufferbuilder.vertex(matrix, x + w / 2.0f, y + h, 0.0f).color(red, green, blue, a).endVertex();

        drawCorner(bufferbuilder, matrix, x + r, y + r, r, 180, 270, red, green, blue, a);
        drawCorner(bufferbuilder, matrix, x + w - r, y + r, r, 270, 360, red, green, blue, a);
        bufferbuilder.vertex(matrix, x + w, y + h, 0.0f).color(red, green, blue, a).endVertex();
        bufferbuilder.vertex(matrix, x, y + h, 0.0f).color(red, green, blue, a).endVertex();

        tesselator.end();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    public static void fillRounded(GuiGraphics graphics, int x, int y, int w, int h, int color, int r)
    {
        if (r <= 1) {
            if (r == 1) {
                graphics.fill(x, y + r, x + w, y + h - r, color);
                graphics.fill(x + r, y, x + w - r, y + r, color);
                graphics.fill(x + r, y + h - r, x + w - r, y + h, color);
            } else {
                graphics.fill(x, y, x + w, y + h, color);
            }
            return;
        }

        org.joml.Matrix4f matrix = graphics.pose().last().pose();
        float a = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_FAN, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);

        bufferbuilder.vertex(matrix, x + w / 2.0f, y + h / 2.0f, 0.0f).color(red, green, blue, a).endVertex();

        drawCorner(bufferbuilder, matrix, x + r, y + r, r, 180, 270, red, green, blue, a);
        drawCorner(bufferbuilder, matrix, x + w - r, y + r, r, 270, 360, red, green, blue, a);
        drawCorner(bufferbuilder, matrix, x + w - r, y + h - r, r, 0, 90, red, green, blue, a);
        drawCorner(bufferbuilder, matrix, x + r, y + h - r, r, 90, 180, red, green, blue, a);
        bufferbuilder.vertex(matrix, x, y + r, 0.0f).color(red, green, blue, a).endVertex();

        tesselator.end();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    public static void outlineRounded(GuiGraphics graphics, int x, int y, int w, int h, int color, int r)
    {
        if (r <= 1) {
            graphics.fill(x + r, y, x + w - r, y + 1, color);
            graphics.fill(x + r, y + h - 1, x + w - r, y + h, color);
            graphics.fill(x, y + r, x + 1, y + h - r, color);
            graphics.fill(x + w - 1, y + r, x + w, y + h - r, color);
            return;
        }

        org.joml.Matrix4f matrix = graphics.pose().last().pose();
        float a = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.DEBUG_LINE_STRIP, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);

        drawCorner(bufferbuilder, matrix, x + r, y + r, r, 180, 270, red, green, blue, a);
        drawCorner(bufferbuilder, matrix, x + w - r, y + r, r, 270, 360, red, green, blue, a);
        drawCorner(bufferbuilder, matrix, x + w - r, y + h - r, r, 0, 90, red, green, blue, a);
        drawCorner(bufferbuilder, matrix, x + r, y + h - r, r, 90, 180, red, green, blue, a);
        bufferbuilder.vertex(matrix, x, y + r, 0.0f).color(red, green, blue, a).endVertex();

        tesselator.end();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static void drawCorner(com.mojang.blaze3d.vertex.BufferBuilder builder, org.joml.Matrix4f matrix, float cx, float cy, float radius, int startAngle, int endAngle, float r, float g, float b, float a) {
        for (int i = startAngle; i <= endAngle; i += 5) {
            float rad = (float) Math.toRadians(i);
            builder.vertex(matrix, cx + (float) Math.cos(rad) * radius, cy + (float) Math.sin(rad) * radius, 0.0f).color(r, g, b, a).endVertex();
        }
    }

    public static void scrollbar(GuiGraphics graphics, int x, int y, int h, int total, int visible, int scroll)
    {
        if (total <= visible)
        {
            return;
        }

        int trackH = h;
        int thumbH = Math.max(16, trackH * visible / total);
        int maxScroll = total - visible;
        int thumbY = y + (trackH - thumbH) * scroll / Math.max(1, maxScroll);

        graphics.fill(x, y, x + 5, y + trackH, UiTheme.BORDER);
        graphics.fill(x, thumbY, x + 5, thumbY + thumbH, UiTheme.BTN_HOVER);
    }

    public static void badge(GuiGraphics graphics, Font font, int x, int y, String text)
    {
        int w = font.width(text) + 10;
        graphics.fill(x, y, x + w, y + 14, UiTheme.ACCENT_DIM);
        graphics.drawString(font, text, x + 5, y + 3, 0xFFFFFFFF);
    }
}
