package com.jesz.createdieselgenerators.port.create;

import com.jesz.createdieselgenerators.port.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * PORT-NOTE(26.1): Create Fly's block entities hardwire their own type ({@code BeltBlockEntity(pos, state)} passes
 * {@code AllBlockEntityTypes.BELT}), and 26.1 checks the block against the type in the constructor. D&D reuses those
 * classes under its own types, so the factory sets the type here and {@code SmartBlockEntityMixin} swaps it in.
 */
public final class BETypeOverride {
    private static final ThreadLocal<BlockEntityType<?>> TYPE = new ThreadLocal<>();

    private BETypeOverride() {
    }

    public static <T extends BlockEntity> BlockEntityFactory<T> of(BiFunction<BlockPos, BlockState, T> constructor) {
        return (type, pos, state) -> {
            TYPE.set(type);
            try {
                return constructor.apply(pos, state);
            } finally {
                TYPE.remove();
            }
        };
    }

    public static BlockEntityType<?> take(BlockEntityType<?> fallback) {
        @Nullable BlockEntityType<?> override = TYPE.get();
        if (override == null) return fallback;
        TYPE.remove();
        return override;
    }
}
