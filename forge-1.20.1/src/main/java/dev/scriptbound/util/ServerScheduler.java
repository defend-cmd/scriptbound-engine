package dev.scriptbound.util;

import dev.scriptbound.ScriptBoundMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class ServerScheduler
{
    private static final class Task
    {
        long runAtTick;
        Runnable action;
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long currentTick;

    private ServerScheduler() {}

    public static synchronized void schedule(int delayTicks, Runnable action)
    {
        Task task = new Task();
        task.runAtTick = currentTick + Math.max(1, delayTicks);
        task.action = action;
        TASKS.add(task);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        List<Runnable> due = null;

        synchronized (ServerScheduler.class)
        {
            currentTick++;
            Iterator<Task> iterator = TASKS.iterator();

            while (iterator.hasNext())
            {
                Task task = iterator.next();

                if (task.runAtTick <= currentTick)
                {
                    if (due == null)
                    {
                        due = new ArrayList<>();
                    }

                    due.add(task.action);
                    iterator.remove();
                }
            }
        }

        if (due != null)
        {
            for (Runnable action : due)
            {
                try
                {
                    action.run();
                }
                catch (RuntimeException e)
                {
                    ScriptBoundMod.LOGGER.error("Scheduled task failed", e);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        synchronized (ServerScheduler.class)
        {
            TASKS.clear();
            currentTick = 0;
        }
    }
}
