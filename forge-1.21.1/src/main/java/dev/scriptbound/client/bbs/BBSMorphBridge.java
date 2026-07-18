package dev.scriptbound.client.bbs;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class BBSMorphBridge
{
    public static void openMorphMenu(Screen parent, Form currentForm, Consumer<Form> onSelected)
    {
        BBSMorphMenu menu = new BBSMorphMenu(() -> Minecraft.getInstance().setScreen(parent), currentForm, onSelected);

        try
        {
            Class<?> uiScreenClass = Class.forName("mchorse.bbs_mod.ui.framework.UIScreen");
            uiScreenClass.getMethod("open", UIBaseMenu.class).invoke(null, menu);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Minecraft.getInstance().setScreen(parent);
        }
    }

    public static class BBSMorphMenu extends UIBaseMenu
    {
        private final Runnable onClose;
        public final UIFormPalette palette;

        public BBSMorphMenu(Runnable onClose, Form currentForm, Consumer<Form> callback)
        {
            super();
            this.onClose = onClose;
            this.palette = UIFormPalette.open(this.main, false, currentForm, (form) -> {
                callback.accept(form);
                this.closeMenu();
            });
        }

        @Override
        protected void closeMenu()
        {
            this.onClose.run();
        }
    }
}
