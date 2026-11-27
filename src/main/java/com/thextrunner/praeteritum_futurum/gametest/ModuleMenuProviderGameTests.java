package com.thextrunner.praeteritum_futurum.gametest;

import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.ModMenuTypes;
import com.thextrunner.praeteritum_futurum.screen.RedstoneConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

@PrefixGameTestTemplate(false)
public class ModuleMenuProviderGameTests {
    private static final String TEST_TEMPLATE = "ruined_outpost";
    private static final String TEST_NAMESPACE = "praeteritum_futurum";

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void genericMenuDelegatesFilterPersistenceToProvider(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        ItemStack moduleStack = new ItemStack(ModItems.FILTER_MODULE.get());
        machine.getModuleHandler().setStackInSlot(0, moduleStack);
        GenericModuleConfigMenu menu = new GenericModuleConfigMenu(
                0, player.getInventory(), moduleStack, 0, machinePos);
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND));

        menu.removed(player);

        ItemStack savedModule = machine.getModuleHandler().getStackInSlot(0);
        ItemStackHandler savedInventory = FilterModuleItem.getFilterInventory(
                savedModule, helper.getLevel().registryAccess());
        if (!savedInventory.getStackInSlot(0).is(Items.DIAMOND)) {
            helper.fail("Closing the generic menu should let the Filter Module save its current slot contents");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void genericMenuDelegatesRedstonePersistenceToProvider(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos machinePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.MACHINE_FRAMEWORK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(machinePos) instanceof MachineFrameworkBlockEntity machine)) {
            helper.fail("Machine framework block must create its block entity");
            return;
        }

        ItemStack moduleStack = new ItemStack(ModItems.REDSTONE_MODULE.get());
        machine.getModuleHandler().setStackInSlot(0, moduleStack);
        RedstoneConfigMenu menu = new RedstoneConfigMenu(
                ModMenuTypes.REDSTONE_MODULE_CONFIG_MENU.get(),
                0, player.getInventory(), moduleStack, 0, machinePos);
        menu.slots.get(RedstoneCircuit.GRID_SIZE - 1).set(new ItemStack(Items.REDSTONE_TORCH));

        menu.removed(player);

        ItemStack savedModule = machine.getModuleHandler().getStackInSlot(0);
        ItemStackHandler savedInventory = RedstoneModuleItem.getRedstoneInventory(
                savedModule, helper.getLevel().registryAccess());
        if (!savedInventory.getStackInSlot(RedstoneCircuit.GRID_SIZE - 1).is(Items.REDSTONE_TORCH)) {
            helper.fail("Closing the generic menu should let the Redstone Module save its current grid contents");
            return;
        }

        helper.succeed();
    }
}