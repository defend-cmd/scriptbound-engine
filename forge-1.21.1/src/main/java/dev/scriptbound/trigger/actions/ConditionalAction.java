package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.checker.Checker;
import dev.scriptbound.checker.CheckerParser;
import dev.scriptbound.trigger.TriggerChain;
import dev.scriptbound.trigger.TriggerContext;

public final class ConditionalAction implements TriggerAction
{
    private final Checker checker;
    private final TriggerAction delegate;

    public ConditionalAction(Checker checker, TriggerAction delegate)
    {
        this.checker = checker == null ? context -> true : checker;
        this.delegate = delegate;
    }

    public static ConditionalAction fromJson(JsonObject entry)
    {
        TriggerAction action = TriggerChain.parseActionBody(entry);

        if (action == null)
        {
            return null;
        }

        Checker checker = entry.has("if") ? CheckerParser.parse(entry.get("if")) : null;
        return new ConditionalAction(checker, action);
    }

    public TriggerAction delegate()
    {
        return this.delegate;
    }

    public Checker checker()
    {
        return this.checker;
    }

    @Override
    public String type()
    {
        return this.delegate.type();
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (this.checker.test(context))
        {
            this.delegate.execute(context);
        }
    }
}
