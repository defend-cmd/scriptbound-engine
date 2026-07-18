package dev.scriptbound.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class UiTextField
{
    private String value = "";
    private String hint = "";
    private boolean focused;
    private int cursor;
    private int selectionEnd;
    private int scrollOffset;

    public UiTextField hint(String hint)
    {
        this.hint = hint == null ? "" : hint;
        return this;
    }

    public String value()
    {
        return this.value;
    }

    public void setValue(String value)
    {
        this.value = value == null ? "" : value;
        this.cursor = this.value.length();
        this.selectionEnd = this.cursor;
        this.scrollOffset = 0;
    }

    public boolean focused()
    {
        return this.focused;
    }

    public void setFocused(boolean focused)
    {
        this.focused = focused;

        if (!focused)
        {
            this.scrollOffset = 0;
            this.selectionEnd = this.cursor;
        }
    }

    private void updateScroll(Font font, int innerW)
    {
        if (this.scrollOffset > this.value.length()) {
            this.scrollOffset = this.value.length();
        }
        if (this.cursor < this.scrollOffset) {
            this.scrollOffset = this.cursor;
        } else {
            while (font.width(this.value.substring(this.scrollOffset, this.cursor)) > innerW && this.scrollOffset < this.cursor) {
                this.scrollOffset++;
            }
        }
    }

    public void render(GuiGraphics graphics, Font font, UiRect rect)
    {
        int r = UiConfig.roundedStyle * 2;
        int border = this.focused ? UiTheme.INPUT_FOCUS : UiTheme.INPUT_BORDER;
        UiDraw.fillRounded(graphics, rect.x(), rect.y(), rect.w(), rect.h(), UiTheme.INPUT, r);
        UiDraw.outlineRounded(graphics, rect.x(), rect.y(), rect.w(), rect.h(), border, r);

        int textX = rect.x() + 6;
        int textY = rect.y() + (rect.h() - 8) / 2;
        int innerW = rect.w() - 12;

        if (this.value.isEmpty() && !this.focused && !this.hint.isEmpty())
        {
            graphics.drawString(font, this.hint, textX, textY, UiTheme.TEXT_DIM);
            return;
        }

        updateScroll(font, innerW);

        String visible = font.plainSubstrByWidth(this.value.substring(this.scrollOffset), innerW);

        if (this.focused)
        {
            int sStart = Math.min(this.cursor, this.selectionEnd);
            int sEnd = Math.max(this.cursor, this.selectionEnd);

            if (sStart < sEnd) {
                int visStart = Math.max(sStart, this.scrollOffset);
                int visEnd = Math.min(sEnd, this.scrollOffset + visible.length());
                if (visStart < visEnd) {
                    int selX1 = textX + font.width(this.value.substring(this.scrollOffset, visStart));
                    int selX2 = textX + font.width(this.value.substring(this.scrollOffset, visEnd));
                    graphics.fill(selX1, rect.y() + 3, selX2, rect.y() + rect.h() - 3, UiTheme.ACCENT | 0x88000000);
                }
            }

            int cursorInView = this.cursor - this.scrollOffset;
            if (cursorInView >= 0 && cursorInView <= visible.length()) {
                int cursorX = textX + font.width(visible.substring(0, cursorInView));

                if (System.currentTimeMillis() / 500 % 2 == 0)
                {
                    graphics.fill(cursorX, rect.y() + 3, cursorX + 1, rect.y() + rect.h() - 3, UiTheme.ACCENT);
                }
            }
        }

        graphics.drawString(font, visible, textX, textY, UiTheme.TEXT);
    }

    public boolean mouseClicked(UiRect rect, double mx, double my, int button)
    {
        if (button != 0)
        {
            return false;
        }

        boolean inside = rect.contains(mx, my);
        this.focused = inside;

        if (inside)
        {
            this.cursor = pickCursor(rect, mx);
            if (!Screen.hasShiftDown()) {
                this.selectionEnd = this.cursor;
            }
        }

        return inside;
    }

    private void deleteSelection()
    {
        if (this.cursor != this.selectionEnd) {
            int start = Math.min(this.cursor, this.selectionEnd);
            int end = Math.max(this.cursor, this.selectionEnd);
            this.value = this.value.substring(0, start) + this.value.substring(end);
            this.cursor = start;
            this.selectionEnd = start;
        }
    }

    public boolean keyPressed(int key)
    {
        if (!this.focused)
        {
            return false;
        }

        if (Screen.isSelectAll(key)) {
            this.cursor = this.value.length();
            this.selectionEnd = 0;
            return true;
        }

        if (Screen.isCopy(key)) {
            int start = Math.min(this.cursor, this.selectionEnd);
            int end = Math.max(this.cursor, this.selectionEnd);
            if (start < end) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.value.substring(start, end));
            }
            return true;
        }

        if (Screen.isPaste(key)) {
            deleteSelection();
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                this.value = this.value.substring(0, this.cursor) + clip + this.value.substring(this.cursor);
                this.cursor += clip.length();
                this.selectionEnd = this.cursor;
            }
            return true;
        }

        if (Screen.isCut(key)) {
            int start = Math.min(this.cursor, this.selectionEnd);
            int end = Math.max(this.cursor, this.selectionEnd);
            if (start < end) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.value.substring(start, end));
                deleteSelection();
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE)
        {
            if (this.cursor != this.selectionEnd) {
                deleteSelection();
            } else if (this.cursor > 0)
            {
                this.value = this.value.substring(0, this.cursor - 1) + this.value.substring(this.cursor);
                this.cursor--;
                this.selectionEnd = this.cursor;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_DELETE)
        {
            if (this.cursor != this.selectionEnd) {
                deleteSelection();
            } else if (this.cursor < this.value.length())
            {
                this.value = this.value.substring(0, this.cursor) + this.value.substring(this.cursor + 1);
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_LEFT)
        {
            if (Screen.hasControlDown()) {

                while (this.cursor > 0 && this.value.charAt(this.cursor - 1) == ' ') this.cursor--;
                while (this.cursor > 0 && this.value.charAt(this.cursor - 1) != ' ') this.cursor--;
            } else {
                this.cursor = Math.max(0, this.cursor - 1);
            }
            if (!Screen.hasShiftDown()) this.selectionEnd = this.cursor;
            return true;
        }

        if (key == GLFW.GLFW_KEY_RIGHT)
        {
            if (Screen.hasControlDown()) {

                while (this.cursor < this.value.length() && this.value.charAt(this.cursor) == ' ') this.cursor++;
                while (this.cursor < this.value.length() && this.value.charAt(this.cursor) != ' ') this.cursor++;
            } else {
                this.cursor = Math.min(this.value.length(), this.cursor + 1);
            }
            if (!Screen.hasShiftDown()) this.selectionEnd = this.cursor;
            return true;
        }

        if (key == GLFW.GLFW_KEY_HOME)
        {
            this.cursor = 0;
            if (!Screen.hasShiftDown()) this.selectionEnd = this.cursor;
            return true;
        }

        if (key == GLFW.GLFW_KEY_END)
        {
            this.cursor = this.value.length();
            if (!Screen.hasShiftDown()) this.selectionEnd = this.cursor;
            return true;
        }

        return false;
    }

    public boolean charTyped(char character)
    {
        if (!this.focused || character < 32 || character == 127)
        {
            return false;
        }

        deleteSelection();
        this.value = this.value.substring(0, this.cursor) + character + this.value.substring(this.cursor);
        this.cursor++;
        this.selectionEnd = this.cursor;
        return true;
    }

    private int pickCursor(UiRect rect, double mx)
    {
        Font font = Minecraft.getInstance().font;
        int textX = rect.x() + 6;
        int localX = (int) mx - textX;

        if (localX < 0) return this.scrollOffset;

        String visible = font.plainSubstrByWidth(this.value.substring(this.scrollOffset), rect.w() - 12);

        for (int i = 0; i <= visible.length(); i++)
        {
            if (font.width(visible.substring(0, i)) > localX)
            {
                return this.scrollOffset + Math.max(0, i - 1);
            }
        }

        return this.scrollOffset + visible.length();
    }
}
