package com.thextrunner.praeteritum_futurum.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class MachineFrameworkRenderer implements BlockEntityRenderer<MachineFrameworkBlockEntity> {

    private static final int FULL_BRIGHT = 15728880;

    public MachineFrameworkRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineFrameworkBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(MachineFrameworkBlock.FACING)
                ? state.getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;

        long levelTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float time = levelTime + partialTick;

        float hue = (time * 0.015f) % 1.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
        int baseR = (rgb >> 16) & 0xFF;
        int baseG = (rgb >> 8) & 0xFF;
        int baseB = rgb & 0xFF;

        float pulse = (float) (Math.sin(time * 0.1) * 0.3f + 0.7f);
        int red = (int) (baseR * pulse);
        int green = (int) (baseG * pulse);
        int blue = (int) (baseB * pulse);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);

        float yRot = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        // Render only the glowing layers and overlays dynamically on top of the baked block model
        VertexConsumer emissiveConsumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS));
        renderGlowFaces(poseStack, emissiveConsumer, blockEntity, time, red, green, blue, packedOverlay);

        poseStack.popPose();
    }

    private void renderGlowFaces(PoseStack poseStack, VertexConsumer consumer, MachineFrameworkBlockEntity blockEntity,
                                 float time, int red, int green, int blue, int packedOverlay) {
        for (RelativeSide side : RelativeSide.values()) {
            String sideName = side.name().toLowerCase();

            TextureAtlasSprite baseSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/machine_framework_" + sideName + "_glow"));
            renderRelativeFace(poseStack, consumer, baseSprite, side, 0.001f, red, green, blue, 255, packedOverlay);

            IoModuleItem.FaceMode faceMode = blockEntity.getEffectiveFaceMode(side);
            if (faceMode != IoModuleItem.FaceMode.DISABLED) {
                TextureAtlasSprite ioSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/machine_framework_io_overlay"));

                int colorInt = faceMode.getDynamicColor((long) time);
                int ioR = (colorInt >> 16) & 0xFF;
                int ioG = (colorInt >> 8) & 0xFF;
                int ioB = colorInt & 0xFF;

                renderRelativeFace(poseStack, consumer, ioSprite, side, 0.002f, ioR, ioG, ioB, 255, packedOverlay);
            }
        }
    }

    private void renderRelativeFace(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                    RelativeSide side, float offset, int red, int green, int blue, int alpha, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        float min = 0.0f;
        float max = 1.0f;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        switch (side) {
            case FRONT -> {
                float z = min - offset;
                vertex(consumer, matrix, max, max, z, red, green, blue, alpha, u0, v0, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, -1.0f);
                vertex(consumer, matrix, min, max, z, red, green, blue, alpha, u1, v0, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, -1.0f);
                vertex(consumer, matrix, min, min, z, red, green, blue, alpha, u1, v1, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, -1.0f);
                vertex(consumer, matrix, max, min, z, red, green, blue, alpha, u0, v1, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, -1.0f);
            }
            case BACK -> {
                float z = max + offset;
                vertex(consumer, matrix, min, max, z, red, green, blue, alpha, u0, v0, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, 1.0f);
                vertex(consumer, matrix, max, max, z, red, green, blue, alpha, u1, v0, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, 1.0f);
                vertex(consumer, matrix, max, min, z, red, green, blue, alpha, u1, v1, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, 1.0f);
                vertex(consumer, matrix, min, min, z, red, green, blue, alpha, u0, v1, packedOverlay, FULL_BRIGHT, 0.0f, 0.0f, 1.0f);
            }
            case LEFT -> {
                float x = max + offset;
                vertex(consumer, matrix, x, max, max, red, green, blue, alpha, u1, v0, packedOverlay, FULL_BRIGHT, 1.0f, 0.0f, 0.0f);
                vertex(consumer, matrix, x, max, min, red, green, blue, alpha, u0, v0, packedOverlay, FULL_BRIGHT, 1.0f, 0.0f, 0.0f);
                vertex(consumer, matrix, x, min, min, red, green, blue, alpha, u0, v1, packedOverlay, FULL_BRIGHT, 1.0f, 0.0f, 0.0f);
                vertex(consumer, matrix, x, min, max, red, green, blue, alpha, u1, v1, packedOverlay, FULL_BRIGHT, 1.0f, 0.0f, 0.0f);
            }
            case RIGHT -> {
                float x = min - offset;
                vertex(consumer, matrix, x, max, min, red, green, blue, alpha, u1, v0, packedOverlay, FULL_BRIGHT, -1.0f, 0.0f, 0.0f);
                vertex(consumer, matrix, x, max, max, red, green, blue, alpha, u0, v0, packedOverlay, FULL_BRIGHT, -1.0f, 0.0f, 0.0f);
                vertex(consumer, matrix, x, min, max, red, green, blue, alpha, u0, v1, packedOverlay, FULL_BRIGHT, -1.0f, 0.0f, 0.0f);
                vertex(consumer, matrix, x, min, min, red, green, blue, alpha, u1, v1, packedOverlay, FULL_BRIGHT, -1.0f, 0.0f, 0.0f);
            }
            case UP -> {
                float y = max + offset;
                vertex(consumer, matrix, min, y, min, red, green, blue, alpha, u0, v0, packedOverlay, FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
                vertex(consumer, matrix, max, y, min, red, green, blue, alpha, u1, v0, packedOverlay, FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
                vertex(consumer, matrix, max, y, max, red, green, blue, alpha, u1, v1, packedOverlay, FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
                vertex(consumer, matrix, min, y, max, red, green, blue, alpha, u0, v1, packedOverlay, FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
            }
            case DOWN -> {
                float y = min - offset;
                vertex(consumer, matrix, min, y, max, red, green, blue, alpha, u0, v0, packedOverlay, FULL_BRIGHT, 0.0f, -1.0f, 0.0f);
                vertex(consumer, matrix, max, y, max, red, green, blue, alpha, u1, v0, packedOverlay, FULL_BRIGHT, 0.0f, -1.0f, 0.0f);
                vertex(consumer, matrix, max, y, min, red, green, blue, alpha, u1, v1, packedOverlay, FULL_BRIGHT, 0.0f, -1.0f, 0.0f);
                vertex(consumer, matrix, min, y, min, red, green, blue, alpha, u0, v1, packedOverlay, FULL_BRIGHT, 0.0f, -1.0f, 0.0f);
            }
        }
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                        int red, int green, int blue, int alpha, float u, float v,
                        int overlay, int light, float normalX, float normalY, float normalZ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }
}