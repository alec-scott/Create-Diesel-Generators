package com.jesz.createdieselgenerators;

import com.jesz.createdieselgenerators.content.tools.ChemicalSprayerProjectileEntity;
import com.jesz.createdieselgenerators.port.registrate.util.entry.EntityEntry;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityDimensions;

import static com.jesz.createdieselgenerators.CreateDieselGenerators.REGISTRATE;

public class CDGEntityTypes {

    public static final EntityEntry<ChemicalSprayerProjectileEntity> CHEMICAL_SPRAYER_PROJECTILE = REGISTRATE.entity("chemical_sprayer_projectile", ChemicalSprayerProjectileEntity::new, MobCategory.MISC)
                    .properties(b -> b.dimensions(EntityDimensions.fixed(0.5f, 0.5f)).trackRangeChunks(4)
                            .trackedUpdateRate(20)
                            .forceTrackedVelocityUpdates(true))
                    .renderer(() -> net.minecraft.client.renderer.entity.NoopRenderer::new).register();
    public static void register(){}
}
