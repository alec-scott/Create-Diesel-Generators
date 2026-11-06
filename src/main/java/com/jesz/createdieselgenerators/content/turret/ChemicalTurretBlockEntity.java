package com.jesz.createdieselgenerators.content.turret;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;

import com.jesz.createdieselgenerators.port.CDGFluidUnits;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.content.tools.ChemicalSprayerProjectileEntity;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import java.util.List;

public class ChemicalTurretBlockEntity extends TurretBlockEntity implements IHaveGoggleInformation {

    public boolean lighterUpgrade = false;
    public boolean shootNextTick = false;
    public float cogRotation = 0;
    public float lastCogRotation = 0;
    public boolean wasShootingLastTick = false;

    public ChemicalTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        return 4;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }

    public SmartFluidTankBehaviour tank;

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.CHEMICAL_TURRET.get(),
                (be, side) -> be.tank.getCapability()
        );
        // PORT-NOTE(26.1): the CC peripheral is registered by compat/computercraft/CCProxy when CC:Tweaked is loaded
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
    }

    public int redstoneSignal;

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide()) {
            lastCogRotation = cogRotation;
            if (wasShootingLastTick)
                cogRotation += getSpeed() * 3 / 10f;
        }

        if (!level.isClientSide()) {
            boolean wasShooting = wasShootingLastTick;
            wasShootingLastTick = false;

            if (redstoneSignal != 0 || shootNextTick) {
                shootFluids();
                shootNextTick = false;
            }

            if (wasShooting && !wasShootingLastTick)
                sendData();
        }

        if (targetedEntity == null)
            return;
        if (controllingEntity == null) {
            targetedEntity = null;
            return;
        }
        if (Math.abs(targetedHorizontalRotation - horizontalRotation) % 360 <= 2 || Math.abs(targetedHorizontalRotation - horizontalRotation) % 360 >= 358)
            if (Math.abs(targetedVerticalRotation - verticalRotation) <= 2)
                shootFluids();
    }

    @Override
    protected void read(ValueInput compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        lighterUpgrade = compound.getBooleanOr("LighterUpgrade", false);
        redstoneSignal = compound.getIntOr("RedstoneSignal", 0);

        if (clientPacket)
            wasShootingLastTick = compound.getBooleanOr("WasShooting", false);
    }

    @Override
    protected void write(ValueOutput compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putBoolean("LighterUpgrade", lighterUpgrade);
        compound.putInt("RedstoneSignal", redstoneSignal);

        if (clientPacket)
            compound.putBoolean("WasShooting", wasShootingLastTick);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, CDGFluidUnits.BUCKET);
        behaviours.add(tank);
        super.addBehaviours(behaviours);
    }

    public void shootFluids() {
        if (getSpeed() == 0) {
            return;
        }

        float shootingForce = getShootingForce();

        if (!level.isClientSide() && !tank.isEmpty()) {
            wasShootingLastTick = true;
            sendData();

            AllSoundEvents.MIXING.playOnServer(level, worldPosition, .75f, 1);
            FluidStack fluidStack = tank.getPrimaryHandler().getFluid().copy();

            boolean flammable = FuelType.getTypeFor(level.registryAccess().lookupOrThrow(CDGRegistries.FUEL_TYPE), fluidStack.getFluid()).normal().speed() != 0;
            ChemicalSprayerProjectileEntity projectile = ChemicalSprayerProjectileEntity.spray(level, fluidStack, (flammable && lighterUpgrade) || fluidStack.getFluid().isSame(Fluids.LAVA), fluidStack.getFluid().isSame(Fluids.WATER));

            Vec3 directionVector = new Vec3(
                    - Math.sin(Math.toRadians(horizontalRotation)) * Math.cos(Math.toRadians(-verticalRotation)),
                    Math.sin(Math.toRadians(-verticalRotation)),
                    - Math.cos(Math.toRadians(horizontalRotation)) * Math.cos(Math.toRadians(-verticalRotation))
            );
            Vec3 barrelTip = Vec3.atCenterOf(worldPosition).add(0, 0.625f, 0).add(directionVector.scale(1.25f));
            projectile.setPos(barrelTip);
            projectile.shoot(directionVector.x, directionVector.y, directionVector.z, shootingForce, 5);

            projectile.setOwner(controllingPlayer != null ? controllingPlayer : controllingEntity);

            level.addFreshEntity(projectile);
            if (t == 1) {
                if (!tank.getPrimaryHandler().getFluid().isEmpty())
                    tank.getPrimaryHandler().extract(tank.getPrimaryHandler().getFluid(), Math.min(CDGFluidUnits.mb(3), tank.getPrimaryHandler().getFluid().getAmount()));
            }
        }
    }
}
