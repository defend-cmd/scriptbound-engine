package dev.scriptbound.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.scriptbound.ScriptBoundMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonFiles
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private JsonFiles() {}

    public static JsonObject readObject(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            return new JsonObject();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
        catch (Exception e)
        {
            ScriptBoundMod.LOGGER.error("Failed to read JSON file {}", path, e);
            return new JsonObject();
        }
    }

    public static void writeObject(Path path, JsonObject object)
    {
        try
        {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
            {
                GSON.toJson(object, writer);
            }
        }
        catch (IOException e)
        {
            ScriptBoundMod.LOGGER.error("Failed to write JSON file {}", path, e);
        }
    }
}
