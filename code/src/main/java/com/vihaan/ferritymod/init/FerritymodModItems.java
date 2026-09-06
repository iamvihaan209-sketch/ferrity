/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.vihaan.ferritymod.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.Item;

import java.util.function.Function;

import com.vihaan.ferritymod.item.FerrityitemItem;
import com.vihaan.ferritymod.FerritymodMod;

public class FerritymodModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(FerritymodMod.MODID);
	public static final DeferredItem<Item> FERRITYITEM;
	static {
		FERRITYITEM = register("ferrityitem", FerrityitemItem::new);
	}

	// Start of user code block custom items
	// End of user code block custom items
	private static <I extends Item> DeferredItem<I> register(String name, Function<Item.Properties, ? extends I> supplier) {
		return REGISTRY.registerItem(name, supplier, Item.Properties::new);
	}
}