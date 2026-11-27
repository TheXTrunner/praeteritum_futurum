package com.thextrunner.praeteritum_futurum.gametest;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.EjectionModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public class EjectionModuleGameTests {
    private static final String TEST_TEMPLATE = "ruined_outpost";
    private static final String TEST_NAMESPACE = "praeteritum_futurum";

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void ejectionModuleRedstoneModesCycleAndDefaultToIgnore(GameTestHelper helper) {
        EjectionModuleItem module = ModItems.EJECTION_MODULE.get();
        ItemStack stack = new ItemStack(module);

        if (!module.supportsRedstoneControl()
                || module.getRedstoneControlMode(stack) != BaseModuleItem.RedstoneControlMode.IGNORE) {
            helper.fail("Ejection should support redstone control and default to IGNORE");
            return;
        }

        module.cycleRedstoneControlMode(stack, false);
        if (module.getRedstoneControlMode(stack) != BaseModuleItem.RedstoneControlMode.HIGH) {
            helper.fail("Normal cycling should advance IGNORE to repeated ejection while powered");
            return;
        }
        module.cycleRedstoneControlMode(stack, false);
        if (module.getRedstoneControlMode(stack) != BaseModuleItem.RedstoneControlMode.LOW) {
            helper.fail("Normal cycling should advance HIGH to repeated ejection while unpowered");
            return;
        }
        module.cycleRedstoneControlMode(stack, false);
        if (module.getRedstoneControlMode(stack) != BaseModuleItem.RedstoneControlMode.PULSE) {
            helper.fail("Normal cycling should advance LOW to pulse-triggered ejection");
            return;
        }
        module.cycleRedstoneControlMode(stack, true);
        if (module.getRedstoneControlMode(stack) != BaseModuleItem.RedstoneControlMode.LOW) {
            helper.fail("Shift-cycling should move backward through ejection control modes");
            return;
        }
        module.cycleRedstoneControlMode(stack, true);
        module.cycleRedstoneControlMode(stack, true);
        if (module.getRedstoneControlMode(stack) != BaseModuleItem.RedstoneControlMode.IGNORE) {
            helper.fail("Reverse cycling should wrap from HIGH back to IGNORE");
            return;
        }
        module.cycleRedstoneControlMode(stack, true);
        if (module.getRedstoneControlMode(stack) != BaseModuleItem.RedstoneControlMode.PULSE) {
            helper.fail("Reverse cycling from IGNORE should wrap to PULSE");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void ejectionPulseModeQueuesOnePassPerRisingEdge(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        int ejectionSlot = 13;
        int catchSlot = GridDirection.LEFT.getAdjacentSlotIndex(ejectionSlot);
        int dropSlot = GridDirection.DOWN.getAdjacentSlotIndex(ejectionSlot);
        int ioSlot = GridDirection.UP.getAdjacentSlotIndex(ejectionSlot);

        EjectionModuleItem ejectionModule = ModItems.EJECTION_MODULE.get();
        ItemStack ejectionStack = new ItemStack(ejectionModule);
        ejectionModule.setRedstoneControlMode(ejectionStack, BaseModuleItem.RedstoneControlMode.PULSE);
        ejectionModule.setInventoryMode(ejectionStack, GridDirection.LEFT, EjectionModuleItem.InventoryMode.CATCH);
        ejectionModule.setInventoryMode(ejectionStack, GridDirection.DOWN, EjectionModuleItem.InventoryMode.DROP);
        machine.getModuleHandler().setStackInSlot(ejectionSlot, ejectionStack);
        machine.getModuleHandler().setStackInSlot(catchSlot, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getModuleHandler().setStackInSlot(dropSlot, new ItemStack(ModItems.INVENTORY_MODULE.get()));

        ItemStack ioStack = new ItemStack(ModItems.IO_MODULE.get());
        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OPEN);
        machine.getModuleHandler().setStackInSlot(ioSlot, ioStack);
        machine.getInventoryHandler().setStackInSlot(dropSlot, new ItemStack(Items.COBBLESTONE, 64));

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        BlockPos leverPos = machinePos.relative(machineFacing);
        helper.getLevel().setBlock(leverPos.below(), Blocks.STONE.defaultBlockState(), 3);
        var unpoweredLever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, false);
        helper.getLevel().setBlock(leverPos, unpoweredLever, 3);

        if (machine.getRedstoneInputSignal() != 0) {
            helper.fail("The OPEN I/O face should read the initially unpowered external lever as zero");
            return;
        }

        helper.runAfterDelay(2, () -> {
            helper.getLevel().setBlock(leverPos, unpoweredLever.setValue(LeverBlock.POWERED, true), 3);
            helper.runAfterDelay(1, () -> {
                helper.getLevel().setBlock(leverPos, unpoweredLever, 3);
                helper.runAfterDelay(Config.getIoTransferTickRate() + 2, () -> {
                    if (!hasExpectedTransfer(machine, catchSlot, dropSlot, 1)) {
                        helper.fail("A short rising-edge pulse should queue one ejection pass");
                        return;
                    }

                    helper.runAfterDelay(Config.getIoTransferTickRate() + 2, () -> {
                        if (!hasExpectedTransfer(machine, catchSlot, dropSlot, 1)) {
                            helper.fail("A single pulse must not eject repeatedly after its pending pass is consumed");
                            return;
                        }

                        helper.getLevel().setBlock(leverPos,
                                unpoweredLever.setValue(LeverBlock.POWERED, true), 3);
                        helper.runAfterDelay(Config.getIoTransferTickRate() + 2, () -> {
                            if (!hasExpectedTransfer(machine, catchSlot, dropSlot, 2)) {
                                helper.fail("A new rising edge should queue exactly one more ejection pass");
                                return;
                            }

                            helper.runAfterDelay(Config.getIoTransferTickRate() + 2, () -> {
                                if (!hasExpectedTransfer(machine, catchSlot, dropSlot, 2)) {
                                    helper.fail("Holding the signal on must not retrigger pulse-mode ejection");
                                    return;
                                }
                                helper.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    private static boolean hasExpectedTransfer(MachineFrameworkBlockEntity machine,
                                               int catchSlot, int dropSlot, int expectedCount) {
        return machine.getInventoryHandler().getStackInSlot(catchSlot).getCount() == expectedCount
                && machine.getInventoryHandler().getStackInSlot(dropSlot).getCount() == 64 - expectedCount;
    }
}