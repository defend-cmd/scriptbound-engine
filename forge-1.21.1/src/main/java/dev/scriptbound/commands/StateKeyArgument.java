package dev.scriptbound.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public class StateKeyArgument implements ArgumentType<String>
{
    private static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(
        Component.literal("Expected state key")
    );

    private StateKeyArgument() {}

    public static StateKeyArgument stateKey()
    {
        return new StateKeyArgument();
    }

    public static String getString(CommandContext<?> context, String name)
    {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException
    {
        int start = reader.getCursor();

        if (!reader.canRead())
        {
            throw ERROR_EMPTY.createWithContext(reader);
        }

        while (reader.canRead() && !Character.isWhitespace(reader.peek()))
        {
            reader.skip();
        }

        if (reader.getCursor() == start)
        {
            throw ERROR_EMPTY.createWithContext(reader);
        }

        return reader.getString().substring(start, reader.getCursor());
    }
}
