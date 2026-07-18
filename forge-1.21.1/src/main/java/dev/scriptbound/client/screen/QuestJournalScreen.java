package dev.scriptbound.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class QuestJournalScreen extends Screen
{
    private final List<String> lines;

    public QuestJournalScreen(List<String> lines)
    {
        super(Component.literal("Quest Journal"));
        this.lines = lines;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int y = 40;

        for (String line : this.lines)
        {
            graphics.drawString(this.font, line, 30, y, 0xE0E0E0);
            y += 12;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
