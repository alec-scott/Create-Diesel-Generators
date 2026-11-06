package com.jesz.createdieselgenerators.content.canister;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * PORT-NOTE(26.1): extract/submit. An enchanted canister (render shape ENTITYBLOCK_ANIMATED) draws its block model plus
 * the enchantment glint.
 */
public class CanisterRenderer implements BlockEntityRenderer<CanisterBlockEntity, CanisterRenderer.State> {
    public CanisterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CanisterBlockEntity be, State state, float partialTicks, Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
        SmartBlockEntityRenderer.extractBase(be, state, crumblingOverlay);
        if (be.capacityEnchantLevel == 0) {
            state.model = null;
            state.glint = null;
            return;
        }
        state.model = CachedBuffers.block(be.getBlockState()).light(state.lightCoords).extractRenderState();
        state.glint = CachedBuffers.block(be.getBlockState()).light(state.lightCoords).extractRenderState();
    }

    @Override
    public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (state.model != null)
            state.model.submit(matrices, queue);
        if (state.glint != null)
            state.glint.submit(RenderTypes.glint(), matrices, queue);
    }

    public static class State extends BlockEntityRenderState {
        public @Nullable SuperByteBufferRenderState model;
        public @Nullable SuperByteBufferRenderState glint;
    }
}
