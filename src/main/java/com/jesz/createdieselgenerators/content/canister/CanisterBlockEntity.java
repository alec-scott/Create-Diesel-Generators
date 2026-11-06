package com.jesz.createdieselgenerators.content.canister;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.jesz.createdieselgenerators.port.CDGFluidUnits;
import com.jesz.createdieselgenerators.port.neoforge.SimpleFluidContent;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.fluid.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * PORT-NOTE(26.1): a Create Fly {@link FluidTank} (droplets; config capacities are mB) instead of a SmartFluidTankBehaviour
 * subclass, since Create Fly's tank segments have a fixed capacity. The item's component patch is kept as a codec value.
 */
public class CanisterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    final FluidTank tank;

    public int capacityEnchantLevel;

    private DataComponentPatch componentPatch = DataComponentPatch.EMPTY;

    public CanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tank = new FluidTank(baseCapacity()) {
            @Override
            public void markDirty() {
                if (level != null && !level.isClientSide()) {
                    setChanged();
                    sendData();
                }
            }
        };
    }

    private static int baseCapacity() {
        return CDGFluidUnits.mb(Math.abs(CDGConfig.CANISTER_CAPACITY.get()));
    }

    private static int capacityAddition() {
        return CDGFluidUnits.mb(CDGConfig.CANISTER_CAPACITY_ENCHANTMENT.get());
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return containedFluidTooltip(tooltip, isPlayerSneaking, tank);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.CANISTER.get(),
                (be, context) -> be.tank
        );
    }

    @Override
    protected void write(ValueOutput compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("CapacityEnchantment", capacityEnchantLevel);
        tank.write(compound.child("TankContent"));
        compound.store("Components", DataComponentPatch.CODEC, componentPatch);
    }

    @Override
    protected void read(ValueInput compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        capacityEnchantLevel = compound.getIntOr("CapacityEnchantment", 0);
        tank.setCapacity(baseCapacity() + capacityAddition() * capacityEnchantLevel);
        tank.read(compound.childOrEmpty("TankContent"));
        componentPatch = compound.read("Components", DataComponentPatch.CODEC).orElse(DataComponentPatch.EMPTY);
    }

    public void setCapacityEnchantLevel(int capacityEnchantLevel) {
        this.capacityEnchantLevel = capacityEnchantLevel;
        tank.setCapacity(baseCapacity() + capacityAddition() * capacityEnchantLevel);
    }

    public void setComponentPatch(DataComponentPatch componentPatch) {
        this.componentPatch = componentPatch;
        SimpleFluidContent content = componentPatch.get(net.minecraft.core.component.DataComponentMap.EMPTY, CDGDataComponents.FLUID_CONTENTS);
        if (content == null || content.isEmpty())
            return;

        tank.setFluid(content.copy());
    }

    public DataComponentPatch getComponentPatch() {
        return componentPatch;
    }
}
