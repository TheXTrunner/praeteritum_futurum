package com.thextrunner.praeteritum_futurum.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads and renders the three IO overlay models (input, output, open) on the
 * Machine Framework block faces. These models are exported from Blockbench and
 * placed in assets/praeteritum_futurum/models/block/.
 */
public class OverlayModelLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayModelLoader.class);

    /** Baked model for INPUT mode overlay (blue port) */
    private static BakedModel inputModel = null;

    /** Baked model for OUTPUT mode overlay (red port) */
    private static BakedModel outputModel = null;

    /** Baked model for OPEN mode overlay (purple port) */
    private static BakedModel openModel = null;

    /** Tracks whether models have been loaded to avoid re-loading */
    private static boolean modelsLoaded = false;

    /**
     * Registers the three IO overlay models so Minecraft bakes them.
     * Since these models are not referenced by any blockstate, they must be
     * registered as standalone models so the model baker knows to load them.
     */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/io-input")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/io-output")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/io-open")));
    }

    /**
     * Called during model baking to load the three IO overlay models from the
     * Minecraft model registry. The models are JSON files exported from Blockbench
     * and placed in assets/praeteritum_futurum/models/block/.
     */
    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        if (modelsLoaded) return;

        ModelResourceLocation inputLoc = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/io-input"));
        ModelResourceLocation outputLoc = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/io-output"));
        ModelResourceLocation openLoc = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/io-open"));

        // Models are loaded from the baking result map
        inputModel = event.getModels().get(inputLoc);
        outputModel = event.getModels().get(outputLoc);
        openModel = event.getModels().get(openLoc);

        LOGGER.info("[IO Overlay] Models loaded - input: {}, output: {}, open: {}",
                inputModel != null ? "OK" : "NULL",
                outputModel != null ? "OK" : "NULL",
                openModel != null ? "OK" : "NULL");
        modelsLoaded = true;
    }

    /** Returns the baked model for a given IO face mode */
    public static BakedModel getModelForMode(IoModuleItem.FaceMode mode) {
        return switch (mode) {
            case INPUT -> inputModel;
            case OUTPUT -> outputModel;
            case OPEN -> openModel;
            case DISABLED -> null;
        };
    }

    /**
     * Renders an overlay baked model on a specific face of the block.
     * The model is rotated to match the face direction and tinted with the
     * given RGB color.
     *
     * The poseStack is expected to already be transformed to block-local coordinates
     * (0,0,0 to 1,1,1). The overlay is positioned flush against the specified face
     * and rotated to face outward.
     */
    public static void renderOverlayOnFace(PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay,
                                           BakedModel overlayModel, Direction face,
                                           float red, float green, float blue) {
        if (overlayModel == null) {
            LOGGER.warn("[IO Overlay] Skipping render: model is NULL for face {}", face);
            return;
        }

        poseStack.pushPose();

        // The baked model already has its coordinates in 0-1 block space
        // (Minecraft converts Blockbench 0-16 coords automatically).
        // The model is designed for the north face (z=0), centered at x=0.5, y=0.5.
        // Elements span [0.25,0.25,0] to [0.75,0.75,0.1875] - centered on the face.
        // We just need to rotate it to face the correct direction.

        switch (face) {
            case NORTH -> {
                // North face: z=0, facing -Z. Model is designed for this face.
                // No additional transform needed - model is already at z=0, centered.
            }
            case SOUTH -> {
                // South face: z=1, facing +Z
                // Rotate 180 around Y, then flip Z to position at z=1
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(Axis.YP.rotationDegrees(180f));
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }
            case EAST -> {
                // East face: x=1, facing +X
                // Rotate 90 around Y, then flip to position at x=1
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90f));
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }
            case WEST -> {
                // West face: x=0, facing -X
                // Rotate -90 around Y, then flip to position at x=0
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(Axis.YP.rotationDegrees(90f));
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }
            case UP -> {
                // Up face: y=1, facing +Y
                // Rotate -90 around X, then flip to position at y=1
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(Axis.XP.rotationDegrees(90f));
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }
            case DOWN -> {
                // Down face: y=0, facing -Y
                // Rotate 90 around X, then flip to position at y=0
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }
        }

        // Collect only the quads facing the target face direction (in model-local space, NORTH)
        // plus the unlit quads. This prevents side faces of the model from rendering
        // on adjacent block faces when the model is rotated, which caused the color
        // bleeding / ghost overlay effect.
        List<BakedQuad> quads = new ArrayList<>();
        // The model is designed for the north face, so get the north-facing quads
        quads.addAll(overlayModel.getQuads(null, Direction.NORTH, null));
        // Add unlit quads (edges, transparent parts, etc.)
        quads.addAll(overlayModel.getQuads(null, null, null));

        if (quads.isEmpty()) {
            LOGGER.warn("[IO Overlay] No quads collected for face {} - model may be empty", face);
            poseStack.popPose();
            return;
        }

        // Use the solid render type with the block atlas
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        // Use full brightness so the overlay is always visible regardless of block lighting
        int fullBright = 0xF000F0;

        for (BakedQuad quad : quads) {
            poseStack.last().pose();
            vertexConsumer.putBulkData(poseStack.last(), quad, red, green, blue, 1.0f, fullBright, packedOverlay, true);
        }

        poseStack.popPose();
    }
}
