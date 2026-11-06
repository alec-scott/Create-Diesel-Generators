package com.jesz.createdieselgenerators.content.tools;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import net.minecraft.server.level.ServerLevel;

import com.jesz.createdieselgenerators.CDGEntityTypes;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.zurrtum.create.AllFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalFluidTags;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

public class ChemicalSprayerProjectileEntity extends AbstractHurtingProjectile {
    public FluidStack stack;
    public boolean fire;
    public boolean cooling;

    public ChemicalSprayerProjectileEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    // PORT-NOTE(26.1): FluidFX.getFluidParticle is client only; set from the client entrypoint
    public static java.util.function.Function<FluidStack, net.minecraft.core.particles.ParticleOptions> clientFluidParticle;

    int t = 0;
    public static ChemicalSprayerProjectileEntity spray(Level level, FluidStack stack, boolean fire, boolean cooling){
        ChemicalSprayerProjectileEntity projectile = new ChemicalSprayerProjectileEntity(CDGEntityTypes.CHEMICAL_SPRAYER_PROJECTILE.get(), level);
        projectile.stack = stack;
        projectile.fire = fire;
        projectile.cooling = cooling;
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("Fire", fire);
        tag.putBoolean("Cooling", cooling);
        if (!stack.isEmpty())
            tag.put("FluidStack", stack.toNbt(level.registryAccess()));

        projectile.getEntityData().set(DATA, tag);
        return projectile;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity owner = getOwner();

        if (fire) {
            hit.getEntity().setRemainingFireTicks((hit.getEntity().getRemainingFireTicks()) + 100);
            hit.getEntity().hurtOrSimulate(damageSources().inFire(), 2);
        } else if(cooling) {
            hit.getEntity().clearFire();
            if (hit.getEntity().getType() == EntityType.ENDERMAN)
                hit.getEntity().hurtOrSimulate(damageSources().generic(), 0.5f);
        }
        else if (stack.getFluid().isSame(AllFluids.POTION) && level() instanceof ServerLevel serverLevel) {
            if (hit.getEntity() instanceof LivingEntity le && le.isAffectedByPotions()) {
                PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
                if (potionContents != null)
                    for (MobEffectInstance effectInstance : potionContents.getAllEffects()){
                        MobEffect effect = effectInstance.getEffect().value();

                        if (effect.isInstantenous()) {
                            effect.applyInstantenousEffect(serverLevel, owner, owner, le, effectInstance.getAmplifier(), 0.5d);
                        } else {
                            le.addEffect(new MobEffectInstance(effectInstance), owner);
                        }
                    }
            }
        } else if (stack.isIn(ConventionalFluidTags.MILK)) {
            if (hit.getEntity() instanceof LivingEntity le && le.isAffectedByPotions())
                le.removeAllEffects();
        } else {
            if (owner instanceof LivingEntity)
                ((LivingEntity) owner).setLastHurtMob(hit.getEntity());
            hit.getEntity().hurtOrSimulate(damageSources().generic(), 0.5f);
        }
        super.onHitEntity(hit);
        remove(RemovalReason.DISCARDED);
    }

    // PORT-NOTE(26.1): was load/saveWithoutId(CompoundTag)
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (stack == null)
            stack = input.read("FluidStack", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (stack != null)
            output.store("FluidStack", FluidStack.OPTIONAL_CODEC, stack);
    }

    static final EntityDataAccessor<CompoundTag> DATA = SynchedEntityData.defineId(ChemicalSprayerProjectileEntity.class, EntityDataSerializers.COMPOUND_TAG);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Fire", fire);
        tag.putBoolean("Cooling", cooling);
        //tag.put("FluidStack", new CompoundTag());
        //stack.save(level().registryAccess(), tag.getCompound("FluidStack"));
        builder.define(DATA, tag);
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            stack = FluidStack.fromNbt(level().registryAccess(), getEntityData().get(DATA).getCompound("FluidStack"));
            fire = getEntityData().get(DATA).getBooleanOr("Fire", false);
            cooling = getEntityData().get(DATA).getBooleanOr("Cooling", false);
            if (stack != null && !stack.isEmpty() && !fire && clientFluidParticle != null)
                level().addParticle(clientFluidParticle.apply(stack), position().x+random.nextDouble()-0.5, position().y+0.3, position().z+random.nextDouble()-0.5, getDeltaMovement().x, getDeltaMovement().y - 0.1, getDeltaMovement().z);
            if (t >= 1) {
                if (fire) {
                    level().addParticle(ParticleTypes.LAVA, position().x, position().y, position().z, getDeltaMovement().x, getDeltaMovement().y - 0.1, getDeltaMovement().z);
                }
                t = 0;
            }
            else
                t++;
        }
        setDeltaMovement(getDeltaMovement().add(0, -0.015, 0));

        if (fire) {

            Fluid fluid = level().getFluidState(BlockPos.containing(position())).getType();
            boolean flammable = FuelType.getTypeFor(level().registryAccess().lookupOrThrow(CDGRegistries.FUEL_TYPE), fluid).normal().speed() != 0;

            if (flammable)
                level().explode(null, getX(), getY(), getZ(), 3, Level.ExplosionInteraction.BLOCK);
            else if (level().getFluidState(new BlockPos((int) getPosition(1).x, (int) getPosition(1).y, (int) getPosition(1).z)).is(Fluids.FLOWING_WATER) || level().getFluidState(new BlockPos((int) getPosition(1).x, (int) getPosition(1).y, (int) getPosition(1).z)).is(Fluids.WATER)) {
                fire = false;
                if(stack.getFluid().isSame(Fluids.LAVA))
                    remove(RemovalReason.DISCARDED);
                getEntityData().get(DATA).putBoolean("Fire", false);
            }
        }


        Entity entity = this.getOwner();
        if (this.level().isClientSide() || (entity == null || !entity.isRemoved()) && this.level().hasChunkAt(this.blockPosition())) {
            if (this.shouldBurn()) {
                this.setRemainingFireTicks(1);
            }

            HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitresult.getType() != HitResult.Type.MISS)
                this.onHit(hitresult);

            this.applyEffectsFromBlocks();
            ProjectileUtil.rotateTowardsMovement(this, 0.2F);

            Vec3 deltaMovement = this.getDeltaMovement();
            double pX = this.getX() + deltaMovement.x;
            double pY = this.getY() + deltaMovement.y;
            double pZ = this.getZ() + deltaMovement.z;
            this.setPos(pX, pY, pZ);

            float inertia = this.getInertia();
            if (this.isInWater())
                inertia = 0.8F;
            this.setDeltaMovement(deltaMovement.scale(inertia));
        } else {
            this.discard();
        }
    }

    @Override
    public boolean isOnFire() {
        return fire;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide()) return;

        BlockPos facePos = hit.getBlockPos().relative(hit.getDirection());

        if (cooling) {
            if (level().getBlockState(facePos).getBlock() instanceof FireBlock)
                level().setBlockAndUpdate(facePos, Blocks.AIR.defaultBlockState());
            for (Direction dir : Direction.values()) {
                BlockPos adj = facePos.relative(dir);
                if (level().getBlockState(adj).getBlock() instanceof FireBlock)
                    level().setBlockAndUpdate(adj, Blocks.AIR.defaultBlockState());
            }
            level().playLocalSound(position().x, position().y, position().z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2, true);
        }

        if (fire && level().getBlockState(facePos).canBeReplaced() && level().getFluidState(facePos).isEmpty()) {
            if (BaseFireBlock.canBePlacedAt(level(), facePos, hit.getDirection())) {
                level().setBlockAndUpdate(facePos, BaseFireBlock.getState(level(), facePos));
            }
        }

        remove(RemovalReason.DISCARDED);
    }

    @Override
    public float getPickRadius() {
        return 0.0f;
    }
}
