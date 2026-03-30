package com.ignilumen.daysgoby.recipe;

import com.ignilumen.daysgoby.registry.ModRecipeSerializers;
import com.ignilumen.daysgoby.util.ArmorLiningUtil;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class RemoveLinerRecipe extends CustomRecipe {
    public RemoveLinerRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return ArmorLiningUtil.findRemovalMatch(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ArmorLiningUtil.RemovalMatch match = ArmorLiningUtil.findRemovalMatch(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }

        return ArmorLiningUtil.removeLining(match.armorStack());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        ArmorLiningUtil.RemovalMatch match = ArmorLiningUtil.findRemovalMatch(input);
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        if (match == null) {
            return remaining;
        }

        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (ArmorLiningUtil.isSnips(stack)) {
                remaining.set(index, stack.copyWithCount(1));
            } else if (index == match.armorIndex()) {
                remaining.set(index, ArmorLiningUtil.restoreLinerStack(match.armorStack(), match.lining()));
            }
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.REMOVE_LINER.get();
    }
}
