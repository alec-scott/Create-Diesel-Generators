package com.jesz.createdieselgenerators.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.api.ui.NarratableComponent;
import snownee.jade.util.FluidTextHelper;

import java.text.NumberFormat;

/**
 * PORT-NOTE(26.1): not in upstream, which had whole mB on NeoForge. Fabric counts 81 droplets per mB, so engines hold part
 * of a mB, and Jade writes that as a fraction (553 17/27mB) with no option to change it. Show decimals instead, as Create's
 * goggle tooltip does (553.48mB). This changes Jade's text for every fluid, not only this mod's. Applied only when Jade is
 * loaded, see CDGMixinPlugin.
 */
@Mixin(FluidTextHelper.class)
public abstract class JadeFluidTextHelperMixin {
    @Shadow
    private static NarratableComponent makeString(String text, long whole, long numerator, long denominator, String unit) {
        throw new AssertionError();
    }

    @Inject(method = "makeString(Ljava/lang/String;JJJLjava/lang/String;)Lsnownee/jade/api/ui/NarratableComponent;", at = @At("HEAD"), cancellable = true)
    private static void createdieselgenerators$decimals(String text, long whole, long numerator, long denominator, String unit, CallbackInfoReturnable<NarratableComponent> cir) {
        if (denominator == 0)
            return;
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        String fraction = format.format((double) numerator / denominator);
        // "0.48" becomes ".48". Anything else (a value that rounds to a whole number) keeps Jade's own text.
        if (!fraction.startsWith("0") || fraction.length() == 1)
            return;
        cir.setReturnValue(makeString((text.isEmpty() ? "0" : text) + fraction.substring(1), whole, 0, 0, unit));
    }
}
