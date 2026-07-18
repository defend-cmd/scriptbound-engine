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

        Method render = Class.forName(FORM_UTILS_CLIENT).getMethod(
            "render",
            Class.forName("mchorse.bbs_mod.forms.forms.Form"),
            contextClass
        );
        render.invoke(null, form, context);
    }

    public static void updateForm(Object form, Object mcEntity) throws ReflectiveOperationException
    {
        Method update = Class.forName("mchorse.bbs_mod.forms.forms.Form").getMethod("update", Class.forName("mchorse.bbs_mod.forms.entities.IEntity"));
        update.invoke(form, mcEntity);
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

            return Class.forName(STRING_TYPE).getConstructor(String.class).newInstance(primitive.getAsString());
        }

        throw new IllegalArgumentException("Unsupported JSON element in form.");
    }
}
