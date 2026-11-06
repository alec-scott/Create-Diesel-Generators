package com.jesz.createdieselgenerators.content.turret;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.content.trains.schedule.hat.TrainHatInfo;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * PORT-NOTE(26.1): render layers get a render state instead of the entity, so the "has a turret position" check
 * ({@code IEntity.getTurretPos() != null}, upstream's {@code shouldRenderOn}) is made at extraction time by
 * {@code LivingEntityRendererMixin} and kept under {@link #HAT_INFO}. Placement follows Create Fly's own hat layer
 * (head model part, {@link TrainHatInfo} offset/scale/cube), which replaces the 1.21.1 AgeableListModel and
 * HierarchicalModel handling. The turret position is not synced: the turret block entity ticks on both sides and sets
 * it on the entities it finds in both worlds, as upstream does.
 */
public class TurretOperatorHatLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    /** Set to the hat info of the entity while it is operating a turret, left unset otherwise. */
    public static final RenderStateDataKey<TrainHatInfo> HAT_INFO = RenderStateDataKey.create(() -> "createdieselgenerators:turret_operator_hat");

    public TurretOperatorHatLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack ms, SubmitNodeCollector queue, int light, S renderState, float limbAngle, float limbDistance) {
        TrainHatInfo info = ((FabricRenderState) renderState).getData(HAT_INFO);
        if (info == null)
            return;

        M entityModel = getParentModel();
        ms.pushPose();

        ModelPart lastChild;
        if (entityModel instanceof HeadedModel model) {
            List<ModelPart> partsToHead = TrainHatInfo.getAdjustedPart(info, model.getHead(), "");
            entityModel.root().translateAndRotate(ms);
            model.translateToHead(ms);
            for (int i = 1; i < partsToHead.size(); i++)
                partsToHead.get(i).translateAndRotate(ms);
            lastChild = partsToHead.getLast();
        } else {
            List<ModelPart> partsToHead = TrainHatInfo.getAdjustedPart(info, entityModel.root(), "head");
            partsToHead.forEach(part -> part.translateAndRotate(ms));
            lastChild = partsToHead.getLast();
        }

        if (!lastChild.isEmpty()) {
            ModelPart.Cube cube = lastChild.cubes.get(Mth.clamp(info.cubeIndex(), 0, lastChild.cubes.size() - 1));
            ms.translate(info.offset().x() / 16.0F, (cube.minY - cube.maxY + info.offset().y()) / 16.0F, info.offset().z() / 16.0F);
            float max = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / 8.0F * info.scale();
            ms.scale(max, max, max);
        }

        ms.scale(1, -1, -1);
        ms.translate(0, -2.25F / 16.0F, 0);
        ms.mulPose(Axis.XP.rotationDegrees(-8.5F));
        BlockState air = Blocks.AIR.defaultBlockState();
        CachedBuffers.partial(CDGPartialModels.TURRET_OPERATOR_HAT, air).light(light).submit(ms, queue);
        ms.popPose();
    }
}
