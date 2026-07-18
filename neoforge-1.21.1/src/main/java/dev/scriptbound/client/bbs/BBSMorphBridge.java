package dev.scriptbound.client.bbs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class BBSMorphBridge
{
    public static void openMorphMenu(Screen parent, Object currentForm, Consumer<Object> onSelected)
    {
    }
}
