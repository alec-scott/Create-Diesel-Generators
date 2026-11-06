package com.jesz.createdieselgenerators.client.model;

import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static com.zurrtum.create.client.flywheel.lib.model.baked.ItemModelRenderHelper.submitQuads;

/**
 * Was ChemicalSprayerItemRenderer, for both the sprayer and the sprayer with lighter (the item definition names the
 * body model). The cog turns slowly, and fast while the holder is using the stack.
 */
public class ChemicalSprayerModel implements ItemModel {
    public static final Identifier ID = CreateDieselGenerators.rl("model/chemical_sprayer");
    private static final Identifier COG_ID = CreateDieselGenerators.rl("item/chemical_sprayer/cog");

    private final ModelRenderProperties settings;
    private final List<BakedQuad> quads;
    private final List<BakedQuad> cogQuads;
    private final Supplier<Vector3fc[]> extents;

    private ChemicalSprayerModel(ModelRenderProperties settings, List<BakedQuad> quads, List<BakedQuad> cogQuads) {
        this.settings = settings;
        this.quads = quads;
        this.cogQuads = cogQuads;
        extents = CDGModelHelper.extentsOf(quads);
    }

    /**
     * Upstream asked the local player whether it was using exactly this stack. Here the holder the model is drawn for
     * is asked (so another player's sprayer in third person spins too); without a holder (GUI, ground) it is still the
     * local player, as upstream. The stack is matched by identity, or by value to survive the first person hand
     * renderer keeping its own copy while the item is re-equipped.
     */
    private static boolean beingUsed(@Nullable ItemStack stack, @Nullable ItemOwner user) {
        if (stack == null)
            return false;
        LivingEntity holder = user instanceof LivingEntity living ? living : Minecraft.getInstance().player;
        if (holder == null || !holder.isUsingItem())
            return false;
        ItemStack used = holder.getItemInHand(holder.getUsedItemHand());
        return used == stack || ItemStack.matches(used, stack);
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
        state.appendModelIdentityElement(this);
        state.setAnimated();
        LayerRenderState body = submitQuads(state, settings, displayContext, quads);
        body.setExtents(extents);

        float worldTime = AnimationTickHolder.getRenderTime() / 10;
        float angle = worldTime * (beingUsed(stack, user) ? -200 : -25);
        angle %= 360;

        // Upstream: rotate about the item centre, then move the cog (modelled around the origin) to (0.5, 0.5, 0.53125).
        LayerRenderState cog = submitQuads(state, settings, displayContext, cogQuads);
        cog.localTransform.set(new Matrix4f()
            .rotateAround(Axis.ZP.rotationDegrees(angle), 0.5f, 0.5f, 0.5f)
            .translate(0.5f, 0.5f, 0.53125f));
        CDGModelHelper.foil(state, stack, body, cog);
    }

    public record Unbaked(Identifier model) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)).apply(i, Unbaked::new)
        );

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(model);
            resolver.markDependency(COG_ID);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            CDGModelHelper.Baked baked = CDGModelHelper.Baked.of(baker, model);
            return new ChemicalSprayerModel(baked.settings(), baked.quads(), CDGModelHelper.Baked.of(baker, COG_ID).quads());
        }
    }
}
