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
 * Was HammerItemRenderer. While the PROCESSING_ITEM component is set (it is synced with the stack, so it shows on
 * other players too) the item being processed is drawn next to the hammer, and the hammer swings, in the same
 * transforms upstream used for first person, third person and everything else.
 */
public class HammerModel implements ItemModel {
    public static final Identifier ID = CreateDieselGenerators.rl("model/hammer");
    private static final Identifier ITEM_ID = CreateDieselGenerators.rl("item/hammer");

    private final ModelRenderProperties settings;
    private final List<BakedQuad> quads;
    private final Supplier<Vector3fc[]> extents;

    private HammerModel(ModelRenderProperties settings, List<BakedQuad> quads) {
        this.settings = settings;
        this.quads = quads;
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
        time -= 0.5f;
        ItemStack processingItem = processing.item();
        ItemTransform host = settings.transforms().getTransform(displayContext);
        float cubic = Math.abs(time * time * time);

        // All transforms below are upstream's, written in the item-centred space of its renderer; see CDGModelHelper.
        if (!displayContext.firstPerson()) {
            if (!displayContext.name().startsWith("THIRD_PERSON")) {
                CDGModelHelper.appendHeldItem(state, resolver, processingItem, ItemDisplayContext.NONE, host, new Matrix4f(), world, user, 0);

                Matrix4f x = new Matrix4f()
                    .translate(0.5f, -0.2f, 0)
                    .scale(0.75f, 0.75f, 1.1f)
                    .translate(-0.5f, -0.5f, -0.5f)
                    .rotate(Axis.ZP.rotationDegrees(cubic * 300))
                    .translate(0.5f, 0.5f, 0.5f);
                hammer(state, stack, displayContext, x);
            } else {
                Matrix4f x = new Matrix4f()
                    .translate(-0.2f, 0.4f, 0)
                    .scale(0.75f)
                    .rotate(Axis.YP.rotationDegrees(77));
                CDGModelHelper.appendHeldItem(state, resolver, processingItem, ItemDisplayContext.NONE, host, x, world, user, 0);

                x = new Matrix4f()
                    .rotate(Axis.YP.rotationDegrees(90))
                    .translate(0, 0, -0.7f)
                    .scale(0.75f, 0.75f, 1.1f)
                    .rotate(Axis.YP.rotationDegrees(77))
                    .translate(-0.5f, -0.5f, -0.5f)
                    .rotate(Axis.ZP.rotationDegrees(cubic * -180 + 80))
                    .translate(0.5f, 0.5f, 0.5f);
                hammer(state, stack, displayContext, x);
            }
        } else {
            boolean flip = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            Matrix4f x = new Matrix4f()
                .translate(0, 0, flip ? -1 : 1)
                .translate(0, 0, -0.6f)
                .rotate(Axis.YP.rotationDegrees(45))
                .translate(-0.5f, -0.5f, -0.5f)
                .rotate(Axis.ZP.rotationDegrees(cubic * 400))
                .translate(0.5f, 0.5f, 0.5f);
            hammer(state, stack, displayContext, x);

            x = new Matrix4f()
                .translate(0, 0, flip ? -1 : 1)
                .translate(-0.5f, 0.4f, 0)
                .translate((float) (Math.cos(time * -Math.PI) / -10), 0, 0)
                .rotate(Axis.YP.rotationDegrees(-45));
            CDGModelHelper.appendHeldItem(state, resolver, processingItem, ItemDisplayContext.NONE, host, x, world, user, 0);
        }
    }

    private void hammer(ItemStackRenderState state, @Nullable ItemStack stack, ItemDisplayContext displayContext, Matrix4f centred) {
        LayerRenderState layer = submitQuads(state, settings, displayContext, quads);
        layer.setExtents(extents);
        layer.localTransform.set(CDGModelHelper.fromCentred(centred));
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
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            CDGModelHelper.Baked baked = CDGModelHelper.Baked.of(baker, ITEM_ID);
            return new HammerModel(baked.settings(), baked.quads());
        }
    }
}
