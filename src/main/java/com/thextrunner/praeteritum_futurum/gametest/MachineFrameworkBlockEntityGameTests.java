package com.thextrunner.praeteritum_futurum.gametest;

import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public class MachineFrameworkBlockEntityGameTests {
    private static final String TEST_TEMPLATE = "ruined_outpost";
    private static final String TEST_NAMESPACE = "praeteritum_futurum";

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void failedModuleCleanupDoesNotPreventOtherCleanups(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        int[] laterCleanupRuns = {0};
        machine.registerModuleCleanup(0, () -> {
            throw new IllegalStateException("Expected cleanup failure for regression test");
        });
        machine.registerModuleCleanup(1, () -> laterCleanupRuns[0]++);

        machine.runAllModuleCleanups();
        machine.runAllModuleCleanups();
        if (laterCleanupRuns[0] != 1) {
            helper.fail("All cleanups should run despite failures, then be cleared so they do not run again");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void moduleObjectStorageChecksRequestedType(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        machine.setModuleObject(0, "stored value");
        if (!"stored value".equals(machine.getModuleObject(0, String.class))) {
            helper.fail("Module object storage should return a value of the requested type");
            return;
        }

        boolean rejectedWrongType = false;
        try {
            machine.getModuleObject(0, Integer.class);
        } catch (ClassCastException expected) {
            rejectedWrongType = true;
        }
        if (!rejectedWrongType) {
            helper.fail("Module object storage should reject a value with the wrong requested type");
            return;
        }

        machine.setModuleObject(0, null);
        if (machine.getModuleObject(0, String.class) != null) {
            helper.fail("Setting a module object to null should clear its storage entry");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void effectiveFaceModeMergesExplicitly(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        ItemStack firstIo = new ItemStack(ModItems.IO_MODULE.get());
        ItemStack secondIo = new ItemStack(ModItems.IO_MODULE.get());
        machine.getModuleHandler().setStackInSlot(0, firstIo);
        machine.getModuleHandler().setStackInSlot(1, secondIo);

        if (machine.getEffectiveFaceMode(RelativeSide.FRONT) != IoModuleItem.FaceMode.DISABLED) {
            helper.fail("Unconfigured I/O faces should resolve to DISABLED");
            return;
        }

        IoModuleItem.setFaceMode(firstIo, RelativeSide.FRONT, IoModuleItem.FaceMode.INPUT);
        IoModuleItem.setFaceMode(secondIo, RelativeSide.FRONT, IoModuleItem.FaceMode.INPUT);
        if (machine.getEffectiveFaceMode(RelativeSide.FRONT) != IoModuleItem.FaceMode.INPUT) {
            helper.fail("Matching INPUT modes should remain INPUT");
            return;
        }

        IoModuleItem.setFaceMode(secondIo, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        if (machine.getEffectiveFaceMode(RelativeSide.FRONT) != IoModuleItem.FaceMode.OPEN) {
            helper.fail("Conflicting INPUT and OUTPUT modes should resolve to OPEN");
            return;
        }

        IoModuleItem.setFaceMode(firstIo, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        IoModuleItem.setFaceMode(secondIo, RelativeSide.FRONT, IoModuleItem.FaceMode.INPUT);
        if (machine.getEffectiveFaceMode(RelativeSide.FRONT) != IoModuleItem.FaceMode.OPEN) {
            helper.fail("Conflicting modes should resolve to OPEN regardless of module order");
            return;
        }

        IoModuleItem.setFaceMode(firstIo, RelativeSide.FRONT, IoModuleItem.FaceMode.OPEN);
        IoModuleItem.setFaceMode(secondIo, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        if (machine.getEffectiveFaceMode(RelativeSide.FRONT) != IoModuleItem.FaceMode.OPEN) {
            helper.fail("An OPEN mode should remain OPEN when combined with another active mode");
            return;
        }

        helper.succeed();
    }
}