package com.ignilumen.daysgoby.specialweapon;

import com.ignilumen.daysgoby.item.XianSwordState;
import com.ignilumen.daysgoby.registry.ModItems;
import com.ignilumen.daysgoby.util.XianSwordUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SpecialWeaponCommands {
    private SpecialWeaponCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("daysgoby_xianqi")
                .requires(source -> source.hasPermission(2))
                .executes(context -> fillHeldXianQi(context.getSource()));
        event.getDispatcher().register(command);
    }

    private static int fillHeldXianQi(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!XianSwordUtil.isXianSword(stack)) {
            source.sendFailure(Component.literal("Main hand is not holding the Xian Sword."));
            return 0;
        }

        XianSwordState state = XianSwordUtil.getState(stack);
        long fullChargeTick = player.level().getGameTime() - XianSwordState.XIAN_QI_FULL_CHARGE_TICKS;
        XianSwordUtil.setState(stack, new XianSwordState(state.undeadKills(), state.moQi(), fullChargeTick));
        source.sendSuccess(() -> Component.literal("Filled Xian Qi on the held Xian Sword."), false);
        return 1;
    }
}

