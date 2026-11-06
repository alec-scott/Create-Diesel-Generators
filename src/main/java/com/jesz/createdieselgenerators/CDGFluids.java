package com.jesz.createdieselgenerators;

import net.minecraft.world.item.DyeColor;

import net.minecraft.world.item.ItemStack;

import com.jesz.createdieselgenerators.content.concrete.ConcreteBucketItem;
import com.jesz.createdieselgenerators.content.concrete.ConcreteFluid;
import com.jesz.createdieselgenerators.port.registrate.util.entry.FluidEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import com.jesz.createdieselgenerators.port.registrate.fabric.SimpleFlowableFluid;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jspecify.annotations.NonNull;


import static com.jesz.createdieselgenerators.CreateDieselGenerators.REGISTRATE;

public class CDGFluids {

    public static final FluidEntry<SimpleFlowableFluid.Flowing> PLANT_OIL = REGISTRATE.fluid("plant_oil",
                    CreateDieselGenerators.rl("block/fluid/plant_oil_still"),
                    CreateDieselGenerators.rl("block/fluid/plant_oil_flow"))
            .renderType(() -> () -> ChunkSectionLayer.TRANSLUCENT)
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(25)
                    .flowSpeed(3)
                    .blastResistance(100f))
            .source(SimpleFlowableFluid.Source::new)
            .block()
            .build()
            .bucket()
            .onRegister(CDGFluids::registerFluidDispenseBehavior)
            .build()
            .register();

    public static final FluidEntry<SimpleFlowableFluid.Flowing> CRUDE_OIL = REGISTRATE.fluid("crude_oil",
                            CreateDieselGenerators.rl("block/crude_oil_still"),
                            CreateDieselGenerators.rl("block/crude_oil_flow"))
            .fluidProperties(p -> p.levelDecreasePerBlock(3)
                    .tickRate(25)
                    .flowSpeed(2)
                    .blastResistance(100f))
            .source(SimpleFlowableFluid.Source::new)
            .block()
            .build()
            .bucket()
            .onRegister(CDGFluids::registerFluidDispenseBehavior)
            .build()
            .register();

    public static final FluidEntry<SimpleFlowableFluid.Flowing> BIODIESEL = REGISTRATE.fluid("biodiesel",
                    CreateDieselGenerators.rl("block/biodiesel_still"),
                    CreateDieselGenerators.rl("block/biodiesel_flow"))
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(25)
                    .flowSpeed(3)
                    .blastResistance(100f))
            .source(SimpleFlowableFluid.Source::new)
            .block()
            .build()
            .bucket()
            .onRegister(CDGFluids::registerFluidDispenseBehavior)
            .build()
            .register();

    public static final FluidEntry<SimpleFlowableFluid.Flowing> DIESEL = REGISTRATE.fluid("diesel",
                    CreateDieselGenerators.rl("block/diesel_still"),
                    CreateDieselGenerators.rl("block/diesel_flow"))
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(25)
                    .flowSpeed(3)
                    .blastResistance(100f))
            .source(SimpleFlowableFluid.Source::new)
            .block()
            .build()
            .bucket()
            .onRegister(CDGFluids::registerFluidDispenseBehavior)
            .build()
            .register();

    public static final FluidEntry<SimpleFlowableFluid.Flowing> GASOLINE = REGISTRATE.fluid("gasoline",
                    CreateDieselGenerators.rl("block/gasoline_still"),
                    CreateDieselGenerators.rl("block/gasoline_flow"))
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(25)
                    .flowSpeed(3)
                    .blastResistance(100f))
            .source(SimpleFlowableFluid.Source::new)
            .block()
            .build()
            .bucket()
            .onRegister(CDGFluids::registerFluidDispenseBehavior)
            .build()
            .register();

    public static final FluidEntry<SimpleFlowableFluid.Flowing> ETHANOL = REGISTRATE.fluid("ethanol",
                            CreateDieselGenerators.rl("block/fluid/ethanol_still"),
                            CreateDieselGenerators.rl("block/fluid/ethanol_flow"))
            .renderType(() -> () -> ChunkSectionLayer.TRANSLUCENT)
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(25)
                    .flowSpeed(5)
                    .blastResistance(100f))
            .source(SimpleFlowableFluid.Source::new)
            .block()
            .build()
            .bucket()
            .onRegister(CDGFluids::registerFluidDispenseBehavior)
            .build()
            .register();

    @SuppressWarnings("unchecked")
    public static final FluidEntry<SimpleFlowableFluid.Flowing>[] CONCRETE = new FluidEntry[DyeColor.values().length];

    static {
        for (DyeColor color : DyeColor.values()) {
            CONCRETE[color.ordinal()] =
                    REGISTRATE.fluid(color.getName() + "_cement",
                                    CreateDieselGenerators.rl("block/cement/" + color.getName() + "_still"),
                                    CreateDieselGenerators.rl("block/cement/" + color.getName() + "_flow"))
                    .fluidProperties(p -> p.levelDecreasePerBlock(8)
                            .tickRate(12)
                            .flowSpeed(1)
                            .blastResistance(100f)).source(p -> new ConcreteFluid(p, color))
                    .block()
                            .build()
                    .bucket((f, p) -> new ConcreteBucketItem(color, f, p))
                            .onRegister(CDGFluids::registerFluidDispenseBehavior)
                            .build()
                    .register();
        }
    }

    public static void register() {}

    // from Create

    private static final DispenseItemBehavior DEFAULT = new DefaultDispenseItemBehavior();
    private static final DispenseItemBehavior DISPENSE_FLUID = new DefaultDispenseItemBehavior(){
        @Override
        protected @NonNull ItemStack execute(BlockSource pSource, ItemStack pStack) {
            DispensibleContainerItem dispensibleContainerItem = (DispensibleContainerItem) pStack.getItem();
            BlockPos pos = pSource.pos().relative(pSource.state().getValue(DispenserBlock.FACING));
            Level level = pSource.level();
            if (dispensibleContainerItem.emptyContents(null, level, pos, null, pStack)) {
                return new ItemStack(Items.BUCKET);
            }
            return DEFAULT.dispense(pSource, pStack);
        }
    };

    private static void registerFluidDispenseBehavior(BucketItem bucket) {
        DispenserBlock.registerBehavior(bucket, DISPENSE_FLUID);
    }
}
