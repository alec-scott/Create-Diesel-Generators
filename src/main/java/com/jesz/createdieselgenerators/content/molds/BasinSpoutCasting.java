package com.jesz.createdieselgenerators.content.molds;

import com.jesz.createdieselgenerators.CDGRecipes;
import com.zurrtum.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.zurrtum.create.content.fluids.spout.SpoutBlockEntity;
import com.zurrtum.create.content.processing.basin.BasinBlockEntity;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** PORT-NOTE(26.1): recipe lookup through the server's recipe access (spouts only fill on the server). */
public class BasinSpoutCasting implements BlockSpoutingBehaviour {
    @Override
    public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack availableFluid, boolean simulate) {
        if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof BasinBlockEntity basin))
            return 0;

        Optional<CastingRecipe> recipe = serverLevel.recipeAccess().recipeMap().byType(CDGRecipes.CASTING).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.matches(basin, availableFluid) && r.fluidIngredient().amount() <= availableFluid.getAmount())
                .findFirst();

        return recipe.map(r -> r.execute(basin, simulate)).orElse(0);
    }
}
