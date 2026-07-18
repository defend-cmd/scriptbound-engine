package dev.scriptbound.script;

import net.minecraft.server.level.ServerPlayer;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public final class ScriptApiFactory
{
    private ScriptApiFactory() {}

    public static Scriptable create(Context cx, Scriptable scope, ServerPlayer player)
    {
        ScriptContext api = new ScriptContext(player);
        ScriptableObject object = new ScriptableObject()
        {
            @Override
            public String getClassName()
            {
                return "ScriptAPI";
            }
        };
        object.setParentScope(scope);

        bind(object, "send", args -> {
            api.send(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "command", args -> {
            api.command(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "getState", args -> api.getState(stringArg(args, 0), stringArg(args, 1)));
        bind(object, "getStateString", args -> api.getStateString(stringArg(args, 0), stringArg(args, 1)));
        bind(object, "setState", args -> {
            api.setState(stringArg(args, 0), stringArg(args, 1), numberArg(args, 2));
            return Undefined.instance;
        });
        bind(object, "setStateString", args -> {
            api.setStateString(stringArg(args, 0), stringArg(args, 1), stringArg(args, 2));
            return Undefined.instance;
        });
        bind(object, "addState", args -> {
            api.addState(stringArg(args, 0), stringArg(args, 1), numberArg(args, 2));
            return Undefined.instance;
        });
        bind(object, "runTrigger", args -> {
            api.runTrigger(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "teleport", args -> {
            api.teleport(numberArg(args, 0), numberArg(args, 1), numberArg(args, 2));
            return Undefined.instance;
        });
        bind(object, "log", args -> {
            api.log(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "removeState", args -> {
            api.removeState(stringArg(args, 0), stringArg(args, 1));
            return Undefined.instance;
        });
        bind(object, "hasState", args -> api.hasState(stringArg(args, 0), stringArg(args, 1)));
        bind(object, "actionbar", args -> {
            api.actionbar(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "giveItem", args -> {
            api.giveItem(stringArg(args, 0), (int) numberArg(args, 1));
            return Undefined.instance;
        });
        bind(object, "playSound", args -> {
            api.playSound(stringArg(args, 0), numberArg(args, 1));
            return Undefined.instance;
        });
        bind(object, "getFaction", args -> api.getFaction(stringArg(args, 0)));
        bind(object, "addFaction", args -> {
            api.addFaction(stringArg(args, 0), numberArg(args, 1));
            return Undefined.instance;
        });
        bind(object, "setFaction", args -> {
            api.setFaction(stringArg(args, 0), numberArg(args, 1));
            return Undefined.instance;
        });
        bind(object, "getFactionAttitude", args -> api.getFactionAttitude(stringArg(args, 0)));
        bind(object, "giveQuest", args -> {
            api.giveQuest(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "completeQuest", args -> {
            api.completeQuest(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "spawnNpc", args -> {
            api.spawnNpc(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "dialogue", args -> {
            api.dialogue(stringArg(args, 0));
            return Undefined.instance;
        });
        bind(object, "delayTrigger", args -> {
            api.delayTrigger((int) numberArg(args, 0), stringArg(args, 1));
            return Undefined.instance;
        });

        return object;
    }

    private static void bind(ScriptableObject target, String name, java.util.function.Function<Object[], Object> handler)
    {
        target.put(name, target, new BaseFunction()
        {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
            {
                return handler.apply(args);
            }
        });
    }

    private static String stringArg(Object[] args, int index)
    {
        if (args == null || index >= args.length || args[index] == null || args[index] == Undefined.instance)
        {
            return "";
        }

        return Context.toString(args[index]);
    }

    private static double numberArg(Object[] args, int index)
    {
        if (args == null || index >= args.length || args[index] == null || args[index] == Undefined.instance)
        {
            return 0D;
        }

        if (args[index] instanceof Number number)
        {
            return number.doubleValue();
        }

        return Context.toNumber(args[index]);
    }
}
