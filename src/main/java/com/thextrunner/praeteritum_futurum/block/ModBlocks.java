package com.thextrunner.praeteritum_futurum.block;

import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registration class for all custom blocks in the mod.
 * <p>
 * NeoForge uses a deferred registration system: instead of creating blocks directly,
 * we register them as lazy suppliers (DeferredBlock) with a DeferredRegister. The register
 * is then attached to the mod's event bus in the main mod constructor, which tells NeoForge
 * to actually create and register the blocks at the right time during game startup.
 * <p>
 * Each block is given a registry name (the string passed to BLOCKS.register) which becomes
 * its resource location: praeteritum_futurum:block_name.
 */
public class ModBlocks {
    // The DeferredRegister is the container that holds all our block definitions.
    // It's tied to our mod ID so all registered blocks get the "praeteritum_futurum:" prefix.
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PraeteritumFuturumMod.MODID);

    // ITEMS register is kept here alongside BLOCKS for convenience — block items
    // (the pickable forms of blocks) are registered in ModItems, but they reference
    // the DeferredBlock handles defined here.
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PraeteritumFuturumMod.MODID);

    /**
     * Salvage Framing — a basic decorative/structural block that drops the resource.
     * Uses default Block class with metal coloring and moderate break speed (2.0 seconds).
     */
    public static final DeferredBlock<Block> SALVAGE_FRAMING =
            BLOCKS.register(
                    "salvage_framing",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)       // Determines the block's color in maps and the F3 debug screen
                                    .destroyTime(2.0f)             // How long it takes to break the block by hand (in seconds)
                    )
            );

    /**
     * Advanced Salvage — a more fragile variant of salvage framing.
     * Breaks much faster (0.7 seconds) to reflect its delicate nature.
     */
    public static final DeferredBlock<Block> ADVANCED_SALVAGE =
            BLOCKS.register(
                    "advanced_salvage",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .destroyTime(0.7f)             // Fragile block — breaks quickly
                    )
            );

    /**
     * Machine Framework — the core block of the entire mod.
     * <p>
     * This is the block that players place to create their automation machines.
     * It extends MachineFrameworkBlock (a custom block class) which handles
     * rotation, right-click interaction, and block entity creation.
     * <p>
     * Properties:
     * - destroyTime(3.5f): Harder to break than regular blocks, reflecting its importance
     * - requiresCorrectToolForDrops(): Must be mined with a pickaxe to drop itself
     */
    public static final DeferredBlock<Block> MACHINE_FRAMEWORK =
            BLOCKS.register(
                    "machine_framework",
                    () -> new MachineFrameworkBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .destroyTime(3.5f)
                                    .requiresCorrectToolForDrops()
                    )
            );
}