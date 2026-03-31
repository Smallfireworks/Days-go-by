package com.ignilumen.daysgoby.registry;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.item.ArmorLining;
import com.ignilumen.daysgoby.item.XianSwordState;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Daysgoby.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArmorLining>> ARMOR_LINING =
            DATA_COMPONENTS.registerComponentType("armor_lining", builder -> builder
                    .persistent(ArmorLining.CODEC)
                    .networkSynchronized(ArmorLining.STREAM_CODEC)
                    .cacheEncoding());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<XianSwordState>> XIAN_SWORD_STATE =
            DATA_COMPONENTS.registerComponentType("xian_sword_state", builder -> builder
                    .persistent(XianSwordState.CODEC)
                    .networkSynchronized(XianSwordState.STREAM_CODEC)
                    .cacheEncoding());

    private ModComponents() {}

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
