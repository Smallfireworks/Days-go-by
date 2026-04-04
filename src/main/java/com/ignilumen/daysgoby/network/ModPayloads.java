package com.ignilumen.daysgoby.network;

import com.ignilumen.daysgoby.wanderlust.JournalLotteryDrawPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private static final String NETWORK_VERSION = "1";

    private ModPayloads() {}

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(
                JournalLotteryDrawPayload.TYPE,
                JournalLotteryDrawPayload.STREAM_CODEC,
                JournalLotteryDrawPayload::handle
        );
    }
}
