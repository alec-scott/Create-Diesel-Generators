package com.jesz.createdieselgenerators.mixins;

import com.jesz.createdieselgenerators.client.render.MoldBasinRender;
import com.zurrtum.create.client.content.processing.basin.BasinRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// PORT-NOTE(26.1): client-only. Carries the mold data of MoldBasinRender from extraction to submission.
@Mixin(BasinRenderer.BasinRenderState.class)
public class BasinRenderStateMixin implements MoldBasinRender.Holder {
    @Unique
    private MoldBasinRender.Data createdieselgenerators$moldData;

    @Override
    public MoldBasinRender.Data cdg$getMoldData() {
        return createdieselgenerators$moldData;
    }

    @Override
    public void cdg$setMoldData(MoldBasinRender.Data data) {
        createdieselgenerators$moldData = data;
    }
}
