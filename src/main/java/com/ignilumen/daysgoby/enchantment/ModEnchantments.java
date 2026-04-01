package com.ignilumen.daysgoby.enchantment;

import com.ignilumen.daysgoby.Daysgoby;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> SHIT_RAIN = key("shit_rain");

    private ModEnchantments() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Daysgoby.MODID, name));
    }
}
