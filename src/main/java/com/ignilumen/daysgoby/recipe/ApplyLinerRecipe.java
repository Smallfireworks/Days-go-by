package com.ignilumen.daysgoby.recipe;

import com.ignilumen.daysgoby.registry.ModRecipeSerializers;
import com.ignilumen.daysgoby.util.ArmorLiningUtil;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ApplyLinerRecipe extends CustomRecipe {
    public ApplyLinerRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return ArmorLiningUtil.findApplyMatch(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ArmorLiningUtil.ApplyMatch match = ArmorLiningUtil.findApplyMatch(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }

        return ArmorLiningUtil.applyLining(match.armorStack(), match.linerType());
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
        return ModRecipeSerializers.APPLY_LINER.get();
    }
}
