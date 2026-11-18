package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.CyclingMode;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.inventory.SingleSlotItemHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Jukebox Module — playlist manager that plays music discs from connected chests.
 * <p>
 * The module's inventory slot holds a single music disc. When a disc is present,
 * the machine plays it with sound and the colorful song title subtitle, just like
 * a vanilla jukebox. The player can manually place/remove discs via the config overlay
 * center slot.
 * <p>
 * Configuration: one direction button points to THE IO module. Clicking that button
 * cycles the mode: DISABLED → QUEUE → SHUFFLE. Mode change resets the playlist position.
 * <p>
 * Playlist building: scans all OPEN faces of the configured IO module in order
 * (UP → FRONT → BACK → LEFT → RIGHT → DOWN), then slot index (0 → 1 → 2...).
 * Creates one long playlist that loops when finished.
 * <p>
 * Disc lifecycle: extract disc from chest → hold in module slot → play sound + subtitle
 * → return to original slot → load next disc. If the original slot is occupied,
 * try any slot in that inventory, then any other inventory with OPEN face.
 * If all inventories are full, stay idle with the disc in the module slot.
 * <p>
 * QUEUE mode: plays discs in strict playlist order, looping when finished.
 * SHUFFLE mode: plays discs in random order, no repeats until all are played, then reshuffles.
 * DISABLED: no auto-load; the module only plays discs manually placed in its slot.
 * <p>
 * The module acts as an innate filter: only music discs can be inserted.
 * Music disc detection uses the {@code c:music_discs} Common Tags tag
 * and the {@link net.minecraft.world.item.JukeboxPlayable} data component fallback.
 * <p>
 * Channel usage:
 * - Channel 0: Play progress (ticks remaining, 0 when not playing)
 * - Channel 1: Total duration of current disc (in ticks, 0 when not playing)
 * - Channel 2: Current playlist position index
 * - Channel 3: Playlist size (total positions)
 * - Channel 4: Stalled flag (1 = disc in slot but can't return to chest; 0 = normal)
 */
public class JukeboxModuleItem extends ProcessingModuleItem {

    private static final String NBT_KEY = "JukeboxConfig";
    private static final String LOG_PREFIX = "[JUKEBOX]";

    /**
     * Face priority order for playlist building.
     * UP first (vanilla-like), then horizontal sides, DOWN last.
     */
    private static final RelativeSide[] FACE_ORDER = {
            RelativeSide.UP,
            RelativeSide.FRONT, RelativeSide.BACK, RelativeSide.LEFT, RelativeSide.RIGHT,
            RelativeSide.DOWN
    };

    /**
     * Playback mode, selected via the configured direction button.
     */
    public enum JukeboxMode implements CyclingMode<JukeboxMode> {
        DISABLED("D", 0xFF555555),
        QUEUE("Q", 0xFF3366CC),
        SHUFFLE("S", 0xFF33CC66);

        private final String label;
        private final int color;

        JukeboxMode(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }

        @Override public JukeboxMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override public JukeboxMode previous() {
            return values()[(ordinal() - 1 + values().length) % values().length];
        }

        public static JukeboxMode fromId(int id) {
            if (id < 0 || id >= values().length) return DISABLED;
            return values()[id];
        }
    }

    /* ==================================================================== */
    /* Constructor & base overrides                                          */
    /* ==================================================================== */

    public JukeboxModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public List<BaseModuleItem.AdjacentModuleType> getAllowedAdjacentModuleTypes() {
        return List.of(BaseModuleItem.AdjacentModuleType.IO);
    }

    @Override
    public int getInventorySlotLimit(MachineFrameworkBlockEntity be, int moduleSlot, int invSlot) {
        return (moduleSlot == invSlot) ? 1 : 0;
    }

    /**
     * Allows the player to interact with the disc slot in the machine UI.
     */
    @Override
    public boolean hasInteractableInventorySlot() {
        return true;
    }

    /**
     * Returns true when the player is holding a music disc, so the UI can place
     * it directly instead of opening the config overlay.
     */
    @Override
    public boolean isItemValidForSlot(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        return isMusicDisc(stack);
    }

    @Override
    public boolean usesCenterSlot() {
        return true; // real disc slot
    }

    /**
     * Exposes the module slot with an innate filter — only music discs accepted.
     */
    @Override
    public @Nullable IItemHandlerModifiable getItemHandlerForSide(
            Level level, BlockPos pos, MachineFrameworkBlockEntity be,
            int slotIndex, ItemStack stack, Direction side) {

        IItemHandlerModifiable delegate = new SingleSlotItemHandler(be.getInventoryHandler(), slotIndex);
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack s) {
                if (!s.isEmpty() && !isMusicDisc(s)) return;
                delegate.setStackInSlot(slot, s);
            }
            @Override public int getSlots() { return delegate.getSlots(); }
            @Override public @NotNull ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(slot); }
            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack s, boolean sim) {
                if (!isMusicDisc(s)) return s;
                return delegate.insertItem(slot, s, sim);
            }
            @Override public @NotNull ItemStack extractItem(int slot, int amt, boolean sim) { return delegate.extractItem(slot, amt, sim); }
            @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, @NotNull ItemStack s) { return isMusicDisc(s); }
        };
    }

    /**
     * Small overlay drawn on the module slot (progress bar).
     */

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    /* ==================================================================== */
    /* NBT configuration helpers — single direction + mode                   */
    /* ==================================================================== */

    @Nullable
    private GridDirection getConfigDirection(ItemStack stack) {
        CompoundTag cfg = getConfigTag(stack);
        if (cfg == null || !cfg.contains("Dir")) return null;
        int idx = cfg.getInt("Dir");
        if (idx < 0 || idx >= GridDirection.VALUES.length) return null;
        return GridDirection.VALUES[idx];
    }

    private void saveConfigDirection(ItemStack stack, @Nullable GridDirection dir) {
        CompoundTag cfg = getConfigTagOrCreate(stack);
        if (dir == null) {
            cfg.remove("Dir");
        } else {
            cfg.putInt("Dir", Arrays.asList(GridDirection.VALUES).indexOf(dir));
        }
        putConfigTag(stack, cfg);
    }

    private JukeboxMode getMode(ItemStack stack) {
        CompoundTag cfg = getConfigTag(stack);
        if (cfg == null || !cfg.contains("Mode")) return JukeboxMode.DISABLED;
        return JukeboxMode.fromId(cfg.getInt("Mode"));
    }

    private void saveMode(ItemStack stack, JukeboxMode mode) {
        CompoundTag cfg = getConfigTagOrCreate(stack);
        cfg.putInt("Mode", mode.ordinal());
        putConfigTag(stack, cfg);
    }

    /** Resets both direction and mode (called when playlist should restart). */
    private void resetPlaylist(ItemStack stack) {
        // Mode change resets playlist position — handled in processModule via channel 2
    }

    @Nullable
    private CompoundTag getConfigTag(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag tag = cd.copyTag();
        if (!tag.contains(NBT_KEY)) return null;
        return tag.getCompound(NBT_KEY);
    }

    private CompoundTag getConfigTagOrCreate(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = (cd != null) ? cd.copyTag() : new CompoundTag();
        if (!tag.contains(NBT_KEY)) {
            tag.put(NBT_KEY, new CompoundTag());
        }
        return tag.getCompound(NBT_KEY);
    }

    private void putConfigTag(ItemStack stack, CompoundTag cfg) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = (cd != null) ? cd.copyTag() : new CompoundTag();
        tag.put(NBT_KEY, cfg);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private int getConfigIoSlotIndex(ItemStack stack, int slotIndex) {
        GridDirection dir = getConfigDirection(stack);
        return (dir != null) ? dir.getAdjacentSlotIndex(slotIndex) : -1;
    }

    /* ==================================================================== */
    /* Inline overlay system                                                 */
    /* ==================================================================== */

    @Override
    public boolean opensInlineOverlay() {
        return true;
    }

    @Override
    public boolean shouldHideSlot(int configuringSlotIndex, int targetSlotIndex) {
        // Don't hide the center slot — the disc is a real item that should be interactable
        int row = configuringSlotIndex / MachineFrameworkBlockEntity.GRID_WIDTH;
        int col = configuringSlotIndex % MachineFrameworkBlockEntity.GRID_WIDTH;

        // Hide adjacent slots (direction buttons)
        if (row > 0 && targetSlotIndex == configuringSlotIndex - MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (row < MachineFrameworkBlockEntity.GRID_ROWS - 1 && targetSlotIndex == configuringSlotIndex + MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (col > 0 && targetSlotIndex == configuringSlotIndex - 1) return true;
        if (col < MachineFrameworkBlockEntity.GRID_WIDTH - 1 && targetSlotIndex == configuringSlotIndex + 1) return true;
        return row > 0 && col > 0 && targetSlotIndex == configuringSlotIndex - MachineFrameworkBlockEntity.GRID_WIDTH - 1;
    }

    @Override
    public boolean allowsCenterSlotInteraction() {
        return true; // player can place/remove discs while config overlay is open
    }

    @Override
    public boolean isInsideOverlay(double mx, double my, int slotX, int slotY) {
        // Direction buttons + close button — NOT the center slot (disc is a real item)
        boolean up    = mx >= slotX     && mx <= slotX + 16 && my >= slotY - 18 && my <= slotY - 2;
        boolean down  = mx >= slotX     && mx <= slotX + 16 && my >= slotY + 18 && my <= slotY + 34;
        boolean left  = mx >= slotX - 18 && mx <= slotX - 2 && my >= slotY     && my <= slotY + 16;
        boolean right = mx >= slotX + 18 && mx <= slotX + 34 && my >= slotY     && my <= slotY + 16;
        boolean close = mx >= slotX - 18 && mx <= slotX - 2 && my >= slotY - 18 && my <= slotY - 2;
        return up || down || left || right || close;
    }


    /* ==================================================================== */
    /* Direction config validation hooks                                     */
    /* ==================================================================== */

    @Override
    public boolean isDirectionConfigured(ItemStack stack, GridDirection dir) {
        GridDirection cfgDir = getConfigDirection(stack);
        return cfgDir == dir && getMode(stack) != JukeboxMode.DISABLED;
    }

    @Override
    public void resetDirection(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity be, int slotIndex) {
        saveConfigDirection(stack, null);
        saveMode(stack, JukeboxMode.DISABLED);
        // Clear playlist state
        be.setModuleData(slotIndex, 2, 0);
        be.setModuleData(slotIndex, 3, 0);
    }

    @Override
    public void resetDirectionMode(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity be, int slotIndex) {
        saveConfigDirection(stack, null);
        saveMode(stack, JukeboxMode.DISABLED);
        be.setModuleData(slotIndex, 2, 0);
        be.setModuleData(slotIndex, 3, 0);
    }

    /* ==================================================================== */
    /* Module removal guard                                                  */
    /* ==================================================================== */

    @Override
    public boolean mayPickupModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be, int slotIndex, ItemStack stack) {
        // Lock module while a song is actively playing
        return !(be.getModuleData(slotIndex, 0) > 0 && be.getModuleData(slotIndex, 1) > 0);
    }

    /* ==================================================================== */
    /* Click handling (client) & server sync                                 */
    /* ==================================================================== */

    @Override
    public void toggleDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity be, int slotIndex, boolean shiftHeld) {
        // Server-side: mirror the client's click
        if (!isAdjacentModuleAllowed(getAdjacentModuleType(be, slotIndex, direction))) return;

        GridDirection cfgDir = getConfigDirection(stack);
        if (cfgDir == direction) {
            // Cycle mode (shift-click cycles in reverse)
            JukeboxMode next = shiftHeld ? getMode(stack).previous() : getMode(stack).next();
            saveMode(stack, next);
            // Reset playlist position and clear all shuffle state on mode change
            be.setModuleData(slotIndex, 2, 0);
            be.setModuleData(slotIndex, 5, 0); // clear shuffle cycle flag
            be.setModuleObject(slotIndex + 100, null); // clear shuffle order list
        } else {
            // Set as configured direction
            saveConfigDirection(stack, direction);
        }
    }

    @Override
    public void handleCustomClick(double mx, double my, int slotX, int slotY, ItemStack stack,
                                  MachineFrameworkBlockEntity be, int slotIndex,
                                  net.minecraft.client.Minecraft mc) {
        if (mc.player == null) return;

        // Auto-validate stale direction config
        validateAndSyncDirectionConfig(stack, be, slotIndex);

        GridDirection clicked = getClickedDirection(slotX, slotY, mx, my, slotIndex);
        if (clicked != null && isAdjacentModuleAllowed(getAdjacentModuleType(be, slotIndex, clicked))) {
            mc.player.playNotifySound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);

            GridDirection cfgDir = getConfigDirection(stack);
            if (cfgDir == clicked) {
                // Client-side preview — server will cycle the actual mode
                saveMode(stack, isShiftHeld() ? getMode(stack).previous() : getMode(stack).next());
            } else {
                saveConfigDirection(stack, clicked);
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.thextrunner.praeteritum_futurum.network.ServerboundToggleModuleDirectionPacket(
                            slotIndex, clicked, be.getBlockPos(), isShiftHeld()));
            return;
        }
    }

    /* ==================================================================== */
    /* Overlay rendering                                                     */
    /* ==================================================================== */

    @Override
    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack,
                                    MachineFrameworkBlockEntity be, net.minecraft.client.gui.Font font) {
        JukeboxMode mode = getMode(stack);
        GridDirection cfgDir = getConfigDirection(stack);
        int slotIndex = getOverlaySlotIndex();
        int cx = slotX + 8, cy = slotY + 8;
        int off = 18;

        // Direction buttons
        if (slotIndex >= 0) {
            if (hasAdjacentSlot(slotIndex, GridDirection.UP))
                drawButton(graphics, font, slotIndex, be, cx, slotY - (off - 8), GridDirection.UP, mode, cfgDir);
            if (hasAdjacentSlot(slotIndex, GridDirection.LEFT))
                drawButton(graphics, font, slotIndex, be, slotX - (off - 8), cy, GridDirection.LEFT, mode, cfgDir);
            if (hasAdjacentSlot(slotIndex, GridDirection.RIGHT))
                drawButton(graphics, font, slotIndex, be, slotX + 16 + (off - 8), cy, GridDirection.RIGHT, mode, cfgDir);
            if (hasAdjacentSlot(slotIndex, GridDirection.DOWN))
                drawButton(graphics, font, slotIndex, be, cx, slotY + 16 + (off - 8), GridDirection.DOWN, mode, cfgDir);
        }

        // Center slot — render the disc item and progress bar
        if (slotIndex >= 0) {
            ItemStack disc = be.getInventoryHandler().getStackInSlot(slotIndex);
            if (!disc.isEmpty()) {
                // Draw a gray background behind the disc
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF888888);
                graphics.renderItem(disc, slotX, slotY);
                graphics.renderItemDecorations(font, disc, slotX, slotY);
                // Draw horizontal progress bar at the bottom if a song is playing
                float progress = getProgressPercent(stack, be, slotIndex);
                if (progress > 0.0F) {
                    int barWidth = (int) (progress * 14.0F);
                    graphics.fill(slotX + 1, slotY + 13, slotX + 15, slotY + 15, 0xFF222222);
                    graphics.fill(slotX + 1, slotY + 13, slotX + 1 + barWidth, slotY + 15, 0xFF33CC33);
                }
            }
        }

        drawCloseButton(graphics, font, slotX, slotY);
    }

    private void drawButton(GuiGraphics g, net.minecraft.client.gui.Font font, int slotIndex,
                            MachineFrameworkBlockEntity be, int x, int y,
                            GridDirection dir, JukeboxMode mode, GridDirection cfgDir) {
        g.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888); // border

        boolean hasIo = isAdjacentModuleAllowed(getAdjacentModuleType(be, slotIndex, dir));
        boolean selected = cfgDir == dir;

        if (hasIo) {
            if (selected) {
                g.fill(x - 7, y - 7, x + 7, y + 7, mode.getColor());
                g.drawCenteredString(font, mode.getLabel(), x, y - 4, 0xFFFFFF);
            } else {
                g.fill(x - 7, y - 7, x + 7, y + 7, JukeboxMode.DISABLED.getColor());
                g.drawCenteredString(font, JukeboxMode.DISABLED.getLabel(), x, y - 4, 0xFFAAAAAA);
            }
        } else {
            g.fill(x - 7, y - 7, x + 7, y + 7, 0xFF333333);
            g.drawCenteredString(font, JukeboxMode.DISABLED.getLabel(), x, y - 4, 0xFF555555);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext ctx,
                                @NotNull List<Component> tips, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tips, flag);
        JukeboxMode mode = getMode(stack);
        GridDirection cfgDir = getConfigDirection(stack);
        if (mode != JukeboxMode.DISABLED && cfgDir != null) {
            tips.add(Component.translatable("tooltip.praeteritum_futurum.configured")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /* ==================================================================== */
    /* Music disc detection & playback                                       */
    /* ==================================================================== */

    private static boolean isMusicDisc(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ModTags.Items.MUSIC_DISCS)) return true;
        return stack.get(DataComponents.JUKEBOX_PLAYABLE) != null;
    }

    private static int getDiscDurationTicks(ItemStack stack, Level level) {
        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), stack);
        if (song.isPresent()) {
            return (int) (song.get().value().lengthInSeconds() * 20);
        }
        return 180 * 20; // fallback: 3 minutes
    }

    /**
     * Plays the disc sound and shows the colorful song title subtitle.
     * The JukeboxSongPlayer is stored in the block entity as a generic module-owned object
     * so it persists across ticks and can be stopped when the disc is removed.
     * Registers a cleanup action so the song stops when the machine is destroyed.
     */
    private static void playDiscSong(Level level, BlockPos pos, ItemStack stack,
                                      MachineFrameworkBlockEntity be, int slotIndex) {
        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), stack);
        song.ifPresent(holder -> {
            JukeboxSongPlayer player = be.getModuleObject(slotIndex);
            if (player == null) {
                player = new JukeboxSongPlayer(() -> {}, pos);
                be.setModuleObject(slotIndex, player);
            }
            // Register cleanup so the song stops when the machine is destroyed
            // Use a final reference so the lambda can capture it
            final JukeboxSongPlayer finalPlayer = player;
            final BlockPos finalPos = pos;
            final Level finalLevel = level;
            be.registerModuleCleanup(slotIndex, () -> {
                finalPlayer.stop(finalLevel, finalLevel.getBlockState(finalPos));
            });
            // Use play() to both play the sound AND show the colorful subtitle
            player.play(level, holder);
        });
    }

    /**
     * Stops the currently playing song for a module slot.
     * Call this when the disc is removed or the song finishes.
     */
    private static void stopDiscSong(Level level, MachineFrameworkBlockEntity be, int slotIndex) {
        JukeboxSongPlayer player = be.getModuleObject(slotIndex);
        if (player != null && level != null) {
            player.stop(level, be.getBlockState());
        }
        be.setModuleObject(slotIndex, (JukeboxSongPlayer) null);
    }

    /* ==================================================================== */
    /* Playlist position                                                     */
    /* ==================================================================== */

    /** A slot in an external inventory accessible via an OPEN IO face. */
    private static class PlaylistPosition {
        final BlockPos externalPos;
        final Direction accessSide;
        final int slotIndex;

        PlaylistPosition(BlockPos pos, Direction side, int slot) {
            this.externalPos = pos;
            this.accessSide = side;
            this.slotIndex = slot;
        }
    }

    /**
     * Builds the full playlist from all OPEN faces of the configured IO module.
     * Order: UP → FRONT → BACK → LEFT → RIGHT → DOWN, then slot 0 → 1 → 2...
     * Includes all slots (even empty ones) so the order is stable.
     */
    private List<PlaylistPosition> buildPlaylist(Level level, BlockPos pos,
            MachineFrameworkBlockEntity be, int slotIndex, ItemStack moduleStack) {

        List<PlaylistPosition> playlist = new ArrayList<>();
        int adjSlot = getConfigIoSlotIndex(moduleStack, slotIndex);
        if (adjSlot < 0) return playlist;

        ItemStack adjModule = be.getModuleHandler().getStackInSlot(adjSlot);
        if (!adjModule.is(ModItems.IO_MODULE.get())) return playlist;

        IoModuleItem ioModule = (IoModuleItem) adjModule.getItem();
        Map<RelativeSide, IoModuleItem.FaceMode> faces = ioModule.getFaceModes(adjModule);

        BlockState state = level.getBlockState(pos);
        Direction facing = state.hasProperty(MachineFrameworkBlock.FACING)
                ? state.getValue(MachineFrameworkBlock.FACING) : Direction.NORTH;

        for (RelativeSide face : FACE_ORDER) {
            if (faces.get(face) != IoModuleItem.FaceMode.OPEN) continue;

            Direction worldDir = face.toWorldDirection(facing);
            BlockPos extPos = pos.relative(worldDir);

            // Chunk load check
            if (level instanceof net.minecraft.server.level.ServerLevel sl
                    && !sl.shouldTickBlocksAt(extPos)) continue;

            IItemHandler inv = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, extPos, worldDir.getOpposite());
            if (inv == null) continue;

            for (int i = 0; i < inv.getSlots(); i++) {
                playlist.add(new PlaylistPosition(extPos, worldDir.getOpposite(), i));
            }
        }

        if (Config.isDebugLoggingEnabled()) {
            PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " BUILD PLAYLIST: {} positions from {} inventories",
                    playlist.size(), countInventories(playlist));
        }
        return playlist;
    }

    /** Counts unique inventories (by block position) in the playlist. */
    private static int countInventories(List<PlaylistPosition> playlist) {
        return (int) playlist.stream().map(p -> p.externalPos).distinct().count();
    }

    /* ==================================================================== */
    /* Disc extract / return                                                 */
    /* ==================================================================== */

    /** Extract one disc from a playlist position into the module's inventory slot. */
    private boolean extractDisc(Level level, MachineFrameworkBlockEntity be,
                                int slotIndex, PlaylistPosition pos) {
        IItemHandler inv = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.externalPos, pos.accessSide);
        if (inv == null) return false;

        ItemStack stack = inv.getStackInSlot(pos.slotIndex);
        if (stack.isEmpty() || !isMusicDisc(stack)) return false;

        ItemStack extracted = inv.extractItem(pos.slotIndex, 1, false);
        if (extracted.isEmpty()) return false;

        // Put disc in the module's inventory slot
        IItemHandlerModifiable handler = be.getInventoryHandler();
        if (!handler.getStackInSlot(slotIndex).isEmpty()) {
            // Slot occupied — put it back
            inv.insertItem(pos.slotIndex, extracted, false);
            return false;
        }
        handler.setStackInSlot(slotIndex, extracted.copyWithCount(1));
        return true;
    }

    /* ==================================================================== */
    /* Core tick logic                                                       */
    /* ==================================================================== */

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be,
                                 int slotIndex, ItemStack moduleStack) {
        if (level.isClientSide()) return;

        JukeboxMode mode = getMode(moduleStack);
        GridDirection cfgDir = getConfigDirection(moduleStack);
        boolean canAutoLoad = mode != JukeboxMode.DISABLED && cfgDir != null;

        IItemHandlerModifiable inv = be.getInventoryHandler();
        ItemStack disc = inv.getStackInSlot(slotIndex);

        int progress = be.getModuleData(slotIndex, 0);
        int total = be.getModuleData(slotIndex, 1);
        boolean isPlaying = progress > 0 && total > 0;
        int stalled = be.getModuleData(slotIndex, 4); // 1 = stalled, can't return disc

        // ---- CASE 1: Disc in slot ----
        if (!disc.isEmpty() && isMusicDisc(disc)) {
            // If stalled, try to un-stall by checking if chests have space
            if (stalled == 1 && canAutoLoad) {
                List<PlaylistPosition> playlist = buildPlaylist(level, pos, be, slotIndex, moduleStack);

                if (Config.isDebugLoggingEnabled()) {
                    PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " UNSTALL CHECK: pos={}, playlistSize={}, disc={}",
                            pos, playlist.size(), disc.getItem());
                }

                // If no inventories are connected yet, keep stalled
                if (playlist.isEmpty()) {
                    if (Config.isDebugLoggingEnabled()) {
                        PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " UNSTALL FAILED: no inventories, keeping stalled");
                    }
                    return;
                }

                // Try to return the disc to any available slot in any connected inventory
                ItemStack remainder = returnDiscToAnyInventory(level, playlist, disc);
                if (remainder.isEmpty()) {
                    // Successfully returned — clear stall and slot.
                    // Do NOT load the next disc here: the disc was just inserted into an arbitrary
                    // slot in the inventory, and calling loadNextDisc on the same tick would
                    // immediately re-extract it (since loadNextDisc scans from the current
                    // playlist position, which may point to the slot we just filled).
                    // Instead, let the next tick's CASE 2 (no disc in slot) auto-load handle
                    // finding the next disc naturally, with a fresh playlist build.
                    inv.setStackInSlot(slotIndex, ItemStack.EMPTY);
                    be.setModuleData(slotIndex, 4, 0); // clear stalled

                    if (Config.isDebugLoggingEnabled()) {
                        PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " UNSTALL SUCCESS: disc returned, waiting for next tick to load");
                    }
                    return;
                }
                // Still stalled — don't do anything this tick
                if (Config.isDebugLoggingEnabled()) {
                    PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " UNSTALL FAILED: inventories full, remainder={}", remainder.getItem());
                }
                return;
            }

            if (!isPlaying) {
                // Start playing
                int duration = getDiscDurationTicks(disc, level);
                be.setModuleData(slotIndex, 0, duration);
                be.setModuleData(slotIndex, 1, duration);
                if (Config.isDebugLoggingEnabled()) {
                    PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " SONG START: disc={}, duration={} ticks", disc.getItem(), duration);
                }
                playDiscSong(level, pos, disc, be, slotIndex);
            } else {
                // Tick down
                progress--;
                be.setModuleData(slotIndex, 0, progress);

                if (progress <= 0) {
                    // Song finished — stop the song player and clear progress
                    be.setModuleData(slotIndex, 0, 0);
                    be.setModuleData(slotIndex, 1, 0);
                    stopDiscSong(level, be, slotIndex);

                    if (Config.isDebugLoggingEnabled()) {
                        PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " SONG FINISH: disc={}, canAutoLoad={}", disc.getItem(), canAutoLoad);
                    }

                    if (!canAutoLoad) return; // leave disc in slot for manual handling

                    // Build playlist to find where this disc came from
                    List<PlaylistPosition> playlist = buildPlaylist(level, pos, be, slotIndex, moduleStack);
                    int playlistPos = be.getModuleData(slotIndex, 2);

                    // Extract the disc from the module slot into a local variable
                    // so the slot is empty before we try to return or load
                    ItemStack finishedDisc = inv.getStackInSlot(slotIndex);
                    inv.setStackInSlot(slotIndex, ItemStack.EMPTY);

                    // If no inventories are connected, hold the disc in the module slot
                    if (!finishedDisc.isEmpty() && playlist.isEmpty()) {
                        inv.setStackInSlot(slotIndex, finishedDisc);
                        be.setModuleData(slotIndex, 4, 1); // set stalled — will retry when inventories appear
                        if (Config.isDebugLoggingEnabled()) {
                            PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " SONG FINISH: no inventories, STALLED disc={}", finishedDisc.getItem());
                        }
                        return;
                    }

                    // Try to return the finished disc to its original position first
                    if (!finishedDisc.isEmpty() && !playlist.isEmpty()) {
                        // Try the original playlist position — if the slot is empty or has the
                        // same disc, return it. If it has a different disc, the chest now holds
                        // new content, so we stall instead of overwriting.
                        ItemStack remainder = returnDiscToOriginalPosition(level, playlist, playlistPos, finishedDisc);
                        if (Config.isDebugLoggingEnabled()) {
                            PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " SONG FINISH: playlistSize={}, playlistPos={}, remainder={}, disc={}",
                                    playlist.size(), playlistPos, remainder.isEmpty() ? "none" : remainder.getItem(), finishedDisc.getItem());
                        }
                        if (!remainder.isEmpty()) {
                            // Original position unavailable — stall with the disc in the slot
                            inv.setStackInSlot(slotIndex, remainder);
                            be.setModuleData(slotIndex, 4, 1); // set stalled
                            if (Config.isDebugLoggingEnabled()) {
                                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " SONG FINISH: original pos unavailable, STALLED");
                            }
                            return;
                        }
                    }

                    // Advance position to the next slot in the playlist
                    if (!playlist.isEmpty()) {
                        // If this is the only disc in any connected inventory, replay it
                        if (playlist.size() == 1) {
                            if (Config.isDebugLoggingEnabled()) {
                                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " SONG FINISH: only disc in inventory, REPLAY");
                            }
                            // Keep playlistPos at 0 — the disc is back in the inventory,
                            // so the next tick's CASE 2 (no disc in slot) will auto-load it again
                            be.setModuleData(slotIndex, 2, 0);
                            be.setModuleData(slotIndex, 3, 1);
                        } else {
                            int nextPos = playlistPos + 1;
                            if (nextPos >= playlist.size()) nextPos = 0; // loop back to start
                            be.setModuleData(slotIndex, 2, nextPos);
                            be.setModuleData(slotIndex, 3, playlist.size());

                            if (Config.isDebugLoggingEnabled()) {
                                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " SONG FINISH: disc returned, nextPos={}/{}", nextPos, playlist.size());
                            }
                            // Load the next disc from the playlist (slot is now empty)
                            loadNextDisc(level, be, slotIndex, playlist, nextPos, mode == JukeboxMode.SHUFFLE);
                        }
                    }
                }
            }
        }
        // ---- CASE 2: No disc in slot ----
        else {
            // Clear playing state if disc was removed
            if (isPlaying) {
                be.setModuleData(slotIndex, 0, 0);
                be.setModuleData(slotIndex, 1, 0);
                // Stop the song when disc is removed
                stopDiscSong(level, be, slotIndex);
                if (Config.isDebugLoggingEnabled()) {
                    PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " DISC REMOVED: song stopped");
                }
            }

            // Clear stalled state when slot is empty
            if (stalled == 1) {
                be.setModuleData(slotIndex, 4, 0);
                if (Config.isDebugLoggingEnabled()) {
                    PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " STALL CLEARED: slot emptied");
                }
            }

            if (!canAutoLoad) return;

            // Auto-load next disc
            int playlistPos = be.getModuleData(slotIndex, 2);
            List<PlaylistPosition> playlist = buildPlaylist(level, pos, be, slotIndex, moduleStack);
            if (Config.isDebugLoggingEnabled()) {
                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " AUTO-LOAD: playlistSize={}, playlistPos={}", playlist.size(), playlistPos);
            }
            if (playlist.isEmpty()) return;

            if (playlistPos >= playlist.size()) {
                playlistPos = 0;
                be.setModuleData(slotIndex, 2, 0);
            }
            be.setModuleData(slotIndex, 3, playlist.size());

            loadNextDisc(level, be, slotIndex, playlist, playlistPos, mode == JukeboxMode.SHUFFLE);
        }
    }

    /**
     * Tries to return a disc to the original playlist position first.
     * If the original slot is empty, returns the disc there.
     * If the original slot holds the same disc (exact match), returns it there.
     * If the original slot holds a different disc, the chest now has new content — stall instead.
     * If the inventory is gone (chest removed), returns the disc as remainder.
     * Returns the remainder (the disc if it couldn't be placed in the original inventory).
     */
    private ItemStack returnDiscToOriginalPosition(Level level, List<PlaylistPosition> playlist, int originalPos, ItemStack disc) {
        if (originalPos < 0 || originalPos >= playlist.size()) {
            if (Config.isDebugLoggingEnabled()) {
                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " RETURN ORIGINAL: pos {} out of bounds [0, {})", originalPos, playlist.size());
            }
            return disc; // invalid position, can't return
        }

        PlaylistPosition orig = playlist.get(originalPos);

        IItemHandler inv = level.getCapability(
                Capabilities.ItemHandler.BLOCK, orig.externalPos, orig.accessSide);
        if (inv == null) {
            if (Config.isDebugLoggingEnabled()) {
                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " RETURN ORIGINAL: inventory null at {}", orig.externalPos);
            }
            return disc; // inventory gone, can't return
        }

        // Check what's in the original slot
        ItemStack slotContent = inv.getStackInSlot(orig.slotIndex);
        if (slotContent.isEmpty()) {
            // Slot is empty — return the disc
            ItemStack remainder = inv.insertItem(orig.slotIndex, disc, false);
            if (Config.isDebugLoggingEnabled()) {
                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " RETURN ORIGINAL: success at empty slot {}", orig.slotIndex);
            }
            return remainder;
        }

        // Slot has something — if it's the same disc type, it's fine (shouldn't happen normally,
        // but handles edge cases like chunk reload). If it's a different disc, the chest now
        // holds new content — we should stall instead of overwriting.
        if (ItemStack.isSameItemSameComponents(slotContent, disc)) {
            // Same disc — this shouldn't normally happen since we extracted it, but handle gracefully
            if (Config.isDebugLoggingEnabled()) {
                PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " RETURN ORIGINAL: slot {} already has same disc", orig.slotIndex);
            }
            return ItemStack.EMPTY; // disc is already there, nothing to do
        }

        // Different disc in the slot — the chest now has new content, stall instead
        if (Config.isDebugLoggingEnabled()) {
            PraeteritumFuturumMod.LOGGER.info(LOG_PREFIX + " RETURN ORIGINAL: slot {} has different disc {}, stalling", orig.slotIndex, slotContent.getItem());
        }
        return disc; // can't return, chest has new content
    }

    /**
     * Tries to return a disc to any available slot in any connected inventory.
     * Returns the remainder (the disc if it couldn't be placed anywhere).
     */
    private ItemStack returnDiscToAnyInventory(Level level, List<PlaylistPosition> playlist, ItemStack disc) {
        ItemStack remainder = disc;
        for (PlaylistPosition p : playlist) {
            IItemHandler inv = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, p.externalPos, p.accessSide);
            if (inv == null) continue;
            for (int i = 0; i < inv.getSlots(); i++) {
                remainder = inv.insertItem(i, remainder, false);
                if (remainder.isEmpty()) return remainder;
            }
        }
        return remainder;
    }

    /**
     * Finds and loads the next disc from the playlist.
     * QUEUE: sequential from startPos. SHUFFLE: random order, no repeats until all played.
     *
     * Shuffle state is stored in moduleData channels:
     * - Channel 2: current playlist position (for QUEUE) or current shuffle index (for SHUFFLE)
     * - Channel 3: playlist size
     * - Channel 5: shuffle cycle flag (1 = in middle of a shuffle cycle; 0 = need to reshuffle)
     */
    private void loadNextDisc(Level level, MachineFrameworkBlockEntity be, int slotIndex,
                              List<PlaylistPosition> playlist, int startPos, boolean shuffle) {
        if (playlist.isEmpty()) return;

        if (shuffle) {
            loadNextDiscShuffle(level, be, slotIndex, playlist);
        } else {
            loadNextDiscQueue(level, be, slotIndex, playlist, startPos);
        }
    }

    /** QUEUE mode: sequential search from startPos, wrapping around. */
    private void loadNextDiscQueue(Level level, MachineFrameworkBlockEntity be, int slotIndex,
                                    List<PlaylistPosition> playlist, int startPos) {
        for (int i = 0; i < playlist.size(); i++) {
            int idx = (startPos + i) % playlist.size();
            if (extractDisc(level, be, slotIndex, playlist.get(idx))) {
                be.setModuleData(slotIndex, 2, idx);
                return;
            }
        }
    }

    /** SHUFFLE mode: random order, no repeats until all positions have been tried. */
    private void loadNextDiscShuffle(Level level, MachineFrameworkBlockEntity be, int slotIndex,
                                      List<PlaylistPosition> playlist) {
        int shuffleCycle = be.getModuleData(slotIndex, 5); // 1 = in cycle, 0 = need reshuffle

        // Get or create the shuffle order list stored in NBT
        List<Integer> shuffleOrder = getShuffleOrder(be, slotIndex);

        // If we need a new shuffle order, generate one
        if (shuffleCycle == 0 || shuffleOrder.isEmpty()) {
            shuffleOrder = new ArrayList<>();
            for (int i = 0; i < playlist.size(); i++) {
                shuffleOrder.add(i);
            }
            Collections.shuffle(shuffleOrder, new Random());
            saveShuffleOrder(be, slotIndex, shuffleOrder);
            be.setModuleData(slotIndex, 5, 1); // mark as in cycle
        }

        // Get the current index within the shuffle order
        int shuffleIdx = be.getModuleData(slotIndex, 2); // reuse channel 2 as shuffle order index

        // Try each position in the shuffle order
        for (int i = shuffleIdx; i < shuffleOrder.size(); i++) {
            int playlistPos = shuffleOrder.get(i);
            if (extractDisc(level, be, slotIndex, playlist.get(playlistPos))) {
                // Save progress within the shuffle order
                be.setModuleData(slotIndex, 2, i + 1); // next time, start from next index
                return;
            }
        }

        // All positions in this shuffle cycle have been tried — reshuffle and start over
        shuffleOrder = new ArrayList<>();
        for (int i = 0; i < playlist.size(); i++) {
            shuffleOrder.add(i);
        }
        Collections.shuffle(shuffleOrder, new Random());
        saveShuffleOrder(be, slotIndex, shuffleOrder);
        be.setModuleData(slotIndex, 2, 0); // reset shuffle order index
        be.setModuleData(slotIndex, 5, 1);

        // Try again with the new shuffle order
        for (int i = 0; i < shuffleOrder.size(); i++) {
            int playlistPos = shuffleOrder.get(i);
            if (extractDisc(level, be, slotIndex, playlist.get(playlistPos))) {
                be.setModuleData(slotIndex, 2, i + 1);
                return;
            }
        }
        // No disc found at all — module stays idle
    }

    /** Get the shuffle order list from module object storage. */
    @SuppressWarnings("unchecked")
    private List<Integer> getShuffleOrder(MachineFrameworkBlockEntity be, int slotIndex) {
        Object obj = be.getModuleObject(slotIndex + 100); // offset to avoid conflict with JukeboxSongPlayer
        if (obj instanceof List) return (List<Integer>) obj;
        return new ArrayList<>();
    }

    /** Save the shuffle order list to module object storage. */
    private void saveShuffleOrder(MachineFrameworkBlockEntity be, int slotIndex, List<Integer> order) {
        be.setModuleObject(slotIndex + 100, order); // offset to avoid conflict with JukeboxSongPlayer
    }

    @Override
    public float getProgressPercent(ItemStack stack, MachineFrameworkBlockEntity be, int slotIndex) {
        int progress = be.getModuleData(slotIndex, 0);
        int total = be.getModuleData(slotIndex, 1);
        if (total <= 0) return 0.0F;
        return 1.0F - ((float) progress / total);
    }

    /**
     * Hides the disc item in the machine screen so the overlay can fully cover the slot.
     */
    @Override
    public boolean hidesSlotItemInMachineScreen(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        // Hide the disc when there's a disc in the slot so the overlay covers it
        return !blockEntity.getInventoryHandler().getStackInSlot(slotIndex).isEmpty();
    }

    /**
     * Renders the module overlay in the machine screen.
     * Hides the disc item with a solid background, then draws the overlay texture and progress bar.
     */
    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        float progress = getProgressPercent(stack, blockEntity, slot.index);
        // Solid fill to fully hide the disc item underneath
        graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xFF444444);
        // Render the overlay texture on top of the solid background
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/jukebox_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16, 16, 16
        );
        // Draw horizontal progress bar (left to right) at the bottom of the slot
        if (progress > 0.0F) {
            int barWidth = (int) (progress * 14.0F);
            graphics.fill(slot.x + 1, slot.y + 13, slot.x + 15, slot.y + 15, 0xFF222222);
            graphics.fill(slot.x + 1, slot.y + 13, slot.x + 1 + barWidth, slot.y + 15, 0xFF33CC33);
        }
    }
}
