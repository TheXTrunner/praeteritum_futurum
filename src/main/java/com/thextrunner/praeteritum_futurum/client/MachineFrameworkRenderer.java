package com.thextrunner.praeteritum_futurum.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom block entity renderer for the Machine Framework.
 * <p>
 * Renders the 3D IO overlay models (input/output/open ports) on the block's faces.
 * The overlays visually indicate which faces are configured for item transfer and in
 * which direction. Each overlay model is loaded by {@link OverlayModelLoader} and
 * positioned on the appropriate face based on the machine's facing direction.
 * <p>
 * The renderer runs every frame on the client side. It does not render the base
 * block model — that is handled by Minecraft's standard block rendering pipeline.
 * This renderer only adds the IO port overlays on top.
 */
public class MachineFrameworkRenderer implements BlockEntityRenderer<MachineFrameworkBlockEntity> {

    /** Logger for debugging overlay rendering issues. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineFrameworkRenderer.class);

    /**
     * Constructs the Machine Framework renderer.
     * No special initialization is needed — models are loaded by OverlayModelLoader.
     *
     * @param context The renderer provider context (unused)
     */
    public MachineFrameworkRenderer(BlockEntityRendererProvider.Context context) {
    }

    /**
     * Renders the IO overlay models on the Machine Framework block.
     * <p>
     * Called every render frame by Minecraft's block entity rendering system.
     * Delegates to {@link #renderIOOverlays} to draw the port models on each face.
     */
    @Override
    public void render(MachineFrameworkBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        long levelTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float time = levelTime + partialTick;

        poseStack.pushPose();

        // Render the 3D IO overlay models on each face
        // The overlay loader handles all positioning and rotation per-face,
        // so we pass the pose stack at the block origin (0,0,0 to 1,1,1).
        renderIOOverlays(poseStack, bufferSource, blockEntity, time, packedLight, packedOverlay);

        poseStack.popPose();
    }

    /**
     * Renders the IO overlay models on all six faces of the block.
     * <p>
     * For each relative side, checks the effective face mode. If the mode is not
     * DISABLED, retrieves the corresponding baked model and renders it on the
     * corresponding world-facing block face.
     *
     * @param poseStack The transformation matrix stack
     * @param bufferSource The buffer source for vertex data
     * @param blockEntity The machine framework block entity
     * @param time The current game time (unused, reserved for animations)
     * @param packedLight The packed light value
     * @param packedOverlay The packed overlay value
     */
    private void renderIOOverlays(PoseStack poseStack, MultiBufferSource bufferSource, MachineFrameworkBlockEntity blockEntity,
                                  float time, int packedLight, int packedOverlay) {
        Direction facing = blockEntity.getBlockState().hasProperty(MachineFrameworkBlock.FACING)
                ? blockEntity.getBlockState().getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;

        for (RelativeSide side : RelativeSide.values()) {
            IoModuleItem.FaceMode faceMode = blockEntity.getEffectiveFaceMode(side);
            if (faceMode == IoModuleItem.FaceMode.DISABLED) continue;

            BakedModel overlayModel = OverlayModelLoader.getModelForMode(faceMode);
            if (overlayModel == null) {
                continue;
            }

            // Convert RelativeSide to world Direction based on block facing
            Direction faceDir = side.toWorldDirection(facing);

            // Use white color tint to preserve the model's original texture colors
            float red = 1.0f;
            float green = 1.0f;
            float blue = 1.0f;

            OverlayModelLoader.renderOverlayOnFace(poseStack, bufferSource, packedLight, packedOverlay,
                    overlayModel, faceDir, red, green, blue);
        }
    }
}