package com.jesz.createdieselgenerators.content.canister;

import com.jesz.createdieselgenerators.port.minecraft.CDGValueIO;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.zurrtum.create.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.jesz.createdieselgenerators.port.neoforge.SimpleFluidContent;

import java.util.List;
import java.util.Optional;

public class CanisterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    CapacityEnchantedFluidTankBehaviour tank;
    BlockState state;

    public int capacityEnchantLevel;

    private DataComponentPatch componentPatch = DataComponentPatch.EMPTY;

    public CanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = state;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return containedFluidTooltip(tooltip, isPlayerSneaking, level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition, null));
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        tank = CapacityEnchantedFluidTankBehaviour.single(this, Math.abs((CDGConfig.CANISTER_CAPACITY.get())), CDGConfig.CANISTER_CAPACITY_ENCHANTMENT.get());
        behaviours.add(tank);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.CANISTER.get(),
                (be, context) -> be.tank.getCapability()
        );
    }

    @Override
    protected void write(ValueOutput compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("CapacityEnchantment", capacityEnchantLevel);
        CDGValueIO.put(compound, "Components", CatnipCodecUtils.encode(DataComponentPatch.CODEC, registries, componentPatch)
                .orElse(new CompoundTag()));
    }

    @Override
    protected void read(ValueInput compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        capacityEnchantLevel = compound.getIntOr("CapacityEnchantment", 0);
        componentPatch = CatnipCodecUtils.decode(DataComponentPatch.CODEC, registries, CDGValueIO.getCompound(compound, "Components")).orElse(DataComponentPatch.EMPTY);
    }

    public void setCapacityEnchantLevel(int capacityEnchantLevel) {
        this.capacityEnchantLevel = capacityEnchantLevel;
        tank.getPrimaryHandler().setCapacity(tank.baseCapacity + tank.capacityAddition * capacityEnchantLevel);
    }

    public void setComponentPatch(DataComponentPatch componentPatch) {
        this.componentPatch = componentPatch;
        Optional<? extends SimpleFluidContent> content = componentPatch.get(CDGDataComponents.FLUID_CONTENTS);
        if (content == null || content.isEmpty())
            return;

        this.tank.getPrimaryHandler().setFluid(content.get().copy());
    }

    public DataComponentPatch getComponentPatch() {
        return componentPatch;
    }

    public static class CapacityEnchantedFluidTankBehaviour extends SmartFluidTankBehaviour {

        int capacityAddition;
        int baseCapacity;

        public CapacityEnchantedFluidTankBehaviour(BehaviourType<SmartFluidTankBehaviour> type, SmartBlockEntity be, int tanks, int tankCapacity, boolean enforceVariety, int capacityAddition) {
            super(type, be, tanks, tankCapacity, enforceVariety);
            this.capacityAddition = capacityAddition;
            this.baseCapacity = tankCapacity;
        }

        public static CapacityEnchantedFluidTankBehaviour single(SmartBlockEntity be, int capacity, int capacityAddition) {
            return new CapacityEnchantedFluidTankBehaviour(TYPE, be, 1, capacity, false, capacityAddition);
        }

        @Override
        public void read(ValueInput compound, boolean clientPacket) {
            super.read(compound, clientPacket);
            if(CDGValueIO.contains(compound, "CapacityEnchantment"))
                getPrimaryHandler().setCapacity(baseCapacity + compound.getIntOr("CapacityEnchantment", 0) * capacityAddition);
        }

    }
}
