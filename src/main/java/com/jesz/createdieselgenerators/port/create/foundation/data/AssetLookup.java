package com.jesz.createdieselgenerators.port.create.foundation.data;

import com.jesz.createdieselgenerators.port.registrate.providers.DataGenContext;
import com.jesz.createdieselgenerators.port.registrate.providers.RegistrateBlockstateProvider;
import com.jesz.createdieselgenerators.port.registrate.providers.RegistrateItemModelProvider;
import com.jesz.createdieselgenerators.port.registrate.util.nullness.NonNullBiConsumer;
import com.jesz.createdieselgenerators.port.porting_lib.models.generators.ModelFile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Function;

public final class AssetLookup {
    private AssetLookup() {
    }

    public static <T extends Block> Function<BlockState, ModelFile> partialBaseModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
        return state -> prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block"));
    }

    /** Upstream signature: the model at {@code block/<name>/block[_suffix...]} (datagen only). */
    public static ModelFile partialBaseModel(DataGenContext<?, ?> ctx, RegistrateBlockstateProvider prov, String... suffix) {
        StringBuilder path = new StringBuilder("block/" + ctx.getName() + "/block");
        for (String s : suffix)
            path.append("_").append(s);
        return prov.models().getExistingFile(prov.modLoc(path.toString()));
    }

    public static <T extends Block> Function<BlockState, ModelFile> forPowered(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
        return state -> prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block" + (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED) ? "_powered" : "")));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> itemModelWithPartials() {
        return (ctx, prov) -> { };
    }
}
