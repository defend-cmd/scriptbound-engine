package dev.scriptbound.client.bbs;

import dev.scriptbound.ScriptBoundMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BBSUiAssets
{
    public static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(ScriptBoundMod.MOD_ID, "textures/gui/icons.png");

    private BBSUiAssets() {}
}
