package com.ignilumen.daysgoby.registry;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.wanderlust.WanderlustProgress;

import java.util.UUID;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
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
                    .sync((holder, to) -> holder instanceof ServerPlayer owner && samePlayer(owner, to), ByteBufCodecs.fromCodecTrusted(WanderlustProgress.CODEC))
                    .build());

    private ModAttachments() {}

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    private static boolean samePlayer(ServerPlayer owner, ServerPlayer viewer) {
        UUID ownerId = owner.getUUID();
        UUID viewerId = viewer.getUUID();
        return ownerId.equals(viewerId);
    }
}
