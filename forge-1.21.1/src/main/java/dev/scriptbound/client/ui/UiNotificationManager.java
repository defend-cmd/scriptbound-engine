package dev.scriptbound.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UiNotificationManager
{
    public static final int TYPE_INFO = 0;
    public static final int TYPE_SUCCESS = 1;
    public static final int TYPE_WARNING = 2;
    public static final int TYPE_ERROR = 3;

    private static final List<Notification> notifications = new ArrayList<>();

    public static void push(int type, String message)
    {
        notifications.add(new Notification(type, message));
    }

    public static boolean mouseClicked(double mouseX, double mouseY)
    {
        long now = System.currentTimeMillis();
        for (Notification n : notifications)
        {
            if (n.dismissing || now > n.endTime) continue;

            if (mouseX >= n.lastX && mouseX <= n.lastX + n.lastW &&
                mouseY >= n.lastY && mouseY <= n.lastY + n.lastH)
            {
                n.dismissing = true;
                n.endTime = now + 300;
                return true;
            }
        }
        return false;
    }

    public static void render(GuiGraphics graphics, Font font, int screenWidth, int mouseX, int mouseY)
    {
        if (notifications.isEmpty()) return;

        int pad = UiScale.px(10);
        int width = UiScale.px(240);
        int startY = pad;

        long now = System.currentTimeMillis();
        Iterator<Notification> it = notifications.iterator();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);

        while (it.hasNext())
        {
            Notification n = it.next();
            if (now > n.endTime)
            {
                it.remove();
                continue;
            }

            long elapsed = now - n.startTime;
            long remaining = n.endTime - now;

            int yOffset = 0;
            if (elapsed < 300) {
                float t = elapsed / 300.0f;
                yOffset = (int) (-40 * (1 - t));
            }

            float alpha = 1.0f;
            if (remaining < 300) {
                alpha = Math.max(0, remaining / 300.0f);
                yOffset = (int) (-40 * (1 - alpha));
            }

            int x = screenWidth / 2 - width / 2;
            int y = startY + yOffset;

            List<FormattedCharSequence> lines = font.split(Component.literal(n.message), width - 16);
            int textH = lines.size() * font.lineHeight;
            int h = Math.max(UiScale.px(24), textH + 16);

            n.lastX = x;
            n.lastY = y;
            n.lastW = width;
            n.lastH = h;

            int r = UiConfig.roundedStyle * 2;
            int color = 0xFF121212;
            int outline = UiTheme.BORDER;

            if (n.type == TYPE_SUCCESS) outline = 0xFF55FF55;
            else if (n.type == TYPE_WARNING) outline = 0xFFFFAA00;
            else if (n.type == TYPE_ERROR) outline = 0xFFFF5555;
            else if (n.type == TYPE_INFO) outline = UiTheme.ACCENT;

            if (!n.dismissing && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + h) {
                outline = UiTheme.ACCENT_DIM;
            }

            UiDraw.fillRounded(graphics, x, y, width, h, color, r);
            UiDraw.outlineRounded(graphics, x, y, width, h, outline, r);

            int textAlpha = (int)(255 * alpha);
            int textColor = (textAlpha << 24) | 0xFFFFFF;

            int lineY = y + 8;
            for (FormattedCharSequence line : lines) {
                graphics.drawCenteredString(font, line, screenWidth / 2, lineY, textColor);
                lineY += font.lineHeight;
            }

            if (!n.dismissing) {
                float progress = Math.max(0, (float)(n.endTime - now) / 10000.0f);
                int barW = (int)((width - 6) * progress);
                UiDraw.fillRounded(graphics, x + 3, y + h - 4, barW, 2, 0xFF666666, 1);
            }

            startY += h + 4;
        }

        graphics.pose().popPose();
    }

    private static class Notification
    {
        int type;
        String message;
        long startTime;
        long endTime;

        boolean dismissing = false;
        int lastX, lastY, lastW, lastH;

        Notification(int type, String message)
        {
            this.type = type;
            this.message = message;
            this.startTime = System.currentTimeMillis();
            this.endTime = this.startTime + 10000;
        }
    }
}
