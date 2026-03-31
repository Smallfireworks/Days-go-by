package com.ignilumen.daysgoby.item;

import com.ignilumen.daysgoby.client.DaysgobyClientHooks;
import com.ignilumen.daysgoby.registry.ModAttachments;
import com.ignilumen.daysgoby.wanderlust.WanderlustProgress;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TravelJournalItem extends Item {
    public TravelJournalItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            WanderlustProgress progress = player.getExistingData(ModAttachments.WANDERLUST_PROGRESS).orElse(new WanderlustProgress());
            DaysgobyClientHooks.openTravelJournal(player, progress);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
