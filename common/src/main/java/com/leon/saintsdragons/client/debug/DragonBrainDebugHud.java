package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonBrainDebug;
import com.leon.saintsdragons.common.network.MessageDragonPathDebug;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class DragonBrainDebugHud {
    private static final int PANEL_WIDTH = 310;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 5;
    private static final int BACKGROUND = 0xB0101218;

    private static Page selectedPage = Page.OVERVIEW;
    private static int selectedChunk;
    private static int contentCapacity = 1;
    private static int selectedEntityId = -1;

    private DragonBrainDebugHud() {
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        MessageDragonBrainDebug snapshot = DragonBrainDebugClient.getSnapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        ensureSelectedEntity(snapshot);
        int width = Math.min(PANEL_WIDTH, Math.max(120, screenWidth - 12));
        int maxLines = Math.max(4, (screenHeight - PADDING * 4) / LINE_HEIGHT);
        contentCapacity = Math.max(1, maxLines - 1);

        List<Line> content = buildPageLines(snapshot, selectedPage);
        int chunkCount = chunkCount(content.size());
        selectedChunk = Math.min(selectedChunk, chunkCount - 1);
        int firstLine = selectedChunk * contentCapacity;
        int lastLine = Math.min(content.size(), firstLine + contentCapacity);

        List<Line> lines = new ArrayList<>(1 + lastLine - firstLine);
        lines.add(new Line(header(snapshot, chunkCount), 0xFFFFFFFF));
        lines.addAll(content.subList(firstLine, lastLine));

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

    public static boolean cyclePage(int direction) {
        MessageDragonBrainDebug snapshot = DragonBrainDebugClient.getSnapshot();
        if (snapshot == null || direction == 0) {
            return false;
        }

        ensureSelectedEntity(snapshot);
        if (direction > 0) {
            int chunks = chunkCount(buildPageLines(snapshot, selectedPage).size());
            if (selectedChunk + 1 < chunks) {
                selectedChunk++;
            } else {
                selectedPage = selectedPage.next();
                selectedChunk = 0;
            }
        } else if (selectedChunk > 0) {
            selectedChunk--;
        } else {
            selectedPage = selectedPage.previous();
            selectedChunk = chunkCount(buildPageLines(snapshot, selectedPage).size()) - 1;
        }
        return true;
    }

    private static void ensureSelectedEntity(MessageDragonBrainDebug snapshot) {
        if (selectedEntityId == snapshot.entityId()) {
            return;
        }
        selectedEntityId = snapshot.entityId();
        selectedPage = Page.OVERVIEW;
        selectedChunk = 0;
    }

    private static int chunkCount(int lineCount) {
        return Math.max(1, (lineCount + contentCapacity - 1) / contentCapacity);
    }

    private static String header(MessageDragonBrainDebug snapshot, int chunkCount) {
        String chunk = chunkCount > 1
                ? " " + (selectedChunk + 1) + "/" + chunkCount
                : "";
        return selectedPage.title + chunk
                + " [" + (selectedPage.ordinal() + 1) + "/" + Page.values().length + "]"
                + " | " + snapshot.dragonName() + " #" + snapshot.entityId()
                + " brain@" + snapshot.gameTime();
    }

    private static List<Line> buildPageLines(MessageDragonBrainDebug snapshot, Page page) {
        return switch (page) {
            case OVERVIEW -> buildOverviewLines(snapshot);
            case BEHAVIOURS -> buildBehaviourLines(snapshot);
            case MEMORIES -> buildMemoryLines(snapshot);
            case PATHING -> buildPathingLines(snapshot);
        };
    }

    private static List<Line> buildOverviewLines(MessageDragonBrainDebug snapshot) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line("Hunger: " + snapshot.hunger() + "/" + snapshot.maxHunger()
                + " " + hungerMeter(snapshot.hunger(), snapshot.maxHunger())
                + "  hunt-food=" + (snapshot.huntFoodPursuit() ? "active" : "idle"),
                snapshot.huntFoodPursuit() ? 0xFFFFB454 : 0xFF65F58A));
        lines.add(new Line("Activity: " + snapshot.activeActivity()
                + "  active=" + String.join(",", snapshot.activeActivities()), 0xFF62D9FF));

        List<MessageDragonBrainDebug.BehaviourState> running = snapshot.behaviours().stream()
                .filter(behaviour -> "RUNNING".equals(behaviour.status()))
                .sorted(Comparator.comparingInt(MessageDragonBrainDebug.BehaviourState::priority))
                .toList();
        lines.add(new Line("Behaviours: " + running.size() + " running / "
                + snapshot.behaviours().size() + " registered", 0xFFFFD866));
        lines.add(new Line("Running: " + names(running), 0xFF65F58A));

        List<MessageDragonBrainDebug.BehaviourState> controlling = running.stream()
                .filter(MessageDragonBrainDebug.BehaviourState::claimsControl)
                .toList();
        lines.add(new Line("Control: " + names(controlling), 0xFFFFB454));

        long populatedMemories = snapshot.memories().stream()
                .filter(memory -> !"<empty>".equals(memory.value()))
                .count();
        lines.add(new Line("Memories: " + populatedMemories + " populated / "
                + snapshot.memories().size() + " tracked", 0xFFD5C7FF));

        MessageDragonPathDebug path = matchingPathSnapshot(snapshot);
        if (path == null) {
            lines.add(new Line("Pathing: unavailable", 0xFF9AA1AD));
        } else {
            lines.add(new Line("Pathing: " + path.locomotionMode() + " / " + path.movementMode()
                    + "  search=" + path.searchType() + "#" + path.searchId(), 0xFF8AD8FF));
        }
        return lines;
    }

    private static List<Line> buildBehaviourLines(MessageDragonBrainDebug snapshot) {
        List<Line> lines = new ArrayList<>();
        List<MessageDragonBrainDebug.BehaviourState> ordered = snapshot.behaviours().stream()
                .sorted(Comparator
                        .comparing((MessageDragonBrainDebug.BehaviourState state) -> !"RUNNING".equals(state.status()))
                        .thenComparingInt(MessageDragonBrainDebug.BehaviourState::priority))
                .toList();
        if (ordered.isEmpty()) {
            return List.of(new Line("No registered behaviours", 0xFF9AA1AD));
        }

        for (MessageDragonBrainDebug.BehaviourState behaviour : ordered) {
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
                    lines.add(new Line("    " + detail, 0xFFB9DCC3));
                }
            }
        }
        return lines;
    }

    private static List<Line> buildMemoryLines(MessageDragonBrainDebug snapshot) {
        if (snapshot.memories().isEmpty()) {
            return List.of(new Line("No tracked memories", 0xFF9AA1AD));
        }

        List<Line> lines = new ArrayList<>(snapshot.memories().size());
        for (MessageDragonBrainDebug.MemoryState memory : snapshot.memories()) {
            int color = "<empty>".equals(memory.value()) ? 0xFF9AA1AD : 0xFFD5C7FF;
            lines.add(new Line(memory.name() + ": " + memory.value(), color));
        }
        return lines;
    }

    private static List<Line> buildPathingLines(MessageDragonBrainDebug brainSnapshot) {
        MessageDragonPathDebug path = matchingPathSnapshot(brainSnapshot);
        if (path == null) {
            return List.of(new Line("No matching path snapshot", 0xFF9AA1AD));
        }

        List<Line> lines = new ArrayList<>();
        lines.add(new Line("Locomotion: " + path.locomotionMode()
                + "  movement=" + path.movementMode(), 0xFF8AD8FF));
        lines.add(new Line("Navigation: next=" + path.navigationNextIndex() + "/"
                + path.navigationNodeCount() + "  shown=" + path.navigationNodes().size()
                + "  first=" + path.navigationFirstIndex(), 0xFFB9DCC3));
        lines.add(new Line("Swim: next=" + path.swimNextIndex() + "/" + path.swimNodeCount()
                + "  shown=" + path.swimNodes().size() + "  first=" + path.swimFirstIndex(),
                0xFFB9DCC3));
        lines.add(new Line("Search: " + path.searchType() + "#" + path.searchId()
                + "  reached=" + path.searchReached()
                + "  time=" + path.searchDurationMicros() + "us", 0xFFFFD866));
        lines.add(new Line("Search sets: closed=" + path.searchClosedNodeCount()
                + " open=" + path.searchOpenNodeCount()
                + " candidates=" + path.searchCandidateNodeCount(), 0xFFFFD866));
        lines.add(new Line("Search route: " + position(path.searchStart())
                + " -> " + position(path.searchTarget()), 0xFFFFD866));
        lines.add(new Line("Movement target: " + position(path.movementTarget()), 0xFF65F58A));
        lines.add(new Line("Swim target: " + position(path.swimTarget())
                + "  endpoint=" + position(path.swimEndpoint()), 0xFF62D9FF));
        lines.add(new Line("Rejected target: " + position(path.rejectedTarget()), 0xFFFF7A7A));
        lines.add(new Line("Combat target: " + position(path.combatTarget()), 0xFFFFB454));
        lines.add(new Line("Swim controller: calculating=" + path.swimCalculating()
                + " moving=" + path.swimMoving()
                + " stuck=" + path.swimStuckTicks()
                + " retries=" + path.swimRetries(), 0xFFB9DCC3));
        return lines;
    }

    private static MessageDragonPathDebug matchingPathSnapshot(MessageDragonBrainDebug brainSnapshot) {
        MessageDragonPathDebug path = DragonPathDebugClient.getSnapshot();
        return path != null && path.entityId() == brainSnapshot.entityId() ? path : null;
    }

    private static String names(List<MessageDragonBrainDebug.BehaviourState> behaviours) {
        if (behaviours.isEmpty()) {
            return "none";
        }
        return behaviours.stream()
                .map(MessageDragonBrainDebug.BehaviourState::name)
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }

    private static String position(Vec3 position) {
        if (position == null) {
            return "none";
        }
        return String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)", position.x, position.y, position.z);
    }

    private static String hungerMeter(int hunger, int maximum) {
        int filled = maximum <= 0 ? 0 : Math.max(0, Math.min(10,
                hunger * 10 / maximum));
        return "[" + "#".repeat(filled) + "-".repeat(10 - filled) + "]";
    }

    private enum Page {
        OVERVIEW("Overview"),
        BEHAVIOURS("Behaviours"),
        MEMORIES("Memories"),
        PATHING("Pathing");

        private final String title;

        Page(String title) {
            this.title = title;
        }

        private Page next() {
            Page[] pages = values();
            return pages[(ordinal() + 1) % pages.length];
        }

        private Page previous() {
            Page[] pages = values();
            return pages[(ordinal() - 1 + pages.length) % pages.length];
        }
    }

    private record Line(String text, int color) {
    }
}
