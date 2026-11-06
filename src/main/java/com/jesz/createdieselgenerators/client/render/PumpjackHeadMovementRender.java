package com.jesz.createdieselgenerators.client.render;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackBearingBlockEntity;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackHoleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderBehaviour;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderState;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.foundation.virtualWorld.VirtualRenderWorld;
import com.zurrtum.create.content.contraptions.ControlledContraptionEntity;
import com.zurrtum.create.content.contraptions.bearing.BearingContraption;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

/** PORT-NOTE(26.1): was PumpjackHeadMovementBehaviour.renderInContraption: the rope from the head down to the well. */
public class PumpjackHeadMovementRender implements MovementRenderBehaviour {
    @Override
    @Nullable
    public MovementRenderState getRenderState(Vec3 camera, Font textRenderer, MovementContext context,
                                              VirtualRenderWorld renderWorld, PoseStack.Pose transform, Matrix4f worldMatrix4f) {
        if (!(context.contraption instanceof BearingContraption contraption) || !(contraption.entity instanceof ControlledContraptionEntity entity))
            return null;
        BlockPos hole = context.data.read("HolePos", BlockPos.CODEC).orElse(null);
        if (hole == null || !(context.world.getBlockEntity(hole) instanceof PumpjackHoleBlockEntity))
            return null;
        Direction facing = contraption.getFacing();
        if (!(context.world.getBlockEntity(contraption.anchor.relative(facing.getOpposite())) instanceof PumpjackBearingBlockEntity))
            return null;

        float partialTicks = AnimationTickHolder.getPartialTicks();
        SuperByteBuffer rope = CachedBuffers.partial(CDGPartialModels.PUMPJACK_ROPE, context.state)
                .transform(transform).translate(context.localPos);
        Vec3 prevPos = context.position.subtract(context.motion);
        double yDst = Mth.lerp(partialTicks, prevPos.y, context.position.y) - hole.getY() - 0.8f;

        if (facing.getOpposite().getAxis() == Direction.Axis.X) {
            double zDst = Mth.lerp(partialTicks, prevPos.z, context.position.z) - hole.getZ() - 0.5f;
            float distance = (float) Math.sqrt(zDst * zDst + yDst * yDst);
            double angle = -entity.getAngle(partialTicks) - (180 * Math.atan2(yDst, zDst) / Math.PI) + 90;
            rope.translate(0.5, 0.5, 0.5).rotateXDegrees((float) angle).scale(1, distance, 1);
        } else {
            double xDst = Mth.lerp(partialTicks, prevPos.x, context.position.x) - hole.getX() - 0.5;
            float distance = (float) Math.sqrt(xDst * xDst + yDst * yDst);
            double angle = -entity.getAngle(partialTicks) + (180 * Math.atan2(yDst, xDst) / Math.PI) - 90;
            rope.translate(0.5, 0.5, 0.5).rotateZDegrees((float) angle).scale(1, distance, 1);
        }

        State state = new State();
        state.rope = rope.useLevelLight(context.world, worldMatrix4f).extractRenderState();
        return state;
    }

    public static class State implements MovementRenderState {
        public SuperByteBufferRenderState rope;

        @Override
        public void submit(PoseStack matrices, SubmitNodeCollector queue) {
            rope.submit(matrices, queue);
        }
    }
}
