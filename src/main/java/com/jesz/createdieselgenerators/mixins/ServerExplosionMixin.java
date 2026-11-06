package com.jesz.createdieselgenerators.mixins;

import com.jesz.createdieselgenerators.events.GameEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// PORT-NOTE(26.1): replaces NeoForge's ExplosionEvent.Detonate (flammable fluids next to an explosion blow up)
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow
    public abstract ServerLevel level();

    @Shadow
    public abstract Vec3 center();

    @Inject(method = "explode", at = @At("HEAD"))
    private void createdieselgenerators$detonate(CallbackInfoReturnable<Integer> cir) {
        GameEvents.onExplosion(level(), center());
    }
}
