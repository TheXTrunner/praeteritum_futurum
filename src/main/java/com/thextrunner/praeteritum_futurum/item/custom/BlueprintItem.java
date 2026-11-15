package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlueprintItem extends Item {

    public BlueprintItem(Properties properties) {
        super(properties);
    }

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

        for (int i = 0; i < 27; i++) {
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

        CustomData.update(DataComponents.CUSTOM_DATA, blueprint, tag -> tag.put("BlueprintLayout", slotList));

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
        if (customData == null || !customData.copyTag().contains("BlueprintLayout")) {
            player.displayClientMessage(Component.literal("§cBlueprint is blank! Shift + Right-Click a configured machine first."), true);
            return;
        }

        boolean hasModulesInstalled = false;
        for (int i = 0; i < 27; i++) {
            if (!machine.getModuleHandler().getStackInSlot(i).isEmpty()) {
                hasModulesInstalled = true;
                break;
            }
        }

        if (hasModulesInstalled) {
            player.displayClientMessage(Component.literal("§cTarget Machine Framework must be empty to apply a blueprint!"), true);
            return;
        }

        ListTag slotList = customData.copyTag().getList("BlueprintLayout", Tag.TAG_COMPOUND);
        if (slotList.isEmpty()) return;

        Map<Item, Integer> requiredModules = new HashMap<>();
        for (int i = 0; i < slotList.size(); i++) {
            CompoundTag slotTag = slotList.getCompound(i);
            ResourceLocation itemId = ResourceLocation.parse(slotTag.getString("Item"));
            Item item = BuiltInRegistries.ITEM.get(itemId);
            requiredModules.put(item, requiredModules.getOrDefault(item, 0) + 1);
        }

        if (!player.isCreative()) {
            Map<Item, Integer> availableCleanModules = countCleanModulesInInventory(player);

            for (Map.Entry<Item, Integer> entry : requiredModules.entrySet()) {
                Item requiredItem = entry.getKey();
                int neededCount = entry.getValue();
                int availableCount = availableCleanModules.getOrDefault(requiredItem, 0);

                if (availableCount < neededCount) {
                    String itemName = new ItemStack(requiredItem).getHoverName().getString();
                    player.displayClientMessage(Component.literal("§cMissing clean modules: Needs " + neededCount + "x " + itemName), true);
                    return;
                }
            }
        }

        for (int i = 0; i < slotList.size(); i++) {
            CompoundTag slotTag = slotList.getCompound(i);
            int slotIndex = slotTag.getInt("Slot");
            ResourceLocation itemId = ResourceLocation.parse(slotTag.getString("Item"));
            Item item = BuiltInRegistries.ITEM.get(itemId);

            if (!player.isCreative()) {
                consumeCleanModule(player, item);
            }

            ItemStack newModuleStack = new ItemStack(item);
            if (slotTag.contains("CustomData")) {
                newModuleStack.set(DataComponents.CUSTOM_DATA, CustomData.of(slotTag.getCompound("CustomData")));
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

    private Map<Item, Integer> countCleanModulesInInventory(Player player) {
        Map<Item, Integer> cleanCounts = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isCleanModule(stack)) {
                cleanCounts.put(stack.getItem(), cleanCounts.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
        }
        return cleanCounts;
    }

    private void consumeCleanModule(Player player, Item targetItem) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(targetItem) && isCleanModule(stack)) {
                stack.shrink(1);
                break;
            }
        }
    }

    private boolean isCleanModule(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null || customData.copyTag().isEmpty();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.copyTag().contains("BlueprintLayout")) {
            ListTag slotList = customData.copyTag().getList("BlueprintLayout", Tag.TAG_COMPOUND);

            Map<Item, Integer> summary = new HashMap<>();
            for (int i = 0; i < slotList.size(); i++) {
                CompoundTag slotTag = slotList.getCompound(i);
                ResourceLocation itemId = ResourceLocation.parse(slotTag.getString("Item"));
                Item item = BuiltInRegistries.ITEM.get(itemId);
                summary.put(item, summary.getOrDefault(item, 0) + 1);
            }

            tooltipComponents.add(Component.literal("§7Stored Configuration:"));
            for (Map.Entry<Item, Integer> entry : summary.entrySet()) {
                String itemName = new ItemStack(entry.getKey()).getHoverName().getString();
                tooltipComponents.add(Component.literal("§8 • §f" + entry.getValue() + "x §e" + itemName));
            }
            tooltipComponents.add(Component.literal("§8Right-Click a machine to paste configuration."));
            tooltipComponents.add(Component.literal("§8Use in crafting table to reset."));
        } else {
            tooltipComponents.add(Component.literal("§7Status: §cEmpty"));
            tooltipComponents.add(Component.literal("§8Shift + Right-Click a machine to copy."));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}