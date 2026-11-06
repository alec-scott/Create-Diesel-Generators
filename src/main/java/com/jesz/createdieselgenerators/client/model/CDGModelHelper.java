package com.jesz.createdieselgenerators.client.model;

import com.zurrtum.create.client.flywheel.lib.model.baked.ItemModelRenderHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Shared pieces of CDG's custom item models (the 26.1 replacement of Create 6's CustomRenderedItemModelRenderer).
 * <p>
 * Upstream's renderers drew in a space centred on the item, after the model's display transform, and
 * PartialItemModelRenderer shifted the baked quads by -0.5 itself. 26.1 layers keep the quads in block space
 * (0..1), apply display transform, then -0.5, then the layer's local transform; so a transform {@code X} written
 * the way the upstream renderers wrote it becomes {@code T(0.5) * X * T(-0.5)} here ({@link #fromCentred}).
 */
public final class CDGModelHelper {
    private CDGModelHelper() {
    }

    /** A baked model with the settings (display transforms etc.) of the model it was baked from. */
    public record Baked(ModelRenderProperties settings, List<BakedQuad> quads) {
        public static Baked of(ModelBaker baker, Identifier id) {
            ResolvedModel model = baker.getModel(id);
            TextureSlots textures = model.getTopTextureSlots();
            List<BakedQuad> quads = model.bakeTopGeometry(textures, baker, BlockModelRotation.IDENTITY).getAll();
            return new Baked(ModelRenderProperties.fromResolvedModel(baker, model, textures), quads);
        }
    }

    public static Supplier<Vector3fc[]> extentsOf(List<BakedQuad> quads) {
        return com.google.common.base.Suppliers.memoize(() -> net.minecraft.client.renderer.item.CuboidItemModelWrapper.computeExtents(quads));
    }

    /** Local transform for a baked partial model that upstream drew after transform {@code centred} (item-centred space). */
    public static Matrix4f fromCentred(Matrix4f centred) {
        return new Matrix4f().translate(0.5f, 0.5f, 0.5f).mul(centred).translate(-0.5f, -0.5f, -0.5f);
    }

    public static void foil(ItemStackRenderState state, @Nullable ItemStack stack, LayerRenderState... layers) {
        if (stack != null && stack.hasFoil()) {
            state.appendModelIdentityElement(FoilType.STANDARD);
            for (LayerRenderState layer : layers)
                layer.setFoilType(FoilType.STANDARD);
        }
    }

    /**
     * Appends the layers of {@code item} the way upstream's {@code renderStatic(item, context, ...)} inside the host's
     * renderer drew them: the host's display transform on the outside, then {@code centred}, then the item's own
     * display transform for {@code itemContext}.
     */
    public static void appendHeldItem(
        ItemStackRenderState state,
        ItemModelResolver resolver,
        ItemStack item,
        ItemDisplayContext itemContext,
        ItemTransform hostTransform,
        Matrix4f centred,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        int first = state.activeLayerCount;
        resolver.appendItemLayers(state, item, itemContext, level, owner, seed);
        for (int i = first; i < state.activeLayerCount; i++) {
            LayerRenderState layer = state.layers[i];
            Matrix4f own = new Matrix4f(ItemModelRenderHelper.getPose(false, layer.itemTransform));
            Matrix4f local = new Matrix4f().translate(0.5f, 0.5f, 0.5f).mul(centred).mul(own).mul(layer.localTransform);
            layer.localTransform.set(local);
            layer.itemTransform = hostTransform;
        }
    }
}
