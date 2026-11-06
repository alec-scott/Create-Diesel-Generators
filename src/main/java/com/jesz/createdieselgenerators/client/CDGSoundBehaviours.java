package com.jesz.createdieselgenerators.client;

import com.jesz.createdieselgenerators.content.diesel_engine.EngineSoundInstance;
import com.jesz.createdieselgenerators.content.diesel_engine.EngineUpgrades;
import com.jesz.createdieselgenerators.content.diesel_engine.IEngine;
import com.jesz.createdieselgenerators.CDGSoundEvents;
import com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.distillation.DistillationTankBlockEntity;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackCrankBlockEntity;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackHoleBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * PORT-NOTE(26.1): the block entities' client-only sound code (was {@code tickClient} behind
 * {@code CatnipServices.PLATFORM.executeOnClientOnly}), as client behaviours that only exist on the client.
 */
public final class CDGSoundBehaviours {
    private CDGSoundBehaviours() {
    }

    /** A positioned looping sound (was CrankSoundInstance / PumpjackSoundInstance / DistillationSoundInstance). */
    public static class LoopingSoundInstance extends AbstractTickableSoundInstance {
        public LoopingSoundInstance(SoundEvent event, float volume, float pitch, Vec3 pos) {
            super(event, SoundSource.BLOCKS, RandomSource.create());
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
            this.volume = volume;
            this.pitch = pitch;
            this.looping = true;
            this.delay = 0;
            this.attenuation = Attenuation.LINEAR;
        }

        @Override
        public void tick() {
        }
    }

    private abstract static class Base<T extends SmartBlockEntity> extends BlockEntityBehaviour<T> {
        public static final BehaviourType<Base<?>> TYPE = new BehaviourType<>();

        Base(T be) {
            super(be);
        }

        @Override
        public BehaviourType<?> getType() {
            return TYPE;
        }

        @Override
        public void tick() {
            if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide())
                tickSound();
        }

        abstract void tickSound();

