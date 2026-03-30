package com.ignilumen.daysgoby.module;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.compat.ToughAsNailsCompat;
import com.ignilumen.daysgoby.config.DaysgobyConfig;

import net.neoforged.fml.ModList;

public final class ModModules {
    private static boolean armorLiningConfigured = true;
    private static boolean wanderlustConfigured = true;

    private ModModules() {}

    public static void refreshFromConfig() {
        armorLiningConfigured = DaysgobyConfig.STARTUP.armorLining.getAsBoolean();
        wanderlustConfigured = DaysgobyConfig.STARTUP.wanderlust.getAsBoolean();

        if (!armorLiningConfigured) {
            Daysgoby.LOGGER.info("Module armor_lining disabled by config");
        } else if (!isArmorLiningPrerequisiteMet()) {
            Daysgoby.LOGGER.info("Module armor_lining disabled because {} is not installed", ToughAsNailsCompat.MOD_ID);
        } else {
            Daysgoby.LOGGER.info("Module armor_lining enabled");
            ToughAsNailsCompat.init();
        }

        if (wanderlustConfigured) {
            Daysgoby.LOGGER.info("Module wanderlust enabled in config (gameplay content not implemented yet)");
        } else {
            Daysgoby.LOGGER.info("Module wanderlust disabled by config");
        }
    }

    public static boolean isArmorLiningEnabled() {
        return armorLiningConfigured && isArmorLiningPrerequisiteMet();
    }

    public static boolean isWanderlustEnabled() {
        return wanderlustConfigured;
    }

    public static boolean isArmorLiningPrerequisiteMet() {
        return ModList.get().isLoaded(ToughAsNailsCompat.MOD_ID);
    }
}
