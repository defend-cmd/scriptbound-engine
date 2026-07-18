package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

public record MessageAction(String text, String subtitle, Mode mode) implements TriggerAction
{
    public enum Mode
    {
        CHAT,
        ACTIONBAR,
        TITLE
    }

    public static MessageAction fromJson(JsonObject json)
    {
        String text = json.has("text") ? json.get("text").getAsString() : "";
        String subtitle = json.has("subtitle") ? json.get("subtitle").getAsString() : "";
        Mode mode = Mode.CHAT;

        if (json.has("mode"))
        {
            mode = switch (json.get("mode").getAsString().trim().toLowerCase())
            {
                case "actionbar" -> Mode.ACTIONBAR;
                case "title" -> Mode.TITLE;
                default -> Mode.CHAT;
            };
        }

        return new MessageAction(text, subtitle, mode);
    }

    @Override
    public String type()
    {
        return "message";
    }

    @Override
    public void execute(TriggerContext context)
    {
        Component component = Component.literal(this.text);

        switch (this.mode)
        {
            case ACTIONBAR -> {
                context.player().displayClientMessage(component, true);
            }
            case TITLE -> {
                context.player().connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                context.player().connection.send(new ClientboundSetTitleTextPacket(component));
                if (this.subtitle != null && !this.subtitle.isEmpty()) {
                    context.player().connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(this.subtitle)));
                } else {
                    context.player().connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
                }
            }
            default -> context.player().sendSystemMessage(component);
        }
    }
}
