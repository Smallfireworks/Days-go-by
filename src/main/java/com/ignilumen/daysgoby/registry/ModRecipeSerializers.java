package com.ignilumen.daysgoby.registry;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.recipe.ApplyLinerRecipe;
import com.ignilumen.daysgoby.recipe.RemoveLinerRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Daysgoby.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ApplyLinerRecipe>> APPLY_LINER =
            RECIPE_SERIALIZERS.register("apply_liner", () -> new SimpleCraftingRecipeSerializer<>(ApplyLinerRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RemoveLinerRecipe>> REMOVE_LINER =
            RECIPE_SERIALIZERS.register("remove_liner", () -> new SimpleCraftingRecipeSerializer<>(RemoveLinerRecipe::new));

    private ModRecipeSerializers() {}

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
