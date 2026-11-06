package com.jesz.createdieselgenerators.client;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;

import java.util.function.Consumer;

/**
 * PORT-NOTE(26.1): a client-only per-tick hook for non-kinetic block entities (ambient audio), registered through
 * Create Fly's client behaviour registry so it never exists on the server.
 */
public class CDGClientTickBehaviour<T extends SmartBlockEntity> extends BlockEntityBehaviour<T> {
    public static final BehaviourType<CDGClientTickBehaviour<?>> TYPE = new BehaviourType<>();
    private final Consumer<T> action;

    public CDGClientTickBehaviour(T be, Consumer<T> action) {
        super(be);
        this.action = action;
    }

    @Override
    public void tick() {
        action.accept(blockEntity);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
