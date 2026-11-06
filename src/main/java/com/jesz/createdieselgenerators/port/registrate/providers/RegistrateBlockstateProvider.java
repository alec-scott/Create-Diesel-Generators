package com.jesz.createdieselgenerators.port.registrate.providers;

import com.jesz.createdieselgenerators.port.registrate.AbstractRegistrate;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;

import com.jesz.createdieselgenerators.port.porting_lib.data.ExistingFileHelper;
import com.jesz.createdieselgenerators.port.porting_lib.models.generators.BlockStateProvider;
import com.jesz.createdieselgenerators.port.porting_lib.models.generators.MultiPartBlockStateBuilder;
import com.jesz.createdieselgenerators.port.porting_lib.models.generators.VariantBlockStateBuilder;
import net.fabricmc.api.EnvType;

import java.util.Optional;

public class RegistrateBlockstateProvider extends BlockStateProvider implements RegistrateProvider {

    private final AbstractRegistrate<?> parent;

    public RegistrateBlockstateProvider(AbstractRegistrate<?> parent, PackOutput packOutput, ExistingFileHelper exFileHelper) {
        super(packOutput, parent.getModid(), exFileHelper);
        this.parent = parent;
    }

    @Override
    public EnvType getSide() {
        return EnvType.CLIENT;
    }

    @Override
    protected void registerStatesAndModels() {
        parent.genData(ProviderType.BLOCKSTATE, this);
    }

    @Override
    public String getName() {
        return "Blockstates";
    }

    ExistingFileHelper getExistingFileHelper() {
        return this.models().existingFileHelper;
    }

    @SuppressWarnings("null")
    public Optional<VariantBlockStateBuilder> getExistingVariantBuilder(Block block) {
        return Optional.ofNullable(registeredBlocks.get(block))
                .filter(b -> b instanceof VariantBlockStateBuilder)
                .map(b -> (VariantBlockStateBuilder) b);
    }

    @SuppressWarnings("null")
    public Optional<MultiPartBlockStateBuilder> getExistingMultipartBuilder(Block block) {
        return Optional.ofNullable(registeredBlocks.get(block))
                .filter(b -> b instanceof MultiPartBlockStateBuilder)
                .map(b -> (MultiPartBlockStateBuilder) b);
    }
}
