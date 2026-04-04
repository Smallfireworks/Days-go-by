package com.ignilumen.daysgoby.item;

import com.ignilumen.daysgoby.client.DaysgobyClientHooks;

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
            DaysgobyClientHooks.openTravelJournal(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
