/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.vihaan.ferritymod.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import com.vihaan.ferritymod.client.renderer.FerrityRenderer;

@EventBusSubscriber(Dist.CLIENT)
public class FerritymodModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(FerritymodModEntities.FERRITY.get(), FerrityRenderer::new);
	}
}