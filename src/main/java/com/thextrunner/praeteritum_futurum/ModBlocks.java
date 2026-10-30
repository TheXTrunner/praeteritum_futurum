package com.thextrunner.praeteritum_futurum;
//this is like necessary for some reason, I only know python and a bit of lua lmao

import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
// these are like the libraries or something? I guess it imports functions from them

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PraeteritumFuturumMod.MODID);
    // I have no idea what this is supposed to do

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PraeteritumFuturumMod.MODID);

    // this is the first block or whatever
    public static final DeferredBlock<Block> SALVAGE_FRAMING =
            BLOCKS.register(
                    "salvage_framing",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .destroyTime(2.5f)//breaking time
                            //and these are like how the block is treated, I guess the properties of how hard it is, color and whatnot
                            //I guess for this initial test we want something super basic and this is as basic as it gets
                    )
            );

    public static final DeferredBlock<Block> ADVANCED_SALVAGE =//another block
            BLOCKS.register(
                    "advanced_salvage",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .destroyTime(0.9f) // this is to adjust the break time, check timer
                    )
            );

    // The Machine Framework block entity casing!
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