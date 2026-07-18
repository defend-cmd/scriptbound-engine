package dev.scriptbound.client.bbs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.scriptbound.compat.BBSCompat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class BbsFormBridge
{
    private static final String FORM_UTILS = "mchorse.bbs_mod.forms.FormUtils";
    private static final String FORM_UTILS_CLIENT = "mchorse.bbs_mod.forms.FormUtilsClient";
    private static final String MAP_TYPE = "mchorse.bbs_mod.data.types.MapType";
    private static final String BASE_TYPE = "mchorse.bbs_mod.data.types.BaseType";
    private static final String STRING_TYPE = "mchorse.bbs_mod.data.types.StringType";
    private static final String DOUBLE_TYPE = "mchorse.bbs_mod.data.types.DoubleType";
    private static final String FLOAT_TYPE = "mchorse.bbs_mod.data.types.FloatType";
    private static final String INT_TYPE = "mchorse.bbs_mod.data.types.IntType";
    private static final String BOOL_TYPE = "mchorse.bbs_mod.data.types.ByteType";
    private static final String LIST_TYPE = "mchorse.bbs_mod.data.types.ListType";
    private static final String SHORT_TYPE = "mchorse.bbs_mod.data.types.ShortType";
    private static final String LONG_TYPE = "mchorse.bbs_mod.data.types.LongType";

    private BbsFormBridge() {}

    public static Object parseForm(String formJson) throws ReflectiveOperationException
    {
        Object mapType = jsonToMapType(JsonParser.parseString(formJson));
        Method fromData = Class.forName(FORM_UTILS).getMethod("fromData", Class.forName(BASE_TYPE));
        return fromData.invoke(null, mapType);
    }

    public static String serializeForm(Object form) throws ReflectiveOperationException
    {
        Method toData = Class.forName(FORM_UTILS).getMethod("toData", Class.forName("mchorse.bbs_mod.forms.forms.Form"));
        Object mapType = toData.invoke(null, form);
        Method toString = Class.forName("mchorse.bbs_mod.data.DataToString").getMethod("toString", Class.forName(BASE_TYPE));
        return (String) toString.invoke(null, mapType);
    }

    public static void openMorphSelector(net.minecraft.client.gui.screens.Screen parent, Object currentForm, java.util.function.Consumer<Object> onSelected)
    {
        try
        {
            Class<?> bridge = Class.forName("dev.scriptbound.client.bbs.BBSMorphBridge");
            Method open = bridge.getMethod("openMorphMenu", net.minecraft.client.gui.screens.Screen.class, Class.forName("mchorse.bbs_mod.forms.forms.Form"), java.util.function.Consumer.class);
            open.invoke(null, parent, currentForm, onSelected);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Object createMcEntity(net.minecraft.world.entity.Entity entity) throws ReflectiveOperationException
    {
        Class<?> mcEntityClass = Class.forName("mchorse.bbs_mod.forms.entities.MCEntity");
        return mcEntityClass.getConstructor(net.minecraft.world.entity.Entity.class).newInstance(entity);
    }

    public static void renderForm(Object form, Object mcEntity, com.mojang.blaze3d.vertex.PoseStack stack, int light, int overlay, float partialTick)
        throws ReflectiveOperationException
    {
        ensureModelLoaded(form);

        Class<?> contextClass = Class.forName("mchorse.bbs_mod.forms.renderers.FormRenderingContext");
        Object context = contextClass.getConstructor().newInstance();
        Class<?> renderTypeClass = Class.forName("mchorse.bbs_mod.forms.renderers.FormRenderType");
        Object entityType = renderTypeClass.getField("ENTITY").get(null);

        Method set = contextClass.getMethod(
            "set",
            renderTypeClass,
            Class.forName("mchorse.bbs_mod.forms.entities.IEntity"),
            com.mojang.blaze3d.vertex.PoseStack.class,
            int.class,
            int.class,
            float.class
        );
        set.invoke(context, entityType, mcEntity, stack, light, overlay, partialTick);

        net.minecraft.client.Camera camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        contextClass.getMethod("camera", net.minecraft.client.Camera.class).invoke(context, camera);

        Class<?> formUtilsClient = Class.forName(FORM_UTILS_CLIENT);

        Class<?> formClass = Class.forName("mchorse.bbs_mod.forms.forms.Form");
        Object renderer = formUtilsClient.getMethod("getRenderer", formClass).invoke(null, form);

        if (renderer == null)
        {
            dev.scriptbound.ScriptBoundMod.LOGGER.warn("BBS getRenderer returned null for form {}", form.getClass().getName());
            return;
        }

        java.lang.reflect.Field currentFormField = formUtilsClient.getDeclaredField("currentForm");
        currentFormField.setAccessible(true);
        java.util.Stack<Object> currentForm = (java.util.Stack<Object>) currentFormField.get(null);

        currentForm.push(form);

        try
        {
            renderer.getClass().getMethod("render", contextClass).invoke(renderer, context);
        }
        finally
        {
            currentForm.pop();
        }

        Object provider = formUtilsClient.getMethod("getProvider").invoke(null);

        if (provider instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource)
        {
            bufferSource.endBatch();
        }
    }

    private static final java.util.Set<String> PRELOADED_MODELS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void ensureModelLoaded(Object form)
    {
        java.lang.reflect.Field modelField;

        try
        {
            modelField = form.getClass().getField("model");
        }
        catch (NoSuchFieldException notAModelForm)
        {
            return;
        }

        try
        {
            Object valueString = modelField.get(form);
            Object keyObj = valueString.getClass().getMethod("get").invoke(valueString);
            String key = keyObj == null ? "" : keyObj.toString();

            if (key.isBlank() || !PRELOADED_MODELS.add(key))
            {
                return;
            }

            Object manager = Class.forName("mchorse.bbs_mod.BBSModClient").getMethod("getModels").invoke(null);
            manager.getClass().getMethod("loadModel", String.class).invoke(manager, key);
            dev.scriptbound.ScriptBoundMod.LOGGER.debug("Preloaded cubic model '{}' synchronously on the render thread.", key);
        }
        catch (Throwable t)
        {
            dev.scriptbound.ScriptBoundMod.LOGGER.error("Failed to preload BBS cubic model for form", t);
        }
    }

    public static void updateForm(Object form, Object mcEntity) throws ReflectiveOperationException
    {

        mcEntity.getClass().getMethod("update").invoke(mcEntity);

        Method update = Class.forName("mchorse.bbs_mod.forms.forms.Form").getMethod("update", Class.forName("mchorse.bbs_mod.forms.entities.IEntity"));
        update.invoke(form, mcEntity);
    }

    public static void bindEntityForm(Object mcEntity, Object form) throws ReflectiveOperationException
    {
        Class<?> formClass = Class.forName("mchorse.bbs_mod.forms.forms.Form");
        mcEntity.getClass().getMethod("setForm", formClass).invoke(mcEntity, form);
    }

    public static boolean available()
    {
        return BBSCompat.isLoaded();
    }

    private static Object jsonToMapType(JsonElement element) throws ReflectiveOperationException
    {
        if (element.isJsonObject())
        {
            Object map = Class.forName(MAP_TYPE).getConstructor().newInstance();
            Method put = Class.forName(MAP_TYPE).getMethod("put", String.class, Class.forName(BASE_TYPE));

            for (var entry : element.getAsJsonObject().entrySet())
            {
                put.invoke(map, entry.getKey(), jsonToBaseType(entry.getValue()));
            }

            return map;
        }

        throw new IllegalArgumentException("Form JSON root must be an object.");
    }

    private static Object jsonToBaseType(JsonElement element) throws ReflectiveOperationException
    {
        if (element.isJsonObject())
        {
            return jsonToMapType(element);
        }

        if (element.isJsonArray())
        {
            Constructor<?> ctor = Class.forName(LIST_TYPE).getConstructor();
            Object list = ctor.newInstance();
            Method add = Class.forName(LIST_TYPE).getMethod("add", Class.forName(BASE_TYPE));

            for (JsonElement child : element.getAsJsonArray())
            {
                add.invoke(list, jsonToBaseType(child));
            }

            return list;
        }

        if (element.isJsonPrimitive())
        {
            var primitive = element.getAsJsonPrimitive();

            if (primitive.isBoolean())
            {
                return Class.forName(BOOL_TYPE).getConstructor(boolean.class).newInstance(primitive.getAsBoolean());
            }

            if (primitive.isNumber())
            {
                Number number = primitive.getAsNumber();

                if (number instanceof Integer || number.longValue() == number.intValue())
                {
                    return Class.forName(INT_TYPE).getConstructor(int.class).newInstance(number.intValue());
                }

                if (number instanceof Float || number.doubleValue() == number.floatValue())
                {
                    return Class.forName(FLOAT_TYPE).getConstructor(float.class).newInstance(number.floatValue());
                }

                return Class.forName(DOUBLE_TYPE).getConstructor(double.class).newInstance(number.doubleValue());
            }

            String raw = primitive.getAsString();
            Object typed = decodeSuffixedNumber(raw);

            if (typed != null)
            {
                return typed;
            }

            return Class.forName(STRING_TYPE).getConstructor(String.class).newInstance(raw);
        }

        throw new IllegalArgumentException("Unsupported JSON element in form.");
    }

    private static Object decodeSuffixedNumber(String value) throws ReflectiveOperationException
    {
        if (value.length() < 2)
        {
            return null;
        }

        char suffix = value.charAt(value.length() - 1);
        String body = value.substring(0, value.length() - 1);

        boolean integral = body.matches("-?\\d+");
        boolean decimal = body.matches("-?\\d+\\.\\d+");

        if (!integral && !decimal)
        {
            return null;
        }

        try
        {
            switch (suffix)
            {
                case 'b': case 'B':
                    if (!integral) return null;
                    return Class.forName(BOOL_TYPE).getConstructor(byte.class).newInstance((byte) Integer.parseInt(body));
                case 's': case 'S':
                    if (!integral) return null;
                    return Class.forName(SHORT_TYPE).getConstructor(short.class).newInstance((short) Integer.parseInt(body));
                case 'l': case 'L':
                    if (!integral) return null;
                    return Class.forName(LONG_TYPE).getConstructor(long.class).newInstance(Long.parseLong(body));
                case 'f': case 'F':
                    return Class.forName(FLOAT_TYPE).getConstructor(float.class).newInstance(Float.parseFloat(body));
                case 'd': case 'D':
                    return Class.forName(DOUBLE_TYPE).getConstructor(double.class).newInstance(Double.parseDouble(body));
                default:
                    return null;
            }
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }
}
