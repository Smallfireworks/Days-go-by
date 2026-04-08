package com.ignilumen.daysgoby.enchantment;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

public final class EnchantmentRuntimeEvents {
    private EnchantmentRuntimeEvents() {}

    public static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        disableIfNeeded(event, ModEnchantments.SHIT_RAIN);
        disableIfNeeded(event, ModEnchantments.TIME_STOP);
    }

    private static void disableIfNeeded(GetEnchantmentLevelEvent event, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey) {
        if (!event.isTargetting(enchantmentKey) || EnchantmentAvailability.isEnabled(enchantmentKey)) {
            return;
        }

        Optional<Holder.Reference<Enchantment>> holder = event.getHolder(enchantmentKey);
        holder.ifPresent(reference -> event.getEnchantments().set(reference, 0));
    }
}
