package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;
import net.minecraft.commands.CommandSourceStack;

public record CommandAction(String command) implements TriggerAction
{
    public static CommandAction fromJson(JsonObject json)
    {
        return new CommandAction(json.has("command") ? json.get("command").getAsString() : "");
    }

    @Override
    public String type()
    {
        return "command";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (this.command.isBlank())
        {
            return;
        }

        CommandSourceStack source = context.asSource();
        context.level().getServer().getCommands().performPrefixedCommand(source, this.command);
    }
}
