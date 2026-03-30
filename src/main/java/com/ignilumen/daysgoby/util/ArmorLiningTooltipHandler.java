package com.ignilumen.daysgoby.util;

import com.ignilumen.daysgoby.item.ArmorLining;
import com.ignilumen.daysgoby.module.ModModules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ArmorLiningTooltipHandler {
    private ArmorLiningTooltipHandler() {}

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ModModules.isArmorLiningEnabled()) {
            return;
        }

        ArmorLining lining = ArmorLiningUtil.getLining(event.getItemStack());
        if (lining == null) {
            return;
        }

        event.getToolTip().add(Component.translatable(
                "tooltip.daysgoby.lined_armor",
                Component.translatable("tooltip.daysgoby." + lining.type().getSerializedName() + "_liner")
        ).withStyle(ChatFormatting.GRAY));
    }
}
