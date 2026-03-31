package com.ignilumen.daysgoby.util;

import com.ignilumen.daysgoby.item.XianSwordState;
import com.ignilumen.daysgoby.registry.ModComponents;
import com.ignilumen.daysgoby.registry.ModItems;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class XianSwordUtil {
    private XianSwordUtil() {}

    public static boolean isXianSword(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.XIAN_SWORD.get());
    }

    public static XianSwordState getState(ItemStack stack) {
        if (!isXianSword(stack)) {
            return XianSwordState.EMPTY;
        }
        return stack.getOrDefault(ModComponents.XIAN_SWORD_STATE.get(), XianSwordState.EMPTY);
    }

    public static void setState(ItemStack stack, XianSwordState state) {
        if (isXianSword(stack)) {
            stack.set(ModComponents.XIAN_SWORD_STATE.get(), state);
        }
    }

    public static ItemStack getMainHandXianSword(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        return isXianSword(stack) ? stack : ItemStack.EMPTY;
    }

    public static ItemStack getAttackerXianSword(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            return getMainHandXianSword(attacker);
        }
        return ItemStack.EMPTY;
    }
}
