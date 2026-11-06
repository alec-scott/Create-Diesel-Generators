package com.jesz.createdieselgenerators.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PORT-NOTE(26.1): extract/submit for the non-kinetic renderers that only draw partials (was renderSafe on a
 * SafeBlockEntityRenderer / SmartBlockEntityRenderer).
 */
public abstract class PartsRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, PartsRenderer.State> {
    public PartsRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(T be, State state, float partialTicks, Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
        SmartBlockEntityRenderer.extractBase(be, state, crumblingOverlay);
        state.parts.clear();
        extractParts(be, state, partialTicks);
    }

    protected abstract void extractParts(T be, State state, float partialTicks);

    @Override
    public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        for (SuperByteBufferRenderState part : state.parts)
            part.submit(matrices, queue);
    }

    public static class State extends BlockEntityRenderState {
        public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
    }
}
