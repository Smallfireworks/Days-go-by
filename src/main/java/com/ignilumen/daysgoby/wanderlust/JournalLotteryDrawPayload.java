package com.ignilumen.daysgoby.wanderlust;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.config.WanderlustConfig;
import com.ignilumen.daysgoby.module.ModModules;
import com.ignilumen.daysgoby.registry.ModAttachments;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JournalLotteryDrawPayload(DrawAmount drawAmount) implements CustomPacketPayload {
    public static final Type<JournalLotteryDrawPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Daysgoby.MODID, "journal_lottery_draw"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalLotteryDrawPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.drawAmount.id()),
            buffer -> new JournalLotteryDrawPayload(DrawAmount.byId(buffer.readVarInt()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JournalLotteryDrawPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !ModModules.isWanderlustEnabled()) {
            return;
        }

        if (!WanderlustConfig.isJournalLotteryEnabled()) {
            player.displayClientMessage(Component.translatable("message.daysgoby.wanderlust.lottery_disabled"), true);
            return;
        }

        WanderlustLotteryManager.LotteryDefinition definition = WanderlustLotteryManager.getDefinition();
        if (definition.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.daysgoby.wanderlust.lottery_unavailable"), true);
            return;
        }

        int costPerDraw = WanderlustConfig.lotteryCostPerDraw();
        WanderlustProgress original = player.getData(ModAttachments.WANDERLUST_PROGRESS);
        int requestedDraws = payload.drawAmount.resolveRequestedDraws(original.journeyTickets(), costPerDraw);
        if (requestedDraws <= 0) {
            player.displayClientMessage(Component.translatable("message.daysgoby.wanderlust.not_enough_tickets"), true);
            return;
        }

        WanderlustProgress progress = original;
        InvWrapper inventory = new InvWrapper(player.getInventory());
        boolean dropIfFull = WanderlustConfig.dropRewardsIfInventoryFull();
        boolean stoppedByInventory = false;
        int completedDraws = 0;
        Map<Item, Integer> summary = new LinkedHashMap<>();

        for (int index = 0; index < requestedDraws; index++) {
            if (progress.journeyTickets() < costPerDraw) {
                break;
            }

            ItemStack reward = definition.draw(player.getRandom());
            if (reward.isEmpty()) {
                Daysgoby.LOGGER.warn("Wanderlust lottery produced an empty reward; stopping draw for {}", player.getGameProfile().getName());
                break;
            }

            if (!dropIfFull) {
                ItemStack simulatedRemainder = ItemHandlerHelper.insertItemStacked(inventory, reward.copy(), true);
                if (!simulatedRemainder.isEmpty()) {
                    stoppedByInventory = true;
                    break;
                }
            }

            progress = progress.spendJourneyTickets(costPerDraw);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, reward.copy(), false);
            if (!remainder.isEmpty()) {
                if (dropIfFull) {
                    ItemEntity dropped = player.drop(remainder, false);
                    if (dropped != null) {
                        dropped.setNoPickUpDelay();
                    }
                } else {
                    progress = progress.addJourneyTickets(costPerDraw);
                    stoppedByInventory = true;
                    break;
                }
            }

            completedDraws++;
            summary.merge(reward.getItem(), reward.getCount(), Integer::sum);
        }

        if (!progress.equals(original)) {
            player.setData(ModAttachments.WANDERLUST_PROGRESS, progress);
        }

        if (completedDraws <= 0) {
            if (stoppedByInventory) {
                player.displayClientMessage(Component.translatable("message.daysgoby.wanderlust.inventory_full"), true);
            }
            return;
        }

        player.displayClientMessage(buildResultMessage(completedDraws, summary, stoppedByInventory), false);
    }

    private static MutableComponent buildResultMessage(int completedDraws, Map<Item, Integer> summary, boolean stoppedByInventory) {
        MutableComponent message = Component.translatable("message.daysgoby.wanderlust.lottery_success", completedDraws);
        boolean first = true;
        int shown = 0;

        for (Map.Entry<Item, Integer> entry : summary.entrySet()) {
            if (shown >= 4) {
                message.append(Component.literal(", ..."));
                break;
            }

            message.append(Component.literal(first ? ": " : ", "));
            message.append(Component.literal(Integer.toString(entry.getValue()) + "x "));
            message.append(new ItemStack(entry.getKey()).getHoverName());
            first = false;
            shown++;
        }

        if (stoppedByInventory) {
            message.append(Component.literal(" "));
            message.append(Component.translatable("message.daysgoby.wanderlust.inventory_full_stop"));
        }

        return message;
    }

    public enum DrawAmount {
        ONE(0),
        TEN(1),
        ALL(2);

        private final int id;

        DrawAmount(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static DrawAmount byId(int id) {
            for (DrawAmount value : values()) {
                if (value.id == id) {
                    return value;
                }
            }
            return ONE;
        }

        public int resolveRequestedDraws(int tickets, int costPerDraw) {
            if (costPerDraw <= 0) {
                return 0;
            }

            return switch (this) {
                case ONE -> tickets >= costPerDraw ? 1 : 0;
                case TEN -> tickets >= costPerDraw * 10 ? 10 : 0;
                case ALL -> tickets / costPerDraw;
            };
        }
    }
}
