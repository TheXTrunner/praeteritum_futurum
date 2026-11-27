package com.thextrunner.praeteritum_futurum.gametest;

import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuitState;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneComponent;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneSlotData;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.SmeltingModuleItem;
import com.thextrunner.praeteritum_futurum.network.ServerboundRedstoneConfigPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public class RedstoneCircuitGameTests {
    private static final String TEST_TEMPLATE = "ruined_outpost";
    private static final String TEST_NAMESPACE = "praeteritum_futurum";

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void redstoneConfigPacketValidationChecksIndicesAndActions(GameTestHelper helper) {
        for (int action = ServerboundRedstoneConfigPacket.ACTION_TOGGLE_LEVER;
             action <= ServerboundRedstoneConfigPacket.ACTION_CYCLE_BUTTON_ATTACHMENT; action++) {
            if (!new ServerboundRedstoneConfigPacket(0, 0, action, BlockPos.ZERO).isValid()) {
                helper.fail("A supported redstone configuration action must be accepted");
                return;
            }
        }

        if (!new ServerboundRedstoneConfigPacket(0, 0,
                ServerboundRedstoneConfigPacket.ACTION_SET_SIGNAL_SOURCE_STRENGTH, 0, BlockPos.ZERO).isValid() ||
            !new ServerboundRedstoneConfigPacket(0, 0,
                ServerboundRedstoneConfigPacket.ACTION_SET_SIGNAL_SOURCE_STRENGTH, 15, BlockPos.ZERO).isValid()) {
            helper.fail("Signal source strengths from 0 to 15 must be accepted");
            return;
        }

        if (!new ServerboundRedstoneConfigPacket(MachineFrameworkBlockEntity.GRID_SIZE - 1,
                RedstoneCircuit.GRID_SIZE - 1, ServerboundRedstoneConfigPacket.ACTION_CYCLE_REPEATER_DELAY,
                BlockPos.ZERO).isValid()) {
            helper.fail("The last valid module and circuit slots must be accepted");
            return;
        }

        if (new ServerboundRedstoneConfigPacket(-1, 0, ServerboundRedstoneConfigPacket.ACTION_TOGGLE_LEVER,
                    BlockPos.ZERO).isValid() ||
            new ServerboundRedstoneConfigPacket(MachineFrameworkBlockEntity.GRID_SIZE, 0,
                    ServerboundRedstoneConfigPacket.ACTION_TOGGLE_LEVER, BlockPos.ZERO).isValid() ||
            new ServerboundRedstoneConfigPacket(0, -1, ServerboundRedstoneConfigPacket.ACTION_TOGGLE_LEVER,
                    BlockPos.ZERO).isValid() ||
            new ServerboundRedstoneConfigPacket(0, RedstoneCircuit.GRID_SIZE,
                    ServerboundRedstoneConfigPacket.ACTION_TOGGLE_LEVER, BlockPos.ZERO).isValid() ||
            new ServerboundRedstoneConfigPacket(0, 0, -1, BlockPos.ZERO).isValid() ||
            new ServerboundRedstoneConfigPacket(0, 0,
                    ServerboundRedstoneConfigPacket.ACTION_SET_SIGNAL_SOURCE_STRENGTH + 1, BlockPos.ZERO).isValid()) {
            helper.fail("Out-of-range indices and unknown actions must be rejected");
            return;
        }
        if (new ServerboundRedstoneConfigPacket(0, 0,
                    ServerboundRedstoneConfigPacket.ACTION_SET_SIGNAL_SOURCE_STRENGTH, -1, BlockPos.ZERO).isValid() ||
            new ServerboundRedstoneConfigPacket(0, 0,
                    ServerboundRedstoneConfigPacket.ACTION_SET_SIGNAL_SOURCE_STRENGTH, 16, BlockPos.ZERO).isValid()) {
            helper.fail("Signal source strengths outside 0-15 must be rejected");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void moduleRedstoneControlUsesAdjacentFacingBordersAndThreePolicies(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        int controlledSlot = 13;
        int leftRedstoneSlot = 12;
        int rightRedstoneSlot = 14;
        int upperRedstoneSlot = 4;
        int lowerRedstoneSlot = 22;
        int remoteRedstoneSlot = 0;
        BaseModuleItem controlledModule = (BaseModuleItem) ModItems.INVENTORY_MODULE.get();
        machine.getModuleHandler().setStackInSlot(controlledSlot, new ItemStack(controlledModule));
        machine.getModuleHandler().setStackInSlot(leftRedstoneSlot, new ItemStack(ModItems.REDSTONE_MODULE.get()));
        machine.getModuleHandler().setStackInSlot(rightRedstoneSlot, new ItemStack(ModItems.REDSTONE_MODULE.get()));
        machine.getModuleHandler().setStackInSlot(upperRedstoneSlot, new ItemStack(ModItems.REDSTONE_MODULE.get()));
        machine.getModuleHandler().setStackInSlot(lowerRedstoneSlot, new ItemStack(ModItems.REDSTONE_MODULE.get()));
        machine.getModuleHandler().setStackInSlot(remoteRedstoneSlot, new ItemStack(ModItems.REDSTONE_MODULE.get()));

        // The controlled slot is between these modules. Only each neighboring module's
        // edge facing the controlled slot should contribute to the signal.
        machine.setModuleData(leftRedstoneSlot, 3, 7);
        machine.setModuleData(leftRedstoneSlot, 2, 15);
        machine.setModuleData(rightRedstoneSlot, 2, 11);
        machine.setModuleData(rightRedstoneSlot, 3, 15);
        machine.setModuleData(upperRedstoneSlot, 1, 9);
        machine.setModuleData(upperRedstoneSlot, 0, 15);
        machine.setModuleData(lowerRedstoneSlot, 0, 8);
        machine.setModuleData(lowerRedstoneSlot, 1, 15);
        machine.setModuleData(remoteRedstoneSlot, 3, 15);

        if (controlledModule.getRedstoneControlSignal(machine, controlledSlot) != 11) {
            helper.fail("Control should use the strongest facing edge from adjacent redstone modules only");
            return;
        }

        if (controlledModule.supportsRedstoneControl() ||
                !controlledModule.isRedstoneControlSatisfied(new ItemStack(controlledModule), machine, controlledSlot)) {
            helper.fail("Modules that have not opted in must remain unaffected by redstone control");
            return;
        }

        if (!BaseModuleItem.RedstoneControlMode.IGNORE.allowsSignal(0) ||
                !BaseModuleItem.RedstoneControlMode.IGNORE.allowsSignal(15) ||
                !BaseModuleItem.RedstoneControlMode.HIGH.allowsSignal(11) ||
                BaseModuleItem.RedstoneControlMode.HIGH.allowsSignal(0) ||
                BaseModuleItem.RedstoneControlMode.LOW.allowsSignal(11) ||
                !BaseModuleItem.RedstoneControlMode.LOW.allowsSignal(0)) {
            helper.fail("IGNORE, HIGH, and LOW must implement their expected powered/unpowered policies");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void processingModuleRedstoneControlReadsEnabledIoWorldInputs(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        int processingSlot = 13;
        ProcessingModuleItem processingModule = (ProcessingModuleItem) ModItems.SMELTING_MODULE.get();
        ItemStack processingStack = new ItemStack(processingModule);
        processingModule.setRedstoneControlMode(processingStack, BaseModuleItem.RedstoneControlMode.HIGH);
        machine.getModuleHandler().setStackInSlot(processingSlot, processingStack);

        ItemStack ioStack = createConfiguredIoModule();
        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.INPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        BlockPos leverPos = machinePos.relative(machineFacing);
        helper.getLevel().setBlock(leverPos.below(), Blocks.STONE.defaultBlockState(), 3);
        var poweredLever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, true);
        helper.getLevel().setBlock(leverPos, poweredLever, 3);

        if (helper.getLevel().getSignal(leverPos, machineFacing.getOpposite()) <= 0) {
            helper.fail("The powered external lever must emit a signal toward the machine");
            return;
        }
        if (!processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
            helper.fail("An INPUT face should let world redstone directly enable the processing module");
            return;
        }

        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OPEN);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        if (!processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
            helper.fail("An OPEN face should let world redstone directly enable the processing module");
            return;
        }

        for (IoModuleItem.FaceMode mode : new IoModuleItem.FaceMode[]{
                IoModuleItem.FaceMode.OUTPUT, IoModuleItem.FaceMode.DISABLED}) {
            IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, mode);
            machine.getModuleHandler().setStackInSlot(1, ioStack);
            if (processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
                helper.fail(mode + " faces must not supply world redstone to module controls");
                return;
            }
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void processingModuleRedstoneControlCyclesAndChecksPolicies(GameTestHelper helper) {
        Item[] processingModules = {
                ModItems.SMELTING_MODULE.get(), ModItems.BLAST_MODULE.get(), ModItems.SMOKER_MODULE.get(),
                ModItems.STONECUTTER_MODULE.get(), ModItems.COMPOSTER_MODULE.get(), ModItems.GRINDSTONE_MODULE.get(),
                ModItems.CARTOGRAPHY_MODULE.get(), ModItems.ANVIL_MODULE.get(), ModItems.BREWING_MODULE.get()
        };
        for (Item item : processingModules) {
            if (!(item instanceof ProcessingModuleItem module) || !module.supportsRedstoneControl()) {
                helper.fail("Regular processing modules should support optional redstone control");
                return;
            }
        }
        BaseModuleItem jukebox = (BaseModuleItem) ModItems.JUKEBOX_MODULE.get();
        if (jukebox.supportsRedstoneControl() || jukebox.isRedstoneControlButton(18, 18, 0, 0)) {
            helper.fail("The Jukebox should not expose or inherit redstone control");
            return;
        }
        BaseModuleItem filter = (BaseModuleItem) ModItems.FILTER_MODULE.get();
        if (filter.supportsRedstoneControl() || filter.isRedstoneControlButton(18, 18, 0, 0)) {
            helper.fail("The basic Filter Module should not expose redstone control");
            return;
        }

        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        int processingSlot = 13;
        int redstoneSlot = 12;
        ProcessingModuleItem processingModule = (ProcessingModuleItem) ModItems.SMELTING_MODULE.get();
        ItemStack processingStack = new ItemStack(processingModule);
        machine.getModuleHandler().setStackInSlot(processingSlot, processingStack);
        machine.getModuleHandler().setStackInSlot(redstoneSlot, new ItemStack(ModItems.REDSTONE_MODULE.get()));

        if (!processingModule.isRedstoneControlButton(18, 18, 0, 0)
                || !processingModule.isPointInOverlay(18, 18, 0, 0)
                || !processingModule.doesOverlayCoverSlot(0, 0, 18, 18)
                || processingModule.isRedstoneControlButton(18, -18, 0, 0)) {
            helper.fail("The redstone-control button should be at bottom-right and cover only its own slot");
            return;
        }

        IoModuleItem ioModule = ModItems.IO_MODULE.get();
        if (!ioModule.isPointInOverlay(36, 0, 0, 0)
                || !ioModule.doesOverlayCoverSlot(0, 0, 36, 0)
                || ioModule.isPointInOverlay(18, 18, 0, 0)
                || ioModule.doesOverlayCoverSlot(0, 0, 18, 18)) {
            helper.fail("The I/O overlay should cover its actual controls without capturing the gaps around them");
            return;
        }

        if (processingModule.getRedstoneControlMode(processingStack) != BaseModuleItem.RedstoneControlMode.IGNORE) {
            helper.fail("Processing modules should default to IGNORE so existing behavior is unchanged");
            return;
        }
        processingModule.cycleRedstoneControlMode(processingStack, false);
        if (processingModule.getRedstoneControlMode(processingStack) != BaseModuleItem.RedstoneControlMode.HIGH) {
            helper.fail("Normal click should cycle IGNORE to HIGH");
            return;
        }
        processingModule.cycleRedstoneControlMode(processingStack, false);
        if (processingModule.getRedstoneControlMode(processingStack) != BaseModuleItem.RedstoneControlMode.LOW) {
            helper.fail("Normal click should cycle HIGH to LOW");
            return;
        }
        processingModule.cycleRedstoneControlMode(processingStack, true);
        if (processingModule.getRedstoneControlMode(processingStack) != BaseModuleItem.RedstoneControlMode.HIGH) {
            helper.fail("Shift-click should cycle the policy backward");
            return;
        }

        processingModule.setRedstoneControlMode(processingStack, BaseModuleItem.RedstoneControlMode.HIGH);
        machine.setModuleData(redstoneSlot, 3, 15);
        if (!processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
            helper.fail("HIGH mode should allow a processing module while its adjacent border is powered");
            return;
        }

        machine.setModuleData(redstoneSlot, 3, 0);
        if (processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
            helper.fail("HIGH mode should block a processing module while its adjacent border is unpowered");
            return;
        }

        processingModule.setRedstoneControlMode(processingStack, BaseModuleItem.RedstoneControlMode.LOW);
        if (!processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
            helper.fail("LOW mode should allow a processing module while its adjacent border is unpowered");
            return;
        }

        machine.setModuleData(redstoneSlot, 3, 15);
        if (processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
            helper.fail("LOW mode should block a processing module while its adjacent border is powered");
            return;
        }

        processingModule.setRedstoneControlMode(processingStack, BaseModuleItem.RedstoneControlMode.IGNORE);
        if (!processingModule.isRedstoneControlSatisfied(processingStack, machine, processingSlot)) {
            helper.fail("IGNORE mode should allow normal processing while powered");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void moduleOverlayHitTestsUseHalfOpenBounds(GameTestHelper helper) {
        BaseModuleItem processing = (BaseModuleItem) ModItems.SMELTING_MODULE.get();
        BaseModuleItem ejection = (BaseModuleItem) ModItems.EJECTION_MODULE.get();
        BaseModuleItem io = ModItems.IO_MODULE.get();
        BaseModuleItem jukebox = (BaseModuleItem) ModItems.JUKEBOX_MODULE.get();

        if (!processing.isInsideOverlay(0, 0, 0, 0)
                || processing.isInsideOverlay(16, 8, 0, 0)
                || !processing.isInsideOverlay(8, -18, 0, 0)
                || processing.isInsideOverlay(8, -2, 0, 0)) {
            helper.fail("Processing overlay hit boxes should include their top/left edges and exclude their right/bottom edges");
            return;
        }

        if (!ejection.isInsideOverlay(0, 0, 0, 0)
                || ejection.isInsideOverlay(16, 8, 0, 0)
                || !ejection.isInsideOverlay(8, -18, 0, 0)
                || ejection.isInsideOverlay(8, -2, 0, 0)) {
            helper.fail("Ejection overlay hit boxes should use the same half-open edge convention");
            return;
        }

        if (!io.isInsideOverlay(0, 0, 0, 0)
                || io.isInsideOverlay(16, 8, 0, 0)
                || !io.isInsideOverlay(36, 0, 0, 0)
                || io.isInsideOverlay(52, 8, 0, 0)) {
            helper.fail("I/O overlay buttons should use half-open 16x16 bounds");
            return;
        }

        if (jukebox.isInsideOverlay(8, 8, 0, 0)
                || !jukebox.isInsideOverlay(0, -18, 0, 0)
                || jukebox.isInsideOverlay(16, -10, 0, 0)) {
            helper.fail("Jukebox should leave its disc slot interactive and use half-open button bounds");
            return;
        }

        if (!processing.isCloseButton(-18, -18, 0, 0)
                || processing.isCloseButton(-2, -10, 0, 0)
                || processing.isCloseButton(-10, -2, 0, 0)) {
            helper.fail("Close-button hit testing should include its top/left edges and exclude its right/bottom edges");
            return;
        }

        if (processing.isInsideOverlay(18, 18, 0, 0)
                || !processing.isPointInOverlay(18, 18, 0, 0)
                || processing.isPointInOverlay(34, 18, 0, 0)
                || processing.isPointInOverlay(18, 34, 0, 0)) {
            helper.fail("Shared redstone-button hit testing should be handled once with half-open bounds");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void processingModulePausesAndResumesWithIoRedstoneControl(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        int processingSlot = 13;
        int inputSlot = GridDirection.UP.getAdjacentSlotIndex(processingSlot);
        int fuelSlot = GridDirection.LEFT.getAdjacentSlotIndex(processingSlot);
        int outputSlot = GridDirection.RIGHT.getAdjacentSlotIndex(processingSlot);
        SmeltingModuleItem processingModule = (SmeltingModuleItem) ModItems.SMELTING_MODULE.get();
        ItemStack processingStack = new ItemStack(processingModule);
        processingModule.setRedstoneControlMode(processingStack, BaseModuleItem.RedstoneControlMode.HIGH);
        ProcessingModuleItem.setSlotMode(processingStack, GridDirection.UP, SmeltingModuleItem.SlotMode.INPUT,
                processingModule.getModuleConfigKey());
        ProcessingModuleItem.setSlotMode(processingStack, GridDirection.LEFT, SmeltingModuleItem.SlotMode.FUEL,
                processingModule.getModuleConfigKey());
        ProcessingModuleItem.setSlotMode(processingStack, GridDirection.RIGHT, SmeltingModuleItem.SlotMode.OUTPUT,
                processingModule.getModuleConfigKey());
        machine.getModuleHandler().setStackInSlot(processingSlot, processingStack);
        machine.getModuleHandler().setStackInSlot(inputSlot, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getModuleHandler().setStackInSlot(fuelSlot, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getModuleHandler().setStackInSlot(outputSlot, new ItemStack(ModItems.INVENTORY_MODULE.get()));
        machine.getInventoryHandler().setStackInSlot(inputSlot, new ItemStack(Items.IRON_ORE));
        machine.getInventoryHandler().setStackInSlot(fuelSlot, new ItemStack(Items.COAL));

        ItemStack ioStack = createConfiguredIoModule();
        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.INPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        BlockPos leverPos = machinePos.relative(machineFacing);
        helper.getLevel().setBlock(leverPos.below(), Blocks.STONE.defaultBlockState(), 3);
        var poweredLever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, true);
        helper.getLevel().setBlock(leverPos, poweredLever, 3);

        if (helper.getLevel().getSignal(leverPos, machineFacing.getOpposite()) <= 0) {
            helper.fail("The powered lever must provide a world signal to the machine");
            return;
        }

        helper.runAfterDelay(5, () -> {
            int poweredCookTime = machine.getModuleData(processingSlot, 2);
            int poweredBurnTime = machine.getModuleData(processingSlot, 0);
            if (poweredCookTime <= 0 || poweredBurnTime <= 0) {
                helper.fail("A powered HIGH-controlled smelting module must start cooking and burning fuel; cook="
                        + poweredCookTime + ", burn=" + poweredBurnTime);
                return;
            }

            helper.getLevel().setBlock(leverPos, poweredLever.setValue(LeverBlock.POWERED, false), 3);
            helper.runAfterDelay(5, () -> {
                if (machine.getModuleData(processingSlot, 2) != poweredCookTime
                        || machine.getModuleData(processingSlot, 0) != poweredBurnTime) {
                    helper.fail("HIGH mode must preserve cooking progress and burn time while its input is off");
                    return;
                }

                processingModule.setRedstoneControlMode(processingStack, BaseModuleItem.RedstoneControlMode.LOW);
                helper.runAfterDelay(5, () -> {
                    int lowModeCookTime = machine.getModuleData(processingSlot, 2);
                    int lowModeBurnTime = machine.getModuleData(processingSlot, 0);
                    if (lowModeCookTime <= poweredCookTime || lowModeBurnTime >= poweredBurnTime) {
                        helper.fail("LOW mode must resume smelting while its input is off");
                        return;
                    }

                    helper.getLevel().setBlock(leverPos, poweredLever, 3);
                    helper.runAfterDelay(5, () -> {
                        if (machine.getModuleData(processingSlot, 2) != lowModeCookTime
                                || machine.getModuleData(processingSlot, 0) != lowModeBurnTime) {
                            helper.fail("LOW mode must pause and preserve progress while its input is on");
                            return;
                        }
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void observerItemMapsToCircuitComponent(GameTestHelper helper) {
        if (RedstoneModuleItem.getComponentFromItem(new ItemStack(Items.OBSERVER)) != RedstoneComponent.OBSERVER) {
            helper.fail("The vanilla observer item must map to a redstone circuit component");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void configurableSignalSourceDrivesDustAndComparatorAndPersists(GameTestHelper helper) {
        ItemStack sourceItem = new ItemStack(ModItems.SIGNAL_SOURCE.get());
        if (RedstoneModuleItem.getComponentFromItem(sourceItem) != RedstoneComponent.SIGNAL_SOURCE) {
            helper.fail("The custom signal source item must map to its circuit component");
            return;
        }

        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(2, 3);
        int comparator = slot(4, 3);
        int directWire = slot(3, 3);
        int attenuatedWire = slot(3, 4);
        circuit.setComponent(source, RedstoneComponent.SIGNAL_SOURCE);
        circuit.setComponent(directWire, RedstoneComponent.WIRE);
        circuit.setComponent(attenuatedWire, RedstoneComponent.WIRE);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 1);

        if (circuit.getSlotData(source).signalSourceStrength != 0) {
            helper.fail("A newly placed signal source should start inactive at strength 0");
            return;
        }

        circuit.setSignalSourceStrength(source, 7);
        circuit.propagateSignals(null);
        assertSignal(helper, circuit, source, 7, "The source itself must report its configured strength");
        assertSignal(helper, circuit, directWire, 7, "Dust directly beside the source must receive full strength");
        assertSignal(helper, circuit, attenuatedWire, 6, "Dust-to-dust propagation must lose one strength");
        if (circuit.getInputStrength(comparator, 1) != 7) {
            helper.fail("A comparator facing the source's dust line must read its analog strength");
            return;
        }

        RedstoneCircuit restoredCircuit = RedstoneCircuit.deserializeNBT(circuit.serializeNBT());
        if (restoredCircuit.getSlotData(source).signalSourceStrength != 7) {
            helper.fail("Configured source strength must persist in circuit NBT");
            return;
        }
        RedstoneCircuitState restoredState = RedstoneCircuitState.deserializeNBT(
                new RedstoneCircuitState(restoredCircuit).serializeNBT());
        if (restoredState.getSignalSourceStrength(source) != 7) {
            helper.fail("Configured source strength must be included in the synchronized circuit state");
            return;
        }

        circuit.setSignalSourceStrength(source, 20);
        if (circuit.getSlotData(source).signalSourceStrength != 15) {
            helper.fail("Signal source strength must clamp to 15");
            return;
        }
        circuit.setSignalSourceStrength(source, -3);
        if (circuit.getSlotData(source).signalSourceStrength != 0) {
            helper.fail("Signal source strength must clamp to 0");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void circuitStateSerializationDoesNotExposeSnapshotArrays(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(4, 3);
        circuit.setComponent(source, RedstoneComponent.REDSTONE_BLOCK);
        circuit.propagateSignals(null);
        RedstoneCircuitState state = circuit.computeState();
        CompoundTag serializedState = state.serializeNBT();

        serializedState.getIntArray("ComponentIds")[source] = -1;
        serializedState.getIntArray("SignalStrengths")[source] = 0;
        if (state.getComponent(source) != RedstoneComponent.REDSTONE_BLOCK
                || state.getSignalStrength(source) != 15) {
            helper.fail("Mutating serialized NBT must not change the read-only circuit snapshot");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void observerDetectsWatchedSlotAndPulsesOppositeItsFacing(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int observer = slot(4, 3);
        int watchedLever = slot(4, 2);
        int sideLever = slot(5, 3);
        int outputWire = slot(4, 4);
        int lateralWire = slot(3, 3);
        circuit.setComponent(observer, RedstoneComponent.OBSERVER);
        circuit.setRotation(observer, 0);
        circuit.setComponent(watchedLever, RedstoneComponent.LEVER);
        circuit.setComponent(sideLever, RedstoneComponent.LEVER);
        circuit.setComponent(outputWire, RedstoneComponent.WIRE);
        circuit.setComponent(lateralWire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null);
        circuit.updateObserverInputs(0);
        circuit.setRotation(observer, 1);
        circuit.updateObserverInputs(0);
        if (circuit.getNextObserverTickTime() >= 0) {
            helper.fail("Rotating an observer must establish a new watched-slot baseline without pulsing");
            return;
        }
        circuit.setRotation(observer, 0);
        circuit.updateObserverInputs(0);
        circuit.toggleLever(sideLever);
        circuit.propagateSignals(null);
        circuit.updateObserverInputs(1);
        if (circuit.getNextObserverTickTime() >= 0) {
            helper.fail("Changing a side slot must not activate the observer");
            return;
        }

        circuit.toggleLever(watchedLever);
        circuit.propagateSignals(null);
        circuit.updateObserverInputs(2);
        if (circuit.getNextObserverTickTime() != 2 + RedstoneCircuit.OBSERVER_DELAY_GAME_TICKS) {
            helper.fail("A watched block-state change must schedule the observer's two-game-tick delay");
            return;
        }
        if (circuit.processObserverTicksAt(3)) {
            helper.fail("The observer must not pulse before its scheduled tick");
            return;
        }
        circuit.propagateSignals(null);
        assertSignal(helper, circuit, outputWire, 0, "Observer output must remain off during its delay");

        if (!circuit.processObserverTicksAt(4)) {
            helper.fail("The observer must begin its pulse at the scheduled tick");
            return;
        }
        circuit.propagateSignals(null);
        assertOutput(helper, circuit, observer, 15, "A triggered observer must emit full strength");
        assertSignal(helper, circuit, outputWire, 15, "The observer must power dust on its output side");
        assertSignal(helper, circuit, lateralWire, 0, "The observer must not power lateral dust");

        if (!circuit.processObserverTicksAt(6)) {
            helper.fail("The observer pulse must end after two game ticks");
            return;
        }
        circuit.propagateSignals(null);
        assertSignal(helper, circuit, outputWire, 0, "Observer output dust must turn off when the pulse ends");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void observerScheduledPulsePersistsAcrossSerialization(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int observer = slot(4, 3);
        int watchedLever = slot(4, 2);
        circuit.setComponent(observer, RedstoneComponent.OBSERVER);
        circuit.setRotation(observer, 0);
        circuit.setComponent(watchedLever, RedstoneComponent.LEVER);
        circuit.propagateSignals(null);
        circuit.updateObserverInputs(10);
        circuit.toggleLever(watchedLever);
        circuit.propagateSignals(null);
        circuit.updateObserverInputs(11);

        circuit = RedstoneCircuit.deserializeNBT(circuit.serializeNBT());
        if (circuit.getNextObserverTickTime() != 11 + RedstoneCircuit.OBSERVER_DELAY_GAME_TICKS) {
            helper.fail("A pending observer pulse must retain its due game time in NBT");
            return;
        }
        if (!circuit.processObserverTicksAt(13)) {
            helper.fail("A deserialized observer must process its pending pulse");
            return;
        }
        circuit.propagateSignals(null);
        if (!circuit.getSlotData(observer).observerPowered || circuit.getNextObserverTickTime() != 15) {
            helper.fail("Observer powered state and pulse-end time must persist");
            return;
        }
        RedstoneCircuit poweredCircuit = RedstoneCircuit.deserializeNBT(circuit.serializeNBT());
        if (!poweredCircuit.processObserverTicksAt(15)) {
            helper.fail("A deserialized active pulse must turn off at its scheduled time");
            return;
        }
        poweredCircuit.propagateSignals(null);
        assertOutput(helper, poweredCircuit, observer, 0, "The persisted observer pulse must finish normally");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void observerIgnoresRepeaterLockOnlyChanges(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int observer = slot(4, 3);
        int watchedRepeater = slot(4, 2);
        circuit.setComponent(observer, RedstoneComponent.OBSERVER);
        circuit.setRotation(observer, 0);
        circuit.setComponent(watchedRepeater, RedstoneComponent.REPEATER);
        circuit.propagateSignals(null);
        circuit.updateObserverInputs(0);

        circuit.getSlotData(watchedRepeater).repeaterLocked = true;
        circuit.updateObserverInputs(1);
        if (circuit.getNextObserverTickTime() >= 0) {
            helper.fail("A repeater lock-only change must not count as an observed block-state change");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void isolatedWireIsUnpowered(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int wire = slot(4, 3);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wire, 0, "An isolated dust wire must be unpowered");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void dustKeepsSourceStrengthAtFirstWire(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(3, 3);
        int firstWire = slot(4, 3);
        int secondWire = slot(5, 3);
        circuit.setComponent(source, RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(firstWire, RedstoneComponent.WIRE);
        circuit.setComponent(secondWire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, firstWire, 15, "Dust next to a redstone block must retain strength 15");
        assertSignal(helper, circuit, secondWire, 14, "Strength must decay once between dust wires");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void dustSignalDiesAfterFifteenDustBlocks(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(0, 0), RedstoneComponent.REDSTONE_BLOCK);
        for (int x = 1; x <= 8; x++) {
            circuit.setComponent(slot(x, 0), RedstoneComponent.WIRE);
        }
        circuit.setComponent(slot(8, 1), RedstoneComponent.WIRE);
        for (int x = 2; x <= 8; x++) {
            circuit.setComponent(slot(x, 2), RedstoneComponent.WIRE);
        }

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, slot(3, 2), 1, "Dust at the fifteenth wire step must have strength 1");
        assertSignal(helper, circuit, slot(2, 2), 0, "Dust at the sixteenth wire step must be unpowered");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void leverPowersAdjacentDustAtFullStrength(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int lever = slot(3, 3);
        int wire = slot(4, 3);
        circuit.setComponent(lever, RedstoneComponent.LEVER);
        circuit.setLeverState(lever, true);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wire, 15, "Dust next to an active lever must have strength 15");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void stoneButtonPulsesForTwentyGameTicks(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int button = slot(3, 3);
        int wire = slot(4, 3);
        circuit.setComponent(button, RedstoneComponent.BUTTON);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        if (!circuit.pressButton(button, 100)) {
            helper.fail("A released button must accept a press");
            return;
        }
        if (circuit.getNextButtonTickTime() != 100 + RedstoneCircuit.STONE_BUTTON_DURATION_GAME_TICKS) {
            helper.fail("A stone button must schedule release after 20 game ticks");
            return;
        }
        if (circuit.pressButton(button, 110) || circuit.getNextButtonTickTime() != 120) {
            helper.fail("Pressing an already-held button must not extend its pulse");
            return;
        }

        circuit.propagateSignals(null);
        assertSignal(helper, circuit, wire, 15, "A pressed button must power adjacent dust at strength 15");
        if (circuit.processButtonTicksAt(119)) {
            helper.fail("A button must not release before its scheduled game tick");
            return;
        }
        circuit.propagateSignals(null);
        assertSignal(helper, circuit, wire, 15, "Button power must remain active until its release tick");

        if (!circuit.processButtonTicksAt(120)) {
            helper.fail("A button must release when its scheduled game tick is processed");
            return;
        }
        circuit.propagateSignals(null);
        assertSignal(helper, circuit, wire, 0, "Adjacent dust must turn off when the button pulse expires");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void woodenButtonsUseThirtyGameTickPulses(GameTestHelper helper) {
        ItemStack stoneButton = new ItemStack(Items.STONE_BUTTON);
        ItemStack woodenButton = new ItemStack(Items.OAK_BUTTON);
        if (RedstoneModuleItem.getComponentFromItem(stoneButton) != RedstoneComponent.BUTTON ||
            RedstoneModuleItem.getComponentFromItem(woodenButton) != RedstoneComponent.BUTTON) {
            helper.fail("Stone and wooden vanilla buttons must both map to the button component");
            return;
        }
        if (RedstoneModuleItem.getButtonPulseDurationGameTicks(stoneButton) !=
                RedstoneCircuit.STONE_BUTTON_DURATION_GAME_TICKS ||
            RedstoneModuleItem.getButtonPulseDurationGameTicks(woodenButton) !=
                RedstoneCircuit.WOODEN_BUTTON_DURATION_GAME_TICKS) {
            helper.fail("Stone and wooden buttons must use their distinct vanilla pulse durations");
            return;
        }

        RedstoneCircuit circuit = new RedstoneCircuit();
        int button = slot(3, 3);
        circuit.setComponent(button, RedstoneComponent.BUTTON);
        circuit.getSlotData(button).buttonDurationGameTicks = RedstoneModuleItem.getButtonPulseDurationGameTicks(woodenButton);
        circuit.pressButton(button, 50);
        if (circuit.getNextButtonTickTime() != 50 + RedstoneCircuit.WOODEN_BUTTON_DURATION_GAME_TICKS) {
            helper.fail("A wooden button must stay pressed for 30 game ticks");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void pressedButtonStronglyPowersItsAttachmentWall(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int button = slot(3, 2);
        int wall = slot(3, 3);
        int wire = slot(3, 4);
        circuit.setComponent(button, RedstoneComponent.BUTTON);
        circuit.setComponent(wall, RedstoneComponent.WALL);
        circuit.setComponent(wire, RedstoneComponent.WIRE);
        circuit.getSlotData(button).buttonRotation = 2;
        circuit.pressButton(button, 100);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wall, 15, "A pressed button must strongly power its selected support wall");
        assertSignal(helper, circuit, wire, 15, "A strongly powered button support wall must power adjacent dust");
        circuit.processButtonTicksAt(120);
        circuit.propagateSignals(null);
        assertSignal(helper, circuit, wall, 0, "The support wall must turn off when the button pulse expires");
        assertSignal(helper, circuit, wire, 0, "Dust powered through the support wall must turn off after release");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void pressedButtonStronglyPowersItsBorderWall(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int button = slot(0, 3);
        circuit.setComponent(button, RedstoneComponent.BUTTON);
        circuit.cycleButtonAttachment(button);
        if (circuit.getSlotData(button).buttonRotation != 3) {
            helper.fail("A button at the west edge should be able to attach to the border wall");
            return;
        }

        circuit.pressButton(button, 100);
        circuit.propagateSignals(null);
        if (circuit.getBoundaryWallOutput(Direction.WEST, 3) != 15) {
            helper.fail("A pressed button attached to the border must strongly power its shared wall");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void buttonAttachmentCyclesOnlyThroughValidWallsAndPerchedState(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int button = slot(4, 3);
        int northWall = slot(4, 2);
        circuit.setComponent(button, RedstoneComponent.BUTTON);
        circuit.setComponent(northWall, RedstoneComponent.WALL);

        circuit.cycleButtonAttachment(button);
        if (circuit.getSlotData(button).buttonRotation != 0) {
            helper.fail("A button should cycle from floor-perched to its only adjacent wall");
            return;
        }
        circuit.cycleButtonAttachment(button);
        if (circuit.getSlotData(button).buttonRotation != 4) {
            helper.fail("A button should cycle from the wall back to floor-perched");
            return;
        }

        circuit.setComponent(northWall, null);
        circuit.cycleButtonAttachment(button);
        if (circuit.getSlotData(button).buttonRotation != 4) {
            helper.fail("A button with no adjacent wall must remain floor-perched");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void buttonPulseStatePersistsThroughCircuitNbt(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int button = slot(3, 3);
        circuit.setComponent(button, RedstoneComponent.BUTTON);
        circuit.getSlotData(button).buttonDurationGameTicks = RedstoneCircuit.WOODEN_BUTTON_DURATION_GAME_TICKS;
        circuit.getSlotData(button).buttonRotation = 2;
        circuit.pressButton(button, 50);

        RedstoneCircuit restored = RedstoneCircuit.deserializeNBT(circuit.serializeNBT());
        RedstoneSlotData restoredButton = restored.getSlotData(button);
        if (restoredButton == null || !restoredButton.buttonPowered ||
            restoredButton.buttonDurationGameTicks != RedstoneCircuit.WOODEN_BUTTON_DURATION_GAME_TICKS ||
            restoredButton.buttonRotation != 2 ||
            restored.getNextButtonTickTime() != 80) {
            helper.fail("Pressed state, wooden duration, attachment, and pending release must survive NBT round-trip");
            return;
        }

        RedstoneCircuitState syncedState = RedstoneCircuitState.deserializeNBT(restored.computeState().serializeNBT());
        if (syncedState.getButtonRotation(button) != 2) {
            helper.fail("Button attachment must survive circuit-state synchronization serialization");
            return;
        }

        if (!restored.processButtonTicksAt(80)) {
            helper.fail("The restored button must release at its persisted due time");
            return;
        }
        restored.propagateSignals(null);
        assertSignal(helper, restored, button, 0, "The restored button must be OFF after its scheduled release");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void isolatedWireDefaultsToCrossAndSyncsItsMask(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int wire = slot(4, 3);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        RedstoneCircuitState state = RedstoneCircuitState.deserializeNBT(circuit.computeState().serializeNBT());
        for (Direction direction : horizontalDirections()) {
            assertWireConnected(helper, circuit, wire, direction, true,
                    "An isolated wire defaults to a four-way cross");
            if (!state.isWireConnected(wire, direction)) {
                helper.fail("The wire connection mask must be included in the client state");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void wireWithOneNeighborExtendsIntoAStraightLine(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int wire = slot(4, 3);
        circuit.setComponent(slot(4, 2), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        assertWireConnected(helper, circuit, wire, Direction.NORTH, true,
                "A wire connects to its adjacent power component");
        assertWireConnected(helper, circuit, wire, Direction.SOUTH, true,
                "A single valid neighbor makes the wire extend in the opposite direction");
        assertWireConnected(helper, circuit, wire, Direction.EAST, false,
                "A single valid neighbor does not create a side arm");
        assertWireConnected(helper, circuit, wire, Direction.WEST, false,
                "A single valid neighbor does not create a side arm");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void wireConnectsToRepeaterAxisAndComparatorSides(GameTestHelper helper) {
        RedstoneCircuit repeaterCircuit = new RedstoneCircuit();
        int repeaterWire = slot(4, 3);
        repeaterCircuit.setComponent(repeaterWire, RedstoneComponent.WIRE);
        repeaterCircuit.setComponent(slot(3, 3), RedstoneComponent.LEVER);
        int repeater = slot(5, 3);
        repeaterCircuit.setComponent(repeater, RedstoneComponent.REPEATER);
        repeaterCircuit.setRotation(repeater, 1);
        assertWireConnected(helper, repeaterCircuit, repeaterWire, Direction.WEST, true,
                "Wire connects to its adjacent source");
        assertWireConnected(helper, repeaterCircuit, repeaterWire, Direction.EAST, true,
                "Wire connects to a repeater on its input/output axis");
        assertWireConnected(helper, repeaterCircuit, repeaterWire, Direction.NORTH, false,
                "A repeater does not connect to dust on its side");

        RedstoneCircuit sideRepeaterCircuit = new RedstoneCircuit();
        int sideRepeaterWire = slot(4, 3);
        sideRepeaterCircuit.setComponent(sideRepeaterWire, RedstoneComponent.WIRE);
        sideRepeaterCircuit.setComponent(slot(3, 3), RedstoneComponent.LEVER);
        int sideRepeater = slot(4, 2);
        sideRepeaterCircuit.setComponent(sideRepeater, RedstoneComponent.REPEATER);
        sideRepeaterCircuit.setRotation(sideRepeater, 1);
        assertWireConnected(helper, sideRepeaterCircuit, sideRepeaterWire, Direction.NORTH, false,
                "A wire does not connect to a repeater's side");

        RedstoneCircuit comparatorCircuit = new RedstoneCircuit();
        int comparatorWire = slot(4, 3);
        comparatorCircuit.setComponent(comparatorWire, RedstoneComponent.WIRE);
        comparatorCircuit.setComponent(slot(3, 3), RedstoneComponent.LEVER);
        int comparator = slot(4, 2);
        comparatorCircuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        comparatorCircuit.setRotation(comparator, 1);
        assertWireConnected(helper, comparatorCircuit, comparatorWire, Direction.NORTH, true,
                "A comparator accepts a dust side input");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void wireOnlyPowersSideWallWhenItsShapePointsThere(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int wire = slot(4, 3);
        circuit.setComponent(slot(3, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(wire, RedstoneComponent.WIRE);
        circuit.setComponent(slot(5, 3), RedstoneComponent.WIRE);
        int sideWall = slot(4, 2);
        circuit.setComponent(sideWall, RedstoneComponent.WALL);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wire, 15, "The source powers the first wire at full strength");
        assertSignal(helper, circuit, sideWall, 0,
                "A wire running east-west does not power an unconnected wall to its north");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void wirePowersWallInItsSingleConnectionLine(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int wire = slot(4, 3);
        circuit.setComponent(slot(3, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(wire, RedstoneComponent.WIRE);
        int wall = slot(5, 3);
        circuit.setComponent(wall, RedstoneComponent.WALL);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wall, 15,
                "A lone source connection makes the wire line extend into and weakly power the wall");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterBoostsAndOnlyPowersItsFacingSide(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(1, 3);
        int inputWire = slot(2, 3);
        int repeater = slot(3, 3);
        int outputWire = slot(4, 3);
        int sideWire = slot(3, 2);
        circuit.setComponent(source, RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(inputWire, RedstoneComponent.WIRE);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);
        circuit.setComponent(outputWire, RedstoneComponent.WIRE);
        circuit.setComponent(sideWire, RedstoneComponent.WIRE);

        for (long gameTime = 0; gameTime <= 2; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }

        assertSignal(helper, circuit, outputWire, 15, "A powered repeater must output strength 15 forward");
        assertSignal(helper, circuit, sideWire, 0, "A repeater must not power a side wire");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void directionalComponentsPublishOutputsOnTheirFacingCircuitEdge(GameTestHelper helper) {
        for (Direction direction : horizontalDirections()) {
            int edgeSlot = edgeSlot(direction);
            int rearSlot = adjacentSlot(edgeSlot, direction.getOpposite());
            int outputChannel = RedstoneCircuit.getBoundarySideIndex(direction);

            RedstoneCircuit repeaterCircuit = new RedstoneCircuit();
            repeaterCircuit.setComponent(rearSlot, RedstoneComponent.REDSTONE_BLOCK);
            repeaterCircuit.setComponent(edgeSlot, RedstoneComponent.REPEATER);
            repeaterCircuit.setRotation(edgeSlot, rotationFor(direction));
            for (long gameTime = 0; gameTime <= 2; gameTime++) {
                evaluateCircuitAt(repeaterCircuit, gameTime);
            }
            if (repeaterCircuit.propagateSignals(null)[outputChannel] != 15) {
                helper.fail("An outward-facing " + direction + " repeater must publish its latched output on that edge");
                return;
            }

            RedstoneCircuit comparatorCircuit = new RedstoneCircuit();
            comparatorCircuit.setComponent(rearSlot, RedstoneComponent.REDSTONE_BLOCK);
            comparatorCircuit.setComponent(edgeSlot, RedstoneComponent.COMPARATOR);
            comparatorCircuit.setRotation(edgeSlot, rotationFor(direction));
            for (long gameTime = 0; gameTime <= 2; gameTime++) {
                evaluateCircuitAt(comparatorCircuit, gameTime);
            }
            if (comparatorCircuit.propagateSignals(null)[outputChannel] != 15) {
                helper.fail("An outward-facing " + direction + " comparator must publish its output on that edge");
                return;
            }
        }

        RedstoneCircuit inwardRepeaterCircuit = new RedstoneCircuit();
        int eastEdgeRepeater = edgeSlot(Direction.EAST);
        inwardRepeaterCircuit.setComponent(eastEdgeRepeater, RedstoneComponent.REPEATER);
        inwardRepeaterCircuit.setRotation(eastEdgeRepeater, rotationFor(Direction.WEST));
        for (long gameTime = 0; gameTime <= 2; gameTime++) {
            evaluateCircuitAt(inwardRepeaterCircuit, gameTime, new int[]{0, 0, 0, 15});
        }
        if (inwardRepeaterCircuit.propagateSignals(new int[]{0, 0, 0, 15})[
                RedstoneCircuit.getBoundarySideIndex(Direction.EAST)] != 0) {
            helper.fail("An inward-facing repeater must not publish its output on the east edge");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterDoesNotOutputBeforeMinimumDelay(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int repeater = slot(3, 3);
        circuit.setComponent(slot(2, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);

        evaluateCircuitAt(circuit, 0);
        assertOutput(helper, circuit, repeater, 0,
                "A repeater must not output during the first game tick of its minimum delay");
        evaluateCircuitAt(circuit, 1);
        assertOutput(helper, circuit, repeater, 0,
                "A newly powered repeater must remain off until two game ticks have elapsed");
        evaluateCircuitAt(circuit, 2);

        assertOutput(helper, circuit, repeater, 15, "A minimum-delay repeater must turn on after two game ticks");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterDelayCyclesAndSynchronizes(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int repeater = slot(3, 3);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);

        int[] expectedDelays = {2, 3, 4, 1};
        for (int expectedDelay : expectedDelays) {
            circuit.cycleRepeaterDelay(repeater);
            if (circuit.getSlotData(repeater).repeaterDelay != expectedDelay ||
                circuit.computeState().getRepeaterDelay(repeater) != expectedDelay) {
                helper.fail("Repeater delay should cycle and sync to " + expectedDelay);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterUsesConfiguredDelayForTurningOnAndOff(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(2, 3);
        int repeater = slot(3, 3);
        circuit.setComponent(source, RedstoneComponent.LEVER);
        circuit.setLeverState(source, true);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);
        circuit.getSlotData(repeater).repeaterDelay = 2;

        for (long gameTime = 0; gameTime <= 3; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
            assertOutput(helper, circuit, repeater, 0, "A two-tick setting must wait four game ticks before turning on");
        }
        evaluateCircuitAt(circuit, 4);
        assertOutput(helper, circuit, repeater, 15, "A two-tick repeater must turn on after four game ticks");

        circuit.setLeverState(source, false);
        for (long gameTime = 5; gameTime <= 8; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
            assertOutput(helper, circuit, repeater, 15, "A two-tick repeater must delay turning off by four game ticks");
        }
        evaluateCircuitAt(circuit, 9);
        assertOutput(helper, circuit, repeater, 0, "A two-tick repeater must turn off after four game ticks");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void twoRepeaterLoopContinuesClockingAfterStartupPulse(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int firstRepeater = slot(2, 1);
        int secondRepeater = slot(5, 4);
        circuit.setComponent(firstRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(firstRepeater, 1);
        circuit.getSlotData(firstRepeater).repeaterDelay = 1;
        circuit.setComponent(secondRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(secondRepeater, 2);
        circuit.getSlotData(secondRepeater).repeaterDelay = 1;

        int[][] wires = {
                {3, 1}, {4, 1}, {5, 1}, {5, 2}, {5, 3},
                {5, 5}, {4, 5}, {3, 5}, {2, 5}, {1, 5}, {0, 5},
                {0, 4}, {0, 3}, {0, 2}, {0, 1}, {1, 1}
        };
        for (int[] wire : wires) {
            circuit.setComponent(slot(wire[0], wire[1]), RedstoneComponent.WIRE);
        }

        int startupLever = slot(1, 0);
        circuit.setComponent(startupLever, RedstoneComponent.LEVER);
        circuit.setLeverState(startupLever, true);

        StringBuilder timeline = new StringBuilder();
        boolean previousFirst = false;
        boolean previousSecond = false;
        int transitions = 0;
        for (long gameTime = 0; gameTime <= 16; gameTime++) {
            if (gameTime == 1) circuit.setLeverState(startupLever, false);
            evaluateCircuitAt(circuit, gameTime);

            boolean firstPowered = circuit.getSlotData(firstRepeater).repeaterPowered;
            boolean secondPowered = circuit.getSlotData(secondRepeater).repeaterPowered;
            if (firstPowered != previousFirst || secondPowered != previousSecond) transitions++;
            timeline.append(firstPowered ? '1' : '0').append(secondPowered ? '1' : '0').append(' ');
            previousFirst = firstPowered;
            previousSecond = secondPowered;
        }

        String expectedTimeline = "00 00 10 10 01 01 10 10 01 01 10 10 01 01 10 10 01 ";
        if (transitions < 6 || !expectedTimeline.contentEquals(timeline)) {
            helper.fail("A pulsed two-repeater loop should alternate every two game ticks; transitions=" + transitions +
                    ", timeline=" + timeline);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void machineScheduledTickReleasesButtonPulse(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }
        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        int button = slot(RedstoneCircuit.GRID_WIDTH - 1, 3);
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(button, RedstoneComponent.BUTTON);
        circuit.pressButton(button, helper.getLevel().getGameTime());
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));

        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        assertModuleOutput(helper, machine, 0, 3, 15, "A pressed edge button must publish its output immediately");
        helper.runAfterDelay(RedstoneCircuit.STONE_BUTTON_DURATION_GAME_TICKS - 1, () -> {
            assertModuleOutput(helper, machine, 0, 3, 15, "The machine must keep a stone button active before expiry");
            helper.runAfterDelay(1, () -> {
                assertModuleOutput(helper, machine, 0, 3, 0,
                        "The scheduled machine block tick must release the button at 20 game ticks");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void machineScheduledTickEmitsObserverPulseOnItsOutputEdge(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }
        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        int observer = slot(4, RedstoneCircuit.GRID_HEIGHT - 1);
        int watchedLever = slot(4, RedstoneCircuit.GRID_HEIGHT - 2);
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(observer, RedstoneComponent.OBSERVER);
        circuit.setRotation(observer, 0);
        circuit.setComponent(watchedLever, RedstoneComponent.LEVER);
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));

        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        ItemStack moduleStack = machine.getModuleHandler().getStackInSlot(0);
        circuit = RedstoneCircuit.fromStack(moduleStack, helper.getLevel().registryAccess());
        circuit.toggleLever(watchedLever);
        circuit.toStack(moduleStack);
        machine.getModuleHandler().setStackInSlot(0, moduleStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        assertModuleOutput(helper, machine, 0, 1, 0, "Observer output must wait for its scheduled delay");

        helper.runAfterDelay(RedstoneCircuit.OBSERVER_DELAY_GAME_TICKS + 1, () -> {
            assertModuleOutput(helper, machine, 0, 1, 15,
                    "The scheduled machine tick must expose the observer pulse on the matching edge");
            helper.runAfterDelay(RedstoneCircuit.OBSERVER_PULSE_GAME_TICKS + 1, () -> {
                assertModuleOutput(helper, machine, 0, 1, 0,
                        "The observer edge output must turn off when its two-tick pulse ends");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void machineTickKeepsMinimumDelayTwoRepeaterClockAlternating(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }
        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        int firstRepeater = slot(2, 1);
        int secondRepeater = slot(5, 4);
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(firstRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(firstRepeater, 1);
        circuit.getSlotData(firstRepeater).repeaterDelay = 1;
        circuit.setComponent(secondRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(secondRepeater, 2);
        circuit.getSlotData(secondRepeater).repeaterDelay = 1;

        int[][] wires = {
                {3, 1}, {4, 1}, {5, 1}, {5, 2}, {5, 3},
                {5, 5}, {4, 5}, {3, 5}, {2, 5}, {1, 5}, {0, 5},
                {0, 4}, {0, 3}, {0, 2}, {0, 1}, {1, 1}
        };
        for (int[] wire : wires) {
            circuit.setComponent(slot(wire[0], wire[1]), RedstoneComponent.WIRE);
        }

        int startupLever = slot(1, 0);
        circuit.setComponent(startupLever, RedstoneComponent.LEVER);
        circuit.setLeverState(startupLever, true);
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));

        RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);
        ItemStack moduleStack = machine.getModuleHandler().getStackInSlot(0);
        RedstoneCircuit runningCircuit = RedstoneCircuit.fromStack(moduleStack, helper.getLevel().registryAccess());
        runningCircuit.setLeverState(startupLever, false);
        runningCircuit.toStack(moduleStack);
        machine.getModuleHandler().setStackInSlot(0, moduleStack);

        StringBuilder timeline = new StringBuilder();
        appendRepeaterClockState(timeline, runningCircuit, firstRepeater, secondRepeater);
        int[] elapsedTicks = {0};
        Runnable[] nextTick = new Runnable[1];
        nextTick[0] = () -> helper.runAfterDelay(1, () -> {
            elapsedTicks[0]++;
            RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);
            ItemStack updatedStack = machine.getModuleHandler().getStackInSlot(0);
            RedstoneCircuit updatedCircuit = RedstoneCircuit.fromStack(
                    updatedStack, helper.getLevel().registryAccess());
            appendRepeaterClockState(timeline, updatedCircuit, firstRepeater, secondRepeater);

            if (elapsedTicks[0] == 12) {
                String expectedTimeline = "00 00 10 10 01 01 10 10 01 01 10 10 01 ";
                if (!expectedTimeline.contentEquals(timeline)) {
                    helper.fail("Machine ticks must preserve the minimum-delay repeater clock; timeline=" + timeline);
                    return;
                }
                helper.succeed();
                return;
            }
            nextTick[0].run();
        });
        nextTick[0].run();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterClockPulsesMachineOutputWhileLampOffDelayKeepsItLit(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }
        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        BlockPos lampPos = machinePos.relative(machineFacing);
        helper.getLevel().setBlock(lampPos, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);

        int firstRepeater = slot(2, 1);
        int secondRepeater = slot(5, 4);
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(firstRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(firstRepeater, 1);
        circuit.getSlotData(firstRepeater).repeaterDelay = 1;
        circuit.setComponent(secondRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(secondRepeater, 2);
        circuit.getSlotData(secondRepeater).repeaterDelay = 1;
        int[][] wires = {
                {3, 1}, {4, 1}, {5, 1}, {5, 2}, {5, 3},
                {5, 5}, {4, 5}, {3, 5}, {2, 5}, {1, 5}, {0, 5},
                {0, 4}, {0, 3}, {0, 2}, {0, 1}, {1, 1}
        };
        for (int[] wire : wires) {
            circuit.setComponent(slot(wire[0], wire[1]), RedstoneComponent.WIRE);
        }
        int startupLever = slot(1, 0);
        circuit.setComponent(startupLever, RedstoneComponent.LEVER);
        circuit.setLeverState(startupLever, true);
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));

        ItemStack ioStack = createConfiguredIoModule();
        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);

        ItemStack moduleStack = machine.getModuleHandler().getStackInSlot(0);
        RedstoneCircuit runningCircuit = RedstoneCircuit.fromStack(moduleStack, helper.getLevel().registryAccess());
        runningCircuit.setLeverState(startupLever, false);
        runningCircuit.toStack(moduleStack);
        machine.getModuleHandler().setStackInSlot(0, moduleStack);

        int[] elapsedTicks = {0};
        int[] signalTransitions = {0};
        int[] previousSignal = {helper.getLevel().getSignal(machinePos, machineFacing.getOpposite())};
        boolean[] sawPoweredSignal = {previousSignal[0] > 0};
        boolean[] sawUnpoweredSignal = {previousSignal[0] == 0};
        Runnable[] nextTick = new Runnable[1];
        nextTick[0] = () -> helper.runAfterDelay(1, () -> {
            elapsedTicks[0]++;
            RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);

            int signal = helper.getLevel().getSignal(machinePos, machineFacing.getOpposite());
            if (signal != previousSignal[0]) signalTransitions[0]++;
            previousSignal[0] = signal;
            sawPoweredSignal[0] |= signal > 0;
            sawUnpoweredSignal[0] |= signal == 0;

            if (elapsedTicks[0] == 12) {
                if (signalTransitions[0] < 4 || !sawPoweredSignal[0] || !sawUnpoweredSignal[0]) {
                    helper.fail("The configured machine output must pulse with the repeater clock; transitions=" +
                            signalTransitions[0] + ", sawOn=" + sawPoweredSignal[0] + ", sawOff=" +
                            sawUnpoweredSignal[0]);
                    return;
                }
                if (!helper.getLevel().getBlockState(lampPos).getValue(RedstoneLampBlock.LIT)) {
                    helper.fail("A lamp should remain lit when the clock's off phases are shorter than its four-tick deactivation delay");
                    return;
                }
                helper.succeed();
                return;
            }
            nextTick[0].run();
        });
        nextTick[0].run();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void machineTwoTickRepeaterClockMatchesVanillaOutputTicks(GameTestHelper helper) {
        int repeaterDelay = 2;
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        var machineState = ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState()
                .setValue(MachineFrameworkBlock.FACING, Direction.NORTH);
        helper.getLevel().setBlock(machinePos, machineState, 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        int firstRepeater = slot(2, 1);
        int secondRepeater = slot(5, 4);
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(firstRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(firstRepeater, 1);
        circuit.getSlotData(firstRepeater).repeaterDelay = repeaterDelay;
        circuit.setComponent(secondRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(secondRepeater, 2);
        circuit.getSlotData(secondRepeater).repeaterDelay = repeaterDelay;
        int[][] moduleWires = {
                {3, 1}, {4, 1}, {5, 1}, {5, 2}, {5, 3},
                {5, 5}, {4, 5}, {3, 5}, {2, 5}, {1, 5}, {0, 5},
                {0, 4}, {0, 3}, {0, 2}, {0, 1}, {1, 1}
        };
        for (int[] wire : moduleWires) {
            circuit.setComponent(slot(wire[0], wire[1]), RedstoneComponent.WIRE);
        }
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));

        ItemStack ioStack = createConfiguredIoModule();
        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        IoModuleItem.setFaceMode(ioStack, RelativeSide.RIGHT, IoModuleItem.FaceMode.INPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        BlockPos machineStartupSource = machinePos.west();

        BlockPos vanillaRepeaterA = helper.absolutePos(new BlockPos(4, 1, 2));
        BlockPos vanillaRepeaterB = helper.absolutePos(new BlockPos(6, 1, 4));
        BlockPos vanillaOutputWire = helper.absolutePos(new BlockPos(3, 1, 5));
        BlockPos vanillaStartupSource = helper.absolutePos(new BlockPos(2, 1, 2));
        int[][] vanillaWires = {
                {5, 2}, {6, 2}, {6, 3}, {6, 5}, {5, 5},
                {4, 5}, {3, 5}, {3, 4}, {3, 3}, {3, 2}
        };

        for (int x = 2; x <= 6; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 2; z <= 7; z++) {
                    helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)),
                            Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int[] wire : vanillaWires) {
            helper.getLevel().setBlock(helper.absolutePos(new BlockPos(wire[0], 0, wire[1])),
                    Blocks.STONE.defaultBlockState(), 3);
        }
        helper.getLevel().setBlock(vanillaRepeaterA.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(vanillaRepeaterB.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(vanillaStartupSource.below(), Blocks.STONE.defaultBlockState(), 3);

        helper.getLevel().setBlock(vanillaRepeaterA,
                Blocks.REPEATER.defaultBlockState()
                        .setValue(RepeaterBlock.FACING, Direction.EAST)
                        .setValue(RepeaterBlock.DELAY, repeaterDelay), 3);
        helper.getLevel().setBlock(vanillaRepeaterB,
                Blocks.REPEATER.defaultBlockState()
                        .setValue(RepeaterBlock.FACING, Direction.SOUTH)
                        .setValue(RepeaterBlock.DELAY, repeaterDelay), 3);
        for (int[] wire : vanillaWires) {
            helper.getLevel().setBlock(helper.absolutePos(new BlockPos(wire[0], 1, wire[1])),
                    Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
        }
        helper.getLevel().setBlock(vanillaStartupSource, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.getLevel().setBlock(machineStartupSource, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);

        StringBuilder machineSignalTimeline = new StringBuilder();
        StringBuilder vanillaSignalTimeline = new StringBuilder();
        int[] elapsedTicks = {0};
        Runnable[] nextTick = new Runnable[1];
        nextTick[0] = () -> helper.runAfterDelay(1, () -> {
            elapsedTicks[0]++;
            if (elapsedTicks[0] == 1) {
                helper.getLevel().setBlock(vanillaStartupSource, Blocks.AIR.defaultBlockState(), 3);
                helper.getLevel().setBlock(machineStartupSource, Blocks.AIR.defaultBlockState(), 3);
            }

            machineSignalTimeline.append(helper.getLevel().getSignal(machinePos, Direction.SOUTH) > 0
                    ? '1' : '0');
            vanillaSignalTimeline.append(getDustPower(helper, vanillaOutputWire) > 0 ? '1' : '0');

            if (elapsedTicks[0] == 40) {
                if (!hasBothSignalStates(machineSignalTimeline) || !hasBothSignalStates(vanillaSignalTimeline)) {
                    helper.fail("Both two-tick repeater clocks must produce ON and OFF output; machine=" +
                            machineSignalTimeline + ", vanilla=" + vanillaSignalTimeline);
                    return;
                }
                if (!machineSignalTimeline.toString().contentEquals(vanillaSignalTimeline)) {
                    helper.fail("Machine and vanilla clock outputs must switch on the same game ticks; machine=" +
                            machineSignalTimeline + ", vanilla=" + vanillaSignalTimeline);
                    return;
                }
                helper.succeed();
                return;
            }
            nextTick[0].run();
        });
        nextTick[0].run();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void adjacentMachineOutputFeedsConfiguredInputFace(GameTestHelper helper) {
        BlockPos sourcePos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos receiverPos = sourcePos.north();
        var northFacingState = ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState()
                .setValue(MachineFrameworkBlock.FACING, Direction.NORTH);
        helper.getLevel().setBlock(sourcePos, northFacingState, 3);
        helper.getLevel().setBlock(receiverPos, northFacingState, 3);
        if (!(helper.getLevel().getBlockEntity(sourcePos) instanceof MachineFrameworkBlockEntity sourceMachine) ||
            !(helper.getLevel().getBlockEntity(receiverPos) instanceof MachineFrameworkBlockEntity receiverMachine)) {
            helper.fail("Both adjacent machines must create their block entities");
            return;
        }

        RedstoneCircuit sourceCircuit = new RedstoneCircuit();
        sourceCircuit.setComponent(slot(4, 0), RedstoneComponent.REDSTONE_BLOCK);
        sourceMachine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), sourceCircuit));
        ItemStack sourceIo = createConfiguredIoModule();
        IoModuleItem.setFaceMode(sourceIo, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        sourceMachine.getModuleHandler().setStackInSlot(1, sourceIo);

        RedstoneCircuit receiverCircuit = new RedstoneCircuit();
        int borderTorch = slot(4, 0);
        receiverCircuit.setComponent(borderTorch, RedstoneComponent.TORCH);
        receiverCircuit.getSlotData(borderTorch).torchRotation = 0;
        receiverMachine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), receiverCircuit));
        ItemStack receiverIo = createConfiguredIoModule();
        IoModuleItem.setFaceMode(receiverIo, RelativeSide.BACK, IoModuleItem.FaceMode.INPUT);
        receiverMachine.getModuleHandler().setStackInSlot(1, receiverIo);

        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), sourcePos, sourceMachine);
        if (sourceMachine.getRedstoneOutputSignal(Direction.NORTH) != 15) {
            helper.fail("The source machine must emit its configured front output toward the receiver");
            return;
        }
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), receiverPos, receiverMachine);
        helper.runAfterDelay(RedstoneCircuit.TORCH_DELAY_GAME_TICKS + 1, () -> {
            if (receiverMachine.getModuleData(0, 0) != 0) {
                helper.fail("The receiver's configured BACK input must turn off its north-border torch; got " +
                        receiverMachine.getModuleData(0, 0));
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void externalLeverStartsMinimumDelayRepeaterClock(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }
        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        int firstRepeater = slot(2, 1);
        int secondRepeater = slot(5, 3);
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(firstRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(firstRepeater, 1);
        circuit.getSlotData(firstRepeater).repeaterDelay = 1;
        circuit.setComponent(secondRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(secondRepeater, 2);
        circuit.getSlotData(secondRepeater).repeaterDelay = 1;

        int[][] wires = {
                {1, 1}, {1, 2}, {1, 3}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4},
                {5, 2}, {5, 1}, {4, 1}, {3, 1}
        };
        for (int[] wire : wires) {
            circuit.setComponent(slot(wire[0], wire[1]), RedstoneComponent.WIRE);
        }
        int externalInputWire = slot(3, 0);
        circuit.setComponent(externalInputWire, RedstoneComponent.WIRE);
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));

        ItemStack ioStack = createConfiguredIoModule();
        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.INPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        BlockPos leverPos = machinePos.relative(machineFacing);
        helper.getLevel().setBlock(leverPos.below(), Blocks.STONE.defaultBlockState(), 3);
        var poweredLever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, true);
        helper.getLevel().setBlock(leverPos, poweredLever, 3);
        if (helper.getLevel().getSignal(leverPos, machineFacing.getOpposite()) <= 0) {
            helper.fail("The powered external lever must emit a signal toward the machine");
            return;
        }

        RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);
        StringBuilder timeline = new StringBuilder();
        ItemStack initialStack = machine.getModuleHandler().getStackInSlot(0);
        RedstoneCircuit initialCircuit = RedstoneCircuit.fromStack(initialStack, helper.getLevel().registryAccess());
        appendRepeaterClockState(timeline, initialCircuit, firstRepeater, secondRepeater);

        int[] elapsedTicks = {0};
        int[] transitions = {0};
        boolean[] previousFirst = {initialCircuit.getSlotData(firstRepeater).repeaterPowered};
        boolean[] previousSecond = {initialCircuit.getSlotData(secondRepeater).repeaterPowered};
        Runnable[] nextTick = new Runnable[1];
        nextTick[0] = () -> helper.runAfterDelay(1, () -> {
            elapsedTicks[0]++;
            if (elapsedTicks[0] == 1) {
                helper.getLevel().setBlock(leverPos, poweredLever.setValue(LeverBlock.POWERED, false), 3);
            }
            RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);

            ItemStack updatedStack = machine.getModuleHandler().getStackInSlot(0);
            RedstoneCircuit updatedCircuit = RedstoneCircuit.fromStack(
                    updatedStack, helper.getLevel().registryAccess());
            boolean firstPowered = updatedCircuit.getSlotData(firstRepeater).repeaterPowered;
            boolean secondPowered = updatedCircuit.getSlotData(secondRepeater).repeaterPowered;
            if (firstPowered != previousFirst[0] || secondPowered != previousSecond[0]) transitions[0]++;
            timeline.append(firstPowered ? '1' : '0').append(secondPowered ? '1' : '0').append(' ');
            previousFirst[0] = firstPowered;
            previousSecond[0] = secondPowered;

            if (elapsedTicks[0] == 12) {
                if (transitions[0] < 4 || timeline.indexOf("10") < 0 || timeline.indexOf("01") < 0 ||
                    timeline.indexOf("11") >= 0) {
                    helper.fail("An external lever pulse should start a continuing one-tick repeater clock; " +
                            "transitions=" + transitions[0] + ", timeline=" + timeline);
                    return;
                }
                helper.succeed();
                return;
            }
            nextTick[0].run();
        });
        nextTick[0].run();
    }

    private static void appendRepeaterClockState(StringBuilder timeline, RedstoneCircuit circuit,
                                                 int firstRepeater, int secondRepeater) {
        timeline.append(circuit.getSlotData(firstRepeater).repeaterPowered ? '1' : '0')
                .append(circuit.getSlotData(secondRepeater).repeaterPowered ? '1' : '0')
                .append(' ');
    }

    private static boolean hasBothSignalStates(CharSequence timeline) {
        String value = timeline.toString();
        return value.indexOf('0') >= 0 && value.indexOf('1') >= 0;
    }

    private static int getDustPower(GameTestHelper helper, BlockPos pos) {
        var state = helper.getLevel().getBlockState(pos);
        return state.hasProperty(RedStoneWireBlock.POWER)
                ? state.getValue(RedStoneWireBlock.POWER)
                : -1;
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterExtendsOnPulseShorterThanItsDelay(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(2, 3);
        int repeater = slot(3, 3);
        circuit.setComponent(source, RedstoneComponent.LEVER);
        circuit.setLeverState(source, true);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);

        evaluateCircuitAt(circuit, 0);
        circuit.setLeverState(source, false);
        evaluateCircuitAt(circuit, 1);
        evaluateCircuitAt(circuit, 2);
        assertOutput(helper, circuit, repeater, 15,
                "A scheduled turn-on must produce the delayed pulse even if the input has already gone low");
        evaluateCircuitAt(circuit, 3);
        assertOutput(helper, circuit, repeater, 15,
                "The extended pulse must remain on for the repeater's minimum two-game-tick delay");
        evaluateCircuitAt(circuit, 4);

        assertOutput(helper, circuit, repeater, 0, "The delayed turn-off must occur after the output pulse");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterSuppressesShortOffPulseWhileTickIsPending(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(2, 3);
        int repeater = slot(3, 3);
        circuit.setComponent(source, RedstoneComponent.LEVER);
        circuit.setLeverState(source, true);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);

        evaluateCircuitAt(circuit, 0);
        evaluateCircuitAt(circuit, 2);
        circuit.setLeverState(source, false);
        evaluateCircuitAt(circuit, 3);
        circuit.setLeverState(source, true);
        evaluateCircuitAt(circuit, 4);
        evaluateCircuitAt(circuit, 5);

        assertOutput(helper, circuit, repeater, 15,
                "A short off-pulse must be suppressed when the pending turn-off finds power restored");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterPendingTickSurvivesCircuitSerialization(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int repeater = slot(3, 3);
        circuit.setComponent(slot(2, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);
        circuit.getSlotData(repeater).repeaterDelay = 2;

        evaluateCircuitAt(circuit, 0);
        circuit = RedstoneCircuit.deserializeNBT(circuit.serializeNBT());
        evaluateCircuitAt(circuit, 3);
        assertOutput(helper, circuit, repeater, 0, "The pending repeater event must remain pending after loading");
        evaluateCircuitAt(circuit, 4);

        assertOutput(helper, circuit, repeater, 15, "The saved pending event must fire at its original due game time");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorWaitsTwoGameTicksToTurnOnAndOff(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(2, 3);
        int comparator = slot(3, 3);
        circuit.setComponent(source, RedstoneComponent.LEVER);
        circuit.setLeverState(source, true);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 1);

        evaluateCircuitAt(circuit, 0);
        assertOutput(helper, circuit, comparator, 0,
                "A comparator must not turn on immediately when its rear input is powered");
        evaluateCircuitAt(circuit, 1);
        assertOutput(helper, circuit, comparator, 0,
                "A comparator must remain off for the first game tick of its delay");
        evaluateCircuitAt(circuit, 2);
        assertOutput(helper, circuit, comparator, 15,
                "A comparator must turn on after its fixed two-game-tick delay");

        circuit.setLeverState(source, false);
        evaluateCircuitAt(circuit, 3);
        assertOutput(helper, circuit, comparator, 15,
                "A comparator must retain its output immediately after its input turns off");
        evaluateCircuitAt(circuit, 4);
        assertOutput(helper, circuit, comparator, 15,
                "A comparator must delay turning off by two game ticks");
        evaluateCircuitAt(circuit, 5);
        assertOutput(helper, circuit, comparator, 0,
                "A comparator must turn off when its delayed update samples an unpowered input");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorSamplesCurrentInputWhenScheduledTickFires(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = edgeSlot(Direction.EAST);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, rotationFor(Direction.WEST));

        evaluateCircuitAt(circuit, 0, new int[]{0, 0, 0, 15});
        assertOutput(helper, circuit, comparator, 0,
                "The comparator must keep its old output while its turn-on tick is pending");
        evaluateCircuitAt(circuit, 1, new int[]{0, 0, 0, 7});
        evaluateCircuitAt(circuit, 2, new int[]{0, 0, 0, 7});
        assertOutput(helper, circuit, comparator, 7,
                "The scheduled comparator tick must use the latest rear input strength");

        evaluateCircuitAt(circuit, 3, new int[]{0, 0, 0, 3});
        assertOutput(helper, circuit, comparator, 7,
                "A powered comparator must retain its strength while a strength update is pending");
        evaluateCircuitAt(circuit, 4, new int[]{0, 0, 0, 3});
        evaluateCircuitAt(circuit, 5, new int[]{0, 0, 0, 3});
        assertOutput(helper, circuit, comparator, 3,
                "Comparator signal-strength changes must also be delayed by two game ticks");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorMissesPulseThatEndsBeforeItsScheduledTick(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int source = slot(2, 3);
        int comparator = slot(3, 3);
        circuit.setComponent(source, RedstoneComponent.LEVER);
        circuit.setLeverState(source, true);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 1);

        evaluateCircuitAt(circuit, 0);
        assertOutput(helper, circuit, comparator, 0,
                "A one-game-tick pulse must not immediately change comparator output");
        circuit.setLeverState(source, false);
        evaluateCircuitAt(circuit, 1);
        assertOutput(helper, circuit, comparator, 0,
                "A comparator must remain off while the short pulse is pending");
        evaluateCircuitAt(circuit, 2);
        assertOutput(helper, circuit, comparator, 0,
                "A one-game-tick pulse that ends before the scheduled sample must not reach a comparator");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorPendingTickSurvivesCircuitSerialization(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(3, 3);
        circuit.setComponent(slot(2, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 1);

        evaluateCircuitAt(circuit, 0);
        circuit = RedstoneCircuit.deserializeNBT(circuit.serializeNBT());
        evaluateCircuitAt(circuit, 1);
        assertOutput(helper, circuit, comparator, 0,
                "A pending comparator output must remain off after loading before its due tick");
        evaluateCircuitAt(circuit, 2);
        assertOutput(helper, circuit, comparator, 15,
                "A pending comparator update must retain its due time through circuit serialization");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterSideLockWaitsForSideRepeaterOutput(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int targetRepeater = slot(4, 3);
        int sideRepeater = slot(4, 2);
        int sideLever = slot(4, 1);
        circuit.setComponent(slot(3, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(targetRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(targetRepeater, 1);
        circuit.setComponent(sideRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(sideRepeater, 2);
        circuit.getSlotData(sideRepeater).repeaterDelay = 2;
        circuit.setComponent(sideLever, RedstoneComponent.LEVER);
        circuit.setLeverState(sideLever, true);

        for (long gameTime = 0; gameTime <= 3; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
            assertRepeaterLocked(helper, circuit, targetRepeater, false,
                    "A side repeater with a pending turn-on must not lock the target");
        }
        evaluateCircuitAt(circuit, 4);
        assertOutput(helper, circuit, sideRepeater, 15, "The side repeater must turn on after its configured delay");
        evaluateCircuitAt(circuit, 5);

        assertRepeaterLocked(helper, circuit, targetRepeater, true,
                "The target must lock once the side repeater's output is on");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterSideLockPersistsUntilSideRepeaterOutputTurnsOff(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int targetRepeater = slot(4, 3);
        int sideRepeater = slot(4, 2);
        int sideLever = slot(4, 1);
        circuit.setComponent(slot(3, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(targetRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(targetRepeater, 1);
        circuit.setComponent(sideRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(sideRepeater, 2);
        circuit.getSlotData(sideRepeater).repeaterDelay = 2;
        circuit.setComponent(sideLever, RedstoneComponent.LEVER);
        circuit.setLeverState(sideLever, true);

        for (long gameTime = 0; gameTime <= 4; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }
        circuit.setLeverState(sideLever, false);
        evaluateCircuitAt(circuit, 5);
        assertRepeaterLocked(helper, circuit, targetRepeater, true,
                "The target must remain locked while the side repeater's delayed turn-off is pending");

        for (long gameTime = 6; gameTime <= 9; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }
        assertOutput(helper, circuit, sideRepeater, 0, "The side repeater must turn off when its delayed tick is due");
        assertRepeaterLocked(helper, circuit, targetRepeater, true,
                "The current propagation must preserve the lock until the side output change is observed");
        evaluateCircuitAt(circuit, 10);

        assertRepeaterLocked(helper, circuit, targetRepeater, false,
                "The target must unlock after the side repeater output turns off");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorLocksRepeaterOnlyWhenItsOutputIsOn(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int targetRepeater = slot(4, 3);
        int sideComparator = slot(4, 2);
        circuit.setComponent(slot(3, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(targetRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(targetRepeater, 1);
        circuit.setComponent(sideComparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(sideComparator, 2);
        circuit.setComponent(slot(0, 1), RedstoneComponent.REDSTONE_BLOCK);
        for (int x = 1; x <= 4; x++) {
            circuit.setComponent(slot(x, 1), RedstoneComponent.WIRE);
        }
        circuit.setComponent(slot(3, 2), RedstoneComponent.REDSTONE_BLOCK);

        circuit.propagateSignals(null);
        circuit.propagateSignals(null);

        assertOutput(helper, circuit, sideComparator, 0,
                "A compare-mode comparator with a stronger side input must output zero");
        assertRepeaterLocked(helper, circuit, targetRepeater, false,
                "A comparator with powered input but zero output must not lock a repeater");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterLocksFromPoweredComparatorOutput(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int targetRepeater = slot(4, 3);
        int sideComparator = slot(4, 2);
        circuit.setComponent(slot(3, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(targetRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(targetRepeater, 1);
        circuit.setComponent(sideComparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(sideComparator, 2);
        circuit.setComponent(slot(0, 1), RedstoneComponent.REDSTONE_BLOCK);
        for (int x = 1; x <= 4; x++) {
            circuit.setComponent(slot(x, 1), RedstoneComponent.WIRE);
        }

        for (long gameTime = 0; gameTime <= 3; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }

        if (circuit.getSlotData(sideComparator).outputSignal <= 0) {
            helper.fail("The side comparator must have a non-zero output for this test");
        }
        assertRepeaterLocked(helper, circuit, targetRepeater, true,
                "A comparator with a non-zero output toward the repeater must lock it");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void ioConfiguredFacesGateWorldRedstoneInputs(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        RedstoneCircuit circuit = new RedstoneCircuit();
        int torch = slot(RedstoneCircuit.GRID_WIDTH / 2, 0);
        circuit.setComponent(torch, RedstoneComponent.TORCH);
        circuit.getSlotData(torch).torchRotation = 0;
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));
        ItemStack ioStack = createConfiguredIoModule();
        machine.getModuleHandler().setStackInSlot(1, ioStack);

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        verifyConfiguredIoInputFaces(helper, machine, machinePos, ioStack, machineFacing,
                RelativeSide.values(), 0, () -> verifyIoFaceModeGating(helper, machine, machinePos, ioStack,
                        machineFacing));
    }

    private static void verifyConfiguredIoInputFaces(GameTestHelper helper, MachineFrameworkBlockEntity machine,
                                                      BlockPos machinePos, ItemStack ioStack, Direction machineFacing,
                                                      RelativeSide[] sides, int sideIndex,
                                                      Runnable onComplete) {
        if (sideIndex >= sides.length) {
            onComplete.run();
            return;
        }

        RelativeSide side = sides[sideIndex];
        for (RelativeSide configuredSide : RelativeSide.values()) {
            IoModuleItem.setFaceMode(ioStack, configuredSide, IoModuleItem.FaceMode.DISABLED);
        }
        IoModuleItem.setFaceMode(ioStack, side, IoModuleItem.FaceMode.INPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);

        BlockPos sourcePos = machinePos.relative(side.toWorldDirection(machineFacing));
        helper.getLevel().setBlock(sourcePos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        helper.runAfterDelay(RedstoneCircuit.TORCH_DELAY_GAME_TICKS + 1, () -> {
            if (machine.getModuleData(0, 0) != 0) {
                helper.fail("The configured " + side + " input face must turn off the border torch");
                return;
            }

            helper.getLevel().setBlock(sourcePos, Blocks.AIR.defaultBlockState(), 3);
            RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
            helper.runAfterDelay(RedstoneCircuit.TORCH_DELAY_GAME_TICKS + 1,
                    () -> verifyConfiguredIoInputFaces(helper, machine, machinePos, ioStack, machineFacing,
                            sides, sideIndex + 1, onComplete));
        });
    }

    private static void verifyIoFaceModeGating(GameTestHelper helper, MachineFrameworkBlockEntity machine,
                                                BlockPos machinePos, ItemStack ioStack,
                                                Direction machineFacing) {
        BlockPos frontSource = machinePos.relative(machineFacing);
        helper.getLevel().setBlock(frontSource, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);

        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        assertModuleOutput(helper, machine, 0, 0, 15,
                "OUTPUT must not feed the adjacent world signal back into the circuit");

        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OPEN);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        helper.runAfterDelay(RedstoneCircuit.TORCH_DELAY_GAME_TICKS + 1, () -> {
            if (machine.getModuleData(0, 0) != 0) {
                helper.fail("OPEN must allow the adjacent world signal to turn off the torch after its delay");
                return;
            }

            IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.DISABLED);
            machine.getModuleHandler().setStackInSlot(1, ioStack);
            RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
            helper.runAfterDelay(RedstoneCircuit.TORCH_DELAY_GAME_TICKS + 1, () -> {
                assertModuleOutput(helper, machine, 0, 0, 15,
                        "DISABLED must block the adjacent world signal and let the torch relight");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void ioOutputFacesExposeStrongestCircuitEdgeWithoutChangingWorldBlocks(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        RedstoneCircuit weakerCircuit = new RedstoneCircuit();
        weakerCircuit.setComponent(slot(1, 2), RedstoneComponent.REDSTONE_BLOCK);
        for (int x = 2; x < RedstoneCircuit.GRID_WIDTH; x++) {
            weakerCircuit.setComponent(slot(x, 2), RedstoneComponent.WIRE);
        }
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), weakerCircuit));

        RedstoneCircuit strongerCircuit = new RedstoneCircuit();
        strongerCircuit.setComponent(slot(4, 0), RedstoneComponent.REDSTONE_BLOCK);
        machine.getModuleHandler().setStackInSlot(2,
                createRedstoneModule(helper.getLevel().registryAccess(), strongerCircuit));

        ItemStack ioStack = createConfiguredIoModule();
        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        IoModuleItem.setFaceMode(ioStack, RelativeSide.BACK, IoModuleItem.FaceMode.OPEN);
        IoModuleItem.setFaceMode(ioStack, RelativeSide.LEFT, IoModuleItem.FaceMode.INPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        BlockPos frontWorldPos = machinePos.relative(RelativeSide.FRONT.toWorldDirection(machineFacing));
        helper.getLevel().setBlock(frontWorldPos, Blocks.AIR.defaultBlockState(), 3);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);

        assertModuleOutput(helper, machine, 0, 3, 9,
                "The weaker circuit must expose its reduced east-edge strength");
        assertModuleOutput(helper, machine, 2, 0, 15,
                "The stronger circuit must expose its north-edge strength");
        assertModuleOutput(helper, machine, 1, IoModuleItem.getRedstoneSignalChannel(RelativeSide.FRONT), 15,
                "An OUTPUT face must expose the strongest aggregate circuit-edge strength");
        assertModuleOutput(helper, machine, 1, IoModuleItem.getRedstoneSignalChannel(RelativeSide.BACK), 15,
                "An OPEN face must expose the strongest aggregate circuit-edge strength");
        assertModuleOutput(helper, machine, 1, IoModuleItem.getRedstoneSignalChannel(RelativeSide.LEFT), 0,
                "An INPUT face must not publish circuit output");
        assertModuleOutput(helper, machine, 1, IoModuleItem.getRedstoneSignalChannel(RelativeSide.RIGHT), 0,
                "A DISABLED face must not publish circuit output");
        if (!helper.getLevel().getBlockState(frontWorldPos).isAir()) {
            helper.fail("Publishing an I/O output must not place or replace a world block");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void ioOutputSignalUsesConfiguredWorldFace(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(4, 0), RedstoneComponent.REDSTONE_BLOCK);
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));
        ItemStack ioStack = createConfiguredIoModule();
        machine.getModuleHandler().setStackInSlot(1, ioStack);

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        for (RelativeSide outputSide : RelativeSide.values()) {
            for (RelativeSide side : RelativeSide.values()) {
                IoModuleItem.setFaceMode(ioStack, side, side == outputSide
                        ? IoModuleItem.FaceMode.OUTPUT : IoModuleItem.FaceMode.DISABLED);
            }
            machine.getModuleHandler().setStackInSlot(1, ioStack);
            RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);

            Direction outputDirection = outputSide.toWorldDirection(machineFacing);
            for (Direction direction : Direction.values()) {
                int expected = direction == outputDirection ? 15 : 0;
                int actual = helper.getLevel().getSignal(machinePos, direction.getOpposite());
                if (actual != expected) {
                    helper.fail("I/O output " + outputSide + " should emit " + expected +
                            " toward " + direction + " but emitted " + actual);
                    return;
                }
            }
        }

        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.INPUT);
        for (RelativeSide side : RelativeSide.values()) {
            if (side != RelativeSide.FRONT) {
                IoModuleItem.setFaceMode(ioStack, side, IoModuleItem.FaceMode.DISABLED);
            }
        }
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        if (helper.getLevel().getSignal(machinePos, machineFacing.getOpposite()) != 0) {
            helper.fail("An INPUT face must not emit a world redstone signal");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void ioOutputUpdatesAdjacentRedstoneLamp(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        Direction machineFacing = helper.getLevel().getBlockState(machinePos)
                .getValue(MachineFrameworkBlock.FACING);
        BlockPos lampPos = machinePos.relative(machineFacing);
        helper.getLevel().setBlock(lampPos, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);

        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(4, 0), RedstoneComponent.REDSTONE_BLOCK);
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));
        ItemStack ioStack = createConfiguredIoModule();
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);

        if (helper.getLevel().getBlockState(lampPos).getValue(RedstoneLampBlock.LIT)) {
            helper.fail("A lamp must stay off while its I/O face is disabled");
            return;
        }

        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OUTPUT);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        if (!helper.getLevel().getBlockState(lampPos).getValue(RedstoneLampBlock.LIT)) {
            helper.fail("A neighboring lamp must turn on when the configured output becomes powered");
            return;
        }

        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.OPEN);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        if (!helper.getLevel().getBlockState(lampPos).getValue(RedstoneLampBlock.LIT)) {
            helper.fail("An OPEN face must continue emitting its circuit output");
            return;
        }

        IoModuleItem.setFaceMode(ioStack, RelativeSide.FRONT, IoModuleItem.FaceMode.DISABLED);
        machine.getModuleHandler().setStackInSlot(1, ioStack);
        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        helper.runAfterDelay(5, () -> {
            if (helper.getLevel().getBlockState(lampPos).getValue(RedstoneLampBlock.LIT)) {
                helper.fail("A neighboring lamp must turn off after the configured output is removed");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void machineTicksComparatorOnlyCircuitsEveryGameTick(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(3, 3);
        circuit.setComponent(slot(2, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 1);
        machine.getModuleHandler().setStackInSlot(0,
                createRedstoneModule(helper.getLevel().registryAccess(), circuit));

        RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);
        assertMachineComparatorOutput(helper, machine, helper.getLevel().registryAccess(), comparator, 0,
                "A comparator-only machine must not output before its scheduled tick");
        helper.runAfterDelay(1, () -> {
            RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);
            assertMachineComparatorOutput(helper, machine, helper.getLevel().registryAccess(), comparator, 0,
                    "A comparator-only machine must evaluate while the two-tick update is pending");
            helper.runAfterDelay(1, () -> {
                RedstoneModuleItem.tickMachineCircuits(helper.getLevel(), machinePos, machine);
                assertMachineComparatorOutput(helper, machine, helper.getLevel().registryAccess(), comparator, 15,
                        "A comparator-only machine must apply the output on its due game tick");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void lockedRepeaterKeepsItsFrozenOutput(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int repeater = slot(3, 3);
        int outputWire = slot(4, 3);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);
        circuit.getSlotData(repeater).repeaterLocked = true;
        circuit.getSlotData(repeater).repeaterLockOutput = true;
        circuit.setComponent(outputWire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null, false);

        assertOutput(helper, circuit, repeater, 15, "A locked ON repeater must keep its frozen output");
        assertSignal(helper, circuit, outputWire, 15, "A locked ON repeater must continue powering its front");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterLockedBeforeDueTickIgnoresThatTick(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int repeater = slot(3, 3);
        circuit.setComponent(slot(2, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);

        circuit.propagateSignals(null, false);
        circuit.scheduleRepeaterTransitions(0);
        circuit.getSlotData(repeater).repeaterLocked = true;
        circuit.propagateSignals(null, false);
        circuit.processRepeaterTicksAt(1);
        if (circuit.getSlotData(repeater).repeaterScheduledGameTime != 2) {
            helper.fail("A future scheduled tick must remain pending while the repeater is locked");
        }

        circuit.propagateSignals(null, false);
        boolean lockedTickChangedOutput = circuit.processRepeaterTicksAt(2);
        if (circuit.getSlotData(repeater).repeaterScheduledGameTime != -1L || lockedTickChangedOutput ||
            circuit.getSlotData(repeater).repeaterPowered) {
            helper.fail("A repeater must ignore a scheduled tick that becomes due while it is locked");
        }

        circuit.getSlotData(repeater).repeaterLocked = false;
        for (long gameTime = 3; gameTime <= 5; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }
        assertOutput(helper, circuit, repeater, 15, "After unlocking, the powered input must schedule a fresh turn-on");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void repeaterChainWorksAgainstSlotIterationOrder(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(7, 3), RedstoneComponent.REDSTONE_BLOCK);
        for (int x = 4; x <= 6; x++) {
            int repeater = slot(x, 3);
            circuit.setComponent(repeater, RedstoneComponent.REPEATER);
            circuit.setRotation(repeater, 3);
        }
        int outputWire = slot(3, 3);
        circuit.setComponent(outputWire, RedstoneComponent.WIRE);

        for (long gameTime = 0; gameTime <= 6; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }

        for (int x = 4; x <= 6; x++) {
            assertOutput(helper, circuit, slot(x, 3), 15,
                    "Each powered repeater must provide a full-strength input to the next repeater");
        }
        assertSignal(helper, circuit, outputWire, 15, "The final repeater must power its output wire");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorComparesAndSubtractsSideStrength(GameTestHelper helper) {
        RedstoneCircuit compareCircuit = comparatorSideCircuit(false);
        for (long gameTime = 0; gameTime <= 2; gameTime++) {
            evaluateCircuitAt(compareCircuit, gameTime);
        }
        int compare = compareCircuit.getSlotData(slot(4, 3)).outputSignal;
        if (compare != 15) {
            helper.fail("Compare mode should pass rear strength 15 when side strength is 14; got " + compare);
        }

        RedstoneCircuit subtractCircuit = comparatorSideCircuit(true);
        for (long gameTime = 0; gameTime <= 4; gameTime++) {
            evaluateCircuitAt(subtractCircuit, gameTime);
        }
        int subtract = subtractCircuit.getSlotData(slot(4, 3)).outputSignal;
        if (subtract != 1) {
            helper.fail("Subtract mode should output 15 - 14 = 1; got " + subtract);
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorAcceptsAdjacentRedstoneBlockSideInput(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(4, 0);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 2);
        circuit.setComponent(slot(5, 0), RedstoneComponent.REDSTONE_BLOCK);

        for (long gameTime = 0; gameTime <= 2; gameTime++) {
            evaluateCircuitAt(circuit, gameTime, new int[]{12, 0, 0, 0});
        }

        assertOutput(helper, circuit, comparator, 0,
                "A directly adjacent redstone block must provide a comparator side input");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorReadsDustSideInput(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(4, 0);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 2);
        circuit.setComponent(slot(5, 0), RedstoneComponent.WIRE);
        circuit.setComponent(slot(6, 0), RedstoneComponent.REDSTONE_BLOCK);

        for (long gameTime = 0; gameTime <= 2; gameTime++) {
            evaluateCircuitAt(circuit, gameTime, new int[]{12, 0, 0, 0});
        }

        assertOutput(helper, circuit, comparator, 0,
                "A side wire at strength 15 must override the comparator's rear strength 12");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorIgnoresLeverSideInput(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(4, 0);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 2);
        int sideLever = slot(5, 0);
        circuit.setComponent(sideLever, RedstoneComponent.LEVER);
        circuit.setLeverState(sideLever, true);

        for (long gameTime = 0; gameTime <= 2; gameTime++) {
            evaluateCircuitAt(circuit, gameTime, new int[]{12, 0, 0, 0});
        }

        assertOutput(helper, circuit, comparator, 12,
                "A lever is a power component, not a valid comparator side transmission input");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorReadsRepeaterPointingIntoSide(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(4, 0);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 2);
        int sideRepeater = slot(5, 0);
        circuit.setComponent(sideRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(sideRepeater, 3);
        circuit.setComponent(slot(6, 0), RedstoneComponent.REDSTONE_BLOCK);
        for (long gameTime = 0; gameTime <= 4; gameTime++) {
            evaluateCircuitAt(circuit, gameTime, new int[]{12, 0, 0, 0});
            if (gameTime == 2) {
                assertOutput(helper, circuit, comparator, 12,
                        "The comparator should first pass the rear signal before the delayed side repeater takes effect");
            }
        }

        assertOutput(helper, circuit, comparator, 0,
                "A delayed repeater side input must eventually override the comparator's weaker rear input");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void comparatorReadsStronglyPoweredWallSideInput(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(4, 0);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 2);
        int wall = slot(5, 0);
        circuit.setComponent(wall, RedstoneComponent.WALL);
        int sideRepeater = slot(6, 0);
        circuit.setComponent(sideRepeater, RedstoneComponent.REPEATER);
        circuit.setRotation(sideRepeater, 3);
        circuit.setComponent(slot(7, 0), RedstoneComponent.REDSTONE_BLOCK);

        for (long gameTime = 0; gameTime <= 4; gameTime++) {
            evaluateCircuitAt(circuit, gameTime, new int[]{12, 0, 0, 0});
            if (gameTime == 2) {
                assertOutput(helper, circuit, comparator, 12,
                        "The comparator should retain its prior output until the wall becomes strongly powered");
            }
        }

        assertOutput(helper, circuit, comparator, 0,
                "A strongly powered solid block beside a comparator must provide a side input");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void stronglyPoweredWallCanPowerAdjacentDust(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(1, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(slot(2, 3), RedstoneComponent.WIRE);
        int repeater = slot(3, 3);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);
        int wall = slot(4, 3);
        int wire = slot(4, 2);
        circuit.setComponent(wall, RedstoneComponent.WALL);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        for (long gameTime = 0; gameTime <= 2; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }

        assertSignal(helper, circuit, wall, 15, "The repeater must strongly power the block in front");
        assertSignal(helper, circuit, wire, 15, "A strongly powered block must power adjacent dust at full strength");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void weaklyPoweredWallDoesNotPowerAdjacentDust(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(1, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(slot(2, 3), RedstoneComponent.WIRE);
        int wall = slot(3, 3);
        int wire = slot(4, 3);
        circuit.setComponent(wall, RedstoneComponent.WALL);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wall, 15, "Dust can weakly power an adjacent wall");
        assertSignal(helper, circuit, wire, 0, "A weakly powered wall must not relay power to adjacent dust");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void wallMountedLeverStronglyPowersAttachmentBlock(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int lever = slot(3, 3);
        circuit.setComponent(lever, RedstoneComponent.LEVER);
        circuit.setLeverState(lever, true);
        circuit.getSlotData(lever).leverRotation = 1;
        int wall = slot(4, 3);
        int wire = slot(4, 2);
        circuit.setComponent(wall, RedstoneComponent.WALL);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wall, 15, "An active wall-mounted lever must strongly power its support block");
        assertSignal(helper, circuit, wire, 15, "The strongly powered support block must power adjacent dust");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void attachedTorchInvertsItsSupportPower(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(1, 3), RedstoneComponent.REDSTONE_BLOCK);
        circuit.setComponent(slot(2, 3), RedstoneComponent.WIRE);
        int repeater = slot(3, 3);
        circuit.setComponent(repeater, RedstoneComponent.REPEATER);
        circuit.setRotation(repeater, 1);
        int support = slot(4, 3);
        int torch = slot(4, 4);
        circuit.setComponent(support, RedstoneComponent.WALL);
        circuit.setComponent(torch, RedstoneComponent.TORCH);
        circuit.getSlotData(torch).torchRotation = 0;

        for (long gameTime = 0; gameTime <= 4; gameTime++) {
            evaluateCircuitAt(circuit, gameTime);
        }

        assertSignal(helper, circuit, support, 15, "The repeater must power the torch's support block");
        assertSignal(helper, circuit, torch, 0, "A wall-mounted torch must turn off when its support is powered");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void attachedTorchPowersAdjacentDustButNotItsSupport(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int torch = slot(3, 3);
        int support = slot(3, 2);
        int sideWall = slot(2, 3);
        int wire = slot(4, 3);
        circuit.setComponent(torch, RedstoneComponent.TORCH);
        circuit.getSlotData(torch).torchRotation = 0;
        circuit.setComponent(support, RedstoneComponent.WALL);
        circuit.setComponent(sideWall, RedstoneComponent.WALL);
        circuit.setComponent(wire, RedstoneComponent.WIRE);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, torch, 15, "An unpowered torch support must leave the torch lit");
        assertSignal(helper, circuit, support, 0, "A torch must not power the block it is attached to");
        assertSignal(helper, circuit, sideWall, 0, "Torch output must skip an adjacent wall cell");
        assertSignal(helper, circuit, wire, 15, "A lit torch must power adjacent dust at strength 15");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void attachedTorchBurnoutPreventsImmediateReactivation(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int lever = slot(2, 3);
        int support = slot(3, 3);
        int torch = slot(4, 3);
        circuit.setComponent(lever, RedstoneComponent.LEVER);
        circuit.getSlotData(lever).leverRotation = 1;
        circuit.setComponent(support, RedstoneComponent.WALL);
        circuit.setComponent(torch, RedstoneComponent.TORCH);
        circuit.getSlotData(torch).torchRotation = 3;

        long gameTime = 0;
        for (int toggle = 0; toggle < 9; toggle++) {
            circuit.setLeverState(lever, true);
            evaluateCircuitAt(circuit, gameTime);
            gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
            evaluateCircuitAt(circuit, gameTime);
            circuit.setLeverState(lever, false);
            evaluateCircuitAt(circuit, gameTime);
            gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
            evaluateCircuitAt(circuit, gameTime);
        }

        RedstoneSlotData torchData = circuit.getSlotData(torch);
        if (!torchData.torchBurnedOut || torchData.torchPowered) {
            helper.fail("The ninth turn-off in the 60-game-tick window must burn out the torch");
            return;
        }

        circuit = RedstoneCircuit.deserializeNBT(circuit.serializeNBT());
        if (!circuit.getSlotData(torch).torchBurnedOut) {
            helper.fail("Torch burnout state must persist through circuit serialization");
            return;
        }

        long burnoutUntil = torchData.torchBurnoutUntil;
        evaluateCircuitAt(circuit, burnoutUntil - 1);
        if (circuit.getSlotData(torch).torchPowered) {
            helper.fail("A burned-out torch must stay off until its scheduled retry is due");
            return;
        }

        evaluateCircuitAt(circuit, burnoutUntil);
        assertSignal(helper, circuit, torch, 15,
                "An unpowered torch must relight when its scheduled 160-game-tick retry runs");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void attachedTorchRetriesAfterBurnoutWindowClearsOnUpdate(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int lever = slot(2, 3);
        int support = slot(3, 3);
        int torch = slot(4, 3);
        circuit.setComponent(lever, RedstoneComponent.LEVER);
        circuit.getSlotData(lever).leverRotation = 1;
        circuit.setComponent(support, RedstoneComponent.WALL);
        circuit.setComponent(torch, RedstoneComponent.TORCH);
        circuit.getSlotData(torch).torchRotation = 3;

        long gameTime = 0;
        for (int toggle = 0; toggle <= RedstoneCircuit.TORCH_BURNOUT_MAX_OFFS; toggle++) {
            circuit.setLeverState(lever, true);
            evaluateCircuitAt(circuit, gameTime);
            gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
            evaluateCircuitAt(circuit, gameTime);
            circuit.setLeverState(lever, false);
            evaluateCircuitAt(circuit, gameTime);
            gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
            evaluateCircuitAt(circuit, gameTime);
        }

        circuit.setLeverState(lever, true);
        evaluateCircuitAt(circuit, gameTime);
        gameTime = 66;
        evaluateCircuitAt(circuit, gameTime);
        circuit.setLeverState(lever, false);
        evaluateCircuitAt(circuit, gameTime);
        if (circuit.getSlotData(torch).torchPowered) {
            helper.fail("Eight turn-offs still within the 60-game-tick window must keep the torch off");
            return;
        }

        gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
        evaluateCircuitAt(circuit, gameTime);
        if (circuit.getSlotData(torch).torchPowered) {
            helper.fail("The torch must not relight while eight turn-offs remain in its 60-game-tick window");
            return;
        }

        gameTime++;
        circuit.setLeverState(lever, true);
        evaluateCircuitAt(circuit, gameTime);
        circuit.setLeverState(lever, false);
        evaluateCircuitAt(circuit, gameTime);
        gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
        evaluateCircuitAt(circuit, gameTime);

        assertSignal(helper, circuit, torch, 15,
                "A support update must let a burned-out torch relight once fewer than eight turn-offs remain");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void attachedTorchDoesNotBurnOutAfterEightTurnOffs(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int lever = slot(2, 3);
        int support = slot(3, 3);
        int torch = slot(4, 3);
        circuit.setComponent(lever, RedstoneComponent.LEVER);
        circuit.getSlotData(lever).leverRotation = 1;
        circuit.setComponent(support, RedstoneComponent.WALL);
        circuit.setComponent(torch, RedstoneComponent.TORCH);
        circuit.getSlotData(torch).torchRotation = 3;

        long gameTime = 0;
        for (int toggle = 0; toggle < RedstoneCircuit.TORCH_BURNOUT_MAX_OFFS; toggle++) {
            circuit.setLeverState(lever, true);
            evaluateCircuitAt(circuit, gameTime);
            gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
            evaluateCircuitAt(circuit, gameTime);
            circuit.setLeverState(lever, false);
            evaluateCircuitAt(circuit, gameTime);
            gameTime += RedstoneCircuit.TORCH_DELAY_GAME_TICKS;
            evaluateCircuitAt(circuit, gameTime);
        }

        if (circuit.getSlotData(torch).torchBurnedOut || !circuit.getSlotData(torch).torchPowered) {
            helper.fail("Eight turn-offs in the burnout window must not burn out the torch");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void attachedTorchUsesTwoGameTickTransitionDelay(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int lever = slot(2, 3);
        int support = slot(3, 3);
        int torch = slot(4, 3);
        circuit.setComponent(lever, RedstoneComponent.LEVER);
        circuit.getSlotData(lever).leverRotation = 1;
        circuit.setComponent(support, RedstoneComponent.WALL);
        circuit.setComponent(torch, RedstoneComponent.TORCH);
        circuit.getSlotData(torch).torchRotation = 3;
        circuit.setLeverState(lever, true);

        evaluateCircuitAt(circuit, 0);
        assertSignal(helper, circuit, torch, 15, "A powered support must not turn a torch off immediately");
        evaluateCircuitAt(circuit, 1);
        assertSignal(helper, circuit, torch, 15, "A torch must remain lit during the first game tick");
        evaluateCircuitAt(circuit, RedstoneCircuit.TORCH_DELAY_GAME_TICKS);
        assertSignal(helper, circuit, torch, 0, "A torch must turn off after two game ticks");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void redstoneBlockDoesNotPowerAdjacentWall(GameTestHelper helper) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(slot(3, 3), RedstoneComponent.REDSTONE_BLOCK);
        int wall = slot(4, 3);
        circuit.setComponent(wall, RedstoneComponent.WALL);

        circuit.propagateSignals(null);

        assertSignal(helper, circuit, wall, 0, "A redstone block must not power an adjacent solid block");
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void legacyCircuitNbtKeepsComponentDefaultsWhenSettingsAreMissingOrMistyped(GameTestHelper helper) {
        CompoundTag circuitTag = new CompoundTag();
        ListTag slots = new ListTag();
        int torch = slot(0, 0);
        int lever = slot(1, 0);
        int repeater = slot(2, 0);
        slots.add(savedSlot(torch, RedstoneComponent.TORCH));
        slots.add(savedSlot(lever, RedstoneComponent.LEVER));
        slots.add(savedSlot(repeater, RedstoneComponent.REPEATER));

        int mistypedTorch = slot(3, 0);
        int mistypedLever = slot(4, 0);
        int mistypedRepeater = slot(5, 0);
        CompoundTag mistypedTorchTag = savedSlot(mistypedTorch, RedstoneComponent.TORCH);
        mistypedTorchTag.putString("TorchRotation", "invalid");
        CompoundTag mistypedLeverTag = savedSlot(mistypedLever, RedstoneComponent.LEVER);
        mistypedLeverTag.putString("LeverRotation", "invalid");
        CompoundTag mistypedRepeaterTag = savedSlot(mistypedRepeater, RedstoneComponent.REPEATER);
        mistypedRepeaterTag.putString("Delay", "invalid");
        slots.add(mistypedTorchTag);
        slots.add(mistypedLeverTag);
        slots.add(mistypedRepeaterTag);
        circuitTag.put("Slots", slots);

        RedstoneCircuit circuit = RedstoneCircuit.deserializeNBT(circuitTag);
        if (circuit.getSlotData(torch).torchRotation != 4 ||
            circuit.getSlotData(lever).leverRotation != 4 ||
            circuit.getSlotData(repeater).repeaterDelay != 1 ||
            circuit.getSlotData(mistypedTorch).torchRotation != 4 ||
            circuit.getSlotData(mistypedLever).leverRotation != 4 ||
            circuit.getSlotData(mistypedRepeater).repeaterDelay != 1) {
            helper.fail("Missing or mistyped settings must retain torch/lever standing and repeater delay defaults");
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void circuitNbtClampsInvalidTorchLeverAndRepeaterSettings(GameTestHelper helper) {
        CompoundTag circuitTag = new CompoundTag();
        ListTag slots = new ListTag();
        int torchBelow = slot(0, 0);
        int torchAbove = slot(1, 0);
        int leverBelow = slot(2, 0);
        int leverAbove = slot(3, 0);
        int repeaterBelow = slot(4, 0);
        int repeaterAbove = slot(5, 0);
        int comparatorBelow = slot(6, 0);
        int comparatorAbove = slot(7, 0);

        CompoundTag torchBelowTag = savedSlot(torchBelow, RedstoneComponent.TORCH);
        torchBelowTag.putInt("TorchRotation", -1);
        CompoundTag torchAboveTag = savedSlot(torchAbove, RedstoneComponent.TORCH);
        torchAboveTag.putInt("TorchRotation", 5);
        CompoundTag leverBelowTag = savedSlot(leverBelow, RedstoneComponent.LEVER);
        leverBelowTag.putInt("LeverRotation", -1);
        CompoundTag leverAboveTag = savedSlot(leverAbove, RedstoneComponent.LEVER);
        leverAboveTag.putInt("LeverRotation", 5);
        CompoundTag repeaterBelowTag = savedSlot(repeaterBelow, RedstoneComponent.REPEATER);
        repeaterBelowTag.putInt("Delay", 0);
        CompoundTag repeaterAboveTag = savedSlot(repeaterAbove, RedstoneComponent.REPEATER);
        repeaterAboveTag.putInt("Delay", 5);
        CompoundTag comparatorBelowTag = savedSlot(comparatorBelow, RedstoneComponent.COMPARATOR);
        comparatorBelowTag.putInt("Rotation", -1);
        CompoundTag comparatorAboveTag = savedSlot(comparatorAbove, RedstoneComponent.COMPARATOR);
        comparatorAboveTag.putInt("Rotation", 4);
        slots.add(torchBelowTag);
        slots.add(torchAboveTag);
        slots.add(leverBelowTag);
        slots.add(leverAboveTag);
        slots.add(repeaterBelowTag);
        slots.add(repeaterAboveTag);
        slots.add(comparatorBelowTag);
        slots.add(comparatorAboveTag);
        circuitTag.put("Slots", slots);

        RedstoneCircuit circuit = RedstoneCircuit.deserializeNBT(circuitTag);
        if (circuit.getSlotData(torchBelow).torchRotation != 0 ||
            circuit.getSlotData(torchAbove).torchRotation != 4 ||
            circuit.getSlotData(leverBelow).leverRotation != 0 ||
            circuit.getSlotData(leverAbove).leverRotation != 4 ||
            circuit.getSlotData(repeaterBelow).repeaterDelay != 1 ||
            circuit.getSlotData(repeaterAbove).repeaterDelay != 4 ||
            circuit.getSlotData(comparatorBelow).rotation != 0 ||
            circuit.getSlotData(comparatorAbove).rotation != 3) {
            helper.fail("Out-of-range settings must be clamped to their valid ranges");
        }
        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void adjacentModulesSettleAlignedSignalsAndClearRemovedSources(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }
        for (Direction direction : Direction.values()) {
            helper.getLevel().setBlock(machinePos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
        }

        int torchAligned = slot(RedstoneCircuit.GRID_WIDTH - 1, 2);
        int torchUnaligned = slot(RedstoneCircuit.GRID_WIDTH - 1, 1);
        RedstoneCircuit leftCircuit = new RedstoneCircuit();
        leftCircuit.setComponent(torchAligned, RedstoneComponent.TORCH);
        leftCircuit.getSlotData(torchAligned).torchRotation = 1;

        RedstoneCircuit middleCircuit = new RedstoneCircuit();
        for (int x = 0; x < RedstoneCircuit.GRID_WIDTH; x++) {
            middleCircuit.setComponent(slot(x, 2), RedstoneComponent.WIRE);
        }

        int sourceLever = slot(0, 2);
        RedstoneCircuit rightCircuit = new RedstoneCircuit();
        rightCircuit.setComponent(sourceLever, RedstoneComponent.LEVER);
        rightCircuit.setLeverState(sourceLever, true);
        rightCircuit.getSlotData(sourceLever).leverRotation = 3;

        var registries = helper.getLevel().registryAccess();
        machine.getModuleHandler().setStackInSlot(0, createRedstoneModule(registries, leftCircuit));
        machine.getModuleHandler().setStackInSlot(1, createRedstoneModule(registries, middleCircuit));
        machine.getModuleHandler().setStackInSlot(2, createRedstoneModule(registries, rightCircuit));

        RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
        helper.runAfterDelay(RedstoneCircuit.TORCH_DELAY_GAME_TICKS + 1, () -> {
            assertModuleOutput(helper, machine, 0, 3, 0,
                    "A higher-index source must turn off a torch at the matching shared-wall position");
            assertModuleOutput(helper, machine, 1, 2, 7,
                    "The middle module must publish its dust signal at the aligned west-edge position");

            ItemStack extinguishedTorchModule = machine.getModuleHandler().getStackInSlot(0).copy();
            RedstoneCircuit extinguishedCircuit = RedstoneCircuit.fromStack(
                    extinguishedTorchModule, helper.getLevel().registryAccess());
            if (extinguishedCircuit.getSlotData(torchAligned).torchPowered) {
                helper.fail("The aligned torch must latch OFF after receiving the shared-wall signal");
                return;
            }

            RedstoneCircuit unalignedCircuit = new RedstoneCircuit();
            unalignedCircuit.setComponent(torchUnaligned, RedstoneComponent.TORCH);
            unalignedCircuit.getSlotData(torchUnaligned).torchRotation = 1;
            machine.getModuleHandler().setStackInSlot(0, createRedstoneModule(registries, unalignedCircuit));
            RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);
            assertModuleOutput(helper, machine, 0, 3, 15,
                    "A torch at a different edge position must remain lit");

            ItemStack rightModule = machine.getModuleHandler().getStackInSlot(2);
            ItemStackHandler rightInventory = RedstoneModuleItem.getRedstoneInventory(rightModule, registries);
            rightInventory.setStackInSlot(sourceLever, ItemStack.EMPTY);
            RedstoneModuleItem.saveRedstoneInventory(rightModule, rightInventory, registries);
            machine.getModuleHandler().setStackInSlot(2, rightModule);
            machine.getModuleHandler().setStackInSlot(0, extinguishedTorchModule);
            RedstoneModuleItem.recomputeMachineCircuits(helper.getLevel(), machinePos, machine);

            assertModuleOutput(helper, machine, 0, 3, 0,
                    "The torch must remain off until its delayed response to source removal");
            helper.runAfterDelay(RedstoneCircuit.TORCH_DELAY_GAME_TICKS + 1, () -> {
                assertModuleOutput(helper, machine, 0, 3, 15,
                        "Removing the source must clear the shared-wall signal and relight the attached torch");
                helper.succeed();
            });
        });
    }

    private static RedstoneCircuit comparatorSideCircuit(boolean subtract) {
        RedstoneCircuit circuit = new RedstoneCircuit();
        int comparator = slot(4, 3);
        circuit.setComponent(comparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(comparator, 0);
        circuit.getSlotData(comparator).comparatorSubtract = subtract;
        circuit.setComponent(slot(4, 4), RedstoneComponent.WIRE);
        circuit.setComponent(slot(4, 5), RedstoneComponent.REDSTONE_BLOCK);

        int sideComparator = slot(5, 3);
        circuit.setComponent(sideComparator, RedstoneComponent.COMPARATOR);
        circuit.setRotation(sideComparator, 3);
        circuit.setComponent(slot(6, 3), RedstoneComponent.WIRE);
        circuit.setComponent(slot(7, 3), RedstoneComponent.WIRE);
        circuit.setComponent(slot(8, 3), RedstoneComponent.REDSTONE_BLOCK);
        return circuit;
    }

    private static ItemStack createRedstoneModule(HolderLookup.Provider registries, RedstoneCircuit circuit) {
        ItemStack moduleStack = new ItemStack(ModItems.REDSTONE_MODULE.get());
        ItemStackHandler inventory = RedstoneModuleItem.getRedstoneInventory(moduleStack, registries);
        for (int slot = 0; slot < RedstoneCircuit.GRID_SIZE; slot++) {
            RedstoneComponent component = circuit.getComponent(slot);
            if (component != null) {
                inventory.setStackInSlot(slot, new ItemStack(componentItem(component)));
            }
        }
        circuit.toStack(moduleStack);
        RedstoneModuleItem.saveRedstoneInventory(moduleStack, inventory, registries);
        return moduleStack;
    }

    private static ItemStack createConfiguredIoModule() {
        ItemStack moduleStack = new ItemStack(ModItems.IO_MODULE.get());
        for (RelativeSide side : RelativeSide.values()) {
            IoModuleItem.setFaceMode(moduleStack, side, IoModuleItem.FaceMode.DISABLED);
        }
        return moduleStack;
    }

    private static Item componentItem(RedstoneComponent component) {
        return switch (component) {
            case WIRE -> Items.REDSTONE;
            case REPEATER -> Items.REPEATER;
            case COMPARATOR -> Items.COMPARATOR;
            case TORCH -> Items.REDSTONE_TORCH;
            case LEVER -> Items.LEVER;
            case BUTTON -> Items.STONE_BUTTON;
            case OBSERVER -> Items.OBSERVER;
            case SIGNAL_SOURCE -> ModItems.SIGNAL_SOURCE.get();
            case REDSTONE_BLOCK -> Items.REDSTONE_BLOCK;
            case WALL -> ModItems.SCRAP_BLOCK_ITEM.get();
        };
    }

    private static int slot(int x, int y) {
        return y * RedstoneCircuit.GRID_WIDTH + x;
    }

    private static int edgeSlot(Direction direction) {
        return switch (direction) {
            case NORTH -> slot(RedstoneCircuit.GRID_WIDTH / 2, 0);
            case SOUTH -> slot(RedstoneCircuit.GRID_WIDTH / 2, RedstoneCircuit.GRID_HEIGHT - 1);
            case WEST -> slot(0, RedstoneCircuit.GRID_HEIGHT / 2);
            case EAST -> slot(RedstoneCircuit.GRID_WIDTH - 1, RedstoneCircuit.GRID_HEIGHT / 2);
            default -> throw new IllegalArgumentException("Expected a horizontal direction");
        };
    }

    private static int adjacentSlot(int index, Direction direction) {
        int x = index % RedstoneCircuit.GRID_WIDTH;
        int y = index / RedstoneCircuit.GRID_WIDTH;
        return switch (direction) {
            case NORTH -> slot(x, y - 1);
            case SOUTH -> slot(x, y + 1);
            case WEST -> slot(x - 1, y);
            case EAST -> slot(x + 1, y);
            default -> throw new IllegalArgumentException("Expected a horizontal direction");
        };
    }

    private static int rotationFor(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException("Expected a horizontal direction");
        };
    }

    private static CompoundTag savedSlot(int index, RedstoneComponent component) {
        CompoundTag slotTag = new CompoundTag();
        slotTag.putInt("Index", index);
        slotTag.putInt("Type", component.getId());
        return slotTag;
    }

    private static Direction[] horizontalDirections() {
        return new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    }

    private static void evaluateCircuitAt(RedstoneCircuit circuit, long gameTime) {
        evaluateCircuitAt(circuit, gameTime, null);
    }

    private static void evaluateCircuitAt(RedstoneCircuit circuit, long gameTime, int[] inputSignals) {
        circuit.propagateSignals(inputSignals);
        while (true) {
            long repeaterTickTime = circuit.getNextRepeaterTickTime();
            long comparatorTickTime = circuit.getNextComparatorTickTime();
            long torchTickTime = circuit.getNextTorchTickTime();
            long nextTickTime = minScheduledTime(repeaterTickTime, comparatorTickTime, torchTickTime);
            if (nextTickTime < 0 || nextTickTime > gameTime) break;
            boolean outputChanged = circuit.processRepeaterTicksAt(nextTickTime);
            outputChanged |= circuit.processComparatorTicksAt(nextTickTime);
            outputChanged |= circuit.processTorchTicksAt(nextTickTime);
            if (outputChanged) {
                circuit.propagateSignals(inputSignals, false);
            }
        }
        circuit.scheduleRepeaterTransitions(gameTime);
        circuit.scheduleComparatorTransitions(gameTime);
        circuit.scheduleTorchTransitions(gameTime);
    }

    private static long minScheduledTime(long... scheduledTimes) {
        long earliest = Long.MAX_VALUE;
        for (long scheduledTime : scheduledTimes) {
            if (scheduledTime >= 0) earliest = Math.min(earliest, scheduledTime);
        }
        return earliest == Long.MAX_VALUE ? -1L : earliest;
    }

    private static void assertMachineComparatorOutput(GameTestHelper helper, MachineFrameworkBlockEntity machine,
                                                      HolderLookup.Provider registries, int comparator,
                                                      int expected, String message) {
        RedstoneCircuit circuit = RedstoneCircuit.fromStack(machine.getModuleHandler().getStackInSlot(0), registries);
        assertOutput(helper, circuit, comparator, expected, message);
    }

    private static void assertSignal(GameTestHelper helper, RedstoneCircuit circuit,
                                     int slot, int expected, String message) {
        int actual = circuit.getSignalStrength(slot);
        if (actual != expected) {
            helper.fail(message + "; expected " + expected + " but got " + actual);
        }
    }

    private static void assertModuleOutput(GameTestHelper helper, MachineFrameworkBlockEntity machine,
                                           int moduleSlot, int channel, int expected, String message) {
        int actual = machine.getModuleData(moduleSlot, channel);
        if (actual != expected) {
            helper.fail(message + "; expected " + expected + " but got " + actual);
        }
    }

    private static void assertWireConnected(GameTestHelper helper, RedstoneCircuit circuit,
                                            int slot, Direction direction, boolean expected, String message) {
        boolean actual = circuit.isWireConnected(slot, direction);
        if (actual != expected) {
            helper.fail(message + "; expected connected=" + expected + " but was " + actual);
        }
    }

    private static void assertOutput(GameTestHelper helper, RedstoneCircuit circuit,
                                     int slot, int expected, String message) {
        RedstoneSlotData data = circuit.getSlotData(slot);
        int actual = data == null ? 0 : data.outputSignal;
        if (actual != expected) {
            helper.fail(message + "; expected " + expected + " but got " + actual);
        }
    }

    private static void assertRepeaterLocked(GameTestHelper helper, RedstoneCircuit circuit,
                                             int slot, boolean expected, String message) {
        RedstoneSlotData data = circuit.getSlotData(slot);
        boolean actual = data != null && data.repeaterLocked;
        if (actual != expected) {
            helper.fail(message + "; expected locked=" + expected + " but was " + actual);
        }
    }

}