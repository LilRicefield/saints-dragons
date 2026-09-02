package com.leon.saintsdragons.client.ui.dialogue;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.network.MessageDialogueChoice;
import com.leon.saintsdragons.common.network.MessageDialogueDismiss;
import com.leon.saintsdragons.common.network.MessageDialogueNameInput;
import com.leon.saintsdragons.common.network.MessageDialogueOpen;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public class IvyDialogueScreen extends Screen {
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/gui/dialogue/dialogue_box.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int BOX_U = 0;
    private static final int BOX_V = 80;
    private static final int BOX_W = 256;
    private static final int BOX_H = 96;
    private static final int TEXT_X = 8;
    private static final int TEXT_Y = 13;
    private static final int TEXT_W = 242;
    private static final int ARROW_U = 2;
    private static final int ARROW_V = 180;
    private static final int ARROW_W = 12;
    private static final int ARROW_H = 8;
    private static final int ARROW_X = 122;
    private static final int ARROW_Y = 81;
    private static final long SLIDE_IN_DURATION_MS = 270L;
    private static final long TEXT_START_DELAY_MS = 120L;
    private static final long TYPE_CHAR_INTERVAL_MS = 36L;
    private static final int BOTTOM_MARGIN = 1;
    private static final int CHOICE_MAX_W = 240;
    private static final int CHOICE_MAX_VISIBLE = 5;
    private static final int CHOICE_MIN_H = 16;
    private static final int CHOICE_GAP = 5;
    private static final int CHOICE_PADDING_X = 0;
    private static final int CHOICE_PADDING_Y = 4;
    private static final int CHOICE_VIEW_TOP_MARGIN = 8;
    private static final int CHOICE_VIEW_BOTTOM_GAP = 10;
    private static final long CAMERA_TURN_DURATION_MS = 260L;
    private static final long CONTINUATION_ARROW_DELAY_MS = 2200L;
    private static final long CONTINUATION_ARROW_FADE_MS = 650L;
    private static final long NORMAL_ARROW_DELAY_MS = 180L;
    private static final long NORMAL_ARROW_FADE_MS = 360L;
    private static final int NAME_INPUT_MAX_RAW_LENGTH = 128;
    private static final int NAME_INPUT_MAX_NON_WHITESPACE = 50;
    private static final long VOICE_BLIP_INTERVAL_MS = 72L;
    private static final double CAMERA_TARGET_Y_OFFSET = -0.25D;

    private MessageDialogueOpen message;
    private final long boxStartTime;
    private long textStartTime;
    private long choiceStartTime;
    private boolean textSkipped;
    private boolean choicesStarted;
    private int choiceScrollOffset;
    private int lastBlipCodePoints;
    private long lastVoiceBlipTime;
    private boolean cameraTurnStarted;
    private boolean cameraTurnActive;
    private long cameraTurnStartTime;
    private float cameraStartYaw;
    private float cameraStartPitch;
    private float cameraTargetYaw;
    private float cameraTargetPitch;
    private EditBox nameInput;
    private Button nameConfirmButton;

    public IvyDialogueScreen(MessageDialogueOpen message) {
        super(Component.translatable("saintsdragons.gui.dialogue.title"));
        this.message = message;
        long now = System.currentTimeMillis();
        this.boxStartTime = now;
        resetTextAnimation(now);
    }

    public void update(MessageDialogueOpen message) {
        this.message = message;
        resetTextAnimation(System.currentTimeMillis());
        if (minecraft != null) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        super.init();
        setupNameInputWidgets();
        beginCameraTurnToSpeaker();
    }

    private void resetTextAnimation(long now) {
        this.textStartTime = now;
        this.choiceStartTime = 0L;
        this.textSkipped = false;
        this.choicesStarted = false;
        this.choiceScrollOffset = 0;
        this.lastBlipCodePoints = 0;
        this.lastVoiceBlipTime = 0L;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateCameraTurn();
        int boxX = (width - BOX_W) / 2;
        int boxY = getBoxY();
        renderChoices(guiGraphics, boxY, mouseX, mouseY);
        guiGraphics.blit(TEXTURE, boxX, boxY, BOX_U, BOX_V, BOX_W, BOX_H, TEXTURE_SIZE, TEXTURE_SIZE);

        int textColor = 0x3A2A1E;
        if (!message.speaker().getString().isBlank()) {
            guiGraphics.drawString(font, message.speaker(), boxX + TEXT_X, boxY + TEXT_Y, textColor, false);
        }
        String visibleText = getVisibleText();
        playVoiceBlipForNewText(visibleText);
        guiGraphics.drawWordWrap(font, getVisibleTextComponent(visibleText), boxX + TEXT_X, boxY + TEXT_Y + 12, TEXT_W, textColor);

        if (isTextComplete()) {
            ensureChoicesStarted();
        }
        if (isTextComplete() && !isNameInputNode() && (message.choices().isEmpty() || isContinuationOnly())) {
            float alpha = getArrowAlpha();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blit(TEXTURE, boxX + ARROW_X, boxY + ARROW_Y, ARROW_U, ARROW_V, ARROW_W, ARROW_H, TEXTURE_SIZE, TEXTURE_SIZE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderChoices(GuiGraphics guiGraphics, int boxY, int mouseX, int mouseY) {
        int count = message.choices().size();
        if (count == 0 || !isTextComplete() || isContinuationOnly()) {
            return;
        }
        if (isNameInputNode()) {
            return;
        }
        ensureChoicesStarted();
        int choiceWidth = getChoiceWidth();
        int choiceX = (width - choiceWidth) / 2;
        int viewportBottom = boxY - CHOICE_VIEW_BOTTOM_GAP;
        int viewportHeight = getChoiceViewportHeight(boxY, choiceWidth);
        if (viewportHeight == 0) {
            return;
        }
        int viewportTop = viewportBottom - viewportHeight;
        int totalHeight = getChoicesTotalHeight(choiceWidth);
        int maxScroll = Math.max(0, totalHeight - viewportHeight);
        choiceScrollOffset = clamp(choiceScrollOffset, maxScroll);
        int choiceY = totalHeight <= viewportHeight ? viewportBottom - totalHeight : viewportTop - choiceScrollOffset;

        guiGraphics.enableScissor(0, viewportTop, width, viewportBottom);
        for (int i = 0; i < count; i++) {
            List<FormattedCharSequence> lines = getChoiceLines(i, choiceWidth);
            int choiceHeight = getChoiceHeight(lines);
            int y = choiceY;
            boolean hovered = mouseX >= choiceX && mouseX < choiceX + choiceWidth && mouseY >= y && mouseY < y + choiceHeight;
            guiGraphics.fill(choiceX - 4, y - 2, choiceX + choiceWidth + 4, y + choiceHeight + 2,
                    hovered ? 0xAAFFF1E1 : 0x66FFF1E1);
            int lineY = y + CHOICE_PADDING_Y;
            for (FormattedCharSequence line : lines) {
                guiGraphics.drawString(font, line, choiceX + CHOICE_PADDING_X, lineY, hovered ? 0x2A1B12 : 0x3A2A1E, false);
                lineY += font.lineHeight;
            }
            choiceY += choiceHeight + CHOICE_GAP;
        }
        guiGraphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!isTextComplete()) {
            skipText();
            return true;
        }
        int boxY = getBoxY();
        if (isNameInputNode()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (isContinuationOnly()) {
            ensureChoicesStarted();
            if (isContinuationReady()) {
                NetworkHandler.sendToServer(new MessageDialogueChoice(message.entityId(), 0));
            }
            return true;
        }
        int clickedChoice = getChoiceAt(mouseX, mouseY, boxY);
        if (clickedChoice >= 0) {
            NetworkHandler.sendToServer(new MessageDialogueChoice(message.entityId(), clickedChoice));
            return true;
        }
        if (message.choices().isEmpty()) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (message.choices().isEmpty() || !isTextComplete() || isNameInputNode() || isContinuationOnly()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int boxY = getBoxY();
        int choiceWidth = getChoiceWidth();
        int choiceX = (width - choiceWidth) / 2;
        int viewportBottom = boxY - CHOICE_VIEW_BOTTOM_GAP;
        int viewportHeight = getChoiceViewportHeight(boxY, choiceWidth);
        int viewportTop = viewportBottom - viewportHeight;
        if (mouseX < choiceX - 4 || mouseX >= choiceX + choiceWidth + 4
                || mouseY < viewportTop || mouseY >= viewportBottom) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int maxScroll = Math.max(0, getChoicesTotalHeight(choiceWidth) - viewportHeight);
        if (maxScroll == 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        choiceScrollOffset = clamp(choiceScrollOffset - (int) Math.round(delta * 18.0D), maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isTextComplete()) {
            if (isAdvanceKey(keyCode)) {
                skipText();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (isNameInputNode()) {
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && isTextComplete()) {
                submitCustomName();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (isContinuationOnly()) {
            if (isAdvanceKey(keyCode)) {
                ensureChoicesStarted();
                if (isContinuationReady()) {
                    NetworkHandler.sendToServer(new MessageDialogueChoice(message.entityId(), 0));
                }
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isAdvanceKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_SPACE
                || keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER;
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            NetworkHandler.sendToServer(new MessageDialogueDismiss(message.entityId()));
        }
        super.onClose();
    }

    private int getChoiceAt(double mouseX, double mouseY, int boxY) {
        ensureChoicesStarted();
        int count = message.choices().size();
        int choiceWidth = getChoiceWidth();
        int choiceX = (width - choiceWidth) / 2;
        int viewportBottom = boxY - CHOICE_VIEW_BOTTOM_GAP;
        int viewportHeight = getChoiceViewportHeight(boxY, choiceWidth);
        int viewportTop = viewportBottom - viewportHeight;
        if (mouseX < choiceX - 4 || mouseX >= choiceX + choiceWidth + 4
                || mouseY < viewportTop || mouseY >= viewportBottom) {
            return -1;
        }
        int totalHeight = getChoicesTotalHeight(choiceWidth);
        int choiceY = totalHeight <= viewportHeight ? viewportBottom - totalHeight : viewportTop - choiceScrollOffset;
        for (int i = 0; i < count; i++) {
            int choiceHeight = getChoiceHeight(getChoiceLines(i, choiceWidth));
            int y = choiceY;
            if (mouseX >= choiceX && mouseX < choiceX + choiceWidth && mouseY >= y && mouseY < y + choiceHeight) {
                return i;
            }
            choiceY += choiceHeight + CHOICE_GAP;
        }
        return -1;
    }

    private int getChoiceWidth() {
        return Math.max(80, Math.min(CHOICE_MAX_W, width - 32));
    }

    private int getChoicesTotalHeight(int choiceWidth) {
        return getChoicesHeight(choiceWidth, message.choices().size());
    }

    private int getChoiceViewportHeight(int boxY, int choiceWidth) {
        int availableHeight = Math.max(0, boxY - CHOICE_VIEW_BOTTOM_GAP - CHOICE_VIEW_TOP_MARGIN);
        int visibleChoices = Math.min(CHOICE_MAX_VISIBLE, message.choices().size());
        return Math.min(availableHeight, getChoicesHeight(choiceWidth, visibleChoices));
    }

    private int getChoicesHeight(int choiceWidth, int count) {
        int totalHeight = 0;
        for (int i = 0; i < count; i++) {
            totalHeight += getChoiceHeight(getChoiceLines(i, choiceWidth));
            if (i < count - 1) {
                totalHeight += CHOICE_GAP;
            }
        }
        return totalHeight;
    }

    private List<FormattedCharSequence> getChoiceLines(int index, int choiceWidth) {
        int textWidth = Math.max(1, choiceWidth - CHOICE_PADDING_X * 2);
        return font.split(message.choices().get(index).text(), textWidth);
    }

    private int getChoiceHeight(List<FormattedCharSequence> lines) {
        return Math.max(CHOICE_MIN_H, lines.size() * font.lineHeight + CHOICE_PADDING_Y * 2);
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private String getVisibleText() {
        String fullText = message.text().getString();
        if (textSkipped) {
            return fullText;
        }
        long elapsed = getTextElapsed() - TEXT_START_DELAY_MS;
        if (elapsed <= 0L) {
            return "";
        }
        int visibleCodePoints = (int) (elapsed / TYPE_CHAR_INTERVAL_MS);
        int totalCodePoints = fullText.codePointCount(0, fullText.length());
        if (visibleCodePoints >= totalCodePoints) {
            return fullText;
        }
        int endIndex = fullText.offsetByCodePoints(0, Math.max(0, visibleCodePoints));
        return fullText.substring(0, endIndex);
    }

    private Component getVisibleTextComponent(String visibleText) {
        int visibleCodePoints = visibleText.codePointCount(0, visibleText.length());
        int[] remaining = {visibleCodePoints};
        MutableComponent visible = Component.literal("");
        message.text().visit((style, text) -> {
            if (remaining[0] <= 0) {
                return java.util.Optional.empty();
            }
            int textCodePoints = text.codePointCount(0, text.length());
            int keptCodePoints = Math.min(remaining[0], textCodePoints);
            if (keptCodePoints > 0) {
                int endIndex = text.offsetByCodePoints(0, keptCodePoints);
                visible.append(Component.literal(text.substring(0, endIndex)).withStyle(style));
            }
            remaining[0] -= keptCodePoints;
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return visible;
    }

    private boolean isTextComplete() {
        if (textSkipped) {
            return true;
        }
        return getTextElapsed() >= getTextDoneElapsed();
    }

    private long getTextDoneElapsed() {
        int codePoints = message.text().getString().codePointCount(0, message.text().getString().length());
        return TEXT_START_DELAY_MS + codePoints * TYPE_CHAR_INTERVAL_MS;
    }

    private void skipText() {
        textSkipped = true;
        String fullText = message.text().getString();
        lastBlipCodePoints = fullText.codePointCount(0, fullText.length());
        ensureChoicesStarted();
    }

    private long getTextElapsed() {
        return Math.max(0L, System.currentTimeMillis() - textStartTime);
    }

    private long getBoxElapsed() {
        return Math.max(0L, System.currentTimeMillis() - boxStartTime);
    }

    private void ensureChoicesStarted() {
        if (choicesStarted || !isTextComplete()) {
            return;
        }
        choicesStarted = true;
        choiceStartTime = System.currentTimeMillis();
        updateNameInputVisibility();
    }

    private long getChoiceElapsed() {
        if (!choicesStarted) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - choiceStartTime);
    }

    private boolean isContinuationChoice(int index) {
        if (index < 0 || index >= message.choices().size()) {
            return false;
        }
        return "...".equals(message.choices().get(index).text().getString().trim());
    }

    private boolean isContinuationOnly() {
        return !isNameInputNode() && message.choices().size() == 1 && isContinuationChoice(0);
    }

    private boolean isNameInputNode() {
        return "NAME_INPUT".equalsIgnoreCase(message.nodeType());
    }

    private boolean isContinuationReady() {
        return getChoiceElapsed() >= getCurrentArrowDelay();
    }

    private float getArrowAlpha() {
        if (isContinuationOnly()) {
            long elapsed = getChoiceElapsed() - getCurrentArrowDelay();
            return Math.min(1.0F, Math.max(0.0F, (float) elapsed / getCurrentArrowFade()));
        }
        return Math.min(1.0F, Math.max(0.0F, (getTextElapsed() - getTextDoneElapsed() - 180.0F) / 360.0F));
    }

    private long getCurrentArrowDelay() {
        return isEllipsisLine() ? CONTINUATION_ARROW_DELAY_MS : NORMAL_ARROW_DELAY_MS;
    }

    private long getCurrentArrowFade() {
        return isEllipsisLine() ? CONTINUATION_ARROW_FADE_MS : NORMAL_ARROW_FADE_MS;
    }

    private boolean isEllipsisLine() {
        return "...".equals(message.text().getString().trim());
    }

    private void setupNameInputWidgets() {
        nameInput = null;
        nameConfirmButton = null;
        if (!isNameInputNode()) {
            return;
        }
        int boxY = height - BOX_H - BOTTOM_MARGIN;
        int inputWidth = Math.min(180, Math.max(120, width - 104));
        int buttonWidth = 64;
        int gap = 6;
        int totalWidth = inputWidth + gap + buttonWidth;
        int x = (width - totalWidth) / 2;
        int y = Math.max(8, boxY - 30);
        nameInput = new EditBox(font, x, y, inputWidth, 20, Component.literal("Name"));
        nameInput.setMaxLength(NAME_INPUT_MAX_RAW_LENGTH);
        nameInput.setFilter(value -> countNonWhitespace(value) <= NAME_INPUT_MAX_NON_WHITESPACE);
        nameInput.setResponder(value -> updateNameInputVisibility());
        addRenderableWidget(nameInput);
        nameConfirmButton = Button.builder(Component.literal("Confirm"), button -> submitCustomName())
                .bounds(x + inputWidth + gap, y, buttonWidth, 20)
                .build();
        addRenderableWidget(nameConfirmButton);
        updateNameInputVisibility();
    }

    private void updateNameInputVisibility() {
        if (nameInput == null || nameConfirmButton == null) {
            return;
        }
        boolean visible = isNameInputNode() && isTextComplete();
        nameInput.setVisible(visible);
        nameConfirmButton.visible = visible;
        nameConfirmButton.active = visible;
        if (visible && !nameInput.isFocused()) {
            nameInput.setFocused(true);
        }
    }

    private void submitCustomName() {
        String value = nameInput == null ? "" : nameInput.getValue();
        NetworkHandler.sendToServer(new MessageDialogueNameInput(message.entityId(), value));
    }

    private static int countNonWhitespace(String value) {
        int count = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (!Character.isWhitespace(codePoint)) {
                count++;
            }
        }
        return count;
    }

    private int getBoxY() {
        int targetY = height - BOX_H - BOTTOM_MARGIN;
        int startY = height + 8;
        float t = Math.min(1.0F, (float) getBoxElapsed() / SLIDE_IN_DURATION_MS);
        float eased = easeOutCubic(t);
        return Math.round(startY + (targetY - startY) * eased);
    }

    private void playVoiceBlipForNewText(String visibleText) {
        if (textSkipped || minecraft == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastVoiceBlipTime < VOICE_BLIP_INTERVAL_MS) {
            return;
        }
        int visibleCodePoints = visibleText.codePointCount(0, visibleText.length());
        if (visibleCodePoints <= lastBlipCodePoints) {
            return;
        }
        String fullText = message.text().getString();
        int totalCodePoints = fullText.codePointCount(0, fullText.length());
        int safeVisibleCodePoints = Math.min(visibleCodePoints, totalCodePoints);
        int playableIndex = -1;
        for (int index = lastBlipCodePoints; index < safeVisibleCodePoints; index++) {
            int charIndex = fullText.offsetByCodePoints(0, index);
            int codePoint = fullText.codePointAt(charIndex);
            if (shouldPlayVoiceBlip(codePoint, index)) {
                playableIndex = index;
                break;
            }
        }
        if (playableIndex >= 0) {
            float pitch = (float) ThreadLocalRandom.current().nextDouble(0.94D, 1.07D);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.IVY_VOICE_BLIP.get(), pitch, 0.42F));
            lastVoiceBlipTime = now;
            lastBlipCodePoints = playableIndex + 1;
            return;
        }
        lastBlipCodePoints = safeVisibleCodePoints;
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

    private void beginCameraTurnToSpeaker() {
        if (cameraTurnStarted || minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        Entity speaker = minecraft.level.getEntity(message.entityId());
        if (speaker == null) {
            return;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 target = speaker.getEyePosition().add(0.0D, CAMERA_TARGET_Y_OFFSET, 0.0D);
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        cameraStartYaw = minecraft.player.getYRot();
        cameraStartPitch = minecraft.player.getXRot();
        cameraTargetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        cameraTargetPitch = (float) (-(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG));
        cameraTurnStartTime = System.currentTimeMillis();
        cameraTurnStarted = true;
        cameraTurnActive = true;
    }

    private void updateCameraTurn() {
        if (!cameraTurnStarted) {
            beginCameraTurnToSpeaker();
        }
        if (!cameraTurnActive || minecraft == null || minecraft.player == null) {
            return;
        }
        float t = Math.min(1.0F, (float) (System.currentTimeMillis() - cameraTurnStartTime) / CAMERA_TURN_DURATION_MS);
        float eased = easeInOutCubic(t);
        float yaw = cameraStartYaw + Mth.wrapDegrees(cameraTargetYaw - cameraStartYaw) * eased;
        float pitch = Mth.lerp(eased, cameraStartPitch, cameraTargetPitch);
        minecraft.player.setYRot(yaw);
        minecraft.player.setYHeadRot(yaw);
        minecraft.player.setXRot(pitch);
        if (t >= 1.0F) {
            cameraTurnActive = false;
        }
    }

    private static float easeOutCubic(float t) {
        float f = 1.0F - t;
        return 1.0F - f * f * f;
    }

    private static float easeInOutCubic(float t) {
        if (t < 0.5F) {
            return 4.0F * t * t * t;
        }
        float f = -2.0F * t + 2.0F;
        return 1.0F - f * f * f / 2.0F;
    }
}
