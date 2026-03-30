package com.ignilumen.daysgoby.recipe;

import com.ignilumen.daysgoby.module.ModModules;
import com.ignilumen.daysgoby.registry.ModItems;
import com.ignilumen.daysgoby.registry.ModRecipeSerializers;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class CraftLinerSnipsRecipe extends CustomRecipe {
    public CraftLinerSnipsRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return ModModules.isArmorLiningEnabled() && hasValidIngredients(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return ModModules.isArmorLiningEnabled() && hasValidIngredients(input)
                ? ModItems.LINER_SNIPS.get().getDefaultInstance()
                : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ModItems.LINER_SNIPS.get().getDefaultInstance();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CRAFT_LINER_SNIPS.get();
    }

    private static boolean hasValidIngredients(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return false;
        }

        boolean hasShears = false;
        boolean hasIronIngot = false;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Items.SHEARS) && !hasShears) {
                hasShears = true;
            } else if (stack.is(Items.IRON_INGOT) && !hasIronIngot) {
                hasIronIngot = true;
            } else {
                return false;
            }
        }

        return hasShears && hasIronIngot;
    }
}