        static void stop(AbstractTickableSoundInstance instance) {
            if (instance != null)
                Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    public static class Crank extends Base<PumpjackCrankBlockEntity> {
        private AbstractTickableSoundInstance rumbleInstance;
        private AbstractTickableSoundInstance hissInstance;

        public Crank(PumpjackCrankBlockEntity be) {
            super(be);
        }

        @Override
        void tickSound() {
            PumpjackCrankBlockEntity be = blockEntity;
            boolean isActive = be.getSpeed() != 0 && be.getBearing() != null;
            Minecraft mc = Minecraft.getInstance();

            if (isActive) {
                if (rumbleInstance == null || rumbleInstance.isStopped() || !mc.getSoundManager().isActive(rumbleInstance)) {
                    rumbleInstance = new LoopingSoundInstance(SoundEvents.MINECART_RIDING, 0.3f, 0.4f, Vec3.atCenterOf(be.getBlockPos()));
                    mc.getSoundManager().play(rumbleInstance);
                }
                if (hissInstance == null || hissInstance.isStopped()) {
                    hissInstance = new LoopingSoundInstance(SoundEvents.BLASTFURNACE_FIRE_CRACKLE, 0.1f, 1.2f, Vec3.atCenterOf(be.getBlockPos()));
                    mc.getSoundManager().play(hissInstance);
                }

                float prevNorm = be.prevAngle % 360;
                float currNorm = be.angle % 360;

                boolean crossedBottom = (prevNorm < 10 && currNorm >= 10) || (prevNorm > 350 && currNorm <= 10);
                boolean crossedTop = (prevNorm < 190 && currNorm >= 190) || (prevNorm > 170 && currNorm <= 170);

                if (crossedBottom) {
                    mc.level.playLocalSound(be.getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.2f, 0.3f, false);
                    mc.level.playLocalSound(be.getBlockPos(), SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.1f, 0.5f, false);
                }
                if (crossedTop) {
                    mc.level.playLocalSound(be.getBlockPos(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.15f, 0.3f, false);
                    mc.level.playLocalSound(be.getBlockPos(), SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.1f, 0.6f, false);
                }
                if (crossedBottom && Math.random() < 0.3)
                    mc.level.playLocalSound(be.getBlockPos(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.15f, 0.5f, false);
            } else {
                stop(rumbleInstance);
                rumbleInstance = null;
                stop(hissInstance);
                hissInstance = null;
            }
        }

        @Override
        public void destroy() {
            stop(rumbleInstance);
            stop(hissInstance);
        }
    }

    public static class PumpjackHole extends Base<PumpjackHoleBlockEntity> {
        private AbstractTickableSoundInstance soundInstance;

        public PumpjackHole(PumpjackHoleBlockEntity be) {
            super(be);
        }

        @Override
        void tickSound() {
            if (blockEntity.isPumping()) {
                if (soundInstance == null || soundInstance.isStopped()) {
                    soundInstance = new LoopingSoundInstance(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 0.2f, 0.4f,
                            Vec3.atCenterOf(blockEntity.getBlockPos()));
                    Minecraft.getInstance().getSoundManager().play(soundInstance);
                }
            } else {
                stop(soundInstance);
                soundInstance = null;
            }
        }

        @Override
        public void destroy() {
            stop(soundInstance);
        }
    }

    public static class Distillation extends Base<DistillationTankBlockEntity> {
        private AbstractTickableSoundInstance soundInstance;

        public Distillation(DistillationTankBlockEntity be) {
            super(be);
        }

        @Override
        void tickSound() {
            if (blockEntity.isProcessing()) {
                if (soundInstance == null || soundInstance.isStopped()) {
                    int width = blockEntity.getWidthForSound();
                    int height = blockEntity.getHeightForSound();
                    soundInstance = new LoopingSoundInstance(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.5f, 0.45f,
                            Vec3.atCenterOf(blockEntity.getBlockPos().offset(width / 2, height / 2, width / 2)));
                    Minecraft.getInstance().getSoundManager().play(soundInstance);
                }
            } else {
                stop(soundInstance);
                soundInstance = null;
            }
        }

        @Override
        public void destroy() {
            stop(soundInstance);
        }
    }

    /** Was {@code EngineUpgrades.createSoundInstance} plus the engines' {@code tickClient}. */
    public static class Engine<T extends SmartBlockEntity & IEngine> extends Base<T> {
        private EngineSoundInstance soundInstance;

        public Engine(T be) {
            super(be);
        }

        private EngineSoundInstance create(Vec3 pos) {
            return new EngineSoundInstance(CDGSoundEvents.ENGINE_NORMAL.get(), SoundSource.NEUTRAL, pos, 0.2f);
        }

        @Override
        void tickSound() {
            T be = blockEntity;
            EngineUpgrades upgrade = be.getUpgrade();
            boolean active;
            Vec3 pos = Vec3.atCenterOf(be.getBlockPos());
            float pitch = upgrade.getPitchMultiplier(be) * be.getFuelSoundPitch() * be.getThrottle();
            float volume = upgrade.getVolume(be);

            if (be instanceof HugeDieselEngineBlockEntity) {
                active = be.enabled() && be.getThrottle() > 0;
                pitch /= 2;
                volume *= be.getThrottle();
            } else if (be instanceof ModularDieselEngineBlockEntity modular) {
                active = modular.isController() && be.enabled() && be.getThrottle() > 0 && !modular.isOverStressed();
                int length = modular.getLength();
                if (be.getBlockState().getValue(com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlock.FACING).getAxis() == Direction.Axis.X)
                    pos = pos.add((double) length / 2 - 0.5, 0, 0);
                else
                    pos = pos.add(0, 0, (double) length / 2 - 0.5);
            } else if (be instanceof DieselEngineBlockEntity normal) {
                active = be.enabled() && be.getThrottle() > 0 && !normal.isOverStressed();
            } else {
                active = false;
            }

            if (active) {
                if (soundInstance == null || soundInstance.isStopped() || soundInstance.getX() != pos.x || soundInstance.getZ() != pos.z) {
                    Minecraft.getInstance().getSoundManager().play(soundInstance = create(pos));
                } else if (soundInstance.active()) {
                    soundInstance.keepAlive();
                    soundInstance.setPitch(pitch);
                    soundInstance.setVolume(volume);
                }
            } else if (soundInstance != null) {
                soundInstance.fadeOut();
                soundInstance = null;
            }
        }

        @Override
        public void destroy() {
            if (soundInstance != null)
                soundInstance.fadeOut();
        }
    }
}
