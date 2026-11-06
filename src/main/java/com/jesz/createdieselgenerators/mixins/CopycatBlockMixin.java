package com.jesz.createdieselgenerators.mixins;

import net.minecraft.world.item.DyeColor;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.content.oil_barrel.OilBarrelBlock;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import com.zurrtum.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopycatBlock.class)
public class CopycatBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    public void use(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        DyeColor dye = stack.get(net.minecraft.core.component.DataComponents.DYE);
        if (dye != null) {
            if (level.getBlockEntity(pos) instanceof CopycatBlockEntity be){
                if(be.getMaterial().is(CDGBlocks.OIL_BARREL.get()))
                    be.setMaterial(be.getMaterial().setValue(OilBarrelBlock.OIL_BARREL_COLOR, OilBarrelBlock.OilBarrelColor.getForDyeColor(dye)));
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

}
