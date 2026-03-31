package com.ignilumen.daysgoby.specialweapon;

import com.ignilumen.daysgoby.item.XianSwordState;
import com.ignilumen.daysgoby.module.ModModules;
import com.ignilumen.daysgoby.registry.ModItems;
import com.ignilumen.daysgoby.registry.ModMobEffects;
import com.ignilumen.daysgoby.util.XianSwordUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class SpecialWeaponEvents {
    private static final float INTIMIDATION_ARMOR_THRESHOLD = 10.0F;
    private static final float INTIMIDATION_ARMOR_PENALTY = 15.0F;
    private static final float MIN_DANGMO_TRUE_DAMAGE = 10.0F;
    private static final int GUARD_ROTATION_TICKS = 40;
    private static final double GUARD_RADIUS = 1.15D;
    private static final double GUARD_SEGMENT_HALF_WIDTH = Math.PI / 4.0D;
    private static final Map<UUID, Set<UUID>> UNDEAD_FIRST_STRIKES = new HashMap<>();
    private static final ThreadLocal<ActiveSkillContext> ACTIVE_SKILL_CONTEXT = new ThreadLocal<>();
    private static boolean guardCounterattackInProgress;

    private SpecialWeaponEvents() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (guardCounterattackInProgress || !ModModules.isSpecialWeaponEnabled() || event.getEntity().level().isClientSide()) {
            return;
        }

        applyBrokenSwordDebuff(event);

        ActiveSkillContext activeSkillContext = ACTIVE_SKILL_CONTEXT.get();
        if (activeSkillContext != null) {
            if (activeSkillContext.mode() == ActiveSkillMode.XIAN) {
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
                event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> 0.0F);
                event.addReductionModifier(DamageContainer.Reduction.MOB_EFFECTS, (container, reduction) -> 0.0F);
                event.addReductionModifier(DamageContainer.Reduction.ABSORPTION, (container, reduction) -> 0.0F);
            }

            if (event.getEntity() instanceof Player player) {
                applyOffhandGuard(event, player);
            }
            return;
        }

        ItemStack swordStack = XianSwordUtil.getAttackerXianSword(event.getSource());
        if (!swordStack.isEmpty() && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);

            float intimidationBonus = computeIntimidationBonus(event.getEntity(), event.getSource(), event.getAmount());
            if (intimidationBonus > 0.0F) {
                event.setAmount(event.getAmount() + intimidationBonus);
            }
        }

        if (event.getEntity() instanceof Player player) {
            applyOffhandGuard(event, player);
        }
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!ModModules.isSpecialWeaponEnabled() || event.getEntity().level().isClientSide() || ACTIVE_SKILL_CONTEXT.get() != null) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        LivingEntity target = event.getEntity();
        ItemStack swordStack = XianSwordUtil.getAttackerXianSword(event.getSource());
        if (swordStack.isEmpty() || !target.getType().is(EntityTypeTags.UNDEAD) || !claimDangmoFirstStrike(player, target)) {
            return;
        }

        XianSwordState state = XianSwordUtil.getState(swordStack);
        float dangmoDamage = Math.max(target.getMaxHealth() * state.dangmoRatio(), MIN_DANGMO_TRUE_DAMAGE);
        event.setNewDamage(event.getNewDamage() + dangmoDamage);

        if (target.level() instanceof ServerLevel serverLevel) {
            spawnDangmoParticles(serverLevel, target);
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!ModModules.isSpecialWeaponEnabled() || event.getEntity().level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }

        ActiveSkillContext activeSkillContext = ACTIVE_SKILL_CONTEXT.get();
        if (activeSkillContext != null) {
            XianSwordState newState = XianSwordUtil.getState(activeSkillContext.stack())
                    .addMoQi(event.getNewDamage())
                    .markSwordDamage(event.getEntity().level().getGameTime());
            XianSwordUtil.setState(activeSkillContext.stack(), newState);
            return;
        }

        ItemStack swordStack = XianSwordUtil.getAttackerXianSword(event.getSource());
        if (swordStack.isEmpty()) {
            return;
        }

        XianSwordState newState = XianSwordUtil.getState(swordStack)
                .addMoQi(event.getNewDamage())
                .markSwordDamage(event.getEntity().level().getGameTime());
        XianSwordUtil.setState(swordStack, newState);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        UNDEAD_FIRST_STRIKES.remove(event.getEntity().getUUID());
        if (!ModModules.isSpecialWeaponEnabled() || event.getEntity().level().isClientSide() || !event.getEntity().getType().is(EntityTypeTags.UNDEAD)) {
            return;
        }

        ActiveSkillContext activeSkillContext = ACTIVE_SKILL_CONTEXT.get();
        if (activeSkillContext != null) {
            XianSwordUtil.setState(activeSkillContext.stack(), XianSwordUtil.getState(activeSkillContext.stack()).addUndeadKill());
            return;
        }

        ItemStack swordStack = XianSwordUtil.getAttackerXianSword(event.getSource());
        if (swordStack.isEmpty()) {
            return;
        }

        XianSwordUtil.setState(swordStack, XianSwordUtil.getState(swordStack).addUndeadKill());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // The continuous guard visuals are rendered client-side now.
    }

    public static boolean hurtWithActiveSkill(Player attacker, ItemStack stack, ActiveSkillMode mode, LivingEntity target, float damage) {
        ActiveSkillContext previous = ACTIVE_SKILL_CONTEXT.get();
        ACTIVE_SKILL_CONTEXT.set(new ActiveSkillContext(stack, mode));
        try {
            return target.hurt(attacker.damageSources().playerAttack(attacker), damage);
        } finally {
            if (previous == null) {
                ACTIVE_SKILL_CONTEXT.remove();
            } else {
                ACTIVE_SKILL_CONTEXT.set(previous);
            }
        }
    }

    public static double getGuardBaseAngle(long gameTime) {
        return (gameTime % GUARD_ROTATION_TICKS) * (Math.PI * 2.0D / GUARD_ROTATION_TICKS);
    }

    public static GuardZone getGuardZoneForAngle(double attackAngle, long gameTime) {
        double baseAngle = getGuardBaseAngle(gameTime);
        GuardZone[] zones = { GuardZone.SWORD, GuardZone.SHIELD, GuardZone.SWORD, GuardZone.SHIELD };

        for (int i = 0; i < zones.length; i++) {
            double centerAngle = baseAngle + i * (Math.PI * 2.0D / 4.0D);
            if (angularDifference(attackAngle, centerAngle) <= GUARD_SEGMENT_HALF_WIDTH) {
                return zones[i];
            }
        }

        return GuardZone.SWORD;
    }

    private static float computeIntimidationBonus(LivingEntity target, DamageSource source, float baseDamage) {
        float armor = target.getArmorValue();
        if (armor >= INTIMIDATION_ARMOR_THRESHOLD) {
            return 0.0F;
        }

        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float zeroArmorDamage = CombatRules.getDamageAfterAbsorb(target, baseDamage, source, 0.0F, toughness);
        float negativeArmorDamage = CombatRules.getDamageAfterAbsorb(target, baseDamage, source, armor - INTIMIDATION_ARMOR_PENALTY, toughness);
        return Math.max(0.0F, negativeArmorDamage - zeroArmorDamage);
    }

    private static boolean claimDangmoFirstStrike(Player attacker, LivingEntity target) {
        return UNDEAD_FIRST_STRIKES.computeIfAbsent(target.getUUID(), ignored -> new HashSet<>()).add(attacker.getUUID());
    }

    private static void applyBrokenSwordDebuff(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(ModMobEffects.BROKEN_SWORD)) {
            event.setAmount(event.getAmount() * 0.8F);
        }
    }

    private static void applyOffhandGuard(LivingIncomingDamageEvent event, Player player) {
        if (!XianSwordUtil.isXianSword(player.getOffhandItem())) {
            return;
        }

        GuardZone guardZone = getGuardZone(player, event.getSource());
        float incomingBeforeGuard = event.getAmount();
        LivingEntity offender = resolveOffender(event.getSource());

        if (event.getSource().getDirectEntity() instanceof Projectile projectile) {
            projectile.discard();
        }

        if (guardZone == GuardZone.SWORD) {
            event.setAmount(incomingBeforeGuard * 0.8F);
            if (offender != null) {
                performGuardCounterattack(player, offender, Math.max(10.0F, incomingBeforeGuard * 0.2F));
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                spawnSwordGuardFeedback(serverLevel, player);
            }
            return;
        }

        event.setAmount(incomingBeforeGuard * 0.2F);
        if (offender != null) {
            offender.addEffect(new MobEffectInstance(ModMobEffects.BROKEN_SWORD, 100, 0, false, true, true));
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            spawnShieldGuardFeedback(serverLevel, player);
        }
    }

    private static GuardZone getGuardZone(Player player, DamageSource source) {
        Entity impactEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        if (impactEntity == null || impactEntity == player) {
            return GuardZone.SWORD;
        }

        double dx = impactEntity.getX() - player.getX();
        double dz = impactEntity.getZ() - player.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) {
            return GuardZone.SWORD;
        }

        double attackAngle = Math.atan2(dz, dx);
        return getGuardZoneForAngle(attackAngle, player.level().getGameTime());
    }

    private static double angularDifference(double first, double second) {
        double difference = first - second;
        while (difference <= -Math.PI) {
            difference += Math.PI * 2.0D;
        }
        while (difference > Math.PI) {
            difference -= Math.PI * 2.0D;
        }
        return Math.abs(difference);
    }

    private static LivingEntity resolveOffender(DamageSource source) {
        if (source.getDirectEntity() instanceof LivingEntity directLiving) {
            return directLiving;
        }
        if (source.getEntity() instanceof LivingEntity sourceLiving) {
            return sourceLiving;
        }
        return null;
    }

    private static void performGuardCounterattack(Player player, LivingEntity offender, float damage) {
        guardCounterattackInProgress = true;
        try {
            offender.hurt(player.damageSources().playerAttack(player), damage);
        } finally {
            guardCounterattackInProgress = false;
        }
    }

    private static void spawnSwordGuardFeedback(ServerLevel level, Player player) {
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY(1.0D), player.getZ(), 2, 0.25D, 0.15D, 0.25D, 0.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.1F);
    }

    private static void spawnShieldGuardFeedback(ServerLevel level, Player player) {
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0D), player.getZ(), 12, 0.4D, 0.25D, 0.4D, 0.02D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private static void spawnDangmoParticles(ServerLevel level, LivingEntity target) {
        ItemStack particleStack = new ItemStack(ModItems.XIAN_SWORD.get());
        double x = target.getX();
        double y = target.getY(0.6D);
        double z = target.getZ();
        double[][] vectors = {
                { 0.85D, 0.0D },
                { -0.85D, 0.0D },
                { 0.0D, 0.85D },
                { 0.0D, -0.85D }
        };

        for (double[] vector : vectors) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleStack), x + vector[0], y, z + vector[1], 6, -vector[0] * 0.18D, 0.02D, -vector[1] * 0.18D, 0.01D);
        }

        level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 16, 0.25D, 0.35D, 0.25D, 0.0D);
        level.playSound(null, x, y, z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    public enum ActiveSkillMode {
        XIAN,
        DEMON
    }

    private record ActiveSkillContext(ItemStack stack, ActiveSkillMode mode) {}

    public enum GuardZone {
        SWORD,
        SHIELD
    }
}

