package dev.scriptbound.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.scriptbound.ScriptBoundConstants;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateStore;
import dev.scriptbound.data.StateValue;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class SBECommands
{
    private SBECommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("sbe")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("version")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("ScriptBound Engine v" + ScriptBoundConstants.VERSION),
                            false
                        );
                        return 1;
                    }))
                .then(buildStateCommand())
                .then(TriggerCommands.build())
                .then(ScriptCommands.build())
                .then(NpcCommands.build())
                .then(DialogueCommands.build())
                .then(QuestCommands.build())
                .then(RegionCommands.build())
                .then(UiCommands.build())
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildStateCommand()
    {
        return Commands.literal("state")
            .then(Commands.literal("set")
                .then(Commands.argument("target", StateTargetArgument.stateTarget())
                    .then(Commands.argument("key", StateKeyArgument.stateKey())
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                try
                                {
                                    StateService.Target target = StateService.resolveTarget(
                                        ctx.getSource(),
                                        StateTargetArgument.getString(ctx, "target")
                                    );
                                    String key = StateKeyArgument.getString(ctx, "key");
                                    StateValue value = StateValue.parse(StringArgumentType.getString(ctx, "value"));
                                    StateService.set(target, key, value);

                                    ctx.getSource().sendSuccess(
                                        () -> Component.literal("Set state " + key + " on " + target.holderId()),
                                        true
                                    );
                                    return 1;
                                }
                                catch (IllegalArgumentException e)
                                {
                                    ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                    return 0;
                                }
                            })))))
            .then(Commands.literal("add")
                .then(Commands.argument("target", StateTargetArgument.stateTarget())
                    .then(Commands.argument("key", StateKeyArgument.stateKey())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                            .executes(ctx -> {
                                try
                                {
                                    StateService.Target target = StateService.resolveTarget(
                                        ctx.getSource(),
                                        StateTargetArgument.getString(ctx, "target")
                                    );
                                    String key = StateKeyArgument.getString(ctx, "key");
                                    double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                    StateService.add(target, key, amount);

                                    ctx.getSource().sendSuccess(
                                        () -> Component.literal("Added " + amount + " to state " + key + " on " + target.holderId()),
                                        true
                                    );
                                    return 1;
                                }
                                catch (IllegalArgumentException e)
                                {
                                    ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                    return 0;
                                }
                            })))))
            .then(Commands.literal("remove")
                .then(Commands.argument("target", StateTargetArgument.stateTarget())
                    .then(Commands.argument("key", StateKeyArgument.stateKey())
                        .executes(ctx -> {
                            try
                            {
                                StateService.Target target = StateService.resolveTarget(
                                    ctx.getSource(),
                                    StateTargetArgument.getString(ctx, "target")
                                );
                                String key = StateKeyArgument.getString(ctx, "key");
                                boolean removed = StateService.remove(target, key);

                                if (!removed)
                                {
                                    ctx.getSource().sendFailure(Component.literal("State not found: " + key));
                                    return 0;
                                }

                                ctx.getSource().sendSuccess(
                                    () -> Component.literal("Removed state " + key + " from " + target.holderId()),
                                    true
                                );
                                return 1;
                            }
                            catch (IllegalArgumentException e)
                            {
                                ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                                return 0;
                            }
                        }))))
            .then(Commands.literal("clear")
                .executes(ctx -> clearStates(ctx.getSource(), "@p", "*"))
                .then(Commands.argument("target", StateTargetArgument.stateTarget())
                    .executes(ctx -> clearStates(
                        ctx.getSource(),
                        StateTargetArgument.getString(ctx, "target"),
                        "*"
                    ))
                    .then(Commands.argument("pattern", StringArgumentType.greedyString())
                        .executes(ctx -> clearStates(
                            ctx.getSource(),
                            StateTargetArgument.getString(ctx, "target"),
                            StringArgumentType.getString(ctx, "pattern")
                        )))))
            .then(Commands.literal("get")
                .then(Commands.argument("target", StateTargetArgument.stateTarget())
                    .executes(ctx -> printStates(
                        ctx.getSource(),
                        StateTargetArgument.getString(ctx, "target"),
                        null
                    ))
                    .then(Commands.argument("key", StateKeyArgument.stateKey())
                        .executes(ctx -> printStates(
                            ctx.getSource(),
                            StateTargetArgument.getString(ctx, "target"),
                            StateKeyArgument.getString(ctx, "key")
                        )))))
            .then(Commands.literal("list")
                .then(Commands.argument("target", StateTargetArgument.stateTarget())
                    .executes(ctx -> printStates(
                        ctx.getSource(),
                        StateTargetArgument.getString(ctx, "target"),
                        null
                    ))));
    }

    private static int clearStates(CommandSourceStack source, String targetToken, String pattern)
    {
        try
        {
            StateService.Target target = StateService.resolveTarget(source, targetToken);
            int count = StateService.clear(target, pattern);

            source.sendSuccess(
                () -> Component.literal("Cleared " + count + " state(s) on " + target.holderId()),
                true
            );
            return 1;
        }
        catch (IllegalArgumentException e)
        {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int printStates(CommandSourceStack source, String targetToken, String key)
    {
        try
        {
            StateService.Target target = StateService.resolveTarget(source, targetToken);
            StateStore store = target.store();

            if (key != null)
            {
                StateValue value = store.get(key);

                if (value == null)
                {
                    source.sendFailure(Component.literal("State not found: " + key));
                    return 0;
                }

                source.sendSuccess(() -> Component.literal(key + " = " + formatValue(value)), false);
                return 1;
            }

            if (store.keys().isEmpty())
            {
                source.sendSuccess(() -> Component.literal("No states on " + target.holderId()), false);
                return 1;
            }

            for (String stateKey : store.keys())
            {
                StateValue value = store.get(stateKey);
                source.sendSuccess(() -> Component.literal(stateKey + " = " + formatValue(value)), false);
            }

            return store.keys().size();
        }
        catch (IllegalArgumentException e)
        {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static String formatValue(StateValue value)
    {
        if (value.isString())
        {
            return "\"" + value.asString() + "\"";
        }

        return String.valueOf(value.asNumber());
    }
}
