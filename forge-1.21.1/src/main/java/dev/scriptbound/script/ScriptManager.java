package dev.scriptbound.script;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ScriptManager
{
    public record Result(boolean success, String message) {}

    private ScriptManager() {}

    public static Result run(MinecraftServer server, String scriptId, ServerPlayer player, String functionName)
    {
        var file = ScriptBoundPaths.scriptFile(server, scriptId);

        if (!Files.isRegularFile(file))
        {
            return new Result(false, "Script file not found: scriptbound/scripts/" + scriptId + ".js");
        }

        Context context = Context.enter();

        try
        {
            context.setOptimizationLevel(-1);
            Scriptable scope = context.initStandardObjects();
            Scriptable api = ScriptApiFactory.create(context, scope, player);
            scope.put("c", scope, api);

            String source = Files.readString(file, StandardCharsets.UTF_8);
            context.evaluateString(scope, source, scriptId + ".js", 1, null);

            String fn = functionName == null || functionName.isBlank() ? "main" : functionName;
            Object callable = scope.get(fn, scope);

            if (!(callable instanceof Function function))
            {
                return new Result(false, "Function '" + fn + "' not found in script '" + scriptId + "'");
            }

            function.call(context, scope, scope, Context.emptyArgs);
            return new Result(true, "Script '" + scriptId + "' executed.");
        }
        catch (Exception e)
        {
            ScriptBoundMod.LOGGER.error("Script '{}' failed for {}", scriptId, player.getGameProfile().getName(), e);
            return new Result(false, "Script error: " + e.getMessage());
        }
        finally
        {
            Context.exit();
        }
    }
}
