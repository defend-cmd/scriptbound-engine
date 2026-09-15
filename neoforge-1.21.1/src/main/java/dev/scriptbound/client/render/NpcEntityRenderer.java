package dev.scriptbound.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.scriptbound.client.bbs.BbsFormBridge;
import dev.scriptbound.entity.NpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class NpcEntityRenderer extends EntityRenderer<NpcEntity>
{
    private final HumanoidMobRenderer<NpcEntity, HumanoidModel<NpcEntity>> fallback;
    private final Map<String, Object> formCache = new ConcurrentHashMap<>();
    private static class EntityCache
    {
        Object mcEntity;
        int lastTick = -1;
    }

    private final Map<Integer, EntityCache> entityCache = new ConcurrentHashMap<>();

    public NpcEntityRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.fallback = new HumanoidMobRenderer<>(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F)
        {
            @Override
            public ResourceLocation getTextureLocation(NpcEntity entity)
            {
                return net.minecraft.client.resources.DefaultPlayerSkin.get(entity.getUUID()).texture();
            }
        };
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(NpcEntity entity, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight)
    {
        if (tryRenderForm(entity, entityYaw, partialTick, stack, buffer, packedLight))
        {
            super.render(entity, entityYaw, partialTick, stack, buffer, packedLight);
            return;
        }

        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(180F - entityYaw));
        this.fallback.render(entity, entityYaw, partialTick, stack, buffer, packedLight);
        stack.popPose();
        super.render(entity, entityYaw, partialTick, stack, buffer, packedLight);
    }

    private boolean tryRenderForm(NpcEntity entity, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight)
    {
        if (!BbsFormBridge.available())
        {
            return false;
        }

        String formJson = entity.getFormJson();

        if (formJson == null || formJson.isBlank() || formJson.equals("{}") || !formJson.contains("bbs:"))
        {
            formJson = "{\"type\":\"bbs:block\",\"block\":\"minecraft:dirt\"}";
        }

        if (formJson == null || formJson.isBlank())
        {
            return false;
        }

        try
        {
            Object form = this.formCache.computeIfAbsent(formJson, key -> {
                try
                {
                    Object f = BbsFormBridge.parseForm(key);
                    dev.scriptbound.ScriptBoundMod.LOGGER.debug("Parsed BBS form successfully for NPC {}.", entity.getBlueprintId());
                    return f;
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalStateException(e);
                }
            });

            if (entity.isRemoved())
            {
                this.entityCache.remove(entity.getId());
                return false;
            }

            EntityCache cache = this.entityCache.computeIfAbsent(entity.getId(), id -> {
                EntityCache c = new EntityCache();
                try
                {
                    c.mcEntity = BbsFormBridge.createMcEntity(entity);
                    BbsFormBridge.bindEntityForm(c.mcEntity, form);
                }
                catch (ReflectiveOperationException e)
                {
                    dev.scriptbound.ScriptBoundMod.LOGGER.error("Failed to create MCEntity for NPC", e);
                }
                return c;
            });

            if (cache.mcEntity == null)
            {
                return false;
            }

            int currentTick = entity.tickCount;
            if (cache.lastTick != currentTick)
            {
                BbsFormBridge.updateForm(form, cache.mcEntity);
                cache.lastTick = currentTick;

                if (currentTick % 600 == 0)
                {
                    this.entityCache.keySet().removeIf(id -> {
                        net.minecraft.world.entity.Entity e = entity.level().getEntity(id);
                        return e == null || e.isRemoved();
                    });
                }
            }

            stack.pushPose();
            float bodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

            stack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

            int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(entity, 0F);

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();

            if (buffer instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
                bs.endBatch();
            }

            dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] Attempting to render BBS form for NPC. JSON Length: {}", formJson.length());
            BbsFormBridge.renderForm(form, cache.mcEntity, stack, packedLight, overlay, partialTick);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            stack.popPose();
            return true;
        }
        catch (Exception e)
        {
            dev.scriptbound.ScriptBoundMod.LOGGER.error("Failed to render BBS form for NPC '{}'. Falling back to vanilla.", entity.getBlueprintId(), e);
            this.formCache.remove(formJson);
            return false;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity)
    {
        return net.minecraft.client.resources.DefaultPlayerSkin.get(entity.getUUID()).texture();
    }
}
