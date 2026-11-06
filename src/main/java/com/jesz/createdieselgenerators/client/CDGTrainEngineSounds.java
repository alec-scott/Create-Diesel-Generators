package com.jesz.createdieselgenerators.client;

import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGSoundEvents;
import com.jesz.createdieselgenerators.content.diesel_engine.EngineSoundInstance;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.CarriageContraption;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** PORT-NOTE(26.1): was DieselEngineMovementBehaviour.clientTick (diesel engines on trains). */
public final class CDGTrainEngineSounds {
    private static final Map<Pair<UUID, BlockPos>, EngineSoundInstance> soundInstances = new HashMap<>();

    private CDGTrainEngineSounds() {
    }

    public static void tick(MovementContext context) {
        if (!(context.contraption instanceof CarriageContraption contraption) || !(contraption.entity instanceof CarriageContraptionEntity entity))
            return;
        if (entity.getCarriage() == null || entity.getCarriage().train == null)
            return;

        if (!CDGConfig.ENGINES_EMIT_SOUND_ON_TRAINS.get() || entity.getCarriage().train.derailed)
            return;

        double trainSpeed = context.motion.length() * 2 / (entity.getCarriage().train.maxSpeed() / 28);
        double lastTrainSpeed = context.data.getDoubleOr("TrainSpeed", 0);
        double acceleration = trainSpeed - lastTrainSpeed;
        context.data.putDouble("TrainSpeed", trainSpeed);

        float throttle = Mth.lerp(0.05f, context.data.getFloatOr("Throttle", 0),
                (float) Math.max(0, Math.min(1, acceleration)));
        context.data.putFloat("Throttle", throttle);

        Pair<UUID, BlockPos> key = Pair.of(entity.getUUID(), context.localPos);
        EngineSoundInstance instance = soundInstances.get(key);

        if (context.disabled) {
            if (instance != null)
                instance.fadeOut();
            return;
        }

        if (instance == null) {
            instance = new EngineSoundInstance(CDGSoundEvents.ENGINE_NORMAL.get(), SoundSource.NEUTRAL, context.position, 0.6f);
            instance.setVolume(1f);
            Minecraft.getInstance().getSoundManager().play(instance);
            soundInstances.put(key, instance);
        } else if (instance.isStopped())
            soundInstances.remove(key);

        instance.setPosition(context.position);

        if (instance.active()) {
            instance.keepAlive();
            float pitch = (float) Math.min(2, Math.max(Math.min(0.14, throttle) * 5 + trainSpeed / 28, 0.1f));
            instance.setPitch(pitch);
        }
    }
}
