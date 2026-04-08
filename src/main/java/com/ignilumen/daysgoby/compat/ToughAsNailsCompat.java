package com.ignilumen.daysgoby.compat;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.util.ArmorLiningUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.minecraft.world.entity.player.Player;

public final class ToughAsNailsCompat {
    public static final String MOD_ID = "toughasnails";

    private static boolean temperatureInitialized;

    private ToughAsNailsCompat() {}

    public static void init() {
        initTemperatureCompat();
    }

    private static void initTemperatureCompat() {
        if (temperatureInitialized) {
            return;
        }
        temperatureInitialized = true;

        try {
            Class<?> modifierInterface = Class.forName("toughasnails.api.temperature.IPlayerTemperatureModifier");
            Class<?> helperClass = Class.forName("toughasnails.api.temperature.TemperatureHelper");
            Class<?> temperatureLevelClass = Class.forName("toughasnails.api.temperature.TemperatureLevel");
            Method registerMethod = helperClass.getMethod("registerPlayerTemperatureModifier", modifierInterface);
            Method incrementMethod = temperatureLevelClass.getMethod("increment", int.class);
            Method decrementMethod = temperatureLevelClass.getMethod("decrement", int.class);

            InvocationHandler handler = (proxy, method, args) -> {
                if ("modify".equals(method.getName())
                        && args != null
                        && args.length == 2
                        && args[0] instanceof Player player) {
                    return applyArmorLikeTemperatureModifier(player, args[1], incrementMethod, decrementMethod);
                }

                if (method.getDeclaringClass() == Object.class) {
                    return handleObjectMethod(proxy, method, args);
                }

                throw new UnsupportedOperationException("Unsupported Tough As Nails callback: " + method.getName());
            };

            Object proxy = Proxy.newProxyInstance(
                    modifierInterface.getClassLoader(),
                    new Class<?>[] { modifierInterface },
                    handler
            );
            registerMethod.invoke(null, proxy);

            Daysgoby.LOGGER.info("Registered Tough As Nails temperature compatibility");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Daysgoby.LOGGER.error("Failed to initialize Tough As Nails compatibility", exception);
        }
    }

    private static Object applyArmorLikeTemperatureModifier(Player player, Object current, Method incrementMethod, Method decrementMethod)
            throws ReflectiveOperationException {
        int heatingPieces = ArmorLiningUtil.getHeatingLiningCount(player);
        int coolingPieces = ArmorLiningUtil.getCoolingLiningCount(player);
        int modifier = heatingPieces / 2 - coolingPieces / 2;
        if (modifier == 0) {
            return current;
        }

        Object adjusted = incrementMethod.invoke(current, modifier);
        String adjustedName = ((Enum<?>) adjusted).name();
        String currentName = ((Enum<?>) current).name();

        if ("HOT".equals(adjustedName) && !"HOT".equals(currentName)) {
            return decrementMethod.invoke(adjusted, 1);
        }
        if ("ICY".equals(adjustedName) && !"ICY".equals(currentName)) {
            return incrementMethod.invoke(adjusted, 1);
        }

        return adjusted;
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "DaysgobyToughAsNailsCompat";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> args != null && args.length == 1 && proxy == args[0];
            default -> null;
        };
    }
}
