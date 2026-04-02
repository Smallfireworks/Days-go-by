package com.ignilumen.daysgoby.enchantment;

import com.ignilumen.daysgoby.config.EnchantmentConfig;
import com.ignilumen.daysgoby.module.ModModules;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ShitRainEvents {
    private static final float VOMIT_CHANCE = 0.35F;
    private static final int PLAYER_NAUSEA_TICKS = 160;
    private static final int MOB_NAUSEA_TICKS = 100;
    private static final int ATTACK_INTENT_WINDOW_TICKS = 40;
    private static final int RETALIATION_UNLOCK_TICKS = 200;
    private static final int FEAR_DURATION_TICKS = 120;
    private static final float AVOID_DISTANCE = 10.0F;
    private static final int AVOID_PRIORITY = 1;
    private static final double AVOID_WALK_SPEED = 1.1D;
    private static final double AVOID_SPRINT_SPEED = 1.35D;
    private static final int CLEANUP_INTERVAL = 40;
    private static final int BROWN_PARTICLE_COLOR = 0xFF6B4528;
    private static final TagKey<EntityType<?>> BOSS_ENTITY_TYPES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final SoundEvent VOMIT_SOUND = SoundEvents.FOX_SPIT;
    private static final SoundEvent BURP_SOUND = SoundEvents.PLAYER_BURP;

    private static final Map<UUID, FoodSnapshot> FOOD_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, FearState> FEAR_STATES = new HashMap<>();
    private static final Map<UUID, Long> ATTACK_INTENT_UNTIL = new HashMap<>();
    private static final Map<UUID, ShitRainAvoidGoal> FEAR_GOALS = new HashMap<>();
    private static final Map<MobPlayerPair, Long> RETALIATION_UNLOCKS = new HashMap<>();

    private ShitRainEvents() {}

    public static void onFoodUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || getShitRainLevel(player) <= 0 || !isEdibleFood(event.getItem())) {
            return;
        }

        FoodData foodData = player.getFoodData();
        FOOD_SNAPSHOTS.put(player.getUUID(), new FoodSnapshot(foodData.getFoodLevel(), foodData.getSaturationLevel()));
    }

    public static void onFoodUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            FOOD_SNAPSHOTS.remove(player.getUUID());
        }
    }

    public static void onFoodUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        FoodSnapshot snapshot = FOOD_SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null || getShitRainLevel(player) <= 0 || !isEdibleFood(event.getItem()) || player.getRandom().nextFloat() > VOMIT_CHANCE) {
            return;
        }

        FoodData foodData = player.getFoodData();
        int gainedFood = Math.max(0, foodData.getFoodLevel() - snapshot.foodLevel());
        float gainedSaturation = Math.max(0.0F, foodData.getSaturationLevel() - snapshot.saturationLevel());

        if (gainedFood > 0) {
            foodData.setFoodLevel(foodData.getFoodLevel() - gainedFood);
        }
        if (gainedSaturation > 0.0F) {
            foodData.setSaturation(foodData.getSaturationLevel() - gainedSaturation);
        }

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, PLAYER_NAUSEA_TICKS, 0, false, true, true));

        if (player.level() instanceof ServerLevel serverLevel) {
            spawnBrownParticles(serverLevel, player, 18, 0.28D, 0.22D);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), VOMIT_SOUND, SoundSource.PLAYERS, 0.9F, 0.9F);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), BURP_SOUND, SoundSource.PLAYERS, 0.35F, 0.55F);
        }
    }

    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        markAttackIntent(player);

        if (event.getTarget() instanceof Mob mob) {
            unlockRetaliation(mob, player);
        }
    }

    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getLevel().isClientSide() || event.getCharge() <= 0) {
            return;
        }

        Player player = event.getEntity();
        if (!event.hasAmmo() && !player.getAbilities().instabuild) {
            return;
        }

        markAttackIntent(player);
    }

    public static void onProjectileJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof Projectile projectile)) {
            return;
        }

        if (projectile.getOwner() instanceof Player player && isAttackIntentProjectile(projectile)) {
            markAttackIntent(player);
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            handlePlayerDamagedByMob(event.getSource(), player);
            return;
        }

        if (event.getEntity() instanceof Mob mob && event.getSource().getEntity() instanceof Player player) {
            unlockRetaliation(mob, player);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            clearPlayerState(player);
        } else if (entity instanceof Mob mob) {
            clearMobState(mob);
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            clearPlayerState(player);
        } else if (event.getEntity() instanceof Mob mob) {
            clearMobState(mob);
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CLEANUP_INTERVAL != 0) {
            return;
        }

        cleanupMissingEntities(event);
    }

    private static void handlePlayerDamagedByMob(DamageSource source, Player player) {
        if (getShitRainLevel(player) <= 0) {
            return;
        }

        Mob attacker = resolveDirectMeleeMob(source);
        if (attacker == null || !canAffectMob(attacker)) {
            return;
        }

        if (hasRetaliationUnlock(attacker, player) || hasAttackIntent(player)) {
            unlockRetaliation(attacker, player);
            return;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            spawnBrownParticles(serverLevel, player, 14, 0.35D, 0.18D);
            spawnBrownParticles(serverLevel, attacker, 14, 0.35D, 0.18D);
        }

        attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, MOB_NAUSEA_TICKS, 0, false, true, true));
        frightenMob(attacker, player);
    }

    @Nullable
    private static Mob resolveDirectMeleeMob(DamageSource source) {
        if (!source.is(DamageTypes.MOB_ATTACK) && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
            return null;
        }

        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile) {
            return null;
        }

        if (source.getEntity() instanceof Mob mob) {
            return mob;
        }

        if (directEntity instanceof Mob mob) {
            return mob;
        }

        return null;
    }

    private static void frightenMob(Mob mob, Player player) {
        long currentTick = getServerTick(mob);
        if (currentTick < 0L) {
            return;
        }

        FEAR_STATES.put(mob.getUUID(), new FearState(player.getUUID(), currentTick + FEAR_DURATION_TICKS));
        clearAggro(mob, player);

        if (mob instanceof PathfinderMob pathfinderMob) {
            FEAR_GOALS.computeIfAbsent(mob.getUUID(), ignored -> {
                ShitRainAvoidGoal goal = new ShitRainAvoidGoal(pathfinderMob);
                pathfinderMob.goalSelector.addGoal(AVOID_PRIORITY, goal);
                return goal;
            });
        }
    }

    private static void unlockRetaliation(Mob mob, Player player) {
        long currentTick = getServerTick(mob);
        if (currentTick < 0L) {
            return;
        }

        RETALIATION_UNLOCKS.put(new MobPlayerPair(mob.getUUID(), player.getUUID()), currentTick + RETALIATION_UNLOCK_TICKS);
        clearFearForPair(mob, player.getUUID());
    }

    private static void clearFearForPair(Mob mob, UUID playerId) {
        FearState fearState = FEAR_STATES.get(mob.getUUID());
        if (fearState != null && fearState.playerId().equals(playerId)) {
            FEAR_STATES.remove(mob.getUUID());
            removeAvoidGoal(mob);
        }
    }

    private static void markAttackIntent(Player player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        ATTACK_INTENT_UNTIL.put(player.getUUID(), (long) server.getTickCount() + ATTACK_INTENT_WINDOW_TICKS);
    }

    private static boolean hasAttackIntent(Player player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        Long attackIntentUntil = ATTACK_INTENT_UNTIL.get(player.getUUID());
        if (attackIntentUntil == null) {
            return false;
        }

        if (attackIntentUntil <= server.getTickCount()) {
            ATTACK_INTENT_UNTIL.remove(player.getUUID());
            return false;
        }

        return true;
    }

    private static boolean hasRetaliationUnlock(Mob mob, Player player) {
        MobPlayerPair pair = new MobPlayerPair(mob.getUUID(), player.getUUID());
        long currentTick = getServerTick(mob);
        if (currentTick < 0L) {
            RETALIATION_UNLOCKS.remove(pair);
            return false;
        }

        Long expiresAtTick = RETALIATION_UNLOCKS.get(pair);
        if (expiresAtTick == null) {
            return false;
        }

        if (expiresAtTick <= currentTick) {
            RETALIATION_UNLOCKS.remove(pair);
            return false;
        }

        return true;
    }

    private static boolean canAffectMob(Mob mob) {
        return !mob.getType().is(BOSS_ENTITY_TYPES) && isWhitelistedMob(mob);
    }

    private static boolean isWhitelistedMob(Mob mob) {
        List<? extends String> whitelist = EnchantmentConfig.STARTUP.shitRain.affectedMobWhitelist.get();
        if (whitelist.isEmpty()) {
            return true;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        for (String entry : whitelist) {
            ResourceLocation allowedId = ResourceLocation.tryParse(entry);
            if (allowedId != null && allowedId.equals(entityId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAttackIntentProjectile(Projectile projectile) {
        return projectile instanceof AbstractArrow || projectile instanceof ThrownTrident;
    }

    private static void clearPlayerState(Player player) {
        UUID playerId = player.getUUID();
        FOOD_SNAPSHOTS.remove(playerId);
        ATTACK_INTENT_UNTIL.remove(playerId);

        Iterator<Map.Entry<MobPlayerPair, Long>> retaliationIterator = RETALIATION_UNLOCKS.entrySet().iterator();
        while (retaliationIterator.hasNext()) {
            if (retaliationIterator.next().getKey().playerId().equals(playerId)) {
                retaliationIterator.remove();
            }
        }

        MinecraftServer server = player.level().getServer();
        Iterator<Map.Entry<UUID, FearState>> fearIterator = FEAR_STATES.entrySet().iterator();
        while (fearIterator.hasNext()) {
            Map.Entry<UUID, FearState> entry = fearIterator.next();
            if (!entry.getValue().playerId().equals(playerId)) {
                continue;
            }

            if (server != null) {
                Mob mob = findMob(server, entry.getKey());
                if (mob != null) {
                    removeAvoidGoal(mob);
                } else {
                    FEAR_GOALS.remove(entry.getKey());
                }
            } else {
                FEAR_GOALS.remove(entry.getKey());
            }

            fearIterator.remove();
        }
    }

    private static void clearMobState(Mob mob) {
        FEAR_STATES.remove(mob.getUUID());
        RETALIATION_UNLOCKS.entrySet().removeIf(entry -> entry.getKey().mobId().equals(mob.getUUID()));
        removeAvoidGoal(mob);
    }

    private static void removeAvoidGoal(Mob mob) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        ShitRainAvoidGoal goal = FEAR_GOALS.remove(mob.getUUID());
        if (goal != null) {
            pathfinderMob.goalSelector.removeGoal(goal);
        }
    }

    private static void cleanupMissingEntities(ServerTickEvent.Post event) {
        long currentTick = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, FearState>> fearIterator = FEAR_STATES.entrySet().iterator();

        while (fearIterator.hasNext()) {
            Map.Entry<UUID, FearState> entry = fearIterator.next();
            Mob mob = findMob(event.getServer(), entry.getKey());
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getValue().playerId());

            if (mob == null || player == null || !player.isAlive() || entry.getValue().expiresAtTick() <= currentTick) {
                if (mob != null) {
                    removeAvoidGoal(mob);
                } else {
                    FEAR_GOALS.remove(entry.getKey());
                }
                fearIterator.remove();
            }
        }

        ATTACK_INTENT_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= currentTick || event.getServer().getPlayerList().getPlayer(entry.getKey()) == null);
        RETALIATION_UNLOCKS.entrySet().removeIf(entry -> entry.getValue() <= currentTick
                || findMob(event.getServer(), entry.getKey().mobId()) == null
                || event.getServer().getPlayerList().getPlayer(entry.getKey().playerId()) == null);
    }

    @Nullable
    private static Mob findMob(MinecraftServer server, UUID mobId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(mobId);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    private static long getServerTick(Entity entity) {
        MinecraftServer server = entity.level().getServer();
        return server != null ? server.getTickCount() : -1L;
    }

    private static void clearAggro(Mob mob, Player player) {
        mob.setTarget(null);
        eraseBrainMemory(mob, MemoryModuleType.ATTACK_TARGET);
        eraseBrainMemory(mob, MemoryModuleType.AVOID_TARGET);
        eraseBrainMemory(mob, MemoryModuleType.ANGRY_AT);

        if (mob instanceof NeutralMob neutralMob && player.getUUID().equals(neutralMob.getPersistentAngerTarget())) {
            neutralMob.stopBeingAngry();
        }
    }

    private static void eraseBrainMemory(Mob mob, MemoryModuleType<?> memoryType) {
        try {
            mob.getBrain().eraseMemory(memoryType);
        } catch (IllegalStateException ignored) {
            // Not every mob registers every brain memory.
        }
    }

    private static void spawnBrownParticles(ServerLevel level, LivingEntity entity, int count, double horizontalSpread, double verticalSpread) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                entity.getX(),
                entity.getY(0.65D),
                entity.getZ(),
                Math.max(6, count / 2),
                horizontalSpread,
                verticalSpread,
                horizontalSpread,
                0.01D
        );
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, BROWN_PARTICLE_COLOR),
                entity.getX(),
                entity.getY(0.65D),
                entity.getZ(),
                count,
                horizontalSpread,
                verticalSpread,
                horizontalSpread,
                0.0D
        );
    }

    private static boolean isEdibleFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && stack.getUseAnimation() == UseAnim.EAT;
    }

    private static int getShitRainLevel(Player player) {
        if (!ModModules.isEnchantmentEnabled()) {
            return 0;
        }

        Optional<Holder.Reference<Enchantment>> enchantment = player.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.SHIT_RAIN);

        if (enchantment.isEmpty()) {
            return 0;
        }

        return player.getItemBySlot(EquipmentSlot.LEGS).getEnchantmentLevel(enchantment.get());
    }

    private record FoodSnapshot(int foodLevel, float saturationLevel) {}

    private record FearState(UUID playerId, long expiresAtTick) {}

    private record MobPlayerPair(UUID mobId, UUID playerId) {}

    private static final class ShitRainAvoidGoal extends Goal {
        private final PathfinderMob mob;
        @Nullable
        private Player fearedPlayer;
        @Nullable
        private Path path;

        private ShitRainAvoidGoal(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            FearState fearState = FEAR_STATES.get(this.mob.getUUID());
            if (fearState == null || fearState.expiresAtTick() <= getServerTick(this.mob) || !(this.mob.level() instanceof ServerLevel serverLevel)) {
                return false;
            }

            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(fearState.playerId());
            if (player == null || !player.isAlive() || hasRetaliationUnlock(this.mob, player)) {
                return false;
            }

            Vec3 destination = DefaultRandomPos.getPosAway(this.mob, 16, 7, player.position());
            if (destination == null || player.distanceToSqr(destination.x, destination.y, destination.z) < player.distanceToSqr(this.mob)) {
                return false;
            }

            this.path = this.mob.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
            if (this.path == null) {
                return false;
            }

            this.fearedPlayer = player;
            return this.mob.distanceToSqr(player) <= AVOID_DISTANCE * AVOID_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            FearState fearState = FEAR_STATES.get(this.mob.getUUID());
            return this.fearedPlayer != null
                    && this.fearedPlayer.isAlive()
                    && fearState != null
                    && fearState.expiresAtTick() > getServerTick(this.mob)
                    && !hasRetaliationUnlock(this.mob, this.fearedPlayer)
                    && !this.mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.mob.getNavigation().moveTo(this.path, AVOID_WALK_SPEED);
        }

        @Override
        public void stop() {
            this.fearedPlayer = null;
            this.path = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.fearedPlayer == null) {
                return;
            }

            clearAggro(this.mob, this.fearedPlayer);
            this.mob.getNavigation().setSpeedModifier(this.mob.distanceToSqr(this.fearedPlayer) < 49.0D ? AVOID_SPRINT_SPEED : AVOID_WALK_SPEED);
        }
    }
}
