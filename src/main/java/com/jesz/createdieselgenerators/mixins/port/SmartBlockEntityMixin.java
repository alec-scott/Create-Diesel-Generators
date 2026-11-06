package com.jesz.createdieselgenerators.mixins.port;

import com.zurrtum.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.jesz.createdieselgenerators.port.create.BETypeOverride;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// PORT-NOTE(26.1): see BETypeOverride
@Mixin(SmartBlockEntity.class)
public abstract class SmartBlockEntityMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/zurrtum/create/foundation/blockEntity/CachedRenderBBBlockEntity;<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"),
            index = 0)
    private static BlockEntityType<?> createdieselgenerators$overrideType(BlockEntityType<?> type) {
        return BETypeOverride.take(type);
    }
}
