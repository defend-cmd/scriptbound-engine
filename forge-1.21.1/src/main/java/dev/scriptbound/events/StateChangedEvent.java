package dev.scriptbound.events;

import dev.scriptbound.data.StateScope;
import dev.scriptbound.data.StateValue;
import net.minecraftforge.eventbus.api.Event;

public class StateChangedEvent extends Event
{
    private final StateScope scope;
    private final String holderId;
    private final String key;
    private final StateValue previous;
    private final StateValue current;

    public StateChangedEvent(StateScope scope, String holderId, String key, StateValue previous, StateValue current)
    {
        this.scope = scope;
        this.holderId = holderId;
        this.key = key;
        this.previous = previous;
        this.current = current;
    }

    public StateScope getScope()
    {
        return this.scope;
    }

    public String getHolderId()
    {
        return this.holderId;
    }

    public String getKey()
    {
        return this.key;
    }

    public StateValue getPrevious()
    {
        return this.previous;
    }

    public StateValue getCurrent()
    {
        return this.current;
    }
}
