package com.jesz.createdieselgenerators.content.canister;

import net.minecraft.client.renderer.rendertype.RenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class CanisterRenderer extends SmartBlockEntityRenderer<CanisterBlockEntity> {
    public CanisterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CanisterBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay);
        if(blockEntity.capacityEnchantLevel == 0)
            return;
        CachedBuffers.block(blockEntity.getBlockState())
                .center()
                .scale(1)
                .uncenter()
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderTypes.cutout()));
        CachedBuffers.block(blockEntity.getBlockState())
                .center()
                .scale(1)
                .uncenter()
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderTypes.glint()));
    }
}
