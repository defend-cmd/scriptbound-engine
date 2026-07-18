package dev.scriptbound.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.scriptbound.entity.MovingBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MovingBlockRenderer extends EntityRenderer<MovingBlockEntity>
{
    private final BlockRenderDispatcher blockRenderer;

    public MovingBlockRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.75F;
    }

    @Override
    public void render(MovingBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        BlockState state = entity.getBlockState();

        if (state.getRenderShape() == RenderShape.INVISIBLE)
        {
            return;
        }

        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        poseStack.pushPose();
        poseStack.translate(x - entity.getX(), y - entity.getY(), z - entity.getZ());
        this.blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MovingBlockEntity entity)
    {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png_atlas");
    }
}
