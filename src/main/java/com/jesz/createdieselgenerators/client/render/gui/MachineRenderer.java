package com.jesz.createdieselgenerators.client.render.gui;

import com.mojang.blaze3d.platform.Lighting.Entry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.client.catnip.gui.render.BlockBakedQuadOutput;
import com.zurrtum.create.client.flywheel.lib.model.baked.ModelConsumer;
import com.zurrtum.create.client.flywheel.lib.model.baked.ModelRenderHelper;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Base of the machine drawings (what upstream did with {@code AnimatedKinetics} and {@code GuiGameElement}), drawn with
 * the pattern of Create Fly's own picture-in-picture renderers (press, mixer, spout): the same pose set-up, the same
 * block drawing through {@link ModelRenderHelper}. After {@link #renderToTexture} has set up the frame, local
 * coordinates are upstream's: the corner of the first block is the origin, x and z as in the world, {@code atLocal}'s y
 * pointing down. See {@link MachineState}.
 */
public abstract class MachineRenderer<T extends MachineState> extends PictureInPictureRenderer<T> {
    private final int below;
    protected final BlockBakedQuadOutput output;
    private final SinglePosVirtualBlockGetter world = SinglePosVirtualBlockGetter.createFullBright();
    private ModelConsumer blockRenderer;

    protected MachineRenderer(BufferSource bufferSource, int below) {
        super(bufferSource);
        this.below = below;
        output = new BlockBakedQuadOutput(bufferSource);
    }

    @Override
    protected float getTranslateY(int height, int windowScaleFactor) {
        return height - (below - MachineState.ORIGIN_DROP) * windowScaleFactor;
    }

    @Override
    protected void renderToTexture(T state, PoseStack matrices) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Entry.ENTITY_IN_UI);
        matrices.scale(1, 1, -1);
        matrices.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrices.mulPose(Axis.YP.rotationDegrees(22.5f));
        matrices.translate(-0.5f, 0, -0.5f);
        output.setPoseStack(matrices);
        blockRenderer = ModelRenderHelper.getHelper(output);
        draw(state, matrices);
        output.clear();
    }

    protected abstract void draw(T state, PoseStack matrices);

    /** {@code GuiGameElement.of(state).atLocal(x, y, z)} with an optional rotation. */
    protected void block(BlockState state, double x, double y, double z, Consumer<PoseStack> rotation) {
        element(state, Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state), x, y, z, rotation);
    }

    /** {@code blockElement(partial).atLocal(x, y, z)} with an optional rotation. */
    protected void partial(PartialModel partial, double x, double y, double z, Consumer<PoseStack> rotation) {
        element(Blocks.AIR.defaultBlockState(), partial.get(), x, y, z, rotation);
    }

    private void element(BlockState state, BlockStateModel model, double x, double y, double z, Consumer<PoseStack> rotation) {
        PoseStack matrices = output.poseStack;
        matrices.pushPose();
        matrices.translate(x, y, z);
        matrices.scale(1, -1, 1);
        if (rotation != null)
            rotation.accept(matrices);
        world.blockState(state);
        output.updateBuffer(model);
        blockRenderer.tesselateBlock(0, 0, 0, world, BlockPos.ZERO, state, model, 42L);
        matrices.popPose();
    }

    /** {@code rotateBlock}: about the centre of the block. */
    protected static Consumer<PoseStack> aroundCentreY(float degrees) {
        return matrices -> matrices.rotateAround(Axis.YP.rotationDegrees(degrees), 0.5f, 0.5f, 0.5f);
    }

    /** {@code rotate}: about the origin of the element. */
    protected static Consumer<PoseStack> aroundOriginY(float degrees) {
        return matrices -> matrices.mulPose(Axis.YP.rotationDegrees(degrees));
    }

    protected static Consumer<PoseStack> aroundOriginZ(float degrees) {
        return matrices -> matrices.mulPose(Axis.ZP.rotationDegrees(degrees));
    }
}
