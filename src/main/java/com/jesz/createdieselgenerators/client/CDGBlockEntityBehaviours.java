package com.jesz.createdieselgenerators.client;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.content.burner.BurnerBlockEntity;
import com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineValueBox;
import com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineValueBox;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineValueBox;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackCrankBlockEntity;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackCrankValueBox;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.RotationDirectionScrollBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.GeneratingKineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.function.Function;

/**
 * PORT-NOTE(26.1): Create Fly keeps a block entity's client-only parts (goggle tooltips, value boxes, sounds, filter
 * slots) in client behaviours registered per type, like its own AllBlockEntityBehaviours (pattern from the CC&A port).
 * Block entities keep their {@code addToGoggleTooltip}, which the tooltip behaviours forward to.
 */
public final class CDGBlockEntityBehaviours {
    private CDGBlockEntityBehaviours() {
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static <T extends SmartBlockEntity> void add(BlockEntityType<T> type, Function<T, BlockEntityBehaviour<?>>... factories) {
        for (Function<T, BlockEntityBehaviour<?>> factory : factories)
            BlockEntityBehaviour.CLIENT_REGISTRY.add(type, (Function<SmartBlockEntity, BlockEntityBehaviour<?>>) factory);
    }

    private static Component rotationTitle() {
        return CreateLang.translateDirect("contraptions.windmill.rotation_direction");
    }

    public static void register() {
        add(CDGBlockEntityTypes.DIESEL_ENGINE.get(),
                CDGKineticTooltipBehaviour.Generating::new,
                be -> new RotationDirectionScrollBehaviour(be, rotationTitle(), new DieselEngineValueBox()),
                CDGSoundBehaviours.Engine::new);
        add(CDGBlockEntityTypes.MODULAR_DIESEL_ENGINE.get(),
                CDGKineticTooltipBehaviour.Generating::new,
                be -> new RotationDirectionScrollBehaviour(be, rotationTitle(), new ModularDieselEngineValueBox()),
                CDGSoundBehaviours.Engine::new);
        add(CDGBlockEntityTypes.HUGE_DIESEL_ENGINE.get(),
                CDGTooltipBehaviour::new,
                be -> new RotationDirectionScrollBehaviour(be, rotationTitle(), new HugeDieselEngineValueBox()),
                CDGSoundBehaviours.Engine::new);
        add(CDGBlockEntityTypes.POWERED_ENGINE_SHAFT.get(), GeneratingKineticTooltipBehaviour::new);

        add(CDGBlockEntityTypes.BURNER.get(),
                CDGKineticTooltipBehaviour.Consuming::new,
                be -> new CDGClientTickBehaviour<>(be, BurnerBlockEntity::tickAudio));
        add(CDGBlockEntityTypes.CHEMICAL_TURRET.get(),
                CDGKineticTooltipBehaviour.Consuming::new,
                be -> new FilteringBehaviour<>(be, new ChemicalTurretValueBox()));
        add(CDGBlockEntityTypes.BASIN_LID.get(), KineticTooltipBehaviour::new);
        add(CDGBlockEntityTypes.ENCASED_GIRDER.get(), KineticTooltipBehaviour::new);
        add(CDGBlockEntityTypes.PUMPJACK_BEARING.get(), KineticTooltipBehaviour::new);
        add(CDGBlockEntityTypes.PUMPJACK_CRANK.get(),
                KineticTooltipBehaviour::new,
                CrankSizeScrollBehaviour::new,
                CDGSoundBehaviours.Crank::new);

        add(CDGBlockEntityTypes.CANISTER.get(), CDGTooltipBehaviour::new);
        add(CDGBlockEntityTypes.OIL_BARREL.get(), CDGTooltipBehaviour::new);
        add(CDGBlockEntityTypes.BULK_FERMENTER.get(), CDGTooltipBehaviour::new);
        add(CDGBlockEntityTypes.DISTILLATION_TANK.get(), CDGTooltipBehaviour::new, CDGSoundBehaviours.Distillation::new);
        add(CDGBlockEntityTypes.PUMPJACK_HOLE.get(), CDGTooltipBehaviour::new, CDGSoundBehaviours.PumpjackHole::new);
    }

    /** Was ChemicalTurretBlockEntity.ChemicalTurretValueBox (client class moved out of the block entity). */
    public static class ChemicalTurretValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 3, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }

    /** The crank size value box (icons were on the common CrankSize enum upstream). */
    public static class CrankSizeScrollBehaviour extends ScrollOptionBehaviour<PumpjackCrankBlockEntity.CrankSize> {
        public CrankSizeScrollBehaviour(PumpjackCrankBlockEntity be) {
            super(CrankSizeIcon.class, CrankSizeIcon::from, Component.translatable("createdieselgenerators.pumpjack_crank.crank_size"),
                    be, new PumpjackCrankValueBox());
        }
    }

    private enum CrankSizeIcon implements INamedIconOptions {
        NORMAL(AllIcons.I_CLEAR), LARGE(AllIcons.I_PLACE);

        private final AllIcons icon;

        CrankSizeIcon(AllIcons icon) {
            this.icon = icon;
        }

        static CrankSizeIcon from(PumpjackCrankBlockEntity.CrankSize size) {
            return size == PumpjackCrankBlockEntity.CrankSize.LARGE ? LARGE : NORMAL;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return "createdieselgenerators.tooltip.crank." + name().toLowerCase(Locale.ROOT);
        }
    }
}
