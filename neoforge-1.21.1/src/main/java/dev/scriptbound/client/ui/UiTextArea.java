package dev.scriptbound.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class UiTextArea
{
    private static final java.util.regex.Pattern SYNTAX_PATTERN = java.util.regex.Pattern.compile(
        "(\".*?\"|'.*?')|" +
        "(\\b\\d+\\.?\\d*\\b)|" +
        "(\\b(?:function|var|let|const|if|else|return|for|while|new|switch|case|break|continue|true|false|null|undefined|class|import|export|this)\\b)|" +
        "(\\w+)|" +
        "(\\s+)|" +
        "(.)"
    );
    private String value = "";
    private String hint = "";
    private boolean focused;
    private int cursor;
    private int scrollLine;
    private boolean selectAll;

    public UiTextArea hint(String hint)
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
        this.scrollLine = 0;
        this.selectAll = false;
    }

    public boolean focused()
    {
        return this.focused;
    }

    public void setFocused(boolean focused)
    {
        this.focused = focused;
    }

    public void render(GuiGraphics graphics, Font font, UiRect rect)
    {
        int border = this.focused ? UiTheme.INPUT_FOCUS : UiTheme.INPUT_BORDER;
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.w(), rect.y() + rect.h(), UiTheme.INPUT);
        graphics.renderOutline(rect.x(), rect.y(), rect.w(), rect.h(), border);

        int innerX = rect.x() + 6;
        int innerY = rect.y() + 6;
        int innerW = rect.w() - 14;
        int innerH = rect.h() - 12;
        int lineH = 10;
        int visibleLines = Math.max(1, innerH / lineH);

        if (this.value.isEmpty() && !this.focused && !this.hint.isEmpty())
        {
            graphics.drawString(font, this.hint, innerX, innerY, UiTheme.TEXT_DIM);
            return;
        }

        String[] lines = this.value.replace("\r", "").split("\n", -1);
        int cursorLine = lineOfCursor(lines, this.cursor);
        this.scrollLine = Mth.clamp(this.scrollLine, 0, Math.max(0, lines.length - visibleLines));

        if (this.focused && cursorLine < this.scrollLine)
        {
            this.scrollLine = cursorLine;
        }
        else if (this.focused && cursorLine >= this.scrollLine + visibleLines)
        {
            this.scrollLine = cursorLine - visibleLines + 1;
        }

        graphics.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        if (this.focused && this.selectAll) {
            graphics.fill(innerX, innerY, innerX + innerW, innerY + innerH, UiTheme.ACCENT | 0x88000000);
        }

        for (int i = 0; i < visibleLines; i++)
        {
            int lineIndex = i + this.scrollLine;

            if (lineIndex >= lines.length)
            {
                break;
            }

            int y = innerY + i * lineH;
            String line = font.plainSubstrByWidth(lines[lineIndex], innerW);
            drawHighlightedLine(graphics, font, line, innerX, y);

            if (this.focused && lineIndex == cursorLine && System.currentTimeMillis() / 500 % 2 == 0)
            {
                int col = columnOfCursor(lines, this.cursor);
                int cx = innerX + font.width(line.substring(0, Math.min(col, line.length())));
                graphics.fill(cx, y - 1, cx + 1, y + 9, UiTheme.ACCENT);
            }
        }

        graphics.disableScissor();
        UiDraw.scrollbar(graphics, rect.x() + rect.w() - 4, innerY, innerH, lines.length, visibleLines, this.scrollLine);
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
            this.cursor = pickCursor(rect, mx, my);
            this.selectAll = false;
        }

        return inside;
    }

    public boolean mouseScrolled(UiRect rect, double mx, double my, double delta)
    {
        if (!rect.contains(mx, my))
        {
            return false;
        }

        this.scrollLine = Math.max(0, this.scrollLine - (int) Math.signum(delta));
        return true;
    }

    public boolean keyPressed(int key)
    {
        if (!this.focused)
        {
            return false;
        }

        if (net.minecraft.client.gui.screens.Screen.isSelectAll(key)) {
            this.selectAll = true;
            this.cursor = this.value.length();
            return true;
        }

        if (net.minecraft.client.gui.screens.Screen.isCopy(key)) {
            if (this.selectAll) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.value);
            }
            return true;
        }

        if (net.minecraft.client.gui.screens.Screen.isPaste(key)) {
            if (this.selectAll) {
                this.value = "";
                this.cursor = 0;
                this.selectAll = false;
            }
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                this.value = this.value.substring(0, this.cursor) + clip + this.value.substring(this.cursor);
                this.cursor += clip.length();
            }
            return true;
        }

        if (net.minecraft.client.gui.screens.Screen.isCut(key)) {
            if (this.selectAll) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.value);
                this.value = "";
                this.cursor = 0;
                this.selectAll = false;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE)
        {
            if (this.selectAll) {
                this.value = "";
                this.cursor = 0;
                this.selectAll = false;
                return true;
            }
            if (this.cursor > 0)
            {
                this.value = this.value.substring(0, this.cursor - 1) + this.value.substring(this.cursor);
                this.cursor--;
            }

            return true;
        }

        if (key == GLFW.GLFW_KEY_DELETE)
        {
            if (this.selectAll) {
                this.value = "";
                this.cursor = 0;
                this.selectAll = false;
                return true;
            }
            if (this.cursor < this.value.length())
            {
                this.value = this.value.substring(0, this.cursor) + this.value.substring(this.cursor + 1);
            }

            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)
        {
            if (this.selectAll) {
                this.value = "";
                this.cursor = 0;
                this.selectAll = false;
            }
            insert("\n");
            return true;
        }

        if (key == GLFW.GLFW_KEY_LEFT)
        {
            this.selectAll = false;
            this.cursor = Math.max(0, this.cursor - 1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_RIGHT)
        {
            this.selectAll = false;
            this.cursor = Math.min(this.value.length(), this.cursor + 1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_UP)
        {
            this.selectAll = false;
            moveVertical(-1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_DOWN)
        {
            this.selectAll = false;
            moveVertical(1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_HOME)
        {
            this.selectAll = false;
            this.cursor = 0;
            return true;
        }

        if (key == GLFW.GLFW_KEY_END)
        {
            this.selectAll = false;
            this.cursor = this.value.length();
            return true;
        }

        return false;
    }

    public boolean charTyped(char character)
    {
        if (!this.focused || character < 32 && character != '\t')
        {
            return false;
        }

        if (this.selectAll) {
            this.value = "";
            this.cursor = 0;
            this.selectAll = false;
        }

        if (character == '\t')
        {
            insert("    ");
            return true;
        }

        if (character == 127)
        {
            return false;
        }

        insert(String.valueOf(character));
        return true;
    }

    private void insert(String text)
    {
        this.value = this.value.substring(0, this.cursor) + text + this.value.substring(this.cursor);
        this.cursor += text.length();
    }

    private void moveVertical(int delta)
    {
        String[] lines = this.value.split("\n", -1);
        int line = lineOfCursor(lines, this.cursor);
        int column = columnOfCursor(lines, this.cursor);
        line = Mth.clamp(line + delta, 0, Math.max(0, lines.length - 1));
        int lineStart = 0;

        for (int i = 0; i < line; i++)
        {
            lineStart += lines[i].length() + 1;
        }

        this.cursor = Math.min(this.value.length(), lineStart + Math.min(column, lines[line].length()));
    }

    private int pickCursor(UiRect rect, double mx, double my)
    {
        Font font = Minecraft.getInstance().font;
        int innerX = rect.x() + 6;
        int innerY = rect.y() + 6;
        int lineH = 10;
        int line = Mth.clamp((int) ((my - innerY) / lineH) + this.scrollLine, 0, Integer.MAX_VALUE);
        String[] lines = this.value.isEmpty() ? new String[] { "" } : this.value.split("\n", -1);
        line = Math.min(line, lines.length - 1);
        int localX = (int) mx - innerX;
        String text = lines[line];

        for (int i = 0; i <= text.length(); i++)
        {
            if (font.width(text.substring(0, i)) > localX)
            {
                int offset = 0;

                for (int j = 0; j < line; j++)
                {
                    offset += lines[j].length() + 1;
                }

                return offset + i;
            }
        }

        int offset = 0;

        for (int j = 0; j < line; j++)
        {
            offset += lines[j].length() + 1;
        }

        return offset + text.length();
    }

    private static int lineOfCursor(String[] lines, int cursor)
    {
        int pos = 0;

        for (int i = 0; i < lines.length; i++)
        {
            int next = pos + lines[i].length() + (i < lines.length - 1 ? 1 : 0);

            if (cursor <= next)
            {
                return i;
            }

            pos = next;
        }

        return Math.max(0, lines.length - 1);
    }

    private static int columnOfCursor(String[] lines, int cursor)
    {
        int pos = 0;

        for (int i = 0; i < lines.length; i++)
        {
            int lineEnd = pos + lines[i].length();

            if (cursor <= lineEnd)
            {
                return cursor - pos;
            }

            pos = lineEnd + 1;
        }

        return 0;
    }

    private void drawHighlightedLine(GuiGraphics graphics, Font font, String line, int x, int y)
    {
        if (line.trim().startsWith("//"))
        {
            graphics.drawString(font, line, x, y, UiTheme.TEXT_DIM);
            return;
        }

        try
        {
            java.util.regex.Matcher matcher = SYNTAX_PATTERN.matcher(line);
            int currentX = x;

            while (matcher.find())
            {
                String token = matcher.group();
                int color = UiTheme.TEXT;

                if (matcher.group(1) != null)
                {
                    color = 0xFF66AA66;
                }
                else if (matcher.group(2) != null)
                {
                    color = 0xFFEE8844;
                }
                else if (matcher.group(3) != null)
                {
                    color = UiTheme.TEXT_CMD;
                }

                if (!token.isBlank())
                {
                    graphics.drawString(font, token, currentX, y, color);
                }
                currentX += font.width(token);
            }
        }
        catch (Exception e)
        {
            graphics.drawString(font, line, x, y, UiTheme.TEXT);
        }
    }
}
