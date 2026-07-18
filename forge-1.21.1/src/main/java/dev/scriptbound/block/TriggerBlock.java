package dev.scriptbound.block;

import dev.scriptbound.block.entity.TriggerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import dev.scriptbound.trigger.TriggerExecutor;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class TriggerBlock extends BaseEntityBlock
{
    public static final com.mojang.serialization.MapCodec<TriggerBlock> CODEC = simpleCodec(TriggerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }

    public TriggerBlock(Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TriggerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return null;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return true;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (level.isClientSide)
        {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        return fireTrigger(level, pos, player, true);
    }

  public static InteractionResult fireTrigger(Level level, BlockPos pos, Player player, boolean rightClick)
    {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
        {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof TriggerBlockEntity triggerBlockEntity))
        {
            return InteractionResult.PASS;
        }

        String triggerId = rightClick ? triggerBlockEntity.getRightTrigger() : triggerBlockEntity.getLeftTrigger();
        String side = rightClick ? "right" : "left";

        if (triggerId == null || triggerId.isBlank())
        {
            serverPlayer.sendSystemMessage(Component.literal("[SBE] No " + side + " trigger bound. Use /sbe trigger block set " + side + " <id>"));
            return InteractionResult.FAIL;
        }

        boolean ok = TriggerExecutor.runAtBlock(serverPlayer.server, triggerId, serverPlayer, serverLevel, pos);

        if (!ok)
        {
            serverPlayer.sendSystemMessage(Component.literal("[SBE] Trigger '" + triggerId + "' not found or empty."));
            return InteractionResult.FAIL;
        }

        return InteractionResult.SUCCESS;
    }
}
