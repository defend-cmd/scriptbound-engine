package dev.scriptbound.client.screen;

import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.network.packet.DialogueAdvancePacket;
import dev.scriptbound.network.packet.DialogueChoicePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class DialogueScreen extends Screen
{
    private final String dialogueId;
    private final int lineIndex;
    private final String speaker;
    private final String text;
    private final List<String> choices;

    public DialogueScreen(String dialogueId, int lineIndex, String speaker, String text, List<String> choices)
    {
        super(Component.literal("Dialogue"));
        this.dialogueId = dialogueId;
        this.lineIndex = lineIndex;
        this.speaker = speaker;
        this.text = text;
        this.choices = choices;
    }

    @Override
    protected void init()
    {
        int y = this.height - 30;

        if (this.choices.isEmpty())
        {
            this.addRenderableWidget(Button.builder(Component.literal("Continue"), button ->
                NetworkHandler.CHANNEL.sendToServer(new DialogueAdvancePacket(this.dialogueId, this.lineIndex))
            ).bounds(this.width / 2 - 50, y, 100, 20).build());
        }
        else
        {
            int buttonWidth = Math.min(220, this.width - 40);
            int startX = (this.width - buttonWidth) / 2;
            y -= this.choices.size() * 24;

            for (int i = 0; i < this.choices.size(); i++)
            {
                int choiceIndex = i;
                this.addRenderableWidget(Button.builder(Component.literal(this.choices.get(i)), button ->
                    NetworkHandler.CHANNEL.sendToServer(new DialogueChoicePacket(this.dialogueId, this.lineIndex, choiceIndex))
                ).bounds(startX, y + i * 24, buttonWidth, 20).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        int boxWidth = Math.min(320, this.width - 40);
        int x = (this.width - boxWidth) / 2;
        int y = this.height / 3;
        graphics.fill(x - 8, y - 24, x + boxWidth + 8, y + 80, 0xAA000000);

        if (!this.speaker.isBlank())
        {
            graphics.drawString(this.font, this.speaker, x, y - 16, 0xFFAA00);
        }

        graphics.drawWordWrap(this.font, Component.literal(this.text), x, y, boxWidth, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return true;
    }
}
