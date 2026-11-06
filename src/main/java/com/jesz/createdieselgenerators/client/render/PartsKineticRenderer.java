package com.jesz.createdieselgenerators.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PORT-NOTE(26.1): upstream renderers extended Create's ShaftRenderer and drew partials in renderSafe. On 26.1 this is
 * extract/submit: the shaft is drawn as Create Fly's KineticBlockEntityRenderer does (skipped while Flywheel shows the
 * ShaftVisual), and subclasses extract their extra partials into {@link State#parts}.
 */
public abstract class PartsKineticRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T, PartsKineticRenderer.State> {
    private final boolean drawShaft;

    public PartsKineticRenderer(BlockEntityRendererProvider.Context context, boolean drawShaft) {
        super(context);
        this.drawShaft = drawShaft;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T be, State state, float partialTicks, Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
        if (drawShaft)
            super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        else
            state.model = null;
        if (!drawShaft || state.support)
            updateBaseRenderState(be, state, be.getLevel(), crumblingOverlay);
        state.parts.clear();
        extractParts(be, state, partialTicks);
    }

    protected abstract void extractParts(T be, State state, float partialTicks);

    /** A model rotating with the block entity's kinetic angle (for block entities without a Flywheel visual). */
    protected com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState rotating(T be, State state, SuperByteBuffer model) {
        return model.cardinalLighting(state.cardinalLighting)
                .rotateCentered(getAngleForBe(be, state.blockPos, state.axis), state.direction)
                .light(state.lightCoords).color(state.color).extractRenderState();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(T be, State state) {
        return CachedBuffers.block(KINETIC_BLOCK, shaft(state.axis));
    }

    @Override
    public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);
        for (SuperByteBufferRenderState part : state.parts)
            part.submit(matrices, queue);
    }

    public static class State extends KineticRenderState {
        public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
    }
}
