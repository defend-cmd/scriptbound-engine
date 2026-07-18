package dev.scriptbound.checker;

import dev.scriptbound.trigger.TriggerContext;

public interface Checker
{
    boolean test(TriggerContext context);
}
