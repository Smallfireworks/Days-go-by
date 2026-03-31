package com.ignilumen.daysgoby.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DemonBleedEffect extends MobEffect {
    private static final Map<UUID, UUID> SOURCES = new HashMap<>();

    public DemonBleedEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A0000);
    }

    public static void trackSource(LivingEntity target, Player source) {
        SOURCES.put(target.getUUID(), source.getUUID());
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (!(livingEntity.level() instanceof ServerLevel serverLevel) || !livingEntity.isAlive()) {
            return true;
        }

        float healthBefore = livingEntity.getHealth();
        float percentage = 0.02F + livingEntity.getRandom().nextFloat() * 0.03F;
        float damage = healthBefore * percentage;
        if (damage <= 0.0F) {
            return true;
        }

        livingEntity.hurt(serverLevel.damageSources().magic(), damage);
        float actualDamage = Math.max(0.0F, healthBefore - livingEntity.getHealth());
        if (actualDamage <= 0.0F) {
            return true;
        }

        UUID sourceId = SOURCES.get(livingEntity.getUUID());
        if (sourceId == null) {
            return true;
        }

        ServerPlayer sourcePlayer = serverLevel.getServer().getPlayerList().getPlayer(sourceId);
        if (sourcePlayer != null && sourcePlayer.isAlive()) {
            sourcePlayer.heal(actualDamage);
        }

        return true;
    }

    @Override
    public void onMobRemoved(LivingEntity livingEntity, int amplifier, Entity.RemovalReason reason) {
        SOURCES.remove(livingEntity.getUUID());
    }
}
