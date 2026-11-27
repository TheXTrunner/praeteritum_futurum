package com.thextrunner.praeteritum_futurum.gametest;

import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.item.custom.BlueprintItem;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneComponent;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneSlotData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;

@PrefixGameTestTemplate(false)
public class BlueprintItemGameTests {
    private static final String TEST_TEMPLATE = "ruined_outpost";
    private static final String TEST_NAMESPACE = "praeteritum_futurum";

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void blueprintCountsEmbeddedRedstoneComponentsAndOnlyReportsDeficits(GameTestHelper helper) {
        RedstoneModuleItem redstoneModule = ModItems.REDSTONE_MODULE.get();
        ItemStack redstoneStack = new ItemStack(redstoneModule);
        ItemStackHandler redstoneInventory = RedstoneModuleItem.getRedstoneInventory(
                redstoneStack, helper.getLevel().registryAccess());
        redstoneInventory.setStackInSlot(0, new ItemStack(Items.REDSTONE, 3));
        redstoneInventory.setStackInSlot(1, new ItemStack(Items.REPEATER));
        RedstoneModuleItem.saveRedstoneInventory(redstoneStack, redstoneInventory,
                helper.getLevel().registryAccess());

        ListTag layout = new ListTag();
        CompoundTag redstoneSlot = createModuleSlot(2, redstoneStack);
        layout.add(redstoneSlot);
        layout.add(createModuleSlot(4, new ItemStack(ModItems.SMELTING_MODULE.get())));

        BlueprintItem.MaterialRequirements requirements = BlueprintItem.getMaterialRequirements(
                layout, helper.getLevel().registryAccess());
        if (requirements == null
                || requirements.modules().getOrDefault(redstoneModule, 0) != 1
                || requirements.modules().getOrDefault(ModItems.SMELTING_MODULE.get(), 0) != 1
                || requirements.components().getOrDefault(Items.REDSTONE, 0) != 3
                || requirements.components().getOrDefault(Items.REPEATER, 0) != 1) {
            helper.fail("Blueprint requirements should include modules and the actual embedded component counts");
            return;
        }

        Map<Item, Integer> available = Map.of(
                redstoneModule, 1,
                ModItems.SMELTING_MODULE.get(), 1,
                Items.REDSTONE, 2,
                Items.REPEATER, 1);
        Map<Item, Integer> cleanModules = Map.of(redstoneModule, 1, ModItems.SMELTING_MODULE.get(), 1);
        Map<Item, Integer> missing = BlueprintItem.getMissingMaterials(requirements, available, cleanModules);
        if (missing.size() != 1 || missing.getOrDefault(Items.REDSTONE, 0) != 1) {
            helper.fail("The missing-material summary should report only the one redstone dust shortage");
            return;
        }

        Map<Item, Integer> allAvailable = Map.of(
                redstoneModule, 1,
                ModItems.SMELTING_MODULE.get(), 1,
                Items.REDSTONE, 3,
                Items.REPEATER, 1);
        if (!BlueprintItem.getMissingMaterials(requirements, allAvailable, cleanModules).isEmpty()) {
            helper.fail("A complete material inventory should have no reported deficits");
            return;
        }

        Map<Item, Integer> noCleanModules = Map.of(Items.REDSTONE, 3, Items.REPEATER, 1,
                redstoneModule, 1, ModItems.SMELTING_MODULE.get(), 1);
        if (BlueprintItem.getMissingMaterials(requirements, noCleanModules, Map.of()).getOrDefault(redstoneModule, 0) != 1
                || BlueprintItem.getMissingMaterials(requirements, noCleanModules, Map.of())
                .getOrDefault(ModItems.SMELTING_MODULE.get(), 0) != 1) {
            helper.fail("Configured module items must not count as clean modules available for blueprint pasting");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void redstoneBlueprintPreservesDesignButClearsRuntimeState(GameTestHelper helper) {
        RedstoneModuleItem module = ModItems.REDSTONE_MODULE.get();
        ItemStack sourceStack = new ItemStack(module);
        ItemStackHandler inventory = RedstoneModuleItem.getRedstoneInventory(
                sourceStack, helper.getLevel().registryAccess());
        inventory.setStackInSlot(0, new ItemStack(Items.REPEATER));
        inventory.setStackInSlot(1, new ItemStack(Items.COMPARATOR));
        inventory.setStackInSlot(2, new ItemStack(Items.REDSTONE_TORCH));
        inventory.setStackInSlot(3, new ItemStack(Items.LEVER));
        inventory.setStackInSlot(4, new ItemStack(Items.OAK_BUTTON));
        inventory.setStackInSlot(5, new ItemStack(Items.OBSERVER));
        inventory.setStackInSlot(6, new ItemStack(ModItems.SIGNAL_SOURCE.get()));

        RedstoneCircuit circuit = new RedstoneCircuit();
        circuit.setComponent(0, RedstoneComponent.REPEATER);
        RedstoneSlotData repeater = circuit.getSlotData(0);
        repeater.rotation = 2;
        repeater.repeaterDelay = 4;
        repeater.repeaterPowered = true;
        repeater.repeaterScheduledGameTime = 120;
        repeater.repeaterLocked = true;
        repeater.repeaterLockOutput = true;

        circuit.setComponent(1, RedstoneComponent.COMPARATOR);
        RedstoneSlotData comparator = circuit.getSlotData(1);
        comparator.rotation = 3;
        comparator.comparatorSubtract = true;
        comparator.outputSignal = 12;
        comparator.comparatorScheduledGameTime = 121;

        circuit.setComponent(2, RedstoneComponent.TORCH);
        RedstoneSlotData torch = circuit.getSlotData(2);
        torch.torchRotation = 1;
        torch.torchPowered = false;
        torch.torchScheduledGameTime = 122;
        torch.torchBurnedOut = true;
        torch.torchBurnoutUntil = 280;
        torch.torchRecentOffTimes = new long[]{100, 110};

        circuit.setComponent(3, RedstoneComponent.LEVER);
        RedstoneSlotData lever = circuit.getSlotData(3);
        lever.leverOn = true;
        lever.leverRotation = 0;

        circuit.setComponent(4, RedstoneComponent.BUTTON);
        RedstoneSlotData button = circuit.getSlotData(4);
        button.buttonPowered = true;
        button.buttonScheduledGameTime = 123;
        button.buttonDurationGameTicks = 30;
        button.buttonRotation = 2;

        circuit.setComponent(5, RedstoneComponent.OBSERVER);
        RedstoneSlotData observer = circuit.getSlotData(5);
        observer.rotation = 1;
        observer.observerPowered = true;
        observer.observerScheduledGameTime = 124;
        observer.observerLastObservedRotation = 0;
        observer.observerObservedStateSignature = 54321;

        circuit.setComponent(6, RedstoneComponent.SIGNAL_SOURCE);
        circuit.getSlotData(6).signalSourceStrength = 9;
        circuit.toStack(sourceStack);
        RedstoneModuleItem.saveRedstoneInventory(sourceStack, inventory, helper.getLevel().registryAccess());

        CompoundTag sourceData = sourceStack.get(DataComponents.CUSTOM_DATA).copyTag();
        CompoundTag blueprintData = module.getBlueprintData(sourceStack);
        if (!blueprintData.contains("RedstoneInventory")
                || !sourceData.getCompound("Circuit").getList("Slots", Tag.TAG_COMPOUND)
                .getCompound(0).getBoolean("RepeaterPowered")) {
            helper.fail("Copying a blueprint must keep the original circuit and embedded inventory untouched");
            return;
        }

        RedstoneCircuit restored = RedstoneCircuit.deserializeNBT(blueprintData.getCompound("Circuit"));
        RedstoneSlotData restoredRepeater = restored.getSlotData(0);
        RedstoneSlotData restoredComparator = restored.getSlotData(1);
        RedstoneSlotData restoredTorch = restored.getSlotData(2);
        RedstoneSlotData restoredLever = restored.getSlotData(3);
        RedstoneSlotData restoredButton = restored.getSlotData(4);
        RedstoneSlotData restoredObserver = restored.getSlotData(5);
        RedstoneSlotData restoredSource = restored.getSlotData(6);

        if (restoredRepeater.rotation != 2 || restoredRepeater.repeaterDelay != 4
                || restoredRepeater.repeaterPowered || restoredRepeater.repeaterScheduledGameTime != -1
                || restoredRepeater.repeaterLocked || restoredRepeater.repeaterLockOutput
                || restoredComparator.rotation != 3 || !restoredComparator.comparatorSubtract
                || restoredComparator.outputSignal != 0 || restoredComparator.comparatorScheduledGameTime != -1
                || restoredTorch.torchRotation != 1 || !restoredTorch.torchPowered
                || restoredTorch.torchScheduledGameTime != -1 || restoredTorch.torchBurnedOut
                || restoredTorch.torchBurnoutUntil != -1 || restoredTorch.torchRecentOffTimes.length != 0
                || !restoredLever.leverOn || restoredLever.leverRotation != 0
                || restoredButton.buttonPowered || restoredButton.buttonScheduledGameTime != -1
                || restoredButton.buttonDurationGameTicks != 30 || restoredButton.buttonRotation != 2
                || restoredObserver.rotation != 1 || restoredObserver.observerPowered
                || restoredObserver.observerScheduledGameTime != -1 || restoredObserver.observerLastObservedRotation != -1
                || restoredObserver.observerObservedStateSignature != Long.MIN_VALUE
                || restoredSource.signalSourceStrength != 9) {
            helper.fail("Blueprint redstone data should preserve design settings and reset transient runtime state");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void cleaningConfiguredRedstoneModuleReturnsEmbeddedComponents(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack configuredModule = new ItemStack(ModItems.REDSTONE_MODULE.get());
        ItemStackHandler components = RedstoneModuleItem.getRedstoneInventory(
                configuredModule, helper.getLevel().registryAccess());
        components.setStackInSlot(0, new ItemStack(Items.REDSTONE, 3));
        components.setStackInSlot(1, new ItemStack(Items.REPEATER));
        RedstoneModuleItem.saveRedstoneInventory(configuredModule, components,
                helper.getLevel().registryAccess());

        SimpleContainer craftingInput = new SimpleContainer(4);
        craftingInput.setItem(0, configuredModule);
        ItemStack cleanedModule = new ItemStack(ModItems.REDSTONE_MODULE.get());
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, cleanedModule, craftingInput));

        if (countInventoryItem(player, Items.REDSTONE) != 3
                || countInventoryItem(player, Items.REPEATER) != 1) {
            helper.fail("Data cleaning must return every component stored inside the Redstone Module");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void cleaningConfiguredRedstoneModuleDropsComponentsWhenInventoryIsFull(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        ItemStack configuredModule = new ItemStack(ModItems.REDSTONE_MODULE.get());
        ItemStackHandler components = RedstoneModuleItem.getRedstoneInventory(
                configuredModule, helper.getLevel().registryAccess());
        components.setStackInSlot(0, new ItemStack(Items.REDSTONE, 3));
        components.setStackInSlot(1, new ItemStack(Items.REPEATER));
        RedstoneModuleItem.saveRedstoneInventory(configuredModule, components,
                helper.getLevel().registryAccess());

        SimpleContainer craftingInput = new SimpleContainer(4);
        craftingInput.setItem(0, configuredModule);
        ItemStack cleanedModule = new ItemStack(ModItems.REDSTONE_MODULE.get());
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, cleanedModule, craftingInput));

        AABB searchBounds = new AABB(player.blockPosition()).inflate(4);
        List<ItemEntity> droppedItems = helper.getLevel().getEntitiesOfClass(ItemEntity.class, searchBounds);
        int droppedDust = droppedItems.stream()
                .filter(itemEntity -> itemEntity.getItem().is(Items.REDSTONE))
                .mapToInt(itemEntity -> itemEntity.getItem().getCount())
                .sum();
        int droppedRepeaters = droppedItems.stream()
                .filter(itemEntity -> itemEntity.getItem().is(Items.REPEATER))
                .mapToInt(itemEntity -> itemEntity.getItem().getCount())
                .sum();
        if (droppedDust != 3 || droppedRepeaters != 1) {
            helper.fail("When inventory is full, Data Cleaning must drop all embedded components nearby");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void survivalBlueprintPasteConsumesModulesAndEmbeddedComponents(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MachineFrameworkBlockEntity machine = placeEmptyMachine(helper, player, new BlockPos(1, 1, 1));
        if (machine == null) return;
        ItemStack blueprint = createRedstoneBlueprint(helper, 3);
        player.getInventory().add(new ItemStack(ModItems.REDSTONE_MODULE.get()));
        player.getInventory().add(new ItemStack(Items.REDSTONE, 3));
        player.getInventory().add(new ItemStack(Items.REPEATER));
        player.setItemInHand(InteractionHand.OFF_HAND, blueprint);

        BlockPos machinePos = machine.getBlockPos();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(machinePos), Direction.UP, machinePos, false);
        ModItems.BLUEPRINT.get().useOn(new UseOnContext(player, InteractionHand.OFF_HAND, hit));

        ItemStack pastedModule = machine.getModuleHandler().getStackInSlot(0);
        if (pastedModule.isEmpty() || !pastedModule.is(ModItems.REDSTONE_MODULE.get())
                || countInventoryItem(player, ModItems.REDSTONE_MODULE.get()) != 0
                || countInventoryItem(player, Items.REDSTONE) != 0
                || countInventoryItem(player, Items.REPEATER) != 0) {
            helper.fail("Successful paste failed: installed=" + BuiltInRegistries.ITEM.getKey(pastedModule.getItem())
                    + ", playerCreative=" + player.isCreative()
                    + ", remaining modules=" + countInventoryItem(player, ModItems.REDSTONE_MODULE.get())
                    + ", dust=" + countInventoryItem(player, Items.REDSTONE)
                    + ", repeaters=" + countInventoryItem(player, Items.REPEATER));
            return;
        }

        ItemStackHandler pastedInventory = RedstoneModuleItem.getRedstoneInventory(
                pastedModule, helper.getLevel().registryAccess());
        if (pastedInventory.getStackInSlot(0).getCount() != 3
                || !pastedInventory.getStackInSlot(0).is(Items.REDSTONE)
                || !pastedInventory.getStackInSlot(1).is(Items.REPEATER)) {
            helper.fail("Successful paste should restore the saved redstone component stacks");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void insufficientBlueprintMaterialsDoNotConsumeAnything(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MachineFrameworkBlockEntity machine = placeEmptyMachine(helper, player, new BlockPos(1, 1, 1));
        if (machine == null) return;
        player.getInventory().add(new ItemStack(ModItems.REDSTONE_MODULE.get()));
        player.getInventory().add(new ItemStack(Items.REDSTONE, 2));
        player.getInventory().add(new ItemStack(Items.REPEATER));
        player.setItemInHand(InteractionHand.OFF_HAND, createRedstoneBlueprint(helper, 3));

        BlockPos machinePos = machine.getBlockPos();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(machinePos), Direction.UP, machinePos, false);
        ModItems.BLUEPRINT.get().useOn(new UseOnContext(player, InteractionHand.OFF_HAND, hit));

        if (!machine.getModuleHandler().getStackInSlot(0).isEmpty()
                || countInventoryItem(player, ModItems.REDSTONE_MODULE.get()) != 1
                || countInventoryItem(player, Items.REDSTONE) != 2
                || countInventoryItem(player, Items.REPEATER) != 1) {
            helper.fail("Rejected paste changed state: installed="
                    + BuiltInRegistries.ITEM.getKey(machine.getModuleHandler().getStackInSlot(0).getItem())
                    + ", playerCreative=" + player.isCreative()
                    + ", modules=" + countInventoryItem(player, ModItems.REDSTONE_MODULE.get())
                    + ", dust=" + countInventoryItem(player, Items.REDSTONE)
                    + ", repeaters=" + countInventoryItem(player, Items.REPEATER));
            return;
        }

        helper.succeed();
    }

    private static MachineFrameworkBlockEntity placeEmptyMachine(GameTestHelper helper, Player player, BlockPos relativePos) {
        BlockPos machinePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Expected a Machine Framework block entity for blueprint application");
            return null;
        }
        machine.setOwnerUUID(player.getUUID().toString());
        return machine;
    }

    private static ItemStack createRedstoneBlueprint(GameTestHelper helper, int dustCount) {
        ItemStack redstoneModule = new ItemStack(ModItems.REDSTONE_MODULE.get());
        ItemStackHandler inventory = RedstoneModuleItem.getRedstoneInventory(
                redstoneModule, helper.getLevel().registryAccess());
        inventory.setStackInSlot(0, new ItemStack(Items.REDSTONE, dustCount));
        inventory.setStackInSlot(1, new ItemStack(Items.REPEATER));
        RedstoneModuleItem.saveRedstoneInventory(redstoneModule, inventory, helper.getLevel().registryAccess());

        ListTag layout = new ListTag();
        layout.add(createModuleSlot(0, redstoneModule));
        ItemStack blueprint = new ItemStack(ModItems.BLUEPRINT.get());
        CustomData.update(DataComponents.CUSTOM_DATA, blueprint, tag -> tag.put("BlueprintLayout", layout));
        return blueprint;
    }

    private static int countInventoryItem(Player player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static CompoundTag createModuleSlot(int slot, ItemStack moduleStack) {
        CompoundTag slotTag = new CompoundTag();
        slotTag.putInt("Slot", slot);
        slotTag.putString("Item", BuiltInRegistries.ITEM.getKey(moduleStack.getItem()).toString());
        CustomData customData = moduleStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) slotTag.put("CustomData", customData.copyTag());
        return slotTag;
    }
}