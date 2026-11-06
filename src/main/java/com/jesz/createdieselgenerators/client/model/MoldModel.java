package com.jesz.createdieselgenerators.client.model;

import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.jesz.createdieselgenerators.content.molds.MoldItem;
import com.jesz.createdieselgenerators.content.molds.MoldType;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.zurrtum.create.client.flywheel.lib.model.baked.ItemModelRenderHelper.submitQuads;

/**
 * Was MoldItemRenderer (and MoldType.model, baked when ModEvents registered the additional models). The model of the
 * mold type in the MOLD_TYPE component is drawn; without a type, or for a type without a model, the plain mold.
 * Every registered MoldType is picked up (the list is open to addons, as upstream).
 */
public class MoldModel implements ItemModel {
    public static final Identifier ID = CreateDieselGenerators.rl("model/mold");
    private static final Identifier ITEM_ID = CreateDieselGenerators.rl("item/mold");

    private record Variant(List<BakedQuad> quads, Supplier<Vector3fc[]> extents) {
    }

    private final ModelRenderProperties settings;
    private final Variant plain;
    private final Map<MoldType, Variant> types;

    private MoldModel(ModelRenderProperties settings, Variant plain, Map<MoldType, Variant> types) {
        this.settings = settings;
        this.plain = plain;
        this.types = types;
    }

    @Override
    public void update(
        ItemStackRenderState state,
        @Nullable ItemStack stack,
        ItemModelResolver resolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel world,
        @Nullable ItemOwner user,
        int seed
    ) {
        MoldType type = stack == null ? null : MoldItem.getMold(stack);
        Variant variant = type == null ? plain : types.getOrDefault(type, plain);
        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(variant);
        LayerRenderState layer = submitQuads(state, settings, displayContext, variant.quads());
        layer.setExtents(variant.extents());
        CDGModelHelper.foil(state, stack, layer);
    }

    public static class Unbaked implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(ITEM_ID);
            for (MoldType type : MoldType.types)
                resolver.markDependency(type.getModelId());
        }

        private static Variant variant(List<BakedQuad> quads) {
            return new Variant(quads, CDGModelHelper.extentsOf(quads));
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            CDGModelHelper.Baked baked = CDGModelHelper.Baked.of(baker, ITEM_ID);
            Map<MoldType, Variant> types = new HashMap<>();
            for (MoldType type : MoldType.types)
                types.put(type, variant(CDGModelHelper.Baked.of(baker, type.getModelId()).quads()));
            return new MoldModel(baked.settings(), variant(baked.quads()), types);
        }
    }
}
