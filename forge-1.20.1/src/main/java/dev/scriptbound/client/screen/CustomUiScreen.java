package dev.scriptbound.client.screen;

import dev.scriptbound.client.ui.UiElementRenderer;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.network.packet.UiClickPacket;
import dev.scriptbound.ui.UiDefinition;
import dev.scriptbound.ui.UiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CustomUiScreen extends Screen
{
    private final String uiId;
    private final UiDefinition def;

    public CustomUiScreen(String id, UiDefinition def)
    {
        super(Component.literal(id));
        this.uiId = id;
        this.def = def;
    }

    public String uiId()
    {
        return this.uiId;
    }

    public UiDefinition definition()
    {
        return this.def;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);

        for (UiElement element : this.def.elements)
        {
            UiElementRenderer.render(graphics, element, 0, 0, this.width, this.height);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            for (int i = this.def.elements.size() - 1; i >= 0; i--)
            {
                String hit = clickable(this.def.elements.get(i), 0, 0, this.width, this.height, mouseX, mouseY);

                if (hit != null)
                {
                    NetworkHandler.CHANNEL.sendToServer(new UiClickPacket(this.uiId, hit));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String clickable(UiElement element, int parentX, int parentY, int parentW, int parentH, double mx, double my)
    {
        if (!element.visible)
        {
            return null;
        }

        int x = element.resolveX(parentX, parentW);
        int y = element.resolveY(parentY, parentH);

        for (int i = element.children.size() - 1; i >= 0; i--)
        {
            String childHit = clickable(element.children.get(i), x, y, element.w, element.h, mx, my);

            if (childHit != null)
            {
                return childHit;
            }
        }

        boolean inside = mx >= x && mx < x + element.w && my >= y && my < y + element.h;
        boolean isClickable = "button".equals(element.type) || (element.onClick != null && !element.onClick.isBlank());

        return inside && isClickable && element.id != null && !element.id.isBlank() ? element.id : null;
    }

    @Override
    public boolean isPauseScreen()
    {
        return this.def.pauseGame;
    }
}
