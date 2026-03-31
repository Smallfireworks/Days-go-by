package com.ignilumen.daysgoby.condition;

import com.ignilumen.daysgoby.module.ModModules;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;

public record ModuleEnabledCondition(String module) implements ICondition {
    public static final MapCodec<ModuleEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(Codec.STRING.fieldOf("module").forGetter(ModuleEnabledCondition::module))
                    .apply(builder, ModuleEnabledCondition::new)
    );

    @Override
    public boolean test(IContext context) {
        return ModModules.isModuleEnabled(module);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "module_enabled(\"" + module + "\")";
    }
}
