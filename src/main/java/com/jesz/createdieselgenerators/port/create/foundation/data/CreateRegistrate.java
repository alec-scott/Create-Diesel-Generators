package com.jesz.createdieselgenerators.port.create.foundation.data;

import com.jesz.createdieselgenerators.port.registrate.AbstractRegistrate;
import com.jesz.createdieselgenerators.port.registrate.builders.Builder;
import com.jesz.createdieselgenerators.port.registrate.builders.NoConfigBuilder;
import com.jesz.createdieselgenerators.port.porting_lib.util.DeferredHolder;
import com.jesz.createdieselgenerators.port.registrate.util.entry.RegistryEntry;
import com.jesz.createdieselgenerators.port.registrate.util.nullness.NonNullConsumer;
import com.jesz.createdieselgenerators.port.registrate.util.nullness.NonNullFunction;
import com.jesz.createdieselgenerators.port.registrate.util.nullness.NonNullSupplier;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.zurrtum.create.api.contraption.storage.item.MountedItemStorageType;
import com.zurrtum.create.api.registry.CreateRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel.UnbakedRoot;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class CreateRegistrate extends AbstractRegistrate<CreateRegistrate> {
    private static final Map<RegistryEntry<?, ?>, ResourceKey<CreativeModeTab>> TAB_LOOKUP = new IdentityHashMap<>();

    @Nullable
    private ResourceKey<CreativeModeTab> currentTab;

    public CreateRegistrate(String modId) {
        super(modId);
    }

    public static CreateRegistrate create(String modId) {
        return new CreateRegistrate(modId);
    }

    /** Upstream Create's tab tracking: entries registered after this call belong to {@code tab}. */
    public CreateRegistrate setCreativeTab(@Nullable ResourceKey<CreativeModeTab> tab) {
        this.currentTab = tab;
        return this;
    }

    /** Block model wrappers by block, registered with Create Fly on the client by {@link #registerBlockModels}. */
    private static final Map<Block, Supplier<BiFunction<BlockState, UnbakedRoot, UnbakedRoot>>> BLOCK_MODELS = new LinkedHashMap<>();

    /**
     * Upstream Create's {@code CreateRegistrate.blockModel}: wraps the block's baked model.
     *
     * <p>PORT-NOTE(fabric): the factory is only resolved on the client (types are erased, so servers never load it)
     * and handed to Create Fly's {@code AllModels.register}.</p>
     */
    public static <T extends Block> NonNullConsumer<? super T> blockModel(Supplier<BiFunction<BlockState, UnbakedRoot, UnbakedRoot>> factory) {
        return block -> BLOCK_MODELS.put(block, factory);
    }

    /** Connected texture behaviours by block, turned into Create Fly CT models on the client. */
    private static final Map<Block, Supplier<? extends com.zurrtum.create.client.foundation.block.connected.ConnectedTextureBehaviour>> CONNECTED_TEXTURES = new LinkedHashMap<>();

    /** Upstream Create's {@code CreateRegistrate.connectedTextures} (client resolved, like {@link #blockModel}). */
    public static <T extends Block> NonNullConsumer<? super T> connectedTextures(
            Supplier<? extends com.zurrtum.create.client.foundation.block.connected.ConnectedTextureBehaviour> behaviour) {
        return block -> CONNECTED_TEXTURES.put(block, behaviour);
    }

    /** Casing connectivity callbacks, run on the client by {@link #registerBlockModels}. */
    private static final java.util.List<Runnable> CASINGS = new java.util.ArrayList<>();

    /** Upstream Create's {@code CreateRegistrate.casingConnectivity} (client resolved, like {@link #blockModel}). */
    public static <T extends Block> NonNullConsumer<? super T> casingConnectivity(java.util.function.BiConsumer<T, CasingConnectivity> consumer) {
        return block -> CASINGS.add(() -> consumer.accept(block, new CasingConnectivity()));
    }

    /** Client only: call once during client init. */
    public static void registerBlockModels() {
        CASINGS.forEach(Runnable::run);
        BLOCK_MODELS.forEach((block, factory) -> com.zurrtum.create.client.AllModels.register(block, factory.get()));
        CONNECTED_TEXTURES.forEach((block, behaviour) -> com.zurrtum.create.client.AllModels.register(block,
            com.zurrtum.create.client.infrastructure.model.CTModel.of(behaviour.get())));
    }

    public static boolean isInCreativeTab(RegistryEntry<?, ?> entry, ResourceKey<CreativeModeTab> tab) {
        return TAB_LOOKUP.get(entry) == tab;
    }

    @Override
    protected <R, T extends R> RegistryEntry<R, T> accept(String name, ResourceKey<? extends Registry<R>> type,
                                                         Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator,
                                                         NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
        if (currentTab != null)
            TAB_LOOKUP.put(entry, currentTab);
        return entry;
    }

    /**
     * Upstream Create's {@code paletteStoneBlock}: a plain stone block with an item.
     *
     * <p>PORT-NOTE(26.1): the blockstate, item model and replaceable tags were datagen only; the committed generated
     * resources cover them.</p>
     */
    public com.jesz.createdieselgenerators.port.registrate.builders.BlockBuilder<Block, CreateRegistrate> paletteStoneBlock(String name, NonNullSupplier<Block> propertiesFrom, boolean worldGenStone, boolean hasNaturalVariants) {
        return block(name, Block::new)
                .initialProperties(propertiesFrom)
                .transform(TagGen.pickaxeOnly())
                .item()
                .build();
    }

    public <T extends DisplaySource> NoConfigBuilder<DisplaySource, T, CreateRegistrate> displaySource(
            String name, NonNullSupplier<T> supplier) {
        return generic(self(), name, (net.minecraft.resources.ResourceKey) CreateRegistries.DISPLAY_SOURCE.key(), supplier);
    }

    public <T extends MountedFluidStorageType<?>> NoConfigBuilder<MountedFluidStorageType<?>, T, CreateRegistrate> mountedFluidStorage(
            String name, NonNullSupplier<T> supplier) {
        return generic(self(), name, (net.minecraft.resources.ResourceKey) CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE.key(), supplier);
    }

    public <T extends MountedItemStorageType<?>> NoConfigBuilder<MountedItemStorageType<?>, T, CreateRegistrate> mountedItemStorage(
            String name, NonNullSupplier<T> supplier) {
        return generic(self(), name, (net.minecraft.resources.ResourceKey) CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE.key(), supplier);
    }

    public <T extends DisplayTarget> NoConfigBuilder<DisplayTarget, T, CreateRegistrate> displayTarget(
            String name, NonNullSupplier<T> supplier) {
        return generic(self(), name, (net.minecraft.resources.ResourceKey) CreateRegistries.DISPLAY_TARGET.key(), supplier);
    }

}
