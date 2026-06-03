package com.leon.saintsdragons.client.ui.codex;

import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class CodexEcologyPanel {
    private static final int TEXT_BOX_WIDTH = 131;
    private static final int TEXT_BOX_HEIGHT = 170;
    private static final int TEXT_LINE_SPACING = 10;
    private static final int PAGE_NAV_OFFSET_Y = 170;
    private static final int LINES_PER_TEXT_PAGE = TEXT_BOX_HEIGHT / TEXT_LINE_SPACING;
    private static final int LINK_GAP = 2;
    private static final int VISIBLE_LINKS = 3;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 25;
    private final List<CodexPageLink> ecologyPageLinks = new ArrayList<>();
    private static final List<ResourceLocation> IGNIVORUS_DROPS = List.of(
            new ResourceLocation("saintsdragons", "ignivorus_scale"),
            new ResourceLocation("saintsdragons", "ignivorus_tooth"),
            new ResourceLocation("saintsdragons", "ignivorus_heart"),
            new ResourceLocation("saintsdragons", "ignivorus_egg")
    );
    private static final List<ResourceLocation> RAEVYX_DROPS = List.of(
            new ResourceLocation("saintsdragons", "raevyx_scale"),
            new ResourceLocation("saintsdragons", "raevyx_egg")
    );
    private static final List<ResourceLocation> VARASUCHUS_DROPS = List.of(
            new ResourceLocation("saintsdragons", "varasuchus_scale"),
            new ResourceLocation("saintsdragons", "varasuchus_egg")
    );
    private static final List<ResourceLocation> CINDERVANE_DROPS = List.of(
            new ResourceLocation("saintsdragons", "cindervane_scale"),
            new ResourceLocation("saintsdragons", "cindervane_egg")
    );
    private static final List<ResourceLocation> STEGONAUT_DROPS = List.of(
            new ResourceLocation("saintsdragons", "stegonaut_scale"),
            new ResourceLocation("saintsdragons", "stegonaut_egg")
    );
    private static final List<ResourceLocation> VOLITANS_DROPS = List.of(
            new ResourceLocation("saintsdragons", "volitans_scale"),
            new ResourceLocation("saintsdragons", "volitans_spine"),
            new ResourceLocation("saintsdragons", "volitans_egg"),
            new ResourceLocation("minecraft", "salmon"),
            new ResourceLocation("minecraft", "cod"),
            new ResourceLocation("minecraft", "tropical_fish"),
            new ResourceLocation("minecraft", "pufferfish")
    );
    private Button ecologyPrevPageButton;
    private Button ecologyNextPageButton;
    private int linkScrollOffset = 0;
    private int lastLinkX;
    private int lastLinkY;
    private int lastLinkWidth;
    private int lastLinkHeight;
    private int lastLinkCount;
    private boolean linkAreaValid = false;


    public void initWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> addWidget,
                            Font font, int leftPos, int topPos, IntSupplier pageSupplier, IntConsumer pageSetter,
                            Supplier<CodexDragonEntry> selectedSupplier, Runnable visibilityUpdater) {
        int contentX = leftPos + 229;
        int contentY = topPos - 3;
        int startY = contentY + 16;
        int navY = startY + PAGE_NAV_OFFSET_Y;
        int centerX = contentX + 63;

        ecologyPrevPageButton = Button.builder(
                        Component.literal("<"),
                        button -> {
                            int page = pageSupplier.getAsInt();
                            if (page > 1) {
                                pageSetter.accept(page - 1);
                                visibilityUpdater.run();
                            }
                        })
                .bounds(centerX - 20, navY, 10, 10)
                .build();
        ecologyPrevPageButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("saintsdragons.gui.draconic_codex.page.previous")));
        addWidget.accept(ecologyPrevPageButton);

        ecologyNextPageButton = Button.builder(
                        Component.literal(">"),
                        button -> {
                            CodexDragonEntry selected = selectedSupplier.get();
                            if (selected != null) {
                                int totalPages = getTotalEcologyPages(selected.dragonType());
                                int page = pageSupplier.getAsInt();
                                if (page < totalPages) {
                                    pageSetter.accept(page + 1);
                                    visibilityUpdater.run();
                                }
                            }
                        })
                .bounds(centerX + 13, navY, 10, 10)
                .build();
        ecologyNextPageButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("saintsdragons.gui.draconic_codex.page.next")));
        addWidget.accept(ecologyNextPageButton);

        updateWidgetVisibility(false, null, pageSupplier.getAsInt());
    }

    public void updateWidgetVisibility(boolean showEcology, CodexDragonEntry selected, int ecologyPage) {
        if (ecologyPrevPageButton != null) {
            ecologyPrevPageButton.visible = showEcology && selected != null && ecologyPage > 1;
            ecologyPrevPageButton.active = ecologyPrevPageButton.visible;
        }

        if (ecologyNextPageButton != null) {
            int totalPages = selected != null ? getTotalEcologyPages(selected.dragonType()) : 1;
            ecologyNextPageButton.visible = showEcology && selected != null && ecologyPage < totalPages;
            ecologyNextPageButton.active = ecologyNextPageButton.visible;
        }
    }

    public boolean handleLinkClick(double mouseX, double mouseY, IntConsumer pageSetter, Runnable visibilityUpdater) {
        for (CodexPageLink link : ecologyPageLinks) {
            if (mouseX >= link.x() && mouseX <= link.x() + link.width()
                    && mouseY >= link.y() && mouseY <= link.y() + link.height()) {
                pageSetter.accept(link.page());
                visibilityUpdater.run();
                return true;
            }
        }
        return false;
    }

    public boolean handleLinkScroll(double mouseX, double mouseY, double delta) {
        if (!linkAreaValid) {
            return false;
        }
        if (mouseX < lastLinkX || mouseX > lastLinkX + lastLinkWidth
                || mouseY < lastLinkY || mouseY > lastLinkY + lastLinkHeight) {
            return false;
        }
        int maxOffset = Math.max(0, lastLinkCount - VISIBLE_LINKS);
        if (maxOffset == 0) {
            return false;
        }
        if (delta < 0 && linkScrollOffset < maxOffset) {
            linkScrollOffset++;
            return true;
        }
        if (delta > 0 && linkScrollOffset > 0) {
            linkScrollOffset--;
            return true;
        }
        return false;
    }

    public void resetLinkScroll() {
        linkScrollOffset = 0;
    }

    public void draw(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int ecologyPage,
                     int contentX, int contentY, int mouseX, int mouseY) {
        int startY = contentY + 16;
        String dragonType = selected.dragonType();
        int totalPages = getTotalEcologyPages(selected.dragonType());

        drawEcologyContent(guiGraphics, font, dragonType, ecologyPage, contentX, startY);

        if (totalPages > 1) {
            drawEcologyPageNavigation(guiGraphics, font, contentX, startY, totalPages,
                    dragonType, ecologyPage, mouseX, mouseY);
        }
    }

    private void drawEcologyContent(GuiGraphics guiGraphics, Font font, String dragonType,
                                    int ecologyPage, int contentX, int startY) {
        int overviewPages = getOverviewPageCount(font, dragonType);
        if (ecologyPage <= overviewPages) {
            drawWrappedTextPage(guiGraphics, font, getOverviewText(dragonType), ecologyPage, contentX, startY);
            return;
        }

        TagKey<Item> favoriteFoods = getFavoriteFoods(dragonType);
        List<ResourceLocation> drops = getDrops(dragonType);
        int nextPage = overviewPages + 1;
        if (favoriteFoods != null && ecologyPage == nextPage) {
            drawPageHeader(guiGraphics, font, contentX, startY,
                    Component.translatable("saintsdragons.gui.draconic_codex.ecology.section.favorite_food"));
            drawFavoriteFoods(guiGraphics, font, contentX, startY, favoriteFoods);
            return;
        }
        if (favoriteFoods != null) {
            nextPage++;
        }

        if (!drops.isEmpty() && ecologyPage == nextPage) {
            drawPageHeader(guiGraphics, font, contentX, startY,
                    Component.translatable("saintsdragons.gui.draconic_codex.ecology.section.drops"));
            drawDrops(guiGraphics, font, contentX, startY, drops);
        }
    }

    private void drawPageHeader(GuiGraphics guiGraphics, Font font, int contentX, int startY, Component header) {
        guiGraphics.drawString(font, header, contentX, startY, CodexLayout.TEXT_COLOR, false);
    }

    private void drawWrappedTextPage(GuiGraphics guiGraphics, Font font, String text, int page, int contentX, int startY) {
        List<FormattedCharSequence> lines = wrapText(font, text);
        int startLine = Math.max(0, page - 1) * LINES_PER_TEXT_PAGE;
        int endLine = Math.min(lines.size(), startLine + LINES_PER_TEXT_PAGE);
        for (int line = startLine; line < endLine; line++) {
            guiGraphics.drawString(font, lines.get(line), contentX,
                    startY + ((line - startLine) * TEXT_LINE_SPACING), CodexLayout.TEXT_COLOR, false);
        }
    }

    private List<FormattedCharSequence> wrapText(Font font, String text) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String[] rawLines = text.split("\\R", -1);
        for (String rawLine : rawLines) {
            if (rawLine.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(font.split(Component.literal(rawLine), TEXT_BOX_WIDTH));
            }
        }
        return lines;
    }

    private int getOverviewPageCount(Font font, String dragonType) {
        return Math.max(1, (int) Math.ceil(wrapText(font, getOverviewText(dragonType)).size() / (float) LINES_PER_TEXT_PAGE));
    }

    private String getOverviewText(String dragonType) {
        String text = readCodexText("ecology/" + dragonType + ".txt");
        if (!text.isBlank()) {
            return text;
        }
        return Component.translatable("saintsdragons.gui.draconic_codex.ecology." + dragonType + ".page1").getString();
    }

    private String readCodexText(String path) {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase();
        String localized = readCodexText(path, lang);
        return localized.isBlank() ? readCodexText(path, "en_us") : localized;
    }

    private String readCodexText(String path, String lang) {
        ResourceLocation resource = new ResourceLocation("saintsdragons", "codex/" + lang + "/" + path);
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(resource)) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(line);
            }
            return text.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void drawFavoriteFoods(GuiGraphics guiGraphics, Font font, int contentX, int startY,
                                   TagKey<Item> foods) {
        int itemX = contentX + 2;
        int itemY = startY + 10;
        int rowGap = 18;

        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(foods)) {
            ItemStack stack = new ItemStack(holder.value());
            guiGraphics.renderItem(stack, itemX, itemY);
            guiGraphics.drawString(font, stack.getHoverName().getString(), itemX + 18, itemY + 4,
                    CodexLayout.TEXT_COLOR, false);
            itemY += rowGap;
        }
    }

    private void drawDrops(GuiGraphics guiGraphics, Font font, int contentX, int startY,
                           List<ResourceLocation> drops) {
        int itemX = contentX + 2;
        int itemY = startY + 10;
        int rowGap = 18;

        net.minecraft.core.Registry<net.minecraft.world.item.Item> registry =
                net.minecraft.core.registries.BuiltInRegistries.ITEM;

        for (ResourceLocation id : drops) {
            net.minecraft.world.item.Item item = registry.get(id);
            if (item == net.minecraft.world.item.Items.AIR) {
                itemY += rowGap;
                continue;
            }
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
            guiGraphics.renderItem(stack, itemX, itemY);
            guiGraphics.drawString(font, stack.getHoverName().getString(), itemX + 18, itemY + 4,
                    CodexLayout.TEXT_COLOR, false);
            itemY += rowGap;
        }
    }

    public int getTotalEcologyPages(String dragonType) {
        int pageCount = getOverviewPageCount(Minecraft.getInstance().font, dragonType);
        if (getFavoriteFoods(dragonType) != null) {
            pageCount++;
        }
        if (!getDrops(dragonType).isEmpty()) {
            pageCount++;
        }
        return pageCount;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private void drawEcologyPageNavigation(GuiGraphics guiGraphics, Font font, int contentX, int startY,
                                           int totalPages, String dragonType, int ecologyPage,
                                           int mouseX, int mouseY) {
        int navY = startY + PAGE_NAV_OFFSET_Y;

        String pageText = ecologyPage + "/" + totalPages;
        int pageTextWidth = font.width(pageText);
        int centerX = contentX + 62;
        guiGraphics.drawString(font, pageText, centerX - pageTextWidth / 2, navY, CodexLayout.TEXT_COLOR, false);

        ecologyPageLinks.clear();
        linkAreaValid = false;
        int overviewPages = getOverviewPageCount(font, dragonType);
        List<SectionLink> sections = new ArrayList<>();
        sections.add(new SectionLink(sectionLabel(1, "overview"), 1, ecologyPage <= overviewPages));

        int index = 2;
        int page = overviewPages + 1;
        if (getFavoriteFoods(dragonType) != null) {
            sections.add(new SectionLink(sectionLabel(index, "favorite_food"), page, ecologyPage == page));
            index++;
            page++;
        }
        if (!getDrops(dragonType).isEmpty()) {
            sections.add(new SectionLink(sectionLabel(index, "drops"), page, ecologyPage == page));
        }
        drawSectionList(guiGraphics, font, contentX, navY, mouseX, mouseY, sections);
    }

    private TagKey<Item> getFavoriteFoods(String dragonType) {
        return switch (dragonType) {
            case "ignivorus" -> ModTags.Items.IGNIVORUS_FOODS;
            case "raevyx" -> ModTags.Items.RAEVYX_FOODS;
            case "varasuchus" -> ModTags.Items.VARASUCHUS_FOODS;
            case "cindervane" -> ModTags.Items.CINDERVANE_FOODS;
            case "stegonaut" -> ModTags.Items.STEGONAUT_FOODS;
            case "volitans" -> ModTags.Items.VOLITANS_FOODS;
            default -> null;
        };
    }

    private List<ResourceLocation> getDrops(String dragonType) {
        return switch (dragonType) {
            case "ignivorus" -> IGNIVORUS_DROPS;
            case "raevyx" -> RAEVYX_DROPS;
            case "varasuchus" -> VARASUCHUS_DROPS;
            case "cindervane" -> CINDERVANE_DROPS;
            case "stegonaut" -> STEGONAUT_DROPS;
            case "volitans" -> VOLITANS_DROPS;
            default -> List.of();
        };
    }

    private record SectionLink(String label, int page, boolean active) {
    }

    private String sectionLabel(int index, String key) {
        return Component.translatable(
                "saintsdragons.gui.draconic_codex.ecology.section",
                index,
                Component.translatable("saintsdragons.gui.draconic_codex.ecology.section." + key)
        ).getString();
    }

    private void drawSectionList(GuiGraphics guiGraphics, Font font, int contentX, int navY,
                                 int mouseX, int mouseY, List<SectionLink> sections) {
        int x = contentX - 135;
        int y = navY - 35;
        int listHeight = VISIBLE_LINKS * font.lineHeight + (VISIBLE_LINKS - 1) * LINK_GAP;
        int maxLabelWidth = 0;
        for (SectionLink section : sections) {
            maxLabelWidth = Math.max(maxLabelWidth, font.width(section.label()));
        }

        int maxOffset = Math.max(0, sections.size() - VISIBLE_LINKS);
        linkScrollOffset = Math.max(0, Math.min(linkScrollOffset, maxOffset));

        int endIndex = Math.min(sections.size(), linkScrollOffset + VISIBLE_LINKS);
        int drawY = y;
        for (int i = linkScrollOffset; i < endIndex; i++) {
            SectionLink section = sections.get(i);
            int labelWidth = font.width(section.label());
            boolean hover = mouseX >= x && mouseX <= x + labelWidth
                    && mouseY >= drawY && mouseY <= drawY + font.lineHeight;
            int color = section.active()
                    ? 0x8B0000
                    : (hover ? 0x7A4A14 : CodexLayout.TEXT_COLOR);
            guiGraphics.drawString(font, section.label(), x, drawY, color, false);
            ecologyPageLinks.add(new CodexPageLink(section.page(), x, drawY, labelWidth, font.lineHeight));
            drawY += font.lineHeight + LINK_GAP;
        }

        int trackX = x + maxLabelWidth + SCROLLBAR_GAP;
        int trackY = y;
        int trackH = listHeight;
        guiGraphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackH, 0x5A000000);

        int knobH = maxOffset == 0 ? trackH : Math.max(6, (trackH * VISIBLE_LINKS) / sections.size());
        int knobY = maxOffset == 0
                ? trackY
                : trackY + (trackH - knobH) * linkScrollOffset / maxOffset;
        guiGraphics.fill(trackX, knobY, trackX + SCROLLBAR_WIDTH, knobY + knobH, 0xB08B6E3A);

        lastLinkX = x;
        lastLinkY = y;
        lastLinkWidth = maxLabelWidth + SCROLLBAR_GAP + SCROLLBAR_WIDTH;
        lastLinkHeight = listHeight;
        lastLinkCount = sections.size();
        linkAreaValid = true;
    }
}
