package com.ignilumen.daysgoby.enchantment;

import com.ignilumen.daysgoby.config.EnchantmentConfig;
import com.ignilumen.daysgoby.module.ModModules;
import com.ignilumen.daysgoby.registry.ModMobEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TimeStopEvents {
    private static final int LEVEL_I_DURATION_TICKS = 60;
    private static final int LEVEL_II_DURATION_TICKS = 120;
    private static final int LEVEL_III_SINGLE_DURATION_TICKS = 180;
    private static final int LEVEL_III_FIELD_DURATION_TICKS = 120;
    private static final int LEVEL_III_AGGREGATION_WINDOW_TICKS = 8;
    private static final int LEVEL_III_FIELD_REFRESH_GRACE_TICKS = 2;
    private static final int CLEANUP_INTERVAL = 4;
    private static final TagKey<EntityType<?>> BOSS_ENTITY_TYPES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "bosses")
    );

    private static final Map<UUID, Long> DEFENDER_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> FROZEN_LIVING_UNTIL = new HashMap<>();
    private static final Map<UUID, FrozenLivingState> FROZEN_LIVING_STATES = new HashMap<>();
    private static final Map<UUID, Boolean> FROZEN_MOB_NO_AI_STATES = new HashMap<>();
    private static final Map<UUID, Long> FROZEN_PROJECTILE_UNTIL = new HashMap<>();
    private static final Map<UUID, Vec3> FROZEN_PROJECTILE_POSITIONS = new HashMap<>();
    private static final Map<UUID, Vec3> FROZEN_PROJECTILE_VELOCITIES = new HashMap<>();
    private static final Map<UUID, Long> FROZEN_OTHER_ENTITY_UNTIL = new HashMap<>();
    private static final Map<UUID, Vec3> FROZEN_OTHER_ENTITY_POSITIONS = new HashMap<>();
    private static final Map<UUID, Vec3> FROZEN_OTHER_ENTITY_VELOCITIES = new HashMap<>();
    private static final Map<UUID, PendingLevelThreeTrigger> PENDING_LEVEL_THREE_TRIGGERS = new HashMap<>();
    private static final Map<UUID, ActiveLevelThreeField> ACTIVE_LEVEL_THREE_FIELDS = new HashMap<>();

    private TimeStopEvents() {}

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }

        LivingEntity defender = event.getEntity();
        int level = getTimeStopLevel(defender);
        if (level <= 0 || level >= 3 || !canTrigger(defender)) {
            return;
        }

        LivingEntity attacker = resolveLivingAttacker(event.getSource());
        if (attacker == null || attacker == defender || isBoss(attacker)) {
            return;
        }

        long currentTick = getServerTick(defender);
        if (currentTick < 0L || isOnCooldown(defender, currentTick)) {
            return;
        }

        applySingleTargetTimeStop(attacker, currentTick + getFreezeDurationTicks(level, TriggerMode.DAMAGE));
        setCooldown(defender, currentTick);
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (isTimeStopped(mob)) {
            event.setNewAboutToBeSetTarget(null);
            event.setCanceled(true);
            return;
        }

        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || !hasTimeStopLevel(player, 3) || isBoss(mob)) {
            return;
        }

        long currentTick = getServerTick(player);
        if (currentTick < 0L || isOnCooldown(player, currentTick) || !hasHostileIntent(mob, player)) {
            return;
        }

        queueLevelThreeTrigger(player, currentTick);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || !isTimeStoppedSource(event.getSource())) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getLevel().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCharge(-1);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().level().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setDuration(0);
        event.setCanceled(true);
        event.getEntity().stopUsingItem();
    }

    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity().level().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setDuration(0);
        event.setCanceled(true);
        event.getEntity().stopUsingItem();
    }

    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity().level().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onSwapItems(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity().level().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onDestroyBlock(LivingDestroyBlockEvent event) {
        if (event.getEntity().level().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide() || !isTimeStopped(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Projectile projectile) {
            Long expiresAt = FROZEN_PROJECTILE_UNTIL.get(projectile.getUUID());
            if (expiresAt == null || expiresAt <= getServerTick(projectile)) {
                clearProjectileState(projectile.getUUID());
                return;
            }

            FROZEN_PROJECTILE_POSITIONS.put(projectile.getUUID(), projectile.position());
            FROZEN_PROJECTILE_VELOCITIES.putIfAbsent(projectile.getUUID(), projectile.getDeltaMovement());
            projectile.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (!(entity instanceof LivingEntity) && isOtherEntityTimeStopped(entity)) {
            FROZEN_OTHER_ENTITY_POSITIONS.put(entity.getUUID(), entity.position());
            FROZEN_OTHER_ENTITY_VELOCITIES.putIfAbsent(entity.getUUID(), entity.getDeltaMovement());
            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        UUID entityId = event.getEntity().getUUID();
        if (event.getEntity() instanceof LivingEntity living) {
            clearLivingState(living);
        } else {
            clearLivingState(entityId);
        }
        clearProjectileState(entityId);
        clearOtherEntityState(entityId);
        PENDING_LEVEL_THREE_TRIGGERS.remove(entityId);
        ACTIVE_LEVEL_THREE_FIELDS.remove(entityId);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        UUID entityId = event.getEntity().getUUID();
        clearLivingState(event.getEntity());
        DEFENDER_COOLDOWNS.remove(entityId);
        PENDING_LEVEL_THREE_TRIGGERS.remove(entityId);
        ACTIVE_LEVEL_THREE_FIELDS.remove(entityId);
    }

    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living && isTimeStopped(living)) {
            FROZEN_LIVING_STATES.putIfAbsent(living.getUUID(), captureLivingState(living));
            suppressActions(living);
            lockLivingState(living);
            return;
        }

        if (entity instanceof Projectile projectile && isProjectileTimeStopped(projectile)) {
            FROZEN_PROJECTILE_POSITIONS.putIfAbsent(projectile.getUUID(), projectile.position());
            FROZEN_PROJECTILE_VELOCITIES.putIfAbsent(projectile.getUUID(), projectile.getDeltaMovement());
            projectile.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (!(entity instanceof LivingEntity) && isOtherEntityTimeStopped(entity)) {
            FROZEN_OTHER_ENTITY_POSITIONS.putIfAbsent(entity.getUUID(), entity.position());
            FROZEN_OTHER_ENTITY_VELOCITIES.putIfAbsent(entity.getUUID(), entity.getDeltaMovement());
            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living) {
            FrozenLivingState frozenState = FROZEN_LIVING_STATES.get(living.getUUID());
            if (frozenState != null) {
                suppressActions(living);
                lockLivingState(living);
            }
            return;
        }

        if (entity instanceof Projectile projectile) {
            Vec3 frozenPosition = FROZEN_PROJECTILE_POSITIONS.get(projectile.getUUID());
            if (frozenPosition != null) {
                projectile.setDeltaMovement(Vec3.ZERO);
                if (projectile.position().distanceToSqr(frozenPosition) > 1.0E-6D) {
                    projectile.setPos(frozenPosition.x, frozenPosition.y, frozenPosition.z);
                }
            }
            return;
        }

        if (!(entity instanceof LivingEntity)) {
            Vec3 frozenPosition = FROZEN_OTHER_ENTITY_POSITIONS.get(entity.getUUID());
            if (frozenPosition != null) {
                entity.setDeltaMovement(Vec3.ZERO);
                if (entity.position().distanceToSqr(frozenPosition) > 1.0E-6D) {
                    entity.setPos(frozenPosition.x, frozenPosition.y, frozenPosition.z);
                }
            }
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        long currentTick = event.getServer().getTickCount();
        refreshActiveLevelThreeFields(event, currentTick);
        if (currentTick % CLEANUP_INTERVAL != 0) {
            return;
        }

        cleanupExpiredState(event.getServer(), currentTick);
        resolvePendingLevelThreeTriggers(event, currentTick);
    }

    private static void queueLevelThreeTrigger(Player defender, long currentTick) {
        PENDING_LEVEL_THREE_TRIGGERS.compute(defender.getUUID(), (uuid, existing) -> {
            long triggerAt = currentTick + LEVEL_III_AGGREGATION_WINDOW_TICKS;
            if (existing == null || existing.triggerAtTick() > triggerAt) {
                return new PendingLevelThreeTrigger(triggerAt);
            }
            return existing;
        });
    }

    private static void resolvePendingLevelThreeTriggers(ServerTickEvent.Post event, long currentTick) {
        Iterator<Map.Entry<UUID, PendingLevelThreeTrigger>> iterator = PENDING_LEVEL_THREE_TRIGGERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingLevelThreeTrigger> entry = iterator.next();
            if (entry.getValue().triggerAtTick() > currentTick) {
                continue;
            }

            iterator.remove();
            Player defender = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (defender == null || !defender.isAlive() || !hasTimeStopLevel(defender, 3) || isOnCooldown(defender, currentTick)) {
                continue;
            }

            List<Mob> aggressors = findAggressors(defender, false);
            if (aggressors.isEmpty()) {
                continue;
            }

            if (aggressors.size() == 1) {
                applySingleTargetTimeStop(aggressors.get(0), currentTick + getFreezeDurationTicks(3, TriggerMode.SINGLE));
            } else {
                activateAreaTimeStop(defender, currentTick, currentTick + getFreezeDurationTicks(3, TriggerMode.FIELD));
            }
            setCooldown(defender, currentTick);
        }
    }

    private static void applySingleTargetTimeStop(LivingEntity target, long expiresAt) {
        long currentTick = getServerTick(target);
        if (currentTick < 0L) {
            return;
        }

        freezeLivingUntil(target, currentTick, expiresAt);
    }

    private static void activateAreaTimeStop(Player defender, long currentTick, long expiresAt) {
        ACTIVE_LEVEL_THREE_FIELDS.put(defender.getUUID(), new ActiveLevelThreeField(expiresAt));
        refreshAreaTimeStop(defender, currentTick, expiresAt);
    }

    private static void refreshActiveLevelThreeFields(ServerTickEvent.Post event, long currentTick) {
        Iterator<Map.Entry<UUID, ActiveLevelThreeField>> iterator = ACTIVE_LEVEL_THREE_FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveLevelThreeField> entry = iterator.next();
            if (entry.getValue().expiresAtTick() <= currentTick) {
                iterator.remove();
                continue;
            }

            Player defender = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (defender == null || !defender.isAlive() || !hasTimeStopLevel(defender, 3) || !canTrigger(defender)) {
                iterator.remove();
                continue;
            }

            refreshAreaTimeStop(defender, currentTick, entry.getValue().expiresAtTick());
        }
    }

    private static void refreshAreaTimeStop(Player defender, long currentTick, long fieldExpiresAt) {
        AABB area = getLevelThreeFieldArea(defender);
        long refreshUntil = Math.min(fieldExpiresAt, currentTick + LEVEL_III_FIELD_REFRESH_GRACE_TICKS);

        for (Player player : defender.level().getEntitiesOfClass(Player.class, area, candidate -> candidate.isAlive() && candidate != defender)) {
            freezeLivingUntil(player, currentTick, refreshUntil);
        }

        for (Mob mob : defender.level().getEntitiesOfClass(Mob.class, area, candidate -> candidate.isAlive() && !isBoss(candidate))) {
            freezeLivingUntil(mob, currentTick, refreshUntil);
        }

        for (Projectile projectile : defender.level().getEntitiesOfClass(Projectile.class, area, Entity::isAlive)) {
            freezeProjectile(projectile, refreshUntil);
        }

        for (Entity entity : defender.level().getEntitiesOfClass(Entity.class, area,
                candidate -> candidate.isAlive()
                        && !(candidate instanceof LivingEntity)
                        && !(candidate instanceof Projectile)
                        && !(candidate instanceof Player)
                        && !isBoss(candidate))) {
            freezeOtherEntity(entity, refreshUntil);
        }
    }

    private static AABB getLevelThreeFieldArea(Player defender) {
        return defender.getBoundingBox().inflate(EnchantmentConfig.STARTUP.timeStop.fieldRadius.get());
    }

    private static void freezeLivingUntil(LivingEntity target, long currentTick, long expiresAt) {
        long appliedUntil = Math.max(FROZEN_LIVING_UNTIL.getOrDefault(target.getUUID(), 0L), expiresAt);
        syncTimeStopEffect(target, currentTick, appliedUntil);
        FROZEN_LIVING_UNTIL.put(target.getUUID(), appliedUntil);
        FROZEN_LIVING_STATES.putIfAbsent(target.getUUID(), captureLivingState(target));
        target.stopUsingItem();
        target.setDeltaMovement(Vec3.ZERO);
        clearAggro(target, null);
        freezeMobAi(target);
        lockLivingState(target);
    }

    private static void freezeProjectile(Projectile projectile, long expiresAt) {
        long appliedUntil = Math.max(FROZEN_PROJECTILE_UNTIL.getOrDefault(projectile.getUUID(), 0L), expiresAt);
        FROZEN_PROJECTILE_UNTIL.put(projectile.getUUID(), appliedUntil);
        FROZEN_PROJECTILE_POSITIONS.put(projectile.getUUID(), projectile.position());
        FROZEN_PROJECTILE_VELOCITIES.putIfAbsent(projectile.getUUID(), projectile.getDeltaMovement());
        projectile.setDeltaMovement(Vec3.ZERO);
    }

    private static void freezeOtherEntity(Entity entity, long expiresAt) {
        long appliedUntil = Math.max(FROZEN_OTHER_ENTITY_UNTIL.getOrDefault(entity.getUUID(), 0L), expiresAt);
        FROZEN_OTHER_ENTITY_UNTIL.put(entity.getUUID(), appliedUntil);
        FROZEN_OTHER_ENTITY_POSITIONS.put(entity.getUUID(), entity.position());
        FROZEN_OTHER_ENTITY_VELOCITIES.putIfAbsent(entity.getUUID(), entity.getDeltaMovement());
        entity.setDeltaMovement(Vec3.ZERO);
    }

    private static void syncTimeStopEffect(LivingEntity target, long currentTick, long expiresAt) {
        int duration = Math.max(1, (int) (expiresAt - currentTick));
        MobEffectInstance currentEffect = target.getEffect(ModMobEffects.TIME_STOP);
        if (currentEffect != null && currentEffect.getDuration() >= duration - 1) {
            return;
        }

        target.removeEffect(ModMobEffects.TIME_STOP);
        target.addEffect(new MobEffectInstance(ModMobEffects.TIME_STOP, duration, 0, false, true, true));
    }

    private static void suppressActions(LivingEntity living) {
        living.stopUsingItem();
        living.setDeltaMovement(Vec3.ZERO);
        living.fallDistance = 0.0F;
        suppressAnimations(living);

        if (living instanceof Player player) {
            player.setSprinting(false);
        }

        if (living instanceof Mob mob) {
            freezeMobAi(mob);
            clearAggro(mob, null);
            mob.getNavigation().stop();
        }
    }

    private static void freezeMobAi(LivingEntity living) {
        if (!(living instanceof Mob mob)) {
            return;
        }

        FROZEN_MOB_NO_AI_STATES.putIfAbsent(mob.getUUID(), mob.isNoAi());
        mob.setNoAi(true);
    }

    private static void restoreMobAi(LivingEntity living) {
        if (!(living instanceof Mob mob)) {
            return;
        }

        Boolean originalNoAi = FROZEN_MOB_NO_AI_STATES.remove(mob.getUUID());
        if (originalNoAi != null) {
            mob.setNoAi(originalNoAi);
        }
    }

    private static void suppressAnimations(LivingEntity living) {
        living.swinging = false;
        living.swingTime = 0;
        living.attackAnim = 0.0F;
        living.oAttackAnim = 0.0F;

        if (living.hurtTime > 0) {
            living.hurtTime = 1;
        }
    }

    private static FrozenLivingState captureLivingState(LivingEntity living) {
        return new FrozenLivingState(
                living.position(),
                living.getXRot(),
                living.getYRot(),
                living.yHeadRot,
                living.yBodyRot,
                living.xRotO,
                living.yRotO,
                living.yHeadRotO,
                living.yBodyRotO
        );
    }

    private static void lockLivingState(LivingEntity living) {
        FrozenLivingState frozenState = FROZEN_LIVING_STATES.get(living.getUUID());
        if (frozenState == null) {
            return;
        }

        Vec3 frozenPosition = frozenState.position();
        if (living.position().distanceToSqr(frozenPosition) > 1.0E-6D) {
            living.teleportTo(frozenPosition.x, frozenPosition.y, frozenPosition.z);
        }

        living.setXRot(frozenState.xRot());
        living.setYRot(frozenState.yRot());
        living.xRotO = frozenState.xRotO();
        living.yRotO = frozenState.yRotO();
        living.yHeadRot = frozenState.yHeadRot();
        living.yBodyRot = frozenState.yBodyRot();
        living.yHeadRotO = frozenState.yHeadRotO();
        living.yBodyRotO = frozenState.yBodyRotO();
    }

    private static void clearAggro(LivingEntity living, @Nullable Player player) {
        if (!(living instanceof Mob mob)) {
            return;
        }

        mob.setTarget(null);
        eraseBrainMemory(mob, MemoryModuleType.ATTACK_TARGET);
        eraseBrainMemory(mob, MemoryModuleType.AVOID_TARGET);
        eraseBrainMemory(mob, MemoryModuleType.ANGRY_AT);

        if (player != null && mob instanceof NeutralMob neutralMob && player.getUUID().equals(neutralMob.getPersistentAngerTarget())) {
            neutralMob.stopBeingAngry();
        }
    }

    private static void eraseBrainMemory(Mob mob, MemoryModuleType<?> memoryType) {
        try {
            mob.getBrain().eraseMemory(memoryType);
        } catch (IllegalStateException ignored) {
        }
    }

    private static List<Mob> findAggressors(Player player, boolean excludeFrozen) {
        double radius = EnchantmentConfig.STARTUP.timeStop.fieldRadius.get();
        AABB area = player.getBoundingBox().inflate(radius);
        List<Mob> result = new ArrayList<>();

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, candidate -> candidate.isAlive() && !isBoss(candidate))) {
            if (excludeFrozen && isTimeStopped(mob)) {
                continue;
            }
            if (hasHostileIntent(mob, player)) {
                result.add(mob);
            }
        }

        return result;
    }

    private static boolean hasHostileIntent(Mob mob, Player player) {
        if (mob.getTarget() == player) {
            return true;
        }
        if (mob instanceof NeutralMob neutralMob && player.getUUID().equals(neutralMob.getPersistentAngerTarget())) {
            return true;
        }
        try {
            return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(player::equals).isPresent();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private static boolean canTrigger(LivingEntity defender) {
        return !defender.level().isClientSide()
                && ModModules.isEnchantmentEnabled()
                && EnchantmentAvailability.isEnabled(ModEnchantments.TIME_STOP)
                && getTimeStopLevel(defender) > 0;
    }

    private static boolean hasTimeStopLevel(Player player, int minimumLevel) {
        return getTimeStopLevel(player) >= minimumLevel;
    }

    private static int getTimeStopLevel(LivingEntity wearer) {
        if (!ModModules.isEnchantmentEnabled()) {
            return 0;
        }

        Optional<Holder.Reference<Enchantment>> enchantment = wearer.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.TIME_STOP);

        if (enchantment.isEmpty()) {
            return 0;
        }

        return wearer.getItemBySlot(EquipmentSlot.CHEST).getEnchantmentLevel(enchantment.get());
    }

    private static int getFreezeDurationTicks(int level, TriggerMode mode) {
        return switch (mode) {
            case DAMAGE -> level >= 2 ? LEVEL_II_DURATION_TICKS : LEVEL_I_DURATION_TICKS;
            case SINGLE -> LEVEL_III_SINGLE_DURATION_TICKS;
            case FIELD -> LEVEL_III_FIELD_DURATION_TICKS;
        };
    }

    private static boolean isTimeStopped(LivingEntity living) {
        long currentTick = getServerTick(living);
        Long expiresAt = FROZEN_LIVING_UNTIL.get(living.getUUID());
        if (expiresAt == null || currentTick < 0L || expiresAt <= currentTick) {
            if (expiresAt != null) {
                clearLivingState(living);
            }
            return false;
        }
        return living.hasEffect(ModMobEffects.TIME_STOP);
    }

    private static boolean isProjectileTimeStopped(Projectile projectile) {
        Long expiresAt = FROZEN_PROJECTILE_UNTIL.get(projectile.getUUID());
        long currentTick = getServerTick(projectile);
        if (expiresAt == null || currentTick < 0L || expiresAt <= currentTick) {
            if (expiresAt != null) {
                clearProjectileState(projectile.getUUID());
            }
            return false;
        }
        return true;
    }

    private static boolean isOtherEntityTimeStopped(Entity entity) {
        Long expiresAt = FROZEN_OTHER_ENTITY_UNTIL.get(entity.getUUID());
        long currentTick = getServerTick(entity);
        if (expiresAt == null || currentTick < 0L || expiresAt <= currentTick) {
            if (expiresAt != null) {
                clearOtherEntityState(entity.getUUID());
            }
            return false;
        }
        return true;
    }

    private static boolean isTimeStoppedSource(DamageSource source) {
        LivingEntity attacker = resolveLivingAttacker(source);
        return attacker != null && isTimeStopped(attacker);
    }

    private static boolean isOnCooldown(LivingEntity defender, long currentTick) {
        Long cooldownUntil = DEFENDER_COOLDOWNS.get(defender.getUUID());
        return cooldownUntil != null && cooldownUntil > currentTick;
    }

    private static void setCooldown(LivingEntity defender, long currentTick) {
        DEFENDER_COOLDOWNS.put(defender.getUUID(), currentTick + EnchantmentConfig.STARTUP.timeStop.cooldownSeconds.get() * 20L);
    }

    private static boolean isBoss(Entity entity) {
        return entity.getType().is(BOSS_ENTITY_TYPES);
    }

    private static void cleanupExpiredState(MinecraftServer server, long currentTick) {
        DEFENDER_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
        PENDING_LEVEL_THREE_TRIGGERS.entrySet().removeIf(entry -> entry.getValue().triggerAtTick() + LEVEL_III_AGGREGATION_WINDOW_TICKS * 4L <= currentTick);
        ACTIVE_LEVEL_THREE_FIELDS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() <= currentTick);

        Iterator<Map.Entry<UUID, Long>> frozenLivingIterator = FROZEN_LIVING_UNTIL.entrySet().iterator();
        while (frozenLivingIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = frozenLivingIterator.next();
            if (entry.getValue() <= currentTick) {
                LivingEntity living = findLivingEntity(server, entry.getKey());
                if (living != null) {
                    restoreMobAi(living);
                } else {
                    FROZEN_MOB_NO_AI_STATES.remove(entry.getKey());
                }
                frozenLivingIterator.remove();
                FROZEN_LIVING_STATES.remove(entry.getKey());
            }
        }

        Iterator<Map.Entry<UUID, Long>> frozenProjectileIterator = FROZEN_PROJECTILE_UNTIL.entrySet().iterator();
        while (frozenProjectileIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = frozenProjectileIterator.next();
            if (entry.getValue() <= currentTick) {
                frozenProjectileIterator.remove();
                FROZEN_PROJECTILE_POSITIONS.remove(entry.getKey());
                FROZEN_PROJECTILE_VELOCITIES.remove(entry.getKey());
            }
        }

        Iterator<Map.Entry<UUID, Long>> frozenOtherIterator = FROZEN_OTHER_ENTITY_UNTIL.entrySet().iterator();
        while (frozenOtherIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = frozenOtherIterator.next();
            if (entry.getValue() <= currentTick) {
                frozenOtherIterator.remove();
                FROZEN_OTHER_ENTITY_POSITIONS.remove(entry.getKey());
                FROZEN_OTHER_ENTITY_VELOCITIES.remove(entry.getKey());
            }
        }
    }

    private static void clearLivingState(UUID entityId) {
        FROZEN_LIVING_UNTIL.remove(entityId);
        FROZEN_LIVING_STATES.remove(entityId);
        FROZEN_MOB_NO_AI_STATES.remove(entityId);
    }

    private static void clearLivingState(LivingEntity living) {
        restoreMobAi(living);
        clearLivingState(living.getUUID());
    }

    private static void clearProjectileState(UUID entityId) {
        FROZEN_PROJECTILE_UNTIL.remove(entityId);
        FROZEN_PROJECTILE_POSITIONS.remove(entityId);
        FROZEN_PROJECTILE_VELOCITIES.remove(entityId);
    }

    private static void clearOtherEntityState(UUID entityId) {
        FROZEN_OTHER_ENTITY_UNTIL.remove(entityId);
        FROZEN_OTHER_ENTITY_POSITIONS.remove(entityId);
        FROZEN_OTHER_ENTITY_VELOCITIES.remove(entityId);
    }

    @Nullable
    private static LivingEntity findLivingEntity(MinecraftServer server, UUID entityId) {
        Player player = server.getPlayerList().getPlayer(entityId);
        if (player != null) {
            return player;
        }

        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }

        return null;
    }

    @Nullable
    private static LivingEntity resolveLivingAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            return living;
        }

        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    private static long getServerTick(Entity entity) {
        return entity.level().getServer() != null ? entity.level().getServer().getTickCount() : -1L;
    }

    private record PendingLevelThreeTrigger(long triggerAtTick) {}

    private record ActiveLevelThreeField(long expiresAtTick) {}

    private record FrozenLivingState(
            Vec3 position,
            float xRot,
            float yRot,
            float yHeadRot,
            float yBodyRot,
            float xRotO,
            float yRotO,
            float yHeadRotO,
            float yBodyRotO
    ) {}

    private enum TriggerMode {
        DAMAGE,
        SINGLE,
        FIELD
    }
}
