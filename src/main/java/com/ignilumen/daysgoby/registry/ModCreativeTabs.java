package com.ignilumen.daysgoby.registry;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.module.ModModules;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Daysgoby.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.daysgoby"))
                    .icon(() -> {
                        if (ModModules.isSpecialWeaponEnabled()) {
                            return ModItems.XIAN_SWORD.get().getDefaultInstance();
                        }
                        if (ModModules.isWanderlustEnabled()) {
                            return ModItems.TRAVEL_JOURNAL.get().getDefaultInstance();
                        }
                        return ModItems.LINER_SNIPS.get().getDefaultInstance();
                    })
                    .displayItems((parameters, output) -> {
                        if (ModModules.isSpecialWeaponEnabled()) {
                            output.accept(ModItems.XIAN_SWORD.get());
                        }
                        if (ModModules.isArmorLiningEnabled()) {
                            output.accept(ModItems.LINER_SNIPS.get());
                        }
                        if (ModModules.isWanderlustEnabled()) {
                            output.accept(ModItems.TRAVEL_JOURNAL.get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
