package com.jesz.createdieselgenerators.content.sheetmetal;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.infrastructure.model.WrapperBlockStateModel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.jesz.createdieselgenerators.content.sheetmetal.SheetMetalPanelBlock.FACING;
import static com.jesz.createdieselgenerators.content.sheetmetal.SheetMetalPanelBlock.ROLL;

/**
 * PORT-NOTE(26.1): was a BakedModelWrapperWithData with cull ModelData. On 26.1 the world-aware parts come from Create
 * Fly's WrapperBlockStateModel; faces towards a connected panel of the same orientation are dropped.
 */
public class SheetMetalPanelModel extends WrapperBlockStateModel {
    public SheetMetalPanelModel(BlockState state, UnbakedRoot unbaked) {
        super(state, unbaked);
    }

    private static boolean[] cullData(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        boolean[] culled = new boolean[3];
        for (Direction d : Iterate.directions) {
            if (d.getAxis() == state.getValue(FACING).getAxis())
                continue;
            BlockState otherState = level.getBlockState(pos.relative(d));
            if (!CDGBlocks.SHEET_METAL_PANEL.has(otherState))
                continue;
            if (otherState.getValue(FACING) != state.getValue(FACING))
                continue;
            if (otherState.getValue(ROLL) != state.getValue(ROLL))
                continue;
            if (!state.getValue(ROLL) && state.getValue(FACING).getAxis().isHorizontal() && d.getAxis().isHorizontal())
                continue;
            if (state.getValue(ROLL) && state.getValue(FACING).getAxis().isHorizontal() && d.getAxis().isVertical())
                continue;
            if (state.getValue(FACING).getAxis().isVertical() && (d.getAxis() == Direction.Axis.Z && state.getValue(ROLL) || (d.getAxis() == Direction.Axis.X && !state.getValue(ROLL))))
                continue;
            culled[d.getAxis().ordinal()] = true;
        }
        return culled;
    }

    @Override
    public void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        boolean[] culled = cullData(world, pos, state);
        List<BlockStateModelPart> modelParts = new ObjectArrayList<>();
        model.collectParts(random, modelParts);
        for (BlockStateModelPart part : modelParts) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            for (BakedQuad quad : part.getQuads(null)) {
                if (!culled[quad.direction().getAxis().ordinal()])
                    builder.addUnculledFace(quad);
            }
            for (Direction direction : Iterate.directions) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    if (!culled[quad.direction().getAxis().ordinal()])
                        builder.addCulledFace(direction, quad);
                }
            }
            parts.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleMaterial()));
        }
    }
}
