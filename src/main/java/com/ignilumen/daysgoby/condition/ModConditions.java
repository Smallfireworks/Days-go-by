package com.ignilumen.daysgoby.condition;

import com.ignilumen.daysgoby.Daysgoby;
import com.mojang.serialization.MapCodec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Daysgoby.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ModuleEnabledCondition>> MODULE_ENABLED =
            CONDITION_CODECS.register("module_enabled", () -> ModuleEnabledCondition.CODEC);

    private ModConditions() {}

    public static void register(IEventBus eventBus) {
        CONDITION_CODECS.register(eventBus);
    }
}
