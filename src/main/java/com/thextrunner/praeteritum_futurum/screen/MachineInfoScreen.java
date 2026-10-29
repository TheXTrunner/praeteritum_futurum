package com.thextrunner.praeteritum_futurum.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import com.thextrunner.praeteritum_futurum.network.ServerboundOpenModuleConfigPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundSetModuleConfigItemPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleIoFacePacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleModuleDirectionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MachineInfoScreen extends AbstractContainerScreen<MachineInfoMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    private Direction getIoClickedDirection(int slotX, int slotY, double mx, double my) {
        if (isInside(mx, my, slotX, slotY)) return Direction.NORTH;
        if (isInside(mx, my, slotX, slotY - 18)) return Direction.UP;
        if (isInside(mx, my, slotX, slotY + 18)) return Direction.DOWN;
        if (isInside(mx, my, slotX - 18, slotY)) return Direction.WEST;
        if (isInside(mx, my, slotX + 18, slotY)) return Direction.EAST;
        if (isInside(mx, my, slotX + 36, slotY)) return Direction.SOUTH;
        return null;
    }

    private int configuringSlotIndex = -1;

    public MachineInfoScreen(MachineInfoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    private boolean isSlotInActiveOverlay(int slotIndex) {
        if (configuringSlotIndex < 0 || configuringSlotIndex >= 27) return false;
        if (slotIndex == configuringSlotIndex) return true;

        int row = configuringSlotIndex / 9;
        int col = configuringSlotIndex % 9;

        if (row > 0 && slotIndex == configuringSlotIndex - 9) return true;
        if (row < 2 && slotIndex == configuringSlotIndex + 9) return true;
        if (col > 0 && slotIndex == configuringSlotIndex - 1) return true;
        return col < 8 && slotIndex == configuringSlotIndex + 1;
    }

    @Override
    protected void renderSlot(@NotNull GuiGraphics graphics, @NotNull Slot slot) {
        if (isSlotInActiveOverlay(slot.index)) {
            return;
        }

        super.renderSlot(graphics, slot);

        if (slot.index < 27) {
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
                com.thextrunner.praeteritum_futurum.client.ModuleScreenRenderer.renderModuleOverlay(graphics, slot, moduleStack, blockEntity, this.font);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (configuringSlotIndex >= 0 && configuringSlotIndex < 27) {
            Slot slot = this.menu.slots.get(configuringSlotIndex);
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();

            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
                int renderX = slot.x + this.leftPos;
                int renderY = slot.y + this.topPos;

                if (moduleStack.is(ModItems.IO_MODULE.get())) {
                    renderIoOverlay(guiGraphics, renderX, renderY, moduleStack);
                } else if (moduleStack.getItem() instanceof ProcessingModuleItem procModule) {
                    renderProcessingOverlay(guiGraphics, renderX, renderY, moduleStack, procModule, blockEntity);
                } else {
                    configuringSlotIndex = -1;
                }
            }
        }
    }

    private void renderProcessingOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack moduleStack, ProcessingModuleItem procModule, MachineFrameworkBlockEntity blockEntity) {
        Map<Direction, ProcessingModuleItem.SlotMode> modes = ProcessingModuleItem.getSlotModes(moduleStack, procModule.getModuleConfigKey());

        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        // Render 4 directional cross buttons perfectly centered on adjacent grid coordinates
        drawProcessingButton(graphics, centerX, slotY - (offset - 8), Direction.UP, modes.get(Direction.UP));
        drawProcessingButton(graphics, slotX - (offset - 8), centerY, Direction.WEST, modes.get(Direction.WEST));
        drawProcessingButton(graphics, slotX + 16 + (offset - 8), centerY, Direction.EAST, modes.get(Direction.EAST));
        drawProcessingButton(graphics, centerX, slotY + 16 + (offset - 8), Direction.DOWN, modes.get(Direction.DOWN));

        // Render center config slot if the module uses one
        if (procModule.hasConfigSlot()) {
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF888888);
            assert blockEntity.getLevel() != null;
            ItemStack targetItem = procModule.getConfigSlotItem(moduleStack, blockEntity.getLevel().registryAccess());
            if (!targetItem.isEmpty()) {
                graphics.renderItem(targetItem, slotX, slotY);
                graphics.renderItemDecorations(this.font, targetItem, slotX, slotY);
            }
        }

        // Close button (cleanly positioned in the top-left diagonal)
        graphics.fill(slotX - 18, slotY - 18, slotX - 2, slotY - 2, 0xFF8B0000);
        graphics.drawCenteredString(this.font, "X", slotX - 10, slotY - 14, 0xFFFFFF);
    }

    private Direction getProcessingClickedDirection(int slotX, int slotY, double mx, double my) {
        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        if (isInsideButton(mx, my, centerX, slotY - (offset - 8))) return Direction.UP;
        if (isInsideButton(mx, my, slotX - (offset - 8), centerY)) return Direction.WEST;
        if (isInsideButton(mx, my, slotX + 16 + (offset - 8), centerY)) return Direction.EAST;
        if (isInsideButton(mx, my, centerX, slotY + 16 + (offset - 8))) return Direction.DOWN;
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configuringSlotIndex >= 0 && configuringSlotIndex < 27) {
            Slot slot = this.menu.slots.get(configuringSlotIndex);
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();

            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
                int slotX = slot.x + this.leftPos;
                int slotY = slot.y + this.topPos;

                // IO Module 6-face layout click handling
                if (moduleStack.is(ModItems.IO_MODULE.get())) {
                    Direction clickedDir = getIoClickedDirection(slotX, slotY, mouseX, mouseY);
                    if (clickedDir != null) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.playNotifySound(
                                    SoundEvents.UI_BUTTON_CLICK.value(),
                                    net.minecraft.sounds.SoundSource.PLAYERS,
                                    0.5F,
                                    1.0F
                            );
                        }
                        Map<Direction, IoModuleItem.FaceMode> modes = IoModuleItem.getFaceModes(moduleStack);
                        IoModuleItem.FaceMode nextMode = modes.get(clickedDir).next();
                        IoModuleItem.setFaceMode(moduleStack, clickedDir, nextMode);

                        PacketDistributor.sendToServer(new ServerboundToggleIoFacePacket(slot.index, clickedDir, blockEntity.getBlockPos()));
                        return true;
                    }

                    // Close Overlay Button
                    if (mouseX >= slotX - 36 && mouseX <= slotX - 20 && mouseY >= slotY && mouseY <= slotY + 16) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.playNotifySound(
                                    SoundEvents.UI_BUTTON_CLICK.value(),
                                    net.minecraft.sounds.SoundSource.PLAYERS,
                                    0.5F,
                                    1.0F
                            );
                        }
                        configuringSlotIndex = -1;
                        return true;
                    }
                }

                // Processing Module click handling
                if (moduleStack.getItem() instanceof ProcessingModuleItem procModule) {
                    // 1. Center config ghost slot click
                    if (procModule.hasConfigSlot() && mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                        ItemStack carried = this.menu.getCarried();
                        ItemStack ghostCopy = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);

                        assert blockEntity.getLevel() != null;
                        procModule.setConfigSlotItem(moduleStack, ghostCopy, blockEntity.getLevel().registryAccess());
                        PacketDistributor.sendToServer(new ServerboundSetModuleConfigItemPacket(slot.index, ghostCopy, blockEntity.getBlockPos()));
                        return true;
                    }

                    // 2. Directional Cross Buttons
                    Direction clickedDir = getProcessingClickedDirection(slotX, slotY, mouseX, mouseY);
                    if (clickedDir != null) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.playNotifySound(
                                    SoundEvents.UI_BUTTON_CLICK.value(),
                                    net.minecraft.sounds.SoundSource.PLAYERS,
                                    0.5F,
                                    1.0F
                            );
                        }
                        ProcessingModuleItem.SlotMode nextMode = procModule.getNextMode(moduleStack, clickedDir);
                        ProcessingModuleItem.setSlotMode(moduleStack, clickedDir, nextMode, procModule.getModuleConfigKey());

                        PacketDistributor.sendToServer(new ServerboundToggleModuleDirectionPacket(slot.index, clickedDir, blockEntity.getBlockPos()));
                        return true;
                    }

                    // 3. Close Overlay Button
                    if (mouseX >= slotX - 18 && mouseX <= slotX - 2 && mouseY >= slotY - 18 && mouseY <= slotY - 2) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.playNotifySound(
                                    SoundEvents.UI_BUTTON_CLICK.value(),
                                    net.minecraft.sounds.SoundSource.PLAYERS,
                                    0.5F,
                                    1.0F
                            );
                        }
                        configuringSlotIndex = -1;
                        return true;
                    }
                }
            }
        }

        // Fall through so player inventory slots can be clicked and dragged normally
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawProcessingButton(GuiGraphics graphics, int x, int y, Direction dir, ProcessingModuleItem.SlotMode mode) {
        char modeChar = switch (mode) {
            case INPUT -> 'I';
            case FUEL -> 'F';
            case OUTPUT -> 'O';
            case DISABLED -> 'X';
        };
        int color = switch (mode) {
            case INPUT -> 0xFF3366CC;
            case FUEL -> 0xFFCC9933;
            case OUTPUT -> 0xFFCC3333;
            case DISABLED -> 0xFF555555;
        };

        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        graphics.fill(x - 7, y - 7, x + 7, y + 7, color);
        graphics.drawCenteredString(this.font, dir.getName().toUpperCase().charAt(0) + ":" + modeChar, x, y - 4, 0xFFFFFF);
    }

    private void renderIoOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack moduleStack) {
        Map<Direction, IoModuleItem.FaceMode> modes = IoModuleItem.getFaceModes(moduleStack);

        drawConfigButton(graphics, slotX, slotY, "N", modes.get(Direction.NORTH));
        drawConfigButton(graphics, slotX, slotY - 18, "U", modes.get(Direction.UP));
        drawConfigButton(graphics, slotX, slotY + 18, "D", modes.get(Direction.DOWN));
        drawConfigButton(graphics, slotX - 18, slotY, "W", modes.get(Direction.WEST));
        drawConfigButton(graphics, slotX + 18, slotY, "E", modes.get(Direction.EAST));
        drawConfigButton(graphics, slotX + 36, slotY, "S", modes.get(Direction.SOUTH));

        graphics.fill(slotX - 36, slotY, slotX - 20, slotY + 16, 0xFF8B0000);
        graphics.drawCenteredString(this.font, "X", slotX - 28, slotY + 4, 0xFFFFFF);
    }

    private void drawConfigButton(GuiGraphics graphics, int x, int y, String label, IoModuleItem.FaceMode mode) {
        int color = switch (mode) {
            case INPUT -> 0xFF3366CC;    // Blue
            case OUTPUT -> 0xFFCC3333;   // Red
            case OPEN -> 0xFF33CC33;     // Green
            case null, default -> 0xFF555555; // Gray (DISABLED or null)
        };

        graphics.fill(x, y, x + 16, y + 16, 0xFF888888);
        graphics.fill(x + 1, y + 1, x + 15, y + 15, color);

        char modeChar = switch (mode) {
            case INPUT -> 'I';
            case OUTPUT -> 'O';
            case OPEN -> 'B'; // 'B' for Both / Bidirectional
            case null, default -> 'X';
        };

        graphics.drawCenteredString(this.font, label + ":" + modeChar, x + 8, y + 4, 0xFFFFFF);
    }

    private boolean isInsideButton(double mx, double my, int x, int y) {
        return mx >= x - 8 && mx < x + 8 && my >= y - 8 && my < y + 8;
    }

    private boolean isInside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, @NotNull ClickType type) {
        if (type == ClickType.QUICK_CRAFT) {
            assert slot != null;
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }

        // Allow player inventory interactions while the module config overlay is open
        if (configuringSlotIndex >= 0 && slot != null && slot.index >= 27) {
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }

        if (slot != null && slot.index < 27) {
            MachineFrameworkBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);

                if (moduleStack.is(ModItems.IO_MODULE.get()) || moduleStack.getItem() instanceof ProcessingModuleItem) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.playNotifySound(
                                SoundEvents.UI_BUTTON_CLICK.value(),
                                net.minecraft.sounds.SoundSource.PLAYERS,
                                0.5F,
                                1.0F
                        );
                    }
                    configuringSlotIndex = slot.index;
                    return;
                }

                if (moduleStack.getItem() instanceof com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.playNotifySound(
                                SoundEvents.UI_BUTTON_CLICK.value(),
                                net.minecraft.sounds.SoundSource.PLAYERS,
                                0.5F,
                                1.0F
                        );
                    }
                    PacketDistributor.sendToServer(new ServerboundOpenModuleConfigPacket(slot.index, blockEntity.getBlockPos()));
                    return;
                }
            }
        }
        configuringSlotIndex = -1;

        if (slot != null) {
            super.slotClicked(slot, slotId, mouseButton, type);
        }
    }
}