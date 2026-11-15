package com.thextrunner.praeteritum_futurum.gametest;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(PraeteritumFuturumMod.MODID)
public final class PistonUnloadedDestinationGameTests {
    private PistonUnloadedDestinationGameTests() {
    }

    @GameTest(template = "empty", batch = "unloadedDestination", timeoutTicks = 40)
    public static void abortsMachinePushWhenDestinationIsUnavailable(GameTestHelper helper) {
        BlockPos pistonPos = new BlockPos(1, 1, 1);
        BlockPos machinePos = pistonPos.relative(Direction.EAST);
        BlockPos destinationPos = machinePos.relative(Direction.EAST);
        BlockPos absoluteDestinationPos = helper.absolutePos(destinationPos);

        helper.setBlock(pistonPos, Blocks.PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState()
                .setValue(MachineFrameworkBlock.FACING, Direction.NORTH));
        helper.setBlock(destinationPos, Blocks.AIR);

        if (!(helper.getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine block entity was not created", machinePos);
            return;
        }
        machine.getModuleHandler().setStackInSlot(0, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));

        // Only the mod's piston preflight sees this destination as unavailable.
        // The real GameTest chunk stays loaded so the test can inspect world state.
        PistonAbortTestFixture.forceUnloadedDestination(helper.getLevel(), absoluteDestinationPos, 20);
        helper.setBlock(pistonPos.below(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(5, () -> {
            int destinationChecks = PistonAbortTestFixture.destinationCheckCount(
                    helper.getLevel(), absoluteDestinationPos);
            PistonAbortTestFixture.clear(helper.getLevel(), absoluteDestinationPos);

            if (destinationChecks == 0) {
                helper.fail("Piston preflight did not check the simulated-unloaded destination", destinationPos);
                return;
            }
            if (!helper.getBlockState(pistonPos).hasProperty(PistonBaseBlock.EXTENDED)
                    || helper.getBlockState(pistonPos).getValue(PistonBaseBlock.EXTENDED)) {
                helper.fail("Piston extended despite the unloaded-destination abort", pistonPos);
                return;
            }
            if (!helper.getBlockState(machinePos).is(ModBlocks.MACHINE_FRAMEWORK.get())) {
                helper.fail("Machine moved or was removed despite the abort", machinePos);
                return;
            }
            if (!helper.getBlockState(destinationPos).isAir()) {
                helper.fail("Destination changed despite the abort", destinationPos);
                return;
            }

            MachineFrameworkBlockEntity remaining = helper.getBlockEntity(machinePos);
            if (remaining == null
                    || !remaining.getModuleHandler().getStackInSlot(0).is(ModItems.INVENTORY_MODULE.get())
                    || remaining.getInventoryHandler().getStackInSlot(0).getCount() != 3
                    || !remaining.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)) {
                helper.fail("Machine module or inventory data changed despite the abort", machinePos);
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "unloadedDestruction", timeoutTicks = 40)
    public static void abortsMachinePushWhenDestructionPositionIsUnavailable(GameTestHelper helper) {
        BlockPos pistonPos = new BlockPos(1, 1, 1);
        BlockPos machinePos = pistonPos.relative(Direction.EAST);
        BlockPos destructionPos = pistonPos.relative(Direction.EAST, 13);
        BlockPos absoluteDestructionPos = helper.absolutePos(destructionPos);

        helper.setBlock(pistonPos, Blocks.PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        for (int x = 3; x <= 13; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.COBBLESTONE);
        }
        helper.setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState()
                .setValue(MachineFrameworkBlock.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(14, 0, 1), Blocks.COBBLESTONE);
        helper.setBlock(destructionPos, Blocks.TORCH);

        if (!(helper.getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine block entity was not created", machinePos);
            return;
        }
        machine.getModuleHandler().setStackInSlot(0, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));

        // The regular destination checks see fully loaded GameTest chunks. This
        // role-specific override reaches the separate toDestroy preflight check.
        PistonAbortTestFixture.forceUnloadedDestructionPosition(
                helper.getLevel(), absoluteDestructionPos, 20);
        helper.setBlock(pistonPos.below(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(5, () -> {
            int destructionChecks = PistonAbortTestFixture.destructionCheckCount(
                    helper.getLevel(), absoluteDestructionPos);
            PistonAbortTestFixture.clearDestructionPosition(helper.getLevel(), absoluteDestructionPos);

            if (destructionChecks == 0) {
                helper.fail("Piston preflight did not check the simulated-unloaded destruction position",
                        destructionPos);
                return;
            }
            if (!helper.getBlockState(pistonPos).hasProperty(PistonBaseBlock.EXTENDED)
                    || helper.getBlockState(pistonPos).getValue(PistonBaseBlock.EXTENDED)) {
                helper.fail("Piston extended despite the unloaded-destruction abort", pistonPos);
                return;
            }
            if (!helper.getBlockState(machinePos).is(ModBlocks.MACHINE_FRAMEWORK.get())
                    || !helper.getBlockState(destructionPos).is(Blocks.TORCH)) {
                helper.fail("Machine or destroyable block changed despite the abort", machinePos);
                return;
            }
            for (int x = 3; x <= 13; x++) {
                if (!helper.getBlockState(new BlockPos(x, 1, 1)).is(Blocks.COBBLESTONE)) {
                    helper.fail("Push line changed despite the unloaded-destruction abort", new BlockPos(x, 1, 1));
                    return;
                }
            }

            MachineFrameworkBlockEntity remaining = helper.getBlockEntity(machinePos);
            if (remaining == null
                    || !remaining.getModuleHandler().getStackInSlot(0).is(ModItems.INVENTORY_MODULE.get())
                    || remaining.getInventoryHandler().getStackInSlot(0).getCount() != 3
                    || !remaining.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)) {
                helper.fail("Machine module or inventory data changed despite the abort", machinePos);
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "stickyRetractionUnavailable", timeoutTicks = 50)
    public static void keepsMachineAndPistonConsistentWhenStickyPullDestinationIsUnavailable(
            GameTestHelper helper) {
        BlockPos pistonPos = new BlockPos(1, 1, 1);
        BlockPos headPos = pistonPos.relative(Direction.EAST);
        BlockPos initialMachinePos = headPos;
        BlockPos pushedMachinePos = pistonPos.relative(Direction.EAST, 2);
        BlockPos absolutePullDestination = helper.absolutePos(headPos);
        BlockPos powerPos = pistonPos.below();

        helper.setBlock(pistonPos, Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(initialMachinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState()
                .setValue(MachineFrameworkBlock.FACING, Direction.NORTH));

        if (!(helper.getBlockEntity(initialMachinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine block entity was not created", initialMachinePos);
            return;
        }
        machine.getModuleHandler().setStackInSlot(0, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(5, () -> {
            if (!helper.getBlockState(pistonPos).getValue(PistonBaseBlock.EXTENDED)
                    || !helper.getBlockState(pushedMachinePos).is(ModBlocks.MACHINE_FRAMEWORK.get())) {
                helper.fail("Sticky piston did not complete the initial machine push", pushedMachinePos);
                return;
            }
            MachineFrameworkBlockEntity pushedMachine = helper.getBlockEntity(pushedMachinePos);
            if (pushedMachine == null
                    || !pushedMachine.getModuleHandler().getStackInSlot(0).is(ModItems.INVENTORY_MODULE.get())
                    || pushedMachine.getInventoryHandler().getStackInSlot(0).getCount() != 3
                    || !pushedMachine.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)) {
                helper.fail("Machine data was not intact after the initial push", pushedMachinePos);
                return;
            }

            // The pull destination is the sticky piston head position. Simulate
            // it as unavailable only for the retraction preflight.
            PistonAbortTestFixture.forceUnloadedDestination(helper.getLevel(), absolutePullDestination, 20);
            helper.setBlock(powerPos, Blocks.AIR);

            helper.runAfterDelay(8, () -> {
                int destinationChecks = PistonAbortTestFixture.destinationCheckCount(
                        helper.getLevel(), absolutePullDestination);
                PistonAbortTestFixture.clear(helper.getLevel(), absolutePullDestination);

                if (destinationChecks == 0) {
                    helper.fail("Sticky retraction did not check the simulated-unloaded pull destination",
                            headPos);
                    return;
                }
                if (!helper.getBlockState(pushedMachinePos).is(ModBlocks.MACHINE_FRAMEWORK.get())
                        || helper.getBlockState(initialMachinePos).is(ModBlocks.MACHINE_FRAMEWORK.get())) {
                    helper.fail("Machine moved or duplicated despite the unavailable pull destination",
                            pushedMachinePos);
                    return;
                }
                MachineFrameworkBlockEntity remaining = helper.getBlockEntity(pushedMachinePos);
                if (remaining == null
                        || !remaining.getModuleHandler().getStackInSlot(0).is(ModItems.INVENTORY_MODULE.get())
                        || remaining.getInventoryHandler().getStackInSlot(0).getCount() != 3
                        || !remaining.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)) {
                    helper.fail("Machine module or inventory data was lost during aborted sticky retraction",
                            pushedMachinePos);
                    return;
                }

                boolean pistonExtended = helper.getBlockState(pistonPos).getValue(PistonBaseBlock.EXTENDED);
                boolean pistonHeadPresent = helper.getBlockState(headPos).is(Blocks.PISTON_HEAD);
                if (pistonExtended != pistonHeadPresent) {
                    helper.fail("Piston base/head state is inconsistent after aborted sticky retraction", pistonPos);
                    return;
                }

                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", batch = "stickySideBranchData", timeoutTicks = 60)
    public static void preservesMachineDataOnSlimeChainSideBranchRetraction(GameTestHelper helper) {
        verifyStickySideBranchRetraction(helper, Blocks.SLIME_BLOCK, 2);
    }

    @GameTest(template = "empty", batch = "stickySideBranchData", timeoutTicks = 60)
    public static void preservesMachineDataOnHoneySideBranchRetraction(GameTestHelper helper) {
        verifyStickySideBranchRetraction(helper, Blocks.HONEY_BLOCK, 1);
    }

    private static void verifyStickySideBranchRetraction(GameTestHelper helper,
                                                          net.minecraft.world.level.block.Block stickyBlock,
                                                          int chainLength) {
        // Keep the structure above the template floor so the test isolates
        // sticky side branches instead of pulling the surrounding ground.
        BlockPos pistonPos = new BlockPos(1, 3, 1);
        BlockPos firstStickyPos = pistonPos.relative(Direction.EAST);
        BlockPos machineSourcePos = firstStickyPos.relative(Direction.EAST, chainLength - 1).south();
        BlockPos machineDestinationPos = machineSourcePos.east();
        BlockPos powerPos = pistonPos.below();

        helper.setBlock(pistonPos, Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        for (int i = 0; i < chainLength; i++) {
            helper.setBlock(firstStickyPos.relative(Direction.EAST, i), stickyBlock);
        }
        helper.setBlock(machineSourcePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState()
                .setValue(MachineFrameworkBlock.FACING, Direction.NORTH));

        if (!(helper.getBlockEntity(machineSourcePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine block entity was not created", machineSourcePos);
            return;
        }
        machine.getModuleHandler().setStackInSlot(0, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(6, () -> {
            if (!helper.getBlockState(pistonPos).getValue(PistonBaseBlock.EXTENDED)
                    || !helper.getBlockState(machineDestinationPos).is(ModBlocks.MACHINE_FRAMEWORK.get())) {
                helper.fail("Sticky piston did not extend the chain and side-branch machine", machineDestinationPos);
                return;
            }
            MachineFrameworkBlockEntity pushedMachine = helper.getBlockEntity(machineDestinationPos);
            if (!hasRegressionMachineData(pushedMachine)) {
                helper.fail("Machine data was lost during the sticky push", machineDestinationPos);
                return;
            }

            helper.setBlock(powerPos, Blocks.AIR);
            helper.runAfterDelay(6, () -> {
                if (helper.getBlockState(pistonPos).getValue(PistonBaseBlock.EXTENDED)
                        || !helper.getBlockState(machineSourcePos).is(ModBlocks.MACHINE_FRAMEWORK.get())
                        || helper.getBlockState(machineDestinationPos).is(ModBlocks.MACHINE_FRAMEWORK.get())) {
                    helper.fail("Sticky retraction did not return the side-branch machine", machineSourcePos);
                    return;
                }
                if (!hasRegressionMachineData(helper.getBlockEntity(machineSourcePos))) {
                    helper.fail("Module or inventory data was lost during sticky side-branch retraction",
                            machineSourcePos);
                    return;
                }
                helper.succeed();
            });
        });
    }

    private static boolean hasRegressionMachineData(MachineFrameworkBlockEntity machine) {
        return machine != null
                && machine.getModuleHandler().getStackInSlot(0).is(ModItems.INVENTORY_MODULE.get())
                && machine.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)
                && machine.getInventoryHandler().getStackInSlot(0).getCount() == 3;
    }
}
