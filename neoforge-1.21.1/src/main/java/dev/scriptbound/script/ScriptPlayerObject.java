package dev.scriptbound.script;

import net.minecraft.server.level.ServerPlayer;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class ScriptPlayerObject extends ScriptableObject
{
    private static boolean classDefined;

    private ScriptContext context;

    @Override
    public String getClassName()
    {
        return "ScriptAPI";
    }

    public static ScriptPlayerObject create(org.mozilla.javascript.Context cx, Scriptable scope, ServerPlayer player)
    {
        try
        {
            if (!classDefined)
            {
                ScriptableObject.defineClass(scope, ScriptPlayerObject.class, true);
                classDefined = true;
            }

            ScriptPlayerObject object = (ScriptPlayerObject) cx.newObject(scope, "ScriptAPI");
            object.context = new ScriptContext(player);
            return object;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to initialize ScriptBound script API", e);
        }
    }

    public void jsFunction_send(String message)
    {
        this.context.send(message);
    }

    public void jsFunction_command(String command)
    {
        this.context.command(command);
    }

    public double jsFunction_getState(String scope, String key)
    {
        return this.context.getState(scope, key);
    }

    public String jsFunction_getStateString(String scope, String key)
    {
        return this.context.getStateString(scope, key);
    }

    public void jsFunction_setState(String scope, String key, double value)
    {
        this.context.setState(scope, key, value);
    }

    public void jsFunction_setStateString(String scope, String key, String value)
    {
        this.context.setStateString(scope, key, value);
    }

    public void jsFunction_addState(String scope, String key, double delta)
    {
        this.context.addState(scope, key, delta);
    }

    public void jsFunction_runTrigger(String triggerId)
    {
        this.context.runTrigger(triggerId);
    }

    public void jsFunction_teleport(double x, double y, double z)
    {
        this.context.teleport(x, y, z);
    }

    public void jsFunction_log(String message)
    {
        this.context.log(message);
    }
}
