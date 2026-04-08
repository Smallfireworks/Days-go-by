package com.ignilumen.daysgoby.registry;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.effect.BrokenSwordEffect;
import com.ignilumen.daysgoby.effect.DemonBleedEffect;
import com.ignilumen.daysgoby.effect.TimeStopEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Daysgoby.MODID);

    public static final DeferredHolder<MobEffect, BrokenSwordEffect> BROKEN_SWORD =
            MOB_EFFECTS.register("broken_sword", BrokenSwordEffect::new);

    public static final DeferredHolder<MobEffect, DemonBleedEffect> DEMON_BLEED =
            MOB_EFFECTS.register("demon_bleed", DemonBleedEffect::new);

    public static final DeferredHolder<MobEffect, TimeStopEffect> TIME_STOP =
            MOB_EFFECTS.register("time_stop", TimeStopEffect::new);

    private ModMobEffects() {}

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
