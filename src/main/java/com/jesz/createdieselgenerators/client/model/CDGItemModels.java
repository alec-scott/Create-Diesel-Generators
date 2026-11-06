package com.jesz.createdieselgenerators.client.model;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.resources.Identifier;

/** Registers CDG's item model types, as Create Fly's AllModels does for its own. Called from the client entrypoint. */
public class CDGItemModels {
    private static <T extends ItemModel.Unbaked> void register(Identifier id, MapCodec<T> codec) {
        ItemModels.ID_MAPPER.put(id, codec);
    }

    public static void register() {
        register(LighterModel.ID, LighterModel.Unbaked.CODEC);
        register(HammerModel.ID, HammerModel.Unbaked.CODEC);
        register(WireCuttersModel.ID, WireCuttersModel.Unbaked.CODEC);
        register(ChemicalSprayerModel.ID, ChemicalSprayerModel.Unbaked.CODEC);
        register(MoldModel.ID, MoldModel.Unbaked.CODEC);
    }
}
