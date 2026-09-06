package com.vihaan.ferritymod.item;

import net.minecraft.world.item.Item;

public class FerrityitemItem extends Item {
	public FerrityitemItem(Item.Properties properties) {
		super(properties.stacksTo(1).fireResistant());
	}
}