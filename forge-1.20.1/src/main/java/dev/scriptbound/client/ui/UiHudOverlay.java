package dev.scriptbound.client.ui;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ui.UiDefinition;
import dev.scriptbound.ui.UiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID, value = Dist.CLIENT)
public final class UiHudOverlay
{
    private UiHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options.hideGui || minecraft.screen != null)
        {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        for (UiDefinition def : ClientUiManager.activeHuds())
        {
            for (UiElement element : def.elements)
            {
                UiElementRenderer.render(graphics, element, 0, 0, screenW, screenH);
            }
        }
    }
}
