package com.jesz.createdieselgenerators;

import com.jesz.createdieselgenerators.client.CDGBlockEntityBehaviours;
import com.jesz.createdieselgenerators.content.turret.TurretOperatorHatLayer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import com.jesz.createdieselgenerators.client.CDGClientEvents;
import com.jesz.createdieselgenerators.client.CDGTrainEngineSounds;
import com.jesz.createdieselgenerators.client.EntityFilterScreen;
import com.jesz.createdieselgenerators.client.TrackLayersBagTooltip;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackHoleBlockEntity;
import com.jesz.createdieselgenerators.content.tools.ChemicalSprayerProjectileEntity;
import com.jesz.createdieselgenerators.content.track_layers_bag.TrackLayersBagComponent;
import com.jesz.createdieselgenerators.contraption.DieselEngineMovementBehaviour;
import com.jesz.createdieselgenerators.port.create.foundation.data.CreateRegistrate;
import com.jesz.createdieselgenerators.port.registrate.util.entry.RegistryEntry;
import com.jesz.createdieselgenerators.ponder.CDGPonderPlugin;
import com.zurrtum.create.client.AllDisplaySourceRenders;
import com.zurrtum.create.client.AllExtensions;
import com.zurrtum.create.client.AllMenuScreens;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.content.fluids.FluidFX;
import com.zurrtum.create.client.content.redstone.displayLink.source.SingleLineDisplaySourceRender;
import com.zurrtum.create.client.foundation.item.ItemDescription;
import com.zurrtum.create.client.foundation.item.KineticStats;
import com.zurrtum.create.client.foundation.item.TooltipModifier;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.config.ModConfig;

/**
 * PORT-NOTE(26.1): was FMLClientSetupEvent, the client mod-bus listeners of ModEvents and the client-only parts of the
 * mod constructor. Render layers of blocks are gone (26.1 takes them from the quads); lighter skins, custom item
 * renderers and the turret operator hat layer are not ported (see PORTING-26.1.md).
 */
public class CreateDieselGeneratorsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigRegistry.INSTANCE.register(CreateDieselGenerators.ID, ModConfig.Type.CLIENT, CDGConfig.CLIENT_SPEC, CreateDieselGenerators.ID + "-client.toml");

        CDGPartialModels.init();
        CDGSpriteShifts.init();
        CreateRegistrate.registerBlockModels();
        CDGBlockEntityBehaviours.register();
        CDGClientEvents.register();
        PonderIndex.addPlugin(new CDGPonderPlugin());
        registerTooltips();

        // Was TurretOperatorHatLayer.registerOnAll: every living renderer (upstream: those with hierarchical or ageable list models,
        // which is all but shulkers here)
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (!(renderer instanceof ShulkerRenderer))
                helper.register(new TurretOperatorHatLayer(renderer));
        });

        AllMenuScreens.register(CDGMenuTypes.ENTITY_FILTER, EntityFilterScreen::create);
        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof TrackLayersBagComponent component ? new TrackLayersBagTooltip(component) : null);
        AllDisplaySourceRenders.register(CDGDisplaySources.PUMPJACK_OIL_AMOUNT.get(), SingleLineDisplaySourceRender::new);

        // Was CustomArmPoseItem
        AllExtensions.ARM_POSE.put(CDGItems.CHEMICAL_SPRAYER.get(), HumanoidModel.ArmPose.CROSSBOW_HOLD);
        AllExtensions.ARM_POSE.put(CDGItems.CHEMICAL_SPRAYER_LIGHTER.get(), HumanoidModel.ArmPose.CROSSBOW_HOLD);

        // Was PumpjackHeadMovementBehaviour.renderInContraption
        com.zurrtum.create.api.behaviour.movement.MovementBehaviour headBehaviour = com.zurrtum.create.api.behaviour.movement.MovementBehaviour.REGISTRY.get(CDGBlocks.PUMPJACK_HEAD.get());
        if (headBehaviour != null)
            headBehaviour.attachRender = new com.jesz.createdieselgenerators.client.render.PumpjackHeadMovementRender();

        // Client hooks of common classes (FluidFX and sounds are client only)
        DieselEngineMovementBehaviour.clientTick = CDGTrainEngineSounds::tick;
        ChemicalSprayerProjectileEntity.clientFluidParticle = FluidFX::getFluidParticle;
        PumpjackHoleBlockEntity.clientPourEffect = (be, fluid) -> FluidFX.spawnPouringLiquid(be.getLevel(), be.getBlockPos(), 20,
                FluidFX.getFluidParticle(new FluidStack(fluid, 81000)), 0.3f, new Vec3(0.1, 1, 0.1), true);
    }

    // Was REGISTRATE.setTooltipModifierFactory in common code; Create Fly's tooltip classes are client only
    private static void registerTooltips() {
        for (RegistryEntry<Item, Item> entry : CreateDieselGenerators.REGISTRATE.<Item, Item>getAll(Registries.ITEM)) {
            Item item = entry.get();
            if (TooltipModifier.REGISTRY.get(item) != null) continue;
            TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
        }
    }
}
