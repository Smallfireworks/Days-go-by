package com.ignilumen.daysgoby.module;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.compat.ToughAsNailsCompat;
import com.ignilumen.daysgoby.config.DaysgobyConfig;

import net.neoforged.fml.ModList;

public final class ModModules {
    public static final String TOUGH_AS_NAILS_COMPAT = "tough_as_nails_compat";
    public static final String ARMOR_LINING = "armor_lining";
    public static final String ENCHANTMENT = "enchantment";
    public static final String SPECIAL_WEAPON = "special_weapon";
    public static final String WANDERLUST = "wanderlust";

    private static boolean toughAsNailsCompatConfigured = true;
    private static boolean armorLiningConfigured = true;
    private static boolean enchantmentConfigured = true;
    private static boolean specialWeaponConfigured = true;
    private static boolean wanderlustConfigured = true;

    private ModModules() {}

    public static void refreshFromConfig() {
        toughAsNailsCompatConfigured = DaysgobyConfig.STARTUP.toughAsNailsCompat.getAsBoolean();
        armorLiningConfigured = DaysgobyConfig.STARTUP.armorLining.getAsBoolean();
        enchantmentConfigured = DaysgobyConfig.STARTUP.enchantment.getAsBoolean();
        specialWeaponConfigured = DaysgobyConfig.STARTUP.specialWeapon.getAsBoolean();
        wanderlustConfigured = DaysgobyConfig.STARTUP.wanderlust.getAsBoolean();

        if (!toughAsNailsCompatConfigured) {
            Daysgoby.LOGGER.info("Module {} disabled by config", TOUGH_AS_NAILS_COMPAT);
        } else if (!isToughAsNailsPrerequisiteMet()) {
            Daysgoby.LOGGER.info("Module {} disabled because {} is not installed", TOUGH_AS_NAILS_COMPAT, ToughAsNailsCompat.MOD_ID);
        } else {
            Daysgoby.LOGGER.info("Module {} enabled", TOUGH_AS_NAILS_COMPAT);
            ToughAsNailsCompat.init();
        }

        logSubmoduleState(ARMOR_LINING, armorLiningConfigured, isArmorLiningEnabled());

        if (specialWeaponConfigured) {
            Daysgoby.LOGGER.info("Module {} enabled", SPECIAL_WEAPON);
        } else {
            Daysgoby.LOGGER.info("Module {} disabled by config", SPECIAL_WEAPON);
        }

        if (enchantmentConfigured) {
            Daysgoby.LOGGER.info("Module {} enabled", ENCHANTMENT);
        } else {
            Daysgoby.LOGGER.info("Module {} disabled by config", ENCHANTMENT);
        }

        if (wanderlustConfigured) {
            Daysgoby.LOGGER.info("Module {} enabled in config (gameplay content not implemented yet)", WANDERLUST);
        } else {
            Daysgoby.LOGGER.info("Module {} disabled by config", WANDERLUST);
        }
    }

    public static boolean isToughAsNailsCompatEnabled() {
        return toughAsNailsCompatConfigured && isToughAsNailsPrerequisiteMet();
    }

    public static boolean isArmorLiningEnabled() {
        return isToughAsNailsCompatEnabled() && armorLiningConfigured;
    }

    public static boolean isWanderlustEnabled() {
        return wanderlustConfigured;
    }

    public static boolean isEnchantmentEnabled() {
        return enchantmentConfigured;
    }

    public static boolean isSpecialWeaponEnabled() {
        return specialWeaponConfigured;
    }

    public static boolean isModuleEnabled(String moduleId) {
        return switch (moduleId) {
            case TOUGH_AS_NAILS_COMPAT -> isToughAsNailsCompatEnabled();
            case ARMOR_LINING -> isArmorLiningEnabled();
            case ENCHANTMENT -> isEnchantmentEnabled();
            case SPECIAL_WEAPON -> isSpecialWeaponEnabled();
            case WANDERLUST -> isWanderlustEnabled();
            default -> false;
        };
    }

    public static boolean isToughAsNailsPrerequisiteMet() {
        return ModList.get().isLoaded(ToughAsNailsCompat.MOD_ID);
    }

    private static void logSubmoduleState(String moduleId, boolean configured, boolean enabled) {
        if (!toughAsNailsCompatConfigured) {
            Daysgoby.LOGGER.info("Module {} disabled because {} is disabled by config", moduleId, TOUGH_AS_NAILS_COMPAT);
            return;
        }
        if (!configured) {
            Daysgoby.LOGGER.info("Module {} disabled by config", moduleId);
            return;
        }
        if (!isToughAsNailsPrerequisiteMet()) {
            Daysgoby.LOGGER.info("Module {} disabled because {} is not installed", moduleId, ToughAsNailsCompat.MOD_ID);
            return;
        }
        if (enabled) {
            Daysgoby.LOGGER.info("Module {} enabled", moduleId);
        }
    }
}
