package dev.scriptbound.trigger.actions;

import dev.scriptbound.trigger.TriggerContext;

public interface TriggerAction
{
    String type();

    void execute(TriggerContext context);
}
