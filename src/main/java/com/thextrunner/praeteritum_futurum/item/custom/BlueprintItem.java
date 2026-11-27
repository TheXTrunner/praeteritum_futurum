package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blueprint — a utility item that copies and pastes Machine Framework configurations.
 * <p>
 * Usage:
 * <ul>
 *   <li>Shift + Right-Click a configured machine to copy its module layout to the blueprint</li>
 *   <li>Right-Click an empty machine to paste the saved configuration</li>
 * </ul>
 * <p>
 * When pasting in Survival, the blueprint consumes clean modules and every redstone
 * component stored inside its Redstone Modules. Creative players pay no material cost.
 * The target machine must be empty and the player must have permission. A crafting table
 * recipe clears the blueprint's data.
 * <p>
 * Stores module types and their CustomData in its own NBT under "BlueprintLayout".
 * The normal tooltip shows compact counts; holding Shift lists only missing materials.
 */
public class BlueprintItem extends Item {
    private static final int MODULE_SLOT_COUNT = MachineFrameworkBlockEntity.GRID_SIZE;
    private static final String BLUEPRINT_LAYOUT_KEY = "BlueprintLayout";

    public record MaterialRequirements(Map<Item, Integer> modules, Map<Item, Integer> components) {}

    /**
     * Constructs the Blueprint item with the given properties.
     *
     * @param properties The item properties (typically stacksTo(1))
     */
    public BlueprintItem(Properties properties) {
        super(properties);
    }

