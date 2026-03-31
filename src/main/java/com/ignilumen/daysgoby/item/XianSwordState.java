package com.ignilumen.daysgoby.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record XianSwordState(int undeadKills, float moQi, long lastSwordDamageTick) {
    public static final int MAX_DANGMO_STACKS = 50;
    public static final int KILLS_PER_DANGMO_STACK = 100;
    public static final int MAX_QI = 100;
    public static final int XIAN_QI_FULL_CHARGE_TICKS = 20 * 60 * 20;
    private static final int XIAN_QI_TICKS_PER_POINT = XIAN_QI_FULL_CHARGE_TICKS / MAX_QI;

    public static final XianSwordState EMPTY = new XianSwordState(0, 0.0F, -1L);

    public static final Codec<XianSwordState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("undead_kills", 0).forGetter(XianSwordState::undeadKills),
            Codec.FLOAT.optionalFieldOf("mo_qi", 0.0F).forGetter(XianSwordState::moQi),
            Codec.LONG.optionalFieldOf("last_sword_damage_tick", -1L).forGetter(XianSwordState::lastSwordDamageTick)
    ).apply(instance, XianSwordState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, XianSwordState> STREAM_CODEC = StreamCodec.of(
            (buffer, state) -> {
                buffer.writeVarInt(state.undeadKills());
                buffer.writeFloat(state.moQi());
                buffer.writeVarLong(state.lastSwordDamageTick());
            },
            buffer -> new XianSwordState(buffer.readVarInt(), buffer.readFloat(), buffer.readVarLong())
    );

    public XianSwordState {
        undeadKills = Math.max(0, undeadKills);
        moQi = Mth.clamp(moQi, 0.0F, MAX_QI);
    }

    public int dangmoStacks() {
        return Math.min(undeadKills / KILLS_PER_DANGMO_STACK, MAX_DANGMO_STACKS);
    }

    public float dangmoPercent() {
        return dangmoStacks() * 0.5F;
    }

    public float dangmoRatio() {
        return dangmoPercent() / 100.0F;
    }

    public int currentXianQi(long gameTime) {
        if (lastSwordDamageTick < 0L) {
            return MAX_QI;
        }

        long elapsedTicks = Math.max(0L, gameTime - lastSwordDamageTick);
        return Math.min(MAX_QI, (int) (elapsedTicks / XIAN_QI_TICKS_PER_POINT));
    }

    public XianSwordState addUndeadKill() {
        return new XianSwordState(undeadKills + 1, moQi, lastSwordDamageTick);
    }

    public XianSwordState addMoQi(float damage) {
        return new XianSwordState(undeadKills, moQi + Math.max(0.0F, damage), lastSwordDamageTick);
    }

    public XianSwordState clearMoQi() {
        return new XianSwordState(undeadKills, 0.0F, lastSwordDamageTick);
    }

    public XianSwordState markSwordDamage(long gameTime) {
        return new XianSwordState(undeadKills, moQi, gameTime);
    }
}
