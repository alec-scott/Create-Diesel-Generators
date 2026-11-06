package com.jesz.createdieselgenerators.content.pumpjack;

import com.jesz.createdieselgenerators.port.CDGFluidUnits;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGTags;
import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.jesz.createdieselgenerators.content.concrete.ConcreteEncasedFluidPipeBlock;
import com.jesz.createdieselgenerators.world.OilChunksSavedData;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.api.goggles.IHaveHoveringInformation;
import com.zurrtum.create.content.fluids.pipes.EncasedPipeBlock;
import com.zurrtum.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.*;

public class PumpjackHoleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation {
    BlockState state;

    SmartFluidTankBehaviour tank;
    public int headPos = 0;
    public int bearingPos = 0;
    public boolean started = false;
    public PumpjackHoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = state;
    }

    @Override
    protected void write(ValueOutput tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("OilAmount", oilAmount);
        tag.putBoolean("Started", started);
    }

    public int oilAmount = 0;

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (valid)
            return false;

        Lang.builder(CreateDieselGenerators.ID).translate("hint.pumpjack_hole_no_pipe.title").style(ChatFormatting.GOLD).forGoggles(tooltip);
        Component hint = Lang.builder(CreateDieselGenerators.ID).translate("hint.pumpjack_hole_no_pipe").component();
        List<Component> cutComponent = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
        for (Component component : cutComponent)
            CreateLang.builder().add(component).forGoggles(tooltip);
        return true;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!valid || !started)
            return false;
        if (oilAmount == Integer.MAX_VALUE) {
            TooltipHelper.addHint(tooltip, "hint.hose_pulley");
            return true;
        }

        CreateLang.builder().add(Component.translatable("createdieselgenerators.goggle.oil_amount")).style(ChatFormatting.GRAY).forGoggles(tooltip);
        CreateLang.text(String.format("%,d", oilAmount)).add(CreateLang.translate("generic.unit.millibuckets")).style(ChatFormatting.GOLD).forGoggles(tooltip);

        return true;
    }

    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        oilAmount = tag.getIntOr("OilAmount", 0);
        started = tag.getBooleanOr("Started", false);
    }

    byte tick = 0;
    public int pipeLength = 0;
    boolean valid = false;
    @Override
    public void tick() {
        super.tick();
        tick++;
        if (tick >= 20) {
            int pipeLength = 0;
            tick = 0;
            boolean valid = false;
            for (int i = 0; i < getBlockPos().getY() - level.getMinY(); i++) {
                pipeLength++;
                BlockState bs = level.getBlockState(getBlockPos().below(i + 1));
                if (bs.getBlock() instanceof PipeBlock || bs.getBlock() instanceof EncasedPipeBlock || bs.getBlock() instanceof ConcreteEncasedFluidPipeBlock) {
                    if (!(bs.getValue(BlockStateProperties.UP) && bs.getValue(BlockStateProperties.DOWN)))
                        break;
                } else if(bs.getBlock() instanceof GlassFluidPipeBlock) {
                    if (!(bs.getValue(AXIS) == Direction.Axis.Y))
                        break;
                } else if (bs.is(CDGTags.PUMPJACK_PIPE)){
                    continue;
                } else if (bs.is(CDGTags.OIL_DEPOSIT)) {
                    valid = true;
                    break;
                } else
                    break;
            }
            if (valid)
                this.pipeLength = pipeLength;
            else
                this.pipeLength = 0;
            this.valid = valid;
        }
    }

    // PORT-NOTE(26.1): the pumping sound is a client behaviour (client/CDGBlockEntityBehaviours)
    public boolean isPumping() {
        return started && valid && oilAmount > 0;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(pipeLength);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, CDGFluidUnits.mb(8000));
        behaviours.add(tank);
    }

    // PORT-NOTE(26.1): FluidFX is client only; set from the client entrypoint
    public static java.util.function.BiConsumer<PumpjackHoleBlockEntity, Fluid> clientPourEffect;

    public void pumpjackRotation(boolean isCrankLarge) {

        List<Fluid> stackList = new ArrayList<>();
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(CDGTags.PUMPJACK_OUTPUT))
            stackList.add(holder.value());

        if (stackList.isEmpty())
            return;

        if (!level.isClientSide() && valid) {
            ChunkPos chunkPos = ChunkPos.containing(getBlockPos());
            oilAmount = OilChunksSavedData.getChunkOilAmount((ServerLevel) level, chunkPos);
            started = true;

            int subtractedAmount = Mth.clamp((int) (1000 * Math.abs((float) headPos / (float) bearingPos)) * (isCrankLarge ? 2 : 1), 0, oilAmount);

            // PORT-NOTE(26.1): chunk oil stays in mB; the tank is in droplets
            FluidStack oilStack = new FluidStack(stackList.get(0), CDGFluidUnits.mb(subtractedAmount));

            subtractedAmount = CDGFluidUnits.toMb(tank.getCapability().insert(oilStack));

            if (oilAmount == Integer.MAX_VALUE)
                return;

            oilAmount -= subtractedAmount;
            OilChunksSavedData.setChunkOilAmount((ServerLevel) level, chunkPos, oilAmount);
        }

        if (level.isClientSide() && oilAmount > 0 && clientPourEffect != null)
            clientPourEffect.accept(this, stackList.getFirst());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.PUMPJACK_HOLE.get(),
                (be, side) -> be.tank.getCapability()
        );
    }
}
