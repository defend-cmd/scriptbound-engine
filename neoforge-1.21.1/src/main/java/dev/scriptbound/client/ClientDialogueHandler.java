package dev.scriptbound.client;

import dev.scriptbound.client.screen.DialogueScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClientDialogueHandler
{
    private ClientDialogueHandler() {}

    public static void open(String dialogueId, int lineIndex, String speaker, String text, List<String> choices)
    {
        Minecraft.getInstance().setScreen(new DialogueScreen(dialogueId, lineIndex, speaker, text, choices));
    }

    public static void close()
    {
        if (Minecraft.getInstance().screen instanceof DialogueScreen)
        {
            Minecraft.getInstance().setScreen(null);
        }
    }
}
