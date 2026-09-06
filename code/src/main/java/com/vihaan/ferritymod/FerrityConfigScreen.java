package com.vihaan.ferritymod;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class FerrityConfigScreen {

	public static void register(
			ModContainer container
	) {

		container.registerExtensionPoint(
				IConfigScreenFactory.class,
				ConfigurationScreen::new
		);
	}

	private FerrityConfigScreen() {
	}
}