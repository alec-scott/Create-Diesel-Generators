package com.jesz.createdieselgenerators;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import com.jesz.createdieselgenerators.port.porting_lib.util.DeferredHolder;
import com.jesz.createdieselgenerators.port.porting_lib.util.DeferredRegister;

public class CDGSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, CreateDieselGenerators.ID);

    public static DeferredHolder<SoundEvent, SoundEvent> ENGINE_NORMAL = registerSoundEvent("engine_normal");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CreateDieselGenerators.rl(name)));
    }

    public static void register() {
    }
}
