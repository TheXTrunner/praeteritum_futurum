package com.thextrunner.praeteritum_futurum.gametest;

import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public class ProcessingModuleGameTests {
    private static final String TEST_TEMPLATE = "ruined_outpost";
    private static final String TEST_NAMESPACE = "praeteritum_futurum";

    @GameTest(template = TEST_TEMPLATE, templateNamespace = TEST_NAMESPACE)
    public static void furnaceFuelModeIsLimitedToOneDirection(GameTestHelper helper) {
        Item[] furnaceModules = {
                ModItems.SMELTING_MODULE.get(),
                ModItems.BLAST_MODULE.get(),
                ModItems.SMOKER_MODULE.get()
        };

        for (Item item : furnaceModules) {
            if (!(item instanceof ProcessingModuleItem module)) {
                helper.fail("Furnace modules should use the shared processing module behavior");
                return;
            }

            ProcessingMode[] modes = module.getSupportedModes();
            if (modes.length != 4 || modes[2].getRole() != ProcessingMode.Role.OTHER
                    || !modes[2].getName().equals("FUEL")) {
                helper.fail("Furnace fuel modes should remain module-specific rather than use a shared role");
                return;
            }

            ItemStack stack = new ItemStack(item);
            String configKey = module.getModuleConfigKey();
            ProcessingModuleItem.setSlotMode(stack, GridDirection.UP, modes[1], configKey);
            ProcessingModuleItem.setSlotMode(stack, GridDirection.LEFT, modes[2], configKey);

            ProcessingMode nextMode = module.getCycledMode(stack, GridDirection.RIGHT, false);
            if (nextMode != modes[3]) {
                helper.fail("Each furnace module should enforce its own one-slot fuel limit");
                return;
            }
        }

        ProcessingModuleItem brewingModule = ModItems.BREWING_MODULE.get();
        ProcessingMode[] brewingModes = brewingModule.getSupportedModes();
        if (brewingModes.length != 3 || brewingModes[2].getRole() != ProcessingMode.Role.OTHER
                || !brewingModes[2].getName().equals("INGREDIENT")) {
            helper.fail("Brewing ingredient mode should remain defined by the Brewing Module");
            return;
        }

        ItemStack brewingStack = new ItemStack(ModItems.BREWING_MODULE.get());
        String brewingConfigKey = brewingModule.getModuleConfigKey();
        ProcessingModuleItem.setSlotMode(brewingStack, GridDirection.UP, brewingModes[1], brewingConfigKey);
        ProcessingModuleItem.setSlotMode(brewingStack, GridDirection.LEFT, brewingModes[2], brewingConfigKey);
        if (brewingModule.getCycledMode(brewingStack, GridDirection.UP, false) != brewingModes[0]) {
            helper.fail("Brewing should enforce its own one-slot ingredient limit during cycling");
            return;
        }

        helper.succeed();
    }
}