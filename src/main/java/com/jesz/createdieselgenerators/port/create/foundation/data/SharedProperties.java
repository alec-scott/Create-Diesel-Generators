package com.jesz.createdieselgenerators.port.create.foundation.data;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Upstream Create's block property presets (Create Fly has no equivalent). */
public class SharedProperties {

	public static Block wooden() {
		return Blocks.STRIPPED_SPRUCE_WOOD;
	}

	public static Block stone() {
		return Blocks.ANDESITE;
	}

	public static Block softMetal() {
		return Blocks.GOLD_BLOCK;
	}

	public static Block copperMetal() {
		return Blocks.COPPER_BLOCK;
	}

	public static Block netheriteMetal() {
		return Blocks.NETHERITE_BLOCK;
	}
}
