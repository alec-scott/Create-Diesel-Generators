package com.jesz.createdieselgenerators.mixins;

import com.jesz.createdieselgenerators.content.turret.TurretOperatorHatLayer;
import com.jesz.createdieselgenerators.mixin_interfaces.IEntity;
import com.zurrtum.create.client.content.trains.schedule.hat.TrainHatInfoReloadListener;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// PORT-NOTE(26.1): client-only. Feeds TurretOperatorHatLayer (upstream's shouldRenderOn check) into the render state.
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void createdieselgenerators$extractTurretHat(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        ((FabricRenderState) state).setData(TurretOperatorHatLayer.HAT_INFO,
                ((IEntity) entity).getTurretPos() != null ? TrainHatInfoReloadListener.getHatInfoFor(entity) : null);
    }
}
