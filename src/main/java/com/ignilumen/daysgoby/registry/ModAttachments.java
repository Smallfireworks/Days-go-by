package com.ignilumen.daysgoby.registry;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.wanderlust.WanderlustProgress;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Daysgoby.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<WanderlustProgress>> WANDERLUST_PROGRESS =
            ATTACHMENT_TYPES.register("wanderlust_progress", () -> AttachmentType.builder(WanderlustProgress::new)
                    .serialize(WanderlustProgress.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
