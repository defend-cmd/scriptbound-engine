package dev.scriptbound.entity;

import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.dialogue.DialogueManager;
import dev.scriptbound.npc.NpcBlueprint;
import dev.scriptbound.trigger.TriggerContext;
import dev.scriptbound.trigger.TriggerExecutor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NpcEntity extends Mob
{
    private static final EntityDataAccessor<String> BLUEPRINT_ID = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> INTERACT_TRIGGER = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ON_DEATH_TRIGGER = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FORM_JSON = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DIALOGUE_ID = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    private boolean invulnerableNpc = true;
    private final java.util.List<net.minecraft.core.BlockPos> patrolPoints = new java.util.ArrayList<>();

    public NpcEntity(EntityType<? extends Mob> type, Level level)
    {
        super(type, level);
    }

    public java.util.List<net.minecraft.core.BlockPos> getPatrolPoints()
    {
        return this.patrolPoints;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20D)
            .add(Attributes.MOVEMENT_SPEED, 0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(BLUEPRINT_ID, "");
        this.entityData.define(INTERACT_TRIGGER, "");
        this.entityData.define(ON_DEATH_TRIGGER, "");
        this.entityData.define(FORM_JSON, "");
        this.entityData.define(DIALOGUE_ID, "");
    }

    public void applyBlueprint(NpcBlueprint blueprint)
    {
        this.entityData.set(BLUEPRINT_ID, blueprint.id());
        this.entityData.set(INTERACT_TRIGGER, blueprint.onInteract());
        this.entityData.set(ON_DEATH_TRIGGER, blueprint.onDeath() != null ? blueprint.onDeath() : "");
        this.entityData.set(DIALOGUE_ID, blueprint.dialogue());
        this.setFormJson(blueprint.formJson());
        this.setCustomName(Component.literal(blueprint.displayName()));
        this.setCustomNameVisible(true);
        this.invulnerableNpc = blueprint.invulnerable();

        if (this.getAttribute(Attributes.MAX_HEALTH) != null)
        {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(blueprint.health());
            this.setHealth((float) blueprint.health());
        }

        this.patrolPoints.clear();
        this.patrolPoints.addAll(blueprint.patrol());

        double speed = blueprint.speed() > 0 ? blueprint.speed() : (this.patrolPoints.isEmpty() ? 0D : 0.25D);

        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null)
        {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }

        this.setPersistenceRequired();
    }

    public String getBlueprintId()
    {
        return this.entityData.get(BLUEPRINT_ID);
    }

    public String getFormJson()
    {
        return this.entityData.get(FORM_JSON);
    }

    public void setFormJson(String formJson)
    {
        this.entityData.set(FORM_JSON, formJson == null ? "" : formJson);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new NpcPatrolGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public void handlePlayerInteract(ServerPlayer player)
    {
        markTalked(player);

        String trigger = this.entityData.get(INTERACT_TRIGGER);
        String dialogue = this.entityData.get(DIALOGUE_ID);

        String blueprintId = this.getBlueprintId();

        if (!blueprintId.isBlank())
        {
            NpcBlueprint blueprint = dev.scriptbound.npc.NpcManager.get(player.server, blueprintId);

            if (!blueprint.onInteract().isBlank() || !blueprint.dialogue().isBlank())
            {
                trigger = blueprint.onInteract();
                dialogue = blueprint.dialogue();
                this.entityData.set(INTERACT_TRIGGER, trigger);
                this.entityData.set(DIALOGUE_ID, dialogue);
            }

            if (!blueprint.faction().isBlank()
                && dev.scriptbound.faction.FactionManager.attitude(player, blueprint.faction())
                    == dev.scriptbound.faction.FactionManager.Attitude.HOSTILE)
            {
                if (!blueprint.onHostileInteract().isBlank())
                {
                    TriggerExecutor.run(
                        player.server,
                        blueprint.onHostileInteract(),
                        new TriggerContext(player, player.serverLevel(), null, null, this, 0)
                    );
                }
                else
                {
                    player.sendSystemMessage(Component.literal(
                        this.getCustomName() != null
                            ? this.getCustomName().getString() + " refuses to talk to you."
                            : "This NPC refuses to talk to you."
                    ));
                }

                return;
            }
        }

        if (trigger != null && !trigger.isBlank())
        {
            TriggerExecutor.run(
                player.server,
                trigger,
                new TriggerContext(player, player.serverLevel(), null, null, this, 0)
            );
            return;
        }

        if (dialogue != null && !dialogue.isBlank())
        {
            DialogueManager.open(player, dialogue);
            return;
        }

        player.sendSystemMessage(Component.literal("NPC '" + this.getBlueprintId() + "' has no interact trigger or dialogue."));
    }

    private void markTalked(ServerPlayer player)
    {
        if (this.getBlueprintId().isBlank())
        {
            return;
        }

        StateService.set(
            StateService.playerTarget(player),
            "talked." + this.getBlueprintId(),
            new StateValue.NumberValue(1)
        );
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            this.handlePlayerInteract(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        return !this.invulnerableNpc && super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source)
    {
        super.die(source);

        if (!this.level().isClientSide)
        {
            String onDeath = this.entityData.get(ON_DEATH_TRIGGER);
            if (onDeath != null && !onDeath.isBlank())
            {
                ServerPlayer killer = null;
                if (source.getEntity() instanceof ServerPlayer player) {
                    killer = player;
                }

                TriggerExecutor.run(
                    this.getServer(),
                    onDeath,
                    new TriggerContext(killer, (net.minecraft.server.level.ServerLevel) this.level(), null, null, this, 0)
                );
            }
        }
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("blueprint", this.entityData.get(BLUEPRINT_ID));
        tag.putString("interact", this.entityData.get(INTERACT_TRIGGER));
        tag.putString("onDeath", this.entityData.get(ON_DEATH_TRIGGER));
        tag.putString("dialogue", this.entityData.get(DIALOGUE_ID));
        tag.putString("form", this.entityData.get(FORM_JSON));
        tag.putBoolean("invulnerable", this.invulnerableNpc);

        long[] patrol = new long[this.patrolPoints.size()];

        for (int i = 0; i < this.patrolPoints.size(); i++)
        {
            patrol[i] = this.patrolPoints.get(i).asLong();
        }

        tag.putLongArray("patrol", patrol);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.entityData.set(BLUEPRINT_ID, tag.getString("blueprint"));
        this.entityData.set(INTERACT_TRIGGER, tag.getString("interact"));
        this.entityData.set(ON_DEATH_TRIGGER, tag.getString("onDeath"));
        this.entityData.set(DIALOGUE_ID, tag.getString("dialogue"));
        this.entityData.set(FORM_JSON, tag.getString("form"));
        this.invulnerableNpc = tag.getBoolean("invulnerable");

        this.patrolPoints.clear();

        for (long packed : tag.getLongArray("patrol"))
        {
            this.patrolPoints.add(net.minecraft.core.BlockPos.of(packed));
        }
    }
}
