package dev.scriptbound.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MovingBlockEntity extends Entity
{
    private static final EntityDataAccessor<BlockState> DATA_BLOCK =
        SynchedEntityData.defineId(MovingBlockEntity.class, EntityDataSerializers.BLOCK_STATE);

    private Vec3 startPos = Vec3.ZERO;
    private Vec3 endPos = Vec3.ZERO;
    private Vec3 lastPos = Vec3.ZERO;
    private BlockPos sourcePos = BlockPos.ZERO;
    private BlockPos targetPos = BlockPos.ZERO;
    private int durationTicks = 40;
    private int elapsedTicks;
    private String easing = "smooth";
    private boolean finished;

    public MovingBlockEntity(EntityType<? extends MovingBlockEntity> type, Level level)
    {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    public static MovingBlockEntity start(
        ServerLevel level,
        BlockPos from,
        BlockPos to,
        int ticks,
        String easing
    )
    {
        BlockState state = level.getBlockState(from);

        if (state.isAir() || state.is(Blocks.BEDROCK))
        {
            return null;
        }

        level.setBlock(from, Blocks.AIR.defaultBlockState(), 3);

        MovingBlockEntity entity = new MovingBlockEntity(dev.scriptbound.registry.ModEntities.MOVING_BLOCK.get(), level);
        entity.setBlockState(state);
        entity.sourcePos = from.immutable();
        entity.targetPos = to.immutable();
        entity.startPos = Vec3.atLowerCornerOf(from);
        entity.endPos = Vec3.atLowerCornerOf(to);
        entity.lastPos = entity.startPos;
        entity.durationTicks = Math.max(1, ticks);
        entity.easing = easing == null || easing.isBlank() ? "smooth" : easing.toLowerCase();
        entity.setPos(entity.startPos.x, entity.startPos.y, entity.startPos.z);
        entity.refreshDimensions();
        level.addFreshEntity(entity);
        return entity;
    }

    public BlockState getBlockState()
    {
        return this.entityData.get(DATA_BLOCK);
    }

    public void setBlockState(BlockState state)
    {
        this.entityData.set(DATA_BLOCK, state);
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(DATA_BLOCK, Blocks.STONE.defaultBlockState());
    }

    @Override
    public boolean canBeCollidedWith()
    {
        return !this.finished;
    }

    @Override
    protected AABB makeBoundingBox()
    {
        return new AABB(this.getX(), this.getY(), this.getZ(), this.getX() + 1.0D, this.getY() + 1.0D, this.getZ() + 1.0D);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose)
    {
        return net.minecraft.world.entity.EntityDimensions.fixed(1.0F, 1.0F);
    }

    @Override
    public boolean isPickable()
    {
        return !this.finished;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public void push(Entity entity)
    {
    }

    @Override
    public void tick()
    {
        super.tick();

        if (this.level().isClientSide)
        {
            return;
        }

        if (this.finished)
        {
            return;
        }

        Vec3 before = position();
        this.elapsedTicks++;
        float t = Mth.clamp(this.elapsedTicks / (float) this.durationTicks, 0F, 1F);
        float eased = applyEasing(t, this.easing);
        Vec3 pos = this.startPos.lerp(this.endPos, eased);
        Vec3 delta = pos.subtract(before);

        applyMotionToEntities(delta);

        setPos(pos.x, pos.y, pos.z);

        this.lastPos = position();

        if (this.elapsedTicks >= this.durationTicks)
        {
            finishMove();
        }
    }

    private void applyMotionToEntities(Vec3 delta)
    {
        if (delta.lengthSqr() < 1.0E-10)
        {
            return;
        }

        AABB currentBounds = getBoundingBox();
        AABB nextBounds = currentBounds.move(delta);
        AABB sweep = currentBounds.minmax(nextBounds).inflate(0.1D, 0.35D, 0.1D);

        for (Entity entity : this.level().getEntities(this, sweep, e -> !e.isSpectator() && e.isAlive() && e.isPushable()))
        {
            if (entity == this)
            {
                continue;
            }

            AABB entityBounds = entity.getBoundingBox();

            boolean onTop = entityBounds.minY >= currentBounds.maxY - 0.15D - Math.max(0, delta.y);
            boolean inColumn = entityBounds.maxX > currentBounds.minX - 0.1D
                            && entityBounds.minX < currentBounds.maxX + 0.1D
                            && entityBounds.maxZ > currentBounds.minZ - 0.1D
                            && entityBounds.minZ < currentBounds.maxZ + 0.1D;

            if (onTop && inColumn)
            {
                entity.move(MoverType.SHULKER_BOX, delta);
                entity.resetFallDistance();
                entity.setOnGround(true);
            }
            else if (nextBounds.intersects(entityBounds) || currentBounds.intersects(entityBounds))
            {
                Vec3 pushVec = calculatePushVector(entityBounds, nextBounds, delta);
                if (pushVec.lengthSqr() > 0)
                {
                    entity.move(MoverType.SHULKER_BOX, pushVec);
                }
            }
        }
    }

    private Vec3 calculatePushVector(AABB entityBounds, AABB blockBounds, Vec3 delta)
    {
        double pushX = 0;
        double pushY = 0;
        double pushZ = 0;
        double epsilon = 1.0E-5;

        if (delta.x > epsilon && entityBounds.minX < blockBounds.maxX)
        {
            pushX = blockBounds.maxX - entityBounds.minX;
        }
        else if (delta.x < -epsilon && entityBounds.maxX > blockBounds.minX)
        {
            pushX = blockBounds.minX - entityBounds.maxX;
        }

        if (delta.y > epsilon && entityBounds.minY < blockBounds.maxY)
        {
            pushY = blockBounds.maxY - entityBounds.minY;
        }
        else if (delta.y < -epsilon && entityBounds.maxY > blockBounds.minY)
        {
            pushY = blockBounds.minY - entityBounds.maxY;
        }

        if (delta.z > epsilon && entityBounds.minZ < blockBounds.maxZ)
        {
            pushZ = blockBounds.maxZ - entityBounds.minZ;
        }
        else if (delta.z < -epsilon && entityBounds.maxZ > blockBounds.minZ)
        {
            pushZ = blockBounds.minZ - entityBounds.maxZ;
        }

        if (pushX > 0) pushX += 1.0E-4;
        else if (pushX < 0) pushX -= 1.0E-4;

        if (pushY > 0) pushY += 1.0E-4;
        else if (pushY < 0) pushY -= 1.0E-4;

        if (pushZ > 0) pushZ += 1.0E-4;
        else if (pushZ < 0) pushZ -= 1.0E-4;

        if (pushX == 0 && pushY == 0 && pushZ == 0)
        {
            return delta;
        }

        return new Vec3(pushX, pushY, pushZ);
    }

    private boolean isStandingOnTop(Entity entity)
    {
        double top = getY() + 1.0D;
        double feet = entity.getY();
        double epsilon = 0.35D;

        if (feet < top - 0.15D || feet > top + epsilon)
        {
            return false;
        }

        AABB platform = getBoundingBox();
        AABB rider = entity.getBoundingBox();

        return rider.maxX > platform.minX - 0.05D
            && rider.minX < platform.maxX + 0.05D
            && rider.maxZ > platform.minZ - 0.05D
            && rider.minZ < platform.maxZ + 0.05D;
    }

    private void finishMove()
    {
        this.finished = true;
        BlockState state = getBlockState();
        Vec3 finalDelta = this.endPos.subtract(position());

        if (finalDelta.lengthSqr() > 1.0E-10)
        {
            applyMotionToEntities(finalDelta);
            setPos(this.endPos.x, this.endPos.y, this.endPos.z);
            refreshDimensions();
        }

        BlockPos place = this.targetPos;

        if (!this.level().getBlockState(this.targetPos).canBeReplaced())
        {
            place = this.sourcePos;
        }

        this.level().setBlock(place, state, 3);
        discard();
    }

    @Override
    public void remove(Entity.RemovalReason reason)
    {
        if (!this.level().isClientSide && !this.finished && reason != Entity.RemovalReason.DISCARDED)
        {
            restoreSource();
        }

        super.remove(reason);
    }

    private void restoreSource()
    {
        if (!this.level().getBlockState(this.sourcePos).isAir())
        {
            return;
        }

        this.level().setBlock(this.sourcePos, getBlockState(), 3);
    }

    public static float applyEasing(float t, String easing)
    {
        return switch (easing)
        {
            case "linear", "sharp" -> t;
            case "snappy" -> t < 0.5F ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            default -> t * t * (3 - 2 * t);
        };
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        if (tag.contains("block"))
        {
            BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(
                BuiltInRegistries.BLOCK.asLookup(),
                tag.getCompound("block")
            );
            setBlockState(state);
        }

        this.sourcePos = new BlockPos(tag.getInt("sx"), tag.getInt("sy"), tag.getInt("sz"));
        this.targetPos = new BlockPos(tag.getInt("tx"), tag.getInt("ty"), tag.getInt("tz"));
        this.durationTicks = tag.getInt("duration");
        this.elapsedTicks = tag.getInt("elapsed");
        this.easing = tag.getString("easing");
        this.startPos = Vec3.atLowerCornerOf(this.sourcePos);
        this.endPos = Vec3.atLowerCornerOf(this.targetPos);
        this.lastPos = this.startPos;
        this.finished = tag.getBoolean("finished");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.put("block", net.minecraft.nbt.NbtUtils.writeBlockState(getBlockState()));
        tag.putInt("sx", this.sourcePos.getX());
        tag.putInt("sy", this.sourcePos.getY());
        tag.putInt("sz", this.sourcePos.getZ());
        tag.putInt("tx", this.targetPos.getX());
        tag.putInt("ty", this.targetPos.getY());
        tag.putInt("tz", this.targetPos.getZ());
        tag.putInt("duration", this.durationTicks);
        tag.putInt("elapsed", this.elapsedTicks);
        tag.putString("easing", this.easing);
        tag.putBoolean("finished", this.finished);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return new ClientboundAddEntityPacket(this);
    }
}
