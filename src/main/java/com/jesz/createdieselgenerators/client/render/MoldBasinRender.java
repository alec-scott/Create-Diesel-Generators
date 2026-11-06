package com.jesz.createdieselgenerators.client.render;

import com.jesz.createdieselgenerators.CDGItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.client.content.processing.basin.BasinRenderer.BasinRenderState;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.filtering.FilteringRenderer.FilterRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.content.processing.basin.BasinBlockEntity;
import com.zurrtum.create.content.processing.basin.BasinInventory;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Was MoldItemRenderer.renderInBasin / renderItemsOnMold, called from BasinRendererMixin.renderSafe. Upstream drew
 * nothing else of the basin while a mold was inside it (no fluid, no ingredient animation): filter, then every stack
 * of the basin, molds large and flat in the basin, anything else small and flat on top of them.
 * <p>
 * Create Fly's basin renderer extracts a render state and submits it later, so the work is split the same way: the
 * client {@code BasinRendererMixin} calls {@link #extract} at the head of {@code extractRenderState} and
 * {@link #submit} at the head of {@code submit}, with the data kept on the render state through
 * {@link Holder} (implemented on {@code BasinRenderState} by {@code BasinRenderStateMixin}).
 */
public final class MoldBasinRender {
    private MoldBasinRender() {
    }

    /** Implemented by BasinRenderState through a mixin. */
    public interface Holder {
        @Nullable Data cdg$getMoldData();

        void cdg$setMoldData(@Nullable Data data);
    }

    public record Item(ItemStackRenderState renderState, boolean mold, Vec3 offset) {
    }

    public record Data(@Nullable FilterRenderState filter, List<Item> items) {
    }

    /** @return true when the basin holds a mold and the state was filled, so the normal extraction is skipped. */
    public static boolean extract(
        BasinBlockEntity basin,
        BasinRenderState state,
        Vec3 cameraPos,
        ItemModelResolver itemModelResolver
    ) {
        ((Holder) state).cdg$setMoldData(null);
        BasinInventory inv = basin.itemCapability;
        Level level = basin.getLevel();
        if (inv == null || level == null)
            return false;
        boolean hasMold = false;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            if (inv.getItem(slot).is(CDGItems.MOLD.get())) {
                hasMold = true;
                break;
            }
        }
        if (!hasMold)
            return false;

        BlockPos pos = basin.getBlockPos();
        BlockState blockState = basin.getBlockState();
        int light = SmartBlockEntityRenderer.getLightCoords(level, pos);
        FilterRenderState filter = FilteringRenderer.getFilterRenderState(
            basin,
            blockState,
            itemModelResolver,
            basin.isVirtual() ? -1 : cameraPos.distanceToSqr(VecHelper.getCenterOf(pos))
        );
        RandomSource r = RandomSource.create(pos.hashCode());
        List<Item> items = new ArrayList<>();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.is(CDGItems.MOLD.get())) {
                items.add(new Item(renderState(itemModelResolver, stack, ItemDisplayContext.GROUND, level), true, Vec3.ZERO));
                continue;
            }
            // Upstream drew the offset for every stack, empty ones included, so the random sequence is the same.
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, r, (float) 1 / 16);
            if (!stack.isEmpty())
                items.add(new Item(renderState(itemModelResolver, stack, ItemDisplayContext.FIXED, level), false, offset));
        }
        ((Holder) state).cdg$setMoldData(new Data(filter, items));
        state.blockPos = pos;
        state.blockState = blockState;
        state.lightCoords = light;
        state.blockEntityType = basin.getType();
        return true;
    }

    private static ItemStackRenderState renderState(ItemModelResolver resolver, ItemStack stack, ItemDisplayContext context, Level level) {
        ItemStackRenderState renderState = new ItemStackRenderState();
        renderState.displayContext = context;
        resolver.appendItemLayers(renderState, stack, context, level, null, 0);
        return renderState;
    }

    /** @return true when the state carried mold data and was submitted. */
    public static boolean submit(BasinRenderState state, PoseStack ms, SubmitNodeCollector queue) {
        Data data = ((Holder) state).cdg$getMoldData();
        if (data == null)
            return false;
        if (data.filter() != null)
            data.filter().submit(state.blockState, queue, ms, state.lightCoords);
        for (Item item : data.items()) {
            ms.pushPose();
            if (item.mold()) {
                // renderInBasin
                ms.translate(0.5, 0.7, 0.5);
                ms.mulPose(Axis.XP.rotationDegrees(90));
                ms.scale(1.75f, 1.75f, 1.75f);
                ms.translate(0, -0.125, 0);
            } else {
                ms.translate(0.5, 0.74, 0.5);
                ms.mulPose(Axis.XP.rotationDegrees(90));
                ms.scale(0.5f, 0.5f, 0.5f);
                ms.translate(item.offset());
            }
            item.renderState().submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            ms.popPose();
        }
        return true;
    }
}
