package dev.scriptbound;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public final class ScriptBoundPaths
{
    public static final String ROOT = "scriptbound";

    private ScriptBoundPaths() {}

    public static Path root(MinecraftServer server)
    {
        return server.getWorldPath(LevelResource.ROOT).resolve(ROOT);
    }

    public static Path statesDir(MinecraftServer server)
    {
        return root(server).resolve("states");
    }

    public static Path globalStatesFile(MinecraftServer server)
    {
        return statesDir(server).resolve("global.json");
    }

    public static Path playerStatesDir(MinecraftServer server)
    {
        return statesDir(server).resolve("players");
    }

    public static Path playerStatesFile(MinecraftServer server, java.util.UUID playerId)
    {
        return playerStatesDir(server).resolve(playerId + ".json");
    }

    public static Path scriptsDir(MinecraftServer server)
    {
        return root(server).resolve("scripts");
    }

    public static Path triggersDir(MinecraftServer server)
    {
        return root(server).resolve("triggers");
    }

    public static Path triggerFile(MinecraftServer server, String id)
    {
        return triggersDir(server).resolve(id + ".json");
    }

    public static Path settingsFile(MinecraftServer server)
    {
        return root(server).resolve("settings.json");
    }

    public static Path npcsDir(MinecraftServer server)
    {
        return root(server).resolve("npcs");
    }

    public static Path npcFile(MinecraftServer server, String id)
    {
        return npcsDir(server).resolve(id + ".json");
    }

    public static Path dialoguesDir(MinecraftServer server)
    {
        return root(server).resolve("dialogues");
    }

    public static Path dialogueFile(MinecraftServer server, String id)
    {
        return dialoguesDir(server).resolve(id + ".json");
    }

    public static Path questsDir(MinecraftServer server)
    {
        return root(server).resolve("quests");
    }

    public static Path questFile(MinecraftServer server, String id)
    {
        return questsDir(server).resolve(id + ".json");
    }

    public static Path scriptFile(MinecraftServer server, String id)
    {
        return scriptsDir(server).resolve(id + ".js");
    }

    public static Path flowsDir(MinecraftServer server)
    {
        return root(server).resolve("flows");
    }

    public static Path flowFile(MinecraftServer server, String id)
    {
        return flowsDir(server).resolve(id + ".json");
    }

    public static Path factionsDir(MinecraftServer server)
    {
        return root(server).resolve("factions");
    }

    public static Path factionFile(MinecraftServer server, String id)
    {
        return factionsDir(server).resolve(id + ".json");
    }
}
