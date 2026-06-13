package com.leon.saintsdragons.client.renderer.npc;

import com.leon.saintsdragons.client.model.npc.IvyTheDragonMerchantModel;
import com.leon.saintsdragons.client.renderer.layer.npc.IvyHeldItemLayer;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class IvyTheDragonMerchantRenderer extends GeoEntityRenderer<com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant> {
    private static final double CHATTER_RENDER_DISTANCE_SQR = 256.0D;
    private static final float CHATTER_Y_OFFSET = 0.42F;
    private static final long CHATTER_TYPE_INTERVAL_MS = 42L;
    private static final long VOICE_BLIP_INTERVAL_MS = 34L;
    private final Map<Integer, ChatterRenderState> chatterStates = new HashMap<>();

    @Override
    public float getMotionAnimThreshold(IvyTheDragonMerchant animatable) {
        return 0.000001f;
    }

    public IvyTheDragonMerchantRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new IvyTheDragonMerchantModel());
        this.addRenderLayer(new IvyHeldItemLayer(this));
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(@NotNull IvyTheDragonMerchant entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        renderIdleChatter(entity, poseStack, bufferSource, packedLight);
    }

    private void renderIdleChatter(IvyTheDragonMerchant entity,
                                   PoseStack poseStack,
                                   MultiBufferSource bufferSource,
                                   int packedLight) {
        String chatter = entity.getIdleChatterText();
        if (chatter.isEmpty()
                || entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > CHATTER_RENDER_DISTANCE_SQR) {
            return;
        }
        ChatterRenderState state = getChatterState(entity.getId(), chatter);
        String visibleChatter = state.visibleText();
        if (visibleChatter.isEmpty()) {
            return;
        }
        playVoiceBlipForNewText(visibleChatter, state);
        Font font = Minecraft.getInstance().font;
        float y = entity.getNameTagOffsetY() + CHATTER_Y_OFFSET;
        poseStack.pushPose();
        poseStack.translate(0.0F, y, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        float x = -font.width(visibleChatter) / 2.0F;
        font.drawInBatch(visibleChatter, x, 0.0F, 0xFFFFFFFF, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private ChatterRenderState getChatterState(int entityId, String text) {
        ChatterRenderState state = chatterStates.get(entityId);
        if (state == null || !state.text.equals(text)) {
            state = new ChatterRenderState(text);
            chatterStates.put(entityId, state);
        }
        return state;
    }

    private void playVoiceBlipForNewText(String visibleText, ChatterRenderState state) {
        long now = System.currentTimeMillis();
        if (now - state.lastVoiceBlipTime < VOICE_BLIP_INTERVAL_MS) {
            return;
        }
        int visibleCodePoints = visibleText.codePointCount(0, visibleText.length());
        if (visibleCodePoints <= state.lastBlipCodePoints) {
            return;
        }
        int totalCodePoints = state.text.codePointCount(0, state.text.length());
        int safeVisibleCodePoints = Math.min(visibleCodePoints, totalCodePoints);
        int playableIndex = -1;
        for (int index = state.lastBlipCodePoints; index < safeVisibleCodePoints; index++) {
            int charIndex = state.text.offsetByCodePoints(0, index);
            int codePoint = state.text.codePointAt(charIndex);
            if (shouldPlayVoiceBlip(codePoint, index)) {
                playableIndex = index;
                break;
            }
        }
        if (playableIndex >= 0) {
            float pitch = (float) ThreadLocalRandom.current().nextDouble(0.94D, 1.07D);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.IVY_VOICE_BLIP.get(), pitch, 0.42F));
            state.lastVoiceBlipTime = now;
            state.lastBlipCodePoints = playableIndex + 1;
            return;
        }
        state.lastBlipCodePoints = safeVisibleCodePoints;
    }

    private static boolean shouldPlayVoiceBlip(int codePoint, int codePointIndex) {
        if (Character.isWhitespace(codePoint)) {
            return false;
        }
        if (".,!?;:()[]{}\"'".indexOf(codePoint) >= 0) {
            return false;
        }
        return codePointIndex % 2 == 0;
    }

    private static final class ChatterRenderState {
        private final String text;
        private final long startedAt;
        private long lastVoiceBlipTime;
        private int lastBlipCodePoints;

        private ChatterRenderState(String text) {
            this.text = text;
            this.startedAt = System.currentTimeMillis();
        }

        private String visibleText() {
            int totalCodePoints = text.codePointCount(0, text.length());
            int visibleCodePoints = Math.min(totalCodePoints, (int) ((System.currentTimeMillis() - startedAt) / CHATTER_TYPE_INTERVAL_MS));
            if (visibleCodePoints <= 0) {
                return "";
            }
            return text.substring(0, text.offsetByCodePoints(0, visibleCodePoints));
        }
    }

}
