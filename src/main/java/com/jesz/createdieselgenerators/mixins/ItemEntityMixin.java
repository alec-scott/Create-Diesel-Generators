package com.jesz.createdieselgenerators.mixins;

import com.jesz.createdieselgenerators.content.tools.lighter.LighterItem;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// PORT-NOTE(26.1): replaces NeoForge's Item#onEntityItemUpdate for the dropped, ignited lighter
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void createdieselgenerators$tick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide() && self.getItem().getItem() instanceof LighterItem)
            LighterItem.onEntityItemUpdate(self.getItem(), self);
    }
}
