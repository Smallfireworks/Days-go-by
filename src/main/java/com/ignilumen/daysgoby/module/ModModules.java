package com.ignilumen.daysgoby.module;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.compat.ToughAsNailsCompat;
import com.ignilumen.daysgoby.config.DaysgobyConfig;

import net.neoforged.fml.ModList;

public final class ModModules {
    public static final String ARMOR_LINING = "armor_lining";
    public static final String SPECIAL_WEAPON = "special_weapon";
    public static final String WANDERLUST = "wanderlust";

    private static boolean armorLiningConfigured = true;
    private static boolean specialWeaponConfigured = true;
    private static boolean wanderlustConfigured = true;

    private ModModules() {}

    public static void refreshFromConfig() {
        armorLiningConfigured = DaysgobyConfig.STARTUP.armorLining.getAsBoolean();
        specialWeaponConfigured = DaysgobyConfig.STARTUP.specialWeapon.getAsBoolean();
        wanderlustConfigured = DaysgobyConfig.STARTUP.wanderlust.getAsBoolean();

        if (!armorLiningConfigured) {
            Daysgoby.LOGGER.info("Module {} disabled by config", ARMOR_LINING);
        } else if (!isArmorLiningPrerequisiteMet()) {
            Daysgoby.LOGGER.info("Module {} disabled because {} is not installed", ARMOR_LINING, ToughAsNailsCompat.MOD_ID);
        } else {
            Daysgoby.LOGGER.info("Module {} enabled", ARMOR_LINING);
            ToughAsNailsCompat.init();
        }

        if (specialWeaponConfigured) {
            Daysgoby.LOGGER.info("Module {} enabled", SPECIAL_WEAPON);
        } else {
            Daysgoby.LOGGER.info("Module {} disabled by config", SPECIAL_WEAPON);
        }

        if (wanderlustConfigured) {
            Daysgoby.LOGGER.info("Module {} enabled in config (gameplay content not implemented yet)", WANDERLUST);
        } else {
            Daysgoby.LOGGER.info("Module {} disabled by config", WANDERLUST);
        }
    }

    public static boolean isArmorLiningEnabled() {
        return armorLiningConfigured && isArmorLiningPrerequisiteMet();
    }

    public static boolean isWanderlustEnabled() {
        return wanderlustConfigured;
    }

    public static boolean isSpecialWeaponEnabled() {
        return specialWeaponConfigured;
    }

    public static boolean isModuleEnabled(String moduleId) {
        return switch (moduleId) {
            case ARMOR_LINING -> isArmorLiningEnabled();
            case SPECIAL_WEAPON -> isSpecialWeaponEnabled();
            case WANDERLUST -> isWanderlustEnabled();
            default -> false;
        };
    }

    public static boolean isArmorLiningPrerequisiteMet() {
        return ModList.get().isLoaded(ToughAsNailsCompat.MOD_ID);
    }
}