    /**
     * Handles right-clicking a block with the blueprint.
     * Shift + Right-Click copies the machine's configuration.
     * Right-Click pastes the saved configuration to an empty machine.
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack blueprintStack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFrameworkBlockEntity machine)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            boolean isSneaking = player.isShiftKeyDown();

            if (isSneaking) {
                copyMachineToBlueprint(machine, blueprintStack, player);
            } else {
                pasteBlueprintToMachine(machine, blueprintStack, player);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void copyMachineToBlueprint(MachineFrameworkBlockEntity machine, ItemStack blueprint, Player player) {
        ListTag slotList = new ListTag();
        int moduleCount = 0;

        for (int i = 0; i < MODULE_SLOT_COUNT; i++) {
            ItemStack module = machine.getModuleHandler().getStackInSlot(i);
            if (!module.isEmpty()) {
                moduleCount++;
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                slotTag.putString("Item", BuiltInRegistries.ITEM.getKey(module.getItem()).toString());

                CustomData customData = module.get(DataComponents.CUSTOM_DATA);
                if (customData != null && module.getItem() instanceof BaseModuleItem baseModule) {
                    CompoundTag cleanTag = baseModule.getBlueprintData(module);
                    if (cleanTag != null && !cleanTag.isEmpty()) {
                        slotTag.put("CustomData", cleanTag);
                    }
                }

                slotList.add(slotTag);
            }
        }

        if (moduleCount == 0) {
            player.displayClientMessage(Component.literal("§cCannot copy: Targeted machine framework is empty!"), true);
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, blueprint, tag -> tag.put(BLUEPRINT_LAYOUT_KEY, slotList));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(Component.literal("§aBlueprint copied! Recorded " + moduleCount + " modules."), true);
    }

    private void pasteBlueprintToMachine(MachineFrameworkBlockEntity machine, ItemStack blueprint, Player player) {
        // Check ownership — only the owner (or ops) can paste blueprints on a private machine
        if (!machine.canModify(player)) {
            player.displayClientMessage(Component.literal("§cThis machine is locked! Only the owner can apply blueprints."), true);
            return;
        }

        CustomData customData = blueprint.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.copyTag().contains(BLUEPRINT_LAYOUT_KEY, Tag.TAG_LIST)) {
            player.displayClientMessage(Component.literal("§cBlueprint is blank! Shift + Right-Click a configured machine first."), true);
            return;
        }

        boolean hasModulesInstalled = false;
        for (int i = 0; i < MODULE_SLOT_COUNT; i++) {
            if (!machine.getModuleHandler().getStackInSlot(i).isEmpty()) {
                hasModulesInstalled = true;
                break;
            }
        }

        if (hasModulesInstalled) {
            player.displayClientMessage(Component.literal("§cTarget Machine Framework must be empty to apply a blueprint!"), true);
            return;
        }

        ListTag slotList = customData.copyTag().getList(BLUEPRINT_LAYOUT_KEY, Tag.TAG_COMPOUND);
        if (slotList.isEmpty()) return;

        MaterialRequirements requirements = getMaterialRequirements(slotList, player.registryAccess());
        if (requirements == null) {
            player.displayClientMessage(Component.literal("§cBlueprint data is invalid and cannot be applied."), true);
            return;
        }

        if (!player.isCreative()) {
            Map<Item, Integer> missing = getMissingMaterials(requirements,
                    countItemsInInventory(player), countCleanModulesInInventory(player));
            if (!missing.isEmpty()) {
                player.displayClientMessage(Component.literal("§cMissing materials: " + formatMaterials(missing)), true);
                return;
            }
        }

        if (!player.isCreative()) {
            consumeMaterials(player, requirements);
        }

        for (int i = 0; i < slotList.size(); i++) {
            CompoundTag slotTag = slotList.getCompound(i);
            int slotIndex = slotTag.getInt("Slot");
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slotTag.getString("Item")));

            ItemStack newModuleStack = new ItemStack(item);
            if (slotTag.contains("CustomData")) {
                CompoundTag moduleData = slotTag.getCompound("CustomData").copy();
                if (item instanceof RedstoneModuleItem redstoneModule) {
                    ItemStack dataCarrier = new ItemStack(redstoneModule);
                    dataCarrier.set(DataComponents.CUSTOM_DATA, CustomData.of(moduleData));
                    moduleData = redstoneModule.getBlueprintData(dataCarrier);
                }
                newModuleStack.set(DataComponents.CUSTOM_DATA, CustomData.of(moduleData));
                newModuleStack.set(DataComponents.MAX_STACK_SIZE, 1);
            }

            machine.getModuleHandler().setStackInSlot(slotIndex, newModuleStack);
        }

        // Force container synchronization so client UI menus update instantly without reopening
        machine.setChanged();
        Level level = machine.getLevel();
        if (level != null) {
            BlockState state = level.getBlockState(machine.getBlockPos());
            level.sendBlockUpdated(machine.getBlockPos(), state, state, 3);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }
        }

        player.level().playSound(null, machine.getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.1F);
        player.displayClientMessage(Component.literal("§aBlueprint applied successfully!"), true);
    }

    public static MaterialRequirements getMaterialRequirements(ListTag slotList, HolderLookup.Provider registries) {
        Map<Item, Integer> modules = new HashMap<>();
        Map<Item, Integer> components = new HashMap<>();
        Set<Integer> occupiedSlots = new HashSet<>();

        for (int i = 0; i < slotList.size(); i++) {
            CompoundTag slotTag = slotList.getCompound(i);
            if (!slotTag.contains("Slot", Tag.TAG_INT) || !slotTag.contains("Item", Tag.TAG_STRING)) return null;

            int slotIndex = slotTag.getInt("Slot");
            ResourceLocation itemId = ResourceLocation.tryParse(slotTag.getString("Item"));
            if (slotIndex < 0 || slotIndex >= MODULE_SLOT_COUNT || !occupiedSlots.add(slotIndex)
                    || itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                return null;
            }

            Item moduleItem = BuiltInRegistries.ITEM.get(itemId);
            if (!(moduleItem instanceof BaseModuleItem)) return null;
            modules.merge(moduleItem, 1, Integer::sum);

            if (moduleItem instanceof RedstoneModuleItem && slotTag.contains("CustomData", Tag.TAG_COMPOUND)) {
                ItemStack moduleStack = new ItemStack(moduleItem);
                moduleStack.set(DataComponents.CUSTOM_DATA, CustomData.of(slotTag.getCompound("CustomData")));
                ItemStackHandler redstoneInventory = RedstoneModuleItem.getRedstoneInventory(moduleStack, registries);
                for (int componentSlot = 0; componentSlot < redstoneInventory.getSlots(); componentSlot++) {
                    ItemStack componentStack = redstoneInventory.getStackInSlot(componentSlot);
                    if (!componentStack.isEmpty()) {
                        components.merge(componentStack.getItem(), componentStack.getCount(), Integer::sum);
                    }
                }
            }
        }

        return new MaterialRequirements(modules, components);
    }

    public static Map<Item, Integer> getMissingMaterials(MaterialRequirements requirements,
                                                          Map<Item, Integer> availableItems,
                                                          Map<Item, Integer> availableCleanModules) {
        Map<Item, Integer> requiredItems = new HashMap<>(requirements.modules());
        requirements.components().forEach((item, count) -> requiredItems.merge(item, count, Integer::sum));
        Map<Item, Integer> missing = new HashMap<>();

        for (Map.Entry<Item, Integer> entry : requiredItems.entrySet()) {
            Item item = entry.getKey();
            int available = requirements.modules().containsKey(item)
                    ? availableCleanModules.getOrDefault(item, 0)
                    : availableItems.getOrDefault(item, 0);
            int shortage = entry.getValue() - available;
            if (shortage > 0) missing.put(item, shortage);
        }
        return missing;
    }

    static Map<Item, Integer> countItemsInInventory(Player player) {
        Map<Item, Integer> itemCounts = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return itemCounts;
    }

    static Map<Item, Integer> countCleanModulesInInventory(Player player) {
        Map<Item, Integer> cleanCounts = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isCleanModule(stack)) {
                cleanCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return cleanCounts;
    }

    private void consumeMaterials(Player player, MaterialRequirements requirements) {
        requirements.modules().forEach((item, count) -> consumeItems(player, item, count, true));
        requirements.components().forEach((item, count) ->
                consumeItems(player, item, count, requirements.modules().containsKey(item)));
        player.getInventory().setChanged();
    }

    private void consumeItems(Player player, Item targetItem, int count, boolean cleanModulesOnly) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(targetItem) && (!cleanModulesOnly || isCleanModule(stack))) {
                int consumed = Math.min(count, stack.getCount());
                stack.shrink(consumed);
                count -= consumed;
                if (count == 0) return;
            }
        }
    }

    static boolean isCleanModule(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null || customData.copyTag().isEmpty();
    }

    private static String formatMaterials(Map<Item, Integer> materials) {
        return materials.entrySet().stream()
                .map(entry -> entry.getValue() + "x " + new ItemStack(entry.getKey()).getHoverName().getString())
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.copyTag().contains(BLUEPRINT_LAYOUT_KEY, Tag.TAG_LIST)) {
            ListTag slotList = customData.copyTag().getList(BLUEPRINT_LAYOUT_KEY, Tag.TAG_COMPOUND);
            MaterialRequirements requirements = context.registries() == null
                    ? null : getMaterialRequirements(slotList, context.registries());

            if (requirements == null) {
                tooltipComponents.add(Component.literal("§cInvalid blueprint data"));
            } else {
                int moduleCount = requirements.modules().values().stream().mapToInt(Integer::intValue).sum();
                int componentCount = requirements.components().values().stream().mapToInt(Integer::intValue).sum();
                tooltipComponents.add(Component.literal("§7Stores " + moduleCount + " modules and "
                        + componentCount + " redstone components."));
                tooltipComponents.add(Component.literal("§8Hold Shift to see missing materials."));
                tooltipComponents.add(Component.literal("§8Right-click a machine to apply."));
            }
            if (FMLEnvironment.dist == Dist.CLIENT) {
                BlueprintTooltipHelper.appendMissingMaterials(requirements, tooltipComponents);
            }
            tooltipComponents.add(Component.literal("§8Use in crafting table to reset."));
        } else {
            tooltipComponents.add(Component.literal("§7Status: §cEmpty"));
            tooltipComponents.add(Component.literal("§8Shift + Right-Click a machine to copy."));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}