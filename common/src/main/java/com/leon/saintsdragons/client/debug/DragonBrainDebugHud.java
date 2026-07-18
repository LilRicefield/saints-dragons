package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonBrainDebug;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class DragonBrainDebugHud {
    private static final int PANEL_WIDTH = 310;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 5;
    private static final int BACKGROUND = 0xB0101218;

    private DragonBrainDebugHud() {
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        MessageDragonBrainDebug snapshot = DragonBrainDebugClient.getSnapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        int width = Math.min(PANEL_WIDTH, Math.max(120, screenWidth - 12));
        int maxLines = Math.max(4, (screenHeight - PADDING * 4) / LINE_HEIGHT);
        List<Line> lines = buildLines(snapshot, maxLines);
        int x = 6;
        int y = 6;
        int height = PADDING * 2 + lines.size() * LINE_HEIGHT;
        graphics.fill(x, y, x + width, y + height, BACKGROUND);

        Font font = minecraft.font;
        int textY = y + PADDING;
        int textWidth = width - PADDING * 2;
        for (Line line : lines) {
            String text = font.plainSubstrByWidth(line.text(), textWidth);
            graphics.drawString(font, text, x + PADDING, textY, line.color(), true);
            textY += LINE_HEIGHT;
        }
    }

    private static List<Line> buildLines(MessageDragonBrainDebug snapshot, int maxLines) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(snapshot.dragonName() + " #" + snapshot.entityId()
                + "  brain@" + snapshot.gameTime(), 0xFFFFFFFF));
        lines.add(new Line("Activity: " + snapshot.activeActivity()
                + "  active=" + String.join(",", snapshot.activeActivities()), 0xFF62D9FF));

        List<MessageDragonBrainDebug.BehaviourState> ordered = snapshot.behaviours().stream()
                .sorted(Comparator
                        .comparing((MessageDragonBrainDebug.BehaviourState state) -> !"RUNNING".equals(state.status()))
                        .thenComparingInt(MessageDragonBrainDebug.BehaviourState::priority))
                .toList();
        if (!ordered.isEmpty()) {
            lines.add(new Line("Behaviours", 0xFFFFD866));
        }
        for (MessageDragonBrainDebug.BehaviourState behaviour : ordered) {
            if (lines.size() >= maxLines) {
                break;
            }
            String prefix;
            int color;
            if ("RUNNING".equals(behaviour.status())) {
                prefix = "+";
                color = 0xFF65F58A;
            } else if ("COOLDOWN".equals(behaviour.status())) {
                prefix = "~";
                color = 0xFFFFB454;
            } else {
                prefix = "-";
                color = 0xFF9AA1AD;
            }
            String suffix = behaviour.cooldownTicks() > 0L ? " cd=" + behaviour.cooldownTicks() : "";
            if (behaviour.claimsControl()) {
                suffix += " control";
            }
            lines.add(new Line(prefix + " [" + behaviour.activity() + ":" + behaviour.priority()
                    + "] " + behaviour.name() + suffix, color));
            if ("RUNNING".equals(behaviour.status())) {
                for (String detail : behaviour.details()) {
                    if (lines.size() >= maxLines) {
                        break;
                    }
                    lines.add(new Line("    " + detail, 0xFFB9DCC3));
                }
            }
        }

        if (!snapshot.memories().isEmpty() && lines.size() < maxLines) {
            lines.add(new Line("Memories", 0xFFFFD866));
        }
        for (MessageDragonBrainDebug.MemoryState memory : snapshot.memories()) {
            if (lines.size() >= maxLines) {
                break;
            }
            lines.add(new Line(memory.name() + ": " + memory.value(), 0xFFD5C7FF));
        }

        if (lines.size() >= maxLines) {
            lines.set(lines.size() - 1, new Line("...", 0xFF9AA1AD));
        }
        return lines;
    }

    private record Line(String text, int color) {
    }
}
