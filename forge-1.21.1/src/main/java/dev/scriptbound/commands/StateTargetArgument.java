package dev.scriptbound.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.scriptbound.ScriptBoundConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class StateTargetArgument implements ArgumentType<String>
{
    private static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(
        Component.literal("Expected state target (~, @p, global, or player name)")
    );

    private StateTargetArgument() {}

    public static StateTargetArgument stateTarget()
    {
        return new StateTargetArgument();
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

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
    {
        builder.suggest(ScriptBoundConstants.GLOBAL_SCOPE);
        builder.suggest("global");
        builder.suggest("@p");

        if (context.getSource() instanceof CommandSourceStack source)
        {
            return SharedSuggestionProvider.suggest(source.getOnlinePlayerNames(), builder);
        }

        return builder.buildFuture();
    }
}
