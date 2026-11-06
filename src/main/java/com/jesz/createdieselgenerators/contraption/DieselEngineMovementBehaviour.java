package com.jesz.createdieselgenerators.contraption;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.CarriageContraption;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;


public class DieselEngineMovementBehaviour extends MovementBehaviour {
    // PORT-NOTE(26.1): the train engine sound is client code (client/CDGTrainEngineSounds), set from the client entrypoint
    public static java.util.function.Consumer<MovementContext> clientTick;

    @Override
    public boolean isActive(MovementContext context) {
        return context.contraption instanceof CarriageContraption && super.isActive(context);
    }

    @Nullable
    @Override
    public ItemStack canBeDisabledVia(MovementContext context) {
        return CDGBlocks.DIESEL_ENGINE.asStack();
    }

    @Override
    public void tick(MovementContext context) {
        if (!context.world.isClientSide())
            return;

        if (clientTick != null)
            clientTick.accept(context);
    }
}
