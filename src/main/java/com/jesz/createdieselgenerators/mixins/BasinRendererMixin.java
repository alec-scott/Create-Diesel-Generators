package com.jesz.createdieselgenerators.mixins;

import com.jesz.createdieselgenerators.client.render.MoldBasinRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.processing.basin.BasinRenderer;
import com.zurrtum.create.client.content.processing.basin.BasinRenderer.BasinRenderState;
import com.zurrtum.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// PORT-NOTE(26.1): client-only. Was BasinRendererMixin (renderSafe / renderItem on Create 6's renderer): molds in a basin.
@Mixin(BasinRenderer.class)
public abstract class BasinRendererMixin {
    @Shadow
    @org.spongepowered.asm.mixin.Final
    protected ItemModelResolver itemModelManager;

    @Inject(
        method = "extractRenderState(Lcom/zurrtum/create/content/processing/basin/BasinBlockEntity;Lcom/zurrtum/create/client/content/processing/basin/BasinRenderer$BasinRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void createdieselgenerators$extractMold(BasinBlockEntity be, BasinRenderState state, float tickProgress, Vec3 cameraPos, CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        if (MoldBasinRender.extract(be, state, cameraPos, itemModelManager))
            ci.cancel();
    }

    @Inject(
        method = "submit(Lcom/zurrtum/create/client/content/processing/basin/BasinRenderer$BasinRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void createdieselgenerators$submitMold(BasinRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (MoldBasinRender.submit(state, matrices, queue))
            ci.cancel();
    }
}
