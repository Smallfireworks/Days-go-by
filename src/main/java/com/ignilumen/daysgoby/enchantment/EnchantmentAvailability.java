package com.ignilumen.daysgoby.enchantment;

import com.ignilumen.daysgoby.config.EnchantmentConfig;
import com.ignilumen.daysgoby.module.ModModules;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentAvailability {
    private EnchantmentAvailability() {}

    public static boolean isEnabled(ResourceKey<Enchantment> enchantmentKey) {
        return ModModules.isEnchantmentEnabled() && switch (enchantmentKey.location().getPath()) {
            case "shit_rain" -> EnchantmentConfig.STARTUP.shitRain.enabled.getAsBoolean();
            case "time_stop" -> EnchantmentConfig.STARTUP.timeStop.enabled.getAsBoolean();
            default -> false;
        };
    }

    public static boolean canAppearInEnchantingTable(ResourceKey<Enchantment> enchantmentKey) {
        return isEnabled(enchantmentKey) && switch (enchantmentKey.location().getPath()) {
            case "shit_rain" -> EnchantmentConfig.STARTUP.shitRain.inEnchantingTable.getAsBoolean();
            case "time_stop" -> EnchantmentConfig.STARTUP.timeStop.inEnchantingTable.getAsBoolean();
            default -> false;
        };
    }

    public static boolean canGenerateOnRandomLoot(ResourceKey<Enchantment> enchantmentKey) {
        return isEnabled(enchantmentKey) && switch (enchantmentKey.location().getPath()) {
            case "shit_rain" -> EnchantmentConfig.STARTUP.shitRain.onRandomLoot.getAsBoolean();
            case "time_stop" -> EnchantmentConfig.STARTUP.timeStop.onRandomLoot.getAsBoolean();
            default -> false;
        };
    }

    public static boolean canBeTradeable(ResourceKey<Enchantment> enchantmentKey) {
        return isEnabled(enchantmentKey) && switch (enchantmentKey.location().getPath()) {
            case "shit_rain" -> EnchantmentConfig.STARTUP.shitRain.tradeable.getAsBoolean();
            case "time_stop" -> EnchantmentConfig.STARTUP.timeStop.tradeable.getAsBoolean();
            default -> false;
        };
    }

    public static boolean canGenerateOnTradedEquipment(ResourceKey<Enchantment> enchantmentKey) {
        return isEnabled(enchantmentKey) && switch (enchantmentKey.location().getPath()) {
            case "shit_rain" -> EnchantmentConfig.STARTUP.shitRain.onTradedEquipment.getAsBoolean();
            case "time_stop" -> EnchantmentConfig.STARTUP.timeStop.onTradedEquipment.getAsBoolean();
            default -> false;
        };
    }

    public static boolean canGenerateOnMobSpawnEquipment(ResourceKey<Enchantment> enchantmentKey) {
        return isEnabled(enchantmentKey) && switch (enchantmentKey.location().getPath()) {
            case "shit_rain" -> EnchantmentConfig.STARTUP.shitRain.onMobSpawnEquipment.getAsBoolean();
            case "time_stop" -> EnchantmentConfig.STARTUP.timeStop.onMobSpawnEquipment.getAsBoolean();
            default -> false;
        };
    }
}
