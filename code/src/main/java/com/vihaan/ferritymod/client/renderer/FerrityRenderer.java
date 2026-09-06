package com.vihaan.ferritymod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import com.vihaan.ferritymod.entity.FerrityEntity;

import org.joml.Quaternionf;

public class FerrityRenderer
		extends EntityRenderer<FerrityEntity, LivingEntityRenderState> {

	private static final Identifier TEXTURE =
			Identifier.parse("ferritymod:textures/entities/ferritytexture.png");

	public FerrityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.25f;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public void extractRenderState(
			FerrityEntity entity,
			LivingEntityRenderState state,
			float partialTicks
	) {
		super.extractRenderState(entity, state, partialTicks);
	}

	@Override
	public void submit(
			LivingEntityRenderState state,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState camera
	) {
		super.submit(state, poseStack, collector, camera);

		poseStack.pushPose();

		poseStack.translate(0.0F, 0.5F, 0.0F);

		poseStack.mulPose(
				new Quaternionf().rotationYXZ(
						(float) Math.toRadians(-camera.yRot),
						(float) Math.toRadians(camera.xRot),
						0.0F
				)
		);

		poseStack.scale(0.5F, 0.5F, 0.5F);

		collector.submitCustomGeometry(
				poseStack,
				RenderTypes.entityCutout(TEXTURE),
				(pose, vertices) ->
						drawFerrity(pose, vertices, state.lightCoords)
		);

		poseStack.popPose();
	}

	private static void drawFerrity(
			PoseStack.Pose pose,
			VertexConsumer vertices,
			int light
	) {
		addVertex(vertices, pose, -1.0F, -1.0F, 0.0F, 1.0F, light);
		addVertex(vertices, pose,  1.0F, -1.0F, 1.0F, 1.0F, light);
		addVertex(vertices, pose,  1.0F,  1.0F, 1.0F, 0.0F, light);
		addVertex(vertices, pose, -1.0F,  1.0F, 0.0F, 0.0F, light);
	}

	private static void addVertex(
			VertexConsumer vertices,
			PoseStack.Pose pose,
			float x,
			float y,
			float u,
			float v,
			int light
	) {
		vertices.addVertex(pose, x, y, 0.0F)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0.0F, 0.0F, 1.0F);
	}
}