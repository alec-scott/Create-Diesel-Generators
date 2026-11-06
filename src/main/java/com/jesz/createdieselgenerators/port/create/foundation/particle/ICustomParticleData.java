package com.jesz.createdieselgenerators.port.create.foundation.particle;

import com.mojang.serialization.MapCodec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Create's custom particle data contract, for Fabric.
 *
 * <p>PORT-NOTE(fabric): Create Fly has no equivalent; providers register through Fabric's
 * {@link ParticleProviderRegistry} instead of NeoForge's {@code RegisterParticleProvidersEvent}.</p>
 */
public interface ICustomParticleData<D extends ParticleOptions> {

	MapCodec<D> getCodec(ParticleType<D> type);

	StreamCodec<? super RegistryFriendlyByteBuf, D> getStreamCodec();

	default ParticleType<D> createType() {
		return new ParticleType<>(false) {
			@Override
			public MapCodec<D> codec() {
				return ICustomParticleData.this.getCodec(this);
			}

			@Override
			public StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec() {
				return ICustomParticleData.this.getStreamCodec();
			}
		};
	}

	@Environment(EnvType.CLIENT)
	ParticleProvider<D> getFactory();

	@Environment(EnvType.CLIENT)
	default void register(ParticleType<D> type) {
		ParticleProviderRegistry.getInstance().register(type, this.getFactory());
	}

}
