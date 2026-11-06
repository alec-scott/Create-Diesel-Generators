package com.jesz.createdieselgenerators.port.create.foundation.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/** Particle data whose provider needs the particle's sprite set (registered as a pending provider). */
public interface ICustomParticleDataWithSprite<D extends ParticleOptions> extends ICustomParticleData<D> {

	@Override
	@Environment(EnvType.CLIENT)
	default ParticleProvider<D> getFactory() {
		throw new IllegalAccessError("This particle type uses a sprite-set provider; see getMetaFactory()");
	}

	@Environment(EnvType.CLIENT)
	ParticleProviderRegistry.PendingParticleProvider<D> getMetaFactory();

	@Override
	@Environment(EnvType.CLIENT)
	default void register(ParticleType<D> type) {
		ParticleProviderRegistry.getInstance().register(type, this.getMetaFactory());
	}

}
