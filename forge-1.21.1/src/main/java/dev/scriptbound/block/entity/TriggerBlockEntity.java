package dev.scriptbound.block.entity;

import dev.scriptbound.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TriggerBlockEntity extends BlockEntity
{
    private String leftTrigger = "";
    private String rightTrigger = "";

    public TriggerBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.TRIGGER_BLOCK.get(), pos, state);
    }

    public String getLeftTrigger()
    {
        return this.leftTrigger;
    }

    public String getRightTrigger()
    {
        return this.rightTrigger;
    }

    public void setLeftTrigger(String leftTrigger)
    {
        this.leftTrigger = leftTrigger == null ? "" : leftTrigger;
        this.setChanged();
    }

    public void setRightTrigger(String rightTrigger)
    {
        this.rightTrigger = rightTrigger == null ? "" : rightTrigger;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.putString("left", this.leftTrigger);
        tag.putString("right", this.rightTrigger);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        this.leftTrigger = tag.getString("left");
        this.rightTrigger = tag.getString("right");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider)
    {
        return this.saveWithoutMetadata(provider);
    }
}
