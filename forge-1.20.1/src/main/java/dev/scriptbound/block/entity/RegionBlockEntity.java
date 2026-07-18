package dev.scriptbound.block.entity;

import dev.scriptbound.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class RegionBlockEntity extends BlockEntity
{
    private int radiusX = 5;
    private int radiusY = 3;
    private int radiusZ = 5;
    private String onEnter = "";
    private String onExit = "";

    public RegionBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.REGION_BLOCK.get(), pos, state);
    }

    public AABB getBounds()
    {
        return new AABB(
            this.worldPosition.getX() - this.radiusX,
            this.worldPosition.getY() - this.radiusY,
            this.worldPosition.getZ() - this.radiusZ,
            this.worldPosition.getX() + this.radiusX + 1,
            this.worldPosition.getY() + this.radiusY + 1,
            this.worldPosition.getZ() + this.radiusZ + 1
        );
    }

    public String getOnEnter()
    {
        return this.onEnter;
    }

    public String getOnExit()
    {
        return this.onExit;
    }

    public void setOnEnter(String onEnter)
    {
        this.onEnter = onEnter == null ? "" : onEnter;
        this.setChanged();
    }

    public void setOnExit(String onExit)
    {
        this.onExit = onExit == null ? "" : onExit;
        this.setChanged();
    }

    public void setRadius(int x, int y, int z)
    {
        this.radiusX = x;
        this.radiusY = y;
        this.radiusZ = z;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("rx", this.radiusX);
        tag.putInt("ry", this.radiusY);
        tag.putInt("rz", this.radiusZ);
        tag.putString("enter", this.onEnter);
        tag.putString("exit", this.onExit);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.radiusX = tag.getInt("rx");
        this.radiusY = tag.getInt("ry");
        this.radiusZ = tag.getInt("rz");
        this.onEnter = tag.getString("enter");
        this.onExit = tag.getString("exit");
    }
}
