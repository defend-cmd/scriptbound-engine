package dev.scriptbound.compat;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.entity.NpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class BBSCompat
{
    private static final boolean LOADED = ModList.get().isLoaded("bbs") || ModList.get().isLoaded("bbs_mod");

    private BBSCompat() {}

    public static boolean isLoaded()
    {
        return LOADED;
    }

    public static void applyForm(NpcEntity npc, String formJson)
    {
        if (formJson == null || formJson.isBlank())
        {
            return;
        }

        npc.setFormJson(formJson);

        if (LOADED)
        {
            ScriptBoundMod.LOGGER.debug("Applied BBS form to NPC '{}'.", npc.getBlueprintId());
        }
    }

    public static void playFilm(ServerPlayer player, String filmId)
    {
        if (!LOADED || filmId == null || filmId.isBlank())
        {
            return;
        }

        try
        {
            Class<?> bbsMod = Class.forName("mchorse.bbs_mod.BBSMod");
            Object films = bbsMod.getMethod("getFilms").invoke(null);
            films.getClass().getMethod("play", ServerPlayer.class, String.class).invoke(films, player, filmId);
        }
        catch (Exception e)
        {
            ScriptBoundMod.LOGGER.warn("Could not play BBS film '{}': {}", filmId, e.getMessage());
        }
    }
}
