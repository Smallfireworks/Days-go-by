package com.ignilumen.daysgoby.util;

import com.ignilumen.daysgoby.item.ArmorLining;
import com.ignilumen.daysgoby.item.LiningType;
import com.ignilumen.daysgoby.registry.ModComponents;
import com.ignilumen.daysgoby.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

public final class ArmorLiningUtil {
    private static final TagKey<Item> TAN_HEATING_ARMOR = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("toughasnails", "heating_armor")
    );
    private static final TagKey<Item> TAN_COOLING_ARMOR = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("toughasnails", "cooling_armor")
    );

    private ArmorLiningUtil() {}

    public static ArmorLining getLining(ItemStack stack) {
        return stack.get(ModComponents.ARMOR_LINING.get());
    }

    public static boolean hasLining(ItemStack stack) {
        return getLining(stack) != null;
    }

    public static boolean isSnips(ItemStack stack) {
        return stack.is(ModItems.LINER_SNIPS.get());
    }

    public static ItemStack applyLining(ItemStack armorStack, LiningType liningType) {
        ItemStack result = armorStack.copy();
        result.setCount(1);
        result.set(ModComponents.ARMOR_LINING.get(), new ArmorLining(liningType));
        return result;
    }

    public static ItemStack removeLining(ItemStack armorStack) {
        ItemStack result = armorStack.copy();
        result.setCount(1);
        result.remove(ModComponents.ARMOR_LINING.get());
        return result;
    }

    public static int getHeatingLiningCount(Player player) {
        return countLinings(player, LiningType.WARMING);
    }

    public static int getCoolingLiningCount(Player player) {
        return countLinings(player, LiningType.COOLING);
    }

    public static ApplyMatch findApplyMatch(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return null;
        }

        ItemStack first = ItemStack.EMPTY;
        ItemStack second = ItemStack.EMPTY;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            if (first.isEmpty()) {
                first = stack;
            } else if (second.isEmpty()) {
                second = stack;
            } else {
                return null;
            }
        }

        if (first.isEmpty() || second.isEmpty()) {
            return null;
        }

        ApplyMatch direct = tryCreateApplyMatch(first, second);
        if (direct != null) {
            return direct;
        }

        return tryCreateApplyMatch(second, first);
    }

    public static RemovalMatch findRemovalMatch(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return null;
        }

        ItemStack armorStack = ItemStack.EMPTY;
        int armorIndex = -1;
        boolean hasSnips = false;

        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }

            if (isSnips(stack)) {
                if (hasSnips) {
                    return null;
                }
                hasSnips = true;
            } else if (isLineableArmor(stack) && hasLining(stack)) {
                if (!armorStack.isEmpty()) {
                    return null;
                }
                armorStack = stack;
                armorIndex = index;
            } else {
                return null;
            }
        }

        ArmorLining lining = getLining(armorStack);
        return hasSnips && !armorStack.isEmpty() && lining != null
                ? new RemovalMatch(armorIndex, armorStack, lining)
                : null;
    }

    public static ItemStack createLinerStack(ItemStack armorStack, LiningType liningType) {
        EquipmentSlot slot = getSupportedArmorSlot(armorStack);
        if (slot == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation linerId = expectedToughAsNailsLinerId(slot, liningType);
        if (!BuiltInRegistries.ITEM.containsKey(linerId)) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(BuiltInRegistries.ITEM.get(linerId));
    }

    public static EquipmentSlot getSupportedArmorSlot(ItemStack stack) {
        EquipmentSlot slot = stack.getEquipmentSlot();
        if (slot != null && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return slot;
        }

        Equipable equipable = Equipable.get(stack);
        if (equipable == null) {
            return null;
        }

        EquipmentSlot equipableSlot = equipable.getEquipmentSlot();
        return equipableSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? equipableSlot : null;
    }

    public static boolean isLineableArmor(ItemStack stack) {
        return getSupportedArmorSlot(stack) != null && stack.getMaxStackSize() == 1;
    }

    public static boolean isLiner(ItemStack stack) {
        return getLinerType(stack) != null;
    }

    public static LiningType getLinerType(ItemStack stack) {
        if (stack.is(TAN_HEATING_ARMOR)) {
            return LiningType.WARMING;
        }
        if (stack.is(TAN_COOLING_ARMOR)) {
            return LiningType.COOLING;
        }
        return null;
    }

    private static ApplyMatch tryCreateApplyMatch(ItemStack armorStack, ItemStack linerStack) {
        if (!isLineableArmor(armorStack) || hasLining(armorStack) || !isLiner(linerStack)) {
            return null;
        }

        EquipmentSlot armorSlot = getSupportedArmorSlot(armorStack);
        LiningType linerType = getLinerType(linerStack);
        if (armorSlot == null || linerType == null || !isCompatibleLinerForSlot(linerStack, armorSlot, linerType)) {
            return null;
        }

        return new ApplyMatch(armorStack, linerType);
    }

    private static boolean isCompatibleLinerForSlot(ItemStack linerStack, EquipmentSlot armorSlot, LiningType liningType) {
        EquipmentSlot linerSlot = getSupportedArmorSlot(linerStack);
        if (linerSlot != armorSlot) {
            return false;
        }

        return switch (liningType) {
            case WARMING -> linerStack.is(TAN_HEATING_ARMOR);
            case COOLING -> linerStack.is(TAN_COOLING_ARMOR);
        };
    }

    private static ResourceLocation expectedToughAsNailsLinerId(EquipmentSlot slot, LiningType liningType) {
        String slotName = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> throw new IllegalArgumentException("Unsupported armor slot: " + slot);
        };
        String prefix = liningType == LiningType.WARMING ? "wool_" : "leaf_";
        return ResourceLocation.fromNamespaceAndPath("toughasnails", prefix + slotName);
    }

    private static int countLinings(Player player, LiningType type) {
        int total = 0;
        for (ItemStack armorStack : player.getArmorSlots()) {
            ArmorLining lining = getLining(armorStack);
            if (lining != null && lining.type() == type) {
                total++;
            }
        }
        return total;
    }

    public record ApplyMatch(ItemStack armorStack, LiningType linerType) {}

    public record RemovalMatch(int armorIndex, ItemStack armorStack, ArmorLining lining) {}
}