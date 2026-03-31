package com.ignilumen.daysgoby.item;

import com.ignilumen.daysgoby.effect.DemonBleedEffect;
import com.ignilumen.daysgoby.registry.ModMobEffects;
import com.ignilumen.daysgoby.specialweapon.SpecialWeaponEvents;
import com.ignilumen.daysgoby.util.XianSwordUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class XianSwordItem extends SwordItem {
    private static final float BASE_ATTACK_DAMAGE = 2.0F;
    private static final float ATTACK_SPEED = -2.4F;
    private static final int ACTIVE_RANGE = 16;
    private static final int ACTIVE_COOLDOWN_TICKS = 100;
    private static final int MIN_ACTIVE_ENERGY = 20;
    private static final float XIAN_ACTIVE_BASE = 20.0F;
    private static final float XIAN_ACTIVE_SCALING = 0.7F;
    private static final float XIAN_UNDEAD_BASE = 10.0F;
    private static final float XIAN_UNDEAD_SCALING = 0.2F;
    private static final float DEMON_ACTIVE_BASE = 10.0F;
    private static final float DEMON_ACTIVE_SCALING = 0.4F;

    public XianSwordItem(Item.Properties properties) {
        super(Tiers.DIAMOND, properties
                .stacksTo(1)
                .attributes(SwordItem.createAttributes(Tiers.DIAMOND, BASE_ATTACK_DAMAGE, ATTACK_SPEED)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        SpecialWeaponEvents.ActiveSkillMode mode = player.isShiftKeyDown()
                ? SpecialWeaponEvents.ActiveSkillMode.DEMON
                : SpecialWeaponEvents.ActiveSkillMode.XIAN;
        XianSwordState state = XianSwordUtil.getState(stack);
        int energy = mode == SpecialWeaponEvents.ActiveSkillMode.XIAN
                ? state.currentXianQi(level.getGameTime())
                : Mth.floor(state.moQi());

        if (energy < MIN_ACTIVE_ENERGY) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable(
                        mode == SpecialWeaponEvents.ActiveSkillMode.XIAN
                                ? "message.daysgoby.xian_sword.not_enough_xian_qi"
                                : "message.daysgoby.xian_sword.not_enough_mo_qi"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        BeamSelection selection = findBeamSelection(level, player, ACTIVE_RANGE);
        if (selection.targets().isEmpty()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.daysgoby.xian_sword.no_target"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, ACTIVE_COOLDOWN_TICKS);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            executeActiveSkill(serverLevel, player, stack, state, mode, energy, selection);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        XianSwordState state = XianSwordUtil.getState(stack);
        String dangmoPercent = String.format(Locale.ROOT, "%.1f", state.dangmoPercent());
        String xianQi = Integer.toString(state.currentXianQi(
                context.level() != null ? context.level().getGameTime() : (state.lastSwordDamageTick() < 0L ? 0L : state.lastSwordDamageTick())
        ));
        String moQi = String.format(Locale.ROOT, "%.1f", state.moQi());

        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.dangmo_layers", state.dangmoStacks(), dangmoPercent)
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.undead_kills", state.undeadKills())
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.xian_qi", xianQi)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.mo_qi", moQi)
                .withStyle(ChatFormatting.DARK_RED));
        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.pressure")
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.dangmo")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.guard")
                .withStyle(ChatFormatting.BLUE));
        tooltipComponents.add(Component.translatable("tooltip.daysgoby.xian_sword.active")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static void executeActiveSkill(ServerLevel level, Player player, ItemStack stack, XianSwordState state,
            SpecialWeaponEvents.ActiveSkillMode mode, int energy, BeamSelection selection) {
        XianSwordState updatedState = mode == SpecialWeaponEvents.ActiveSkillMode.DEMON
                ? state.clearMoQi().markSwordDamage(level.getGameTime())
                : state.markSwordDamage(level.getGameTime());
        XianSwordUtil.setState(stack, updatedState);

        float baseDamage = mode == SpecialWeaponEvents.ActiveSkillMode.XIAN
                ? XIAN_ACTIVE_BASE + XIAN_ACTIVE_SCALING * energy
                : DEMON_ACTIVE_BASE + DEMON_ACTIVE_SCALING * energy;
        float undeadBonus = XIAN_UNDEAD_BASE + XIAN_UNDEAD_SCALING * energy;
        int swordCount = Math.max(1, energy / 10 + level.random.nextInt(5));

        spawnBeamParticles(level, player, stack, mode, swordCount, selection.start(), selection.end());
        if (mode == SpecialWeaponEvents.ActiveSkillMode.XIAN) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.9F, 1.2F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.9F, 0.8F);
        }

        for (int index = 0; index < selection.targets().size(); index++) {
            LivingEntity target = selection.targets().get(index);
            float scale = index == 0 ? 1.0F : index == 1 ? 0.7F : 0.5F;
            float damage = baseDamage * scale;

            if (mode == SpecialWeaponEvents.ActiveSkillMode.XIAN && target.getType().is(EntityTypeTags.UNDEAD)) {
                damage += undeadBonus * scale;
            }

            SpecialWeaponEvents.hurtWithActiveSkill(player, stack, mode, target, damage);
            spawnImpactParticles(level, stack, mode, target);

            if (mode == SpecialWeaponEvents.ActiveSkillMode.DEMON) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModMobEffects.DEMON_BLEED, 60, 0, false, true, true));
                DemonBleedEffect.trackSource(target, player);
            }
        }
    }

    private static BeamSelection findBeamSelection(Level level, Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 maxEnd = start.add(look.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        Vec3 beam = end.subtract(start);
        AABB searchBox = player.getBoundingBox().expandTowards(beam).inflate(1.5D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> target != player && target.isAlive() && !target.isSpectator());
        List<TargetHit> hits = new ArrayList<>();

        for (LivingEntity candidate : candidates) {
            Optional<Vec3> hitPosition = candidate.getBoundingBox().inflate(0.35D).clip(start, end);
            if (hitPosition.isPresent()) {
                hits.add(new TargetHit(candidate, start.distanceToSqr(hitPosition.get())));
            }
        }

        hits.sort(Comparator.comparingDouble(TargetHit::distanceSqr));
        List<LivingEntity> targets = hits.stream().map(TargetHit::target).toList();
        return new BeamSelection(start, end, targets);
    }

    private static void spawnBeamParticles(ServerLevel level, Player player, ItemStack stack, SpecialWeaponEvents.ActiveSkillMode mode,
            int swordCount, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = 12;

        for (int swordIndex = 0; swordIndex < swordCount; swordIndex++) {
            double phase = swordIndex / (double) Math.max(1, swordCount);
            for (int step = 0; step <= steps; step++) {
                double progress = Math.min(1.0D, Math.max(0.0D, step / (double) steps + phase * 0.08D));
                Vec3 point = start.add(delta.scale(progress));
                if (mode == SpecialWeaponEvents.ActiveSkillMode.XIAN) {
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), point.x, point.y, point.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                    level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
                } else {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                    level.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }

        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.0D), player.getZ(), Math.max(8, swordCount), 0.35D, 0.45D, 0.35D, 0.02D);
    }

    private static void spawnImpactParticles(ServerLevel level, ItemStack stack, SpecialWeaponEvents.ActiveSkillMode mode, LivingEntity target) {
        double x = target.getX();
        double y = target.getY(0.6D);
        double z = target.getZ();

        if (mode == SpecialWeaponEvents.ActiveSkillMode.XIAN) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), x, y, z, 12, 0.2D, 0.3D, 0.2D, 0.02D);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 10, 0.25D, 0.35D, 0.25D, 0.0D);
        } else {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 16, 0.25D, 0.35D, 0.25D, 0.01D);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 8, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }

    private record TargetHit(LivingEntity target, double distanceSqr) {}

    private record BeamSelection(Vec3 start, Vec3 end, List<LivingEntity> targets) {}
}





