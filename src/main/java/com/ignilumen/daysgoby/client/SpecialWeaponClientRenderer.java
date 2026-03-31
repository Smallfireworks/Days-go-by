package com.ignilumen.daysgoby.client;

import com.ignilumen.daysgoby.registry.ModItems;
import com.ignilumen.daysgoby.specialweapon.SpecialWeaponEvents;
import com.ignilumen.daysgoby.util.XianSwordUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

public final class SpecialWeaponClientRenderer {
    private static final int GUARD_ROTATION_TICKS = 40;
    private static final int RING_SEGMENTS = 4;
    private static final float THIRD_PERSON_RADIUS = 1.10F;
    private static final float THIRD_PERSON_HEIGHT = 1.08F;
    private static final float FIRST_PERSON_RADIUS = 0.86F;
    private static final float FIRST_PERSON_Y_OFFSET = -0.18F;
    private static final float SWORD_SCALE = 0.84F;
    private static final float SHIELD_SCALE = 1.30F;
    private static final int FULL_BRIGHT = 15728880;

    private SpecialWeaponClientRenderer() {}

    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getEntity();
        if (!XianSwordUtil.isXianSword(player.getOffhandItem()) || player.isSpectator()) {
            return;
        }
        if (player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        float time = player.level().getGameTime() + event.getPartialTick();
        renderGuardOrbit(
                minecraft.getItemRenderer(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                player,
                time,
                event.getPackedLight(),
                THIRD_PERSON_RADIUS,
                THIRD_PERSON_HEIGHT,
                player.getId() * 31,
                false
        );
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson() || player.isSpectator() || !XianSwordUtil.isXianSword(player.getOffhandItem())) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float time = player.level().getGameTime() + partialTick;
        double x = Mth.lerp(partialTick, player.xOld, player.getX());
        double y = Mth.lerp(partialTick, player.yOld, player.getY()) + player.getEyeHeight() + FIRST_PERSON_Y_OFFSET;
        double z = Mth.lerp(partialTick, player.zOld, player.getZ());
        Vec3 cameraPosition = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(x - cameraPosition.x, y - cameraPosition.y, z - cameraPosition.z);
        renderGuardOrbit(
                minecraft.getItemRenderer(),
                poseStack,
                minecraft.renderBuffers().bufferSource(),
                player,
                time,
                FULL_BRIGHT,
                FIRST_PERSON_RADIUS,
                0.0F,
                player.getId() * 67,
                true
        );
        poseStack.popPose();
        minecraft.renderBuffers().bufferSource().endBatch();
    }

    private static void renderGuardOrbit(
            ItemRenderer itemRenderer,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            Player player,
            float time,
            int packedLight,
            float radius,
            float height,
            int seedBase,
            boolean firstPerson
    ) {
        double baseAngle = SpecialWeaponEvents.getGuardBaseAngle((long) time) + (time - (long) time) * (Math.PI * 2.0D / GUARD_ROTATION_TICKS);
        ItemStack swordStack = ModItems.XIAN_SWORD.get().getDefaultInstance();
        ItemStack shieldStack = Items.SHIELD.getDefaultInstance();

        for (int index = 0; index < RING_SEGMENTS; index++) {
            double angle = baseAngle + index * (Math.PI * 2.0D / RING_SEGMENTS);
            SpecialWeaponEvents.GuardZone zone = SpecialWeaponEvents.getGuardZoneForAngle(angle, (long) time);
            boolean shield = zone == SpecialWeaponEvents.GuardZone.SHIELD;
            renderGuardItem(itemRenderer, poseStack, bufferSource, player, shield ? shieldStack : swordStack, angle, shield, packedLight, radius, height, seedBase + index, firstPerson);
        }
    }

    private static void renderGuardItem(
            ItemRenderer itemRenderer,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            Player player,
            ItemStack stack,
            double angle,
            boolean shield,
            int packedLight,
            float radius,
            float height,
            int seed,
            boolean firstPerson
    ) {
        float x = (float) (Math.cos(angle) * radius);
        float z = (float) (Math.sin(angle) * radius);
        float rotationDegrees = (float) (-Math.toDegrees(angle) + 90.0D);

        poseStack.pushPose();
        poseStack.translate(x, height, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees));
        poseStack.mulPose(Axis.XP.rotationDegrees(firstPerson ? 4.0F : 12.0F));

        if (shield) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.scale(SHIELD_SCALE, SHIELD_SCALE, SHIELD_SCALE);
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(seed % 2 == 0 ? 18.0F : -18.0F));
            poseStack.scale(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE);
        }

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                player.level(),
                seed
        );
        poseStack.popPose();
    }
}

