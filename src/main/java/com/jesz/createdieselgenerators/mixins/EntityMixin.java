package com.jesz.createdieselgenerators.mixins;

import com.jesz.createdieselgenerators.mixin_interfaces.IEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// PORT-NOTE(26.1): load/save take ValueInput/ValueOutput; "TurretPos" is a BlockPos codec (int array, as NbtUtils did)
@Mixin(Entity.class)
public abstract class EntityMixin implements IEntity {

    @Unique
    public BlockPos create_diesel_generators$turretPos;

    @Inject(method = "load", at = @At("HEAD"))
    public void createdieselgenerators$load(ValueInput input, CallbackInfo ci) {
        create_diesel_generators$turretPos = input.read("TurretPos", BlockPos.CODEC).orElse(null);
    }

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    public void createdieselgenerators$save(ValueOutput output, CallbackInfo ci) {
        if (create_diesel_generators$turretPos != null)
            output.store("TurretPos", BlockPos.CODEC, create_diesel_generators$turretPos);
    }

    @Override
    public BlockPos getTurretPos() {
        return create_diesel_generators$turretPos;
    }

    @Override
    public void setTurretPos(BlockPos pos) {
        create_diesel_generators$turretPos = pos;
    }
}
