package com.jesz.createdieselgenerators.client.model;

import java.util.function.Supplier;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.infrastructure.component.SandPaperItemComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static com.zurrtum.create.client.flywheel.lib.model.baked.ItemModelRenderHelper.submitQuads;

/**
 * Was WireCuttersItemRenderer. While PROCESSING_ITEM is set the item turns beside the cutters, which flip between
 * the closed and the cut model every five ticks. The same transforms in every display context, as upstream.
 */
public class WireCuttersModel implements ItemModel {
    public static final Identifier ID = CreateDieselGenerators.rl("model/wire_cutters");
    private static final Identifier ITEM_ID = CreateDieselGenerators.rl("item/wire_cutters");
    private static final Identifier CUT_ID = CreateDieselGenerators.rl("item/wire_cutters_cut");

    private final ModelRenderProperties settings;
    private final List<BakedQuad> quads;
    private final List<BakedQuad> cutQuads;
    private final Supplier<Vector3fc[]> extents;

    private WireCuttersModel(ModelRenderProperties settings, List<BakedQuad> quads, List<BakedQuad> cutQuads) {
        this.settings = settings;
        this.quads = quads;
        this.cutQuads = cutQuads;
        extents = CDGModelHelper.extentsOf(quads);
    }

    @Override
    public void update(
        ItemStackRenderState state,
        @Nullable ItemStack stack,
        ItemModelResolver resolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel world,
        @Nullable ItemOwner user,
        int seed
    ) {
        state.appendModelIdentityElement(this);
        SandPaperItemComponent processing = stack == null ? null : stack.get(CDGDataComponents.PROCESSING_ITEM);
        if (processing == null || Minecraft.getInstance().player == null) {
            LayerRenderState layer = submitQuads(state, settings, displayContext, quads);
            layer.setExtents(extents);
            CDGModelHelper.foil(state, stack, layer);
            return;
        }
        state.setAnimated();
        float time = ((AnimationTickHolder.getTicks() + AnimationTickHolder.getPartialTicks()) % 10) / 10;
        ItemTransform host = settings.transforms().getTransform(displayContext);

        Matrix4f x = new Matrix4f()
            .translate(0.1f, 0.2f, 0)
            .rotate(Axis.ZP.rotationDegrees((float) ((AnimationTickHolder.getTicks() + 5) / 10) * -30));
        CDGModelHelper.appendHeldItem(state, resolver, processing.item(), ItemDisplayContext.GUI, host, x, world, user, 0);

        x = new Matrix4f()
            .translate(0, 0, 0.1f)
            .rotate(Axis.YP.rotationDegrees(32));
        LayerRenderState layer = submitQuads(state, settings, displayContext, time > 0.5 ? quads : cutQuads);
        layer.setExtents(extents);
        layer.localTransform.set(CDGModelHelper.fromCentred(x));
        CDGModelHelper.foil(state, stack, layer);
    }

    public static class Unbaked implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(ITEM_ID);
            resolver.markDependency(CUT_ID);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            CDGModelHelper.Baked baked = CDGModelHelper.Baked.of(baker, ITEM_ID);
            return new WireCuttersModel(baked.settings(), baked.quads(), CDGModelHelper.Baked.of(baker, CUT_ID).quads());
        }
    }
}
