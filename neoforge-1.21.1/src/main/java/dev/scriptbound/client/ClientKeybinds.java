package dev.scriptbound.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.scriptbound.ScriptBoundMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ClientKeybinds
{
    public static final KeyMapping DASHBOARD = new KeyMapping(
        "key.scriptbound.dashboard",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_EQUAL,
        "key.categories.scriptbound"
    );

    public static final KeyMapping JOURNAL = new KeyMapping(
        "key.scriptbound.journal",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        "key.categories.scriptbound"
    );

    private ClientKeybinds() {}
}
