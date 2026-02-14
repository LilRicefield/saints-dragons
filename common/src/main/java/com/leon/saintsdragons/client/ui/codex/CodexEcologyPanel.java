package com.leon.saintsdragons.client.ui.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class CodexEcologyPanel {
    private static final int LINK_GAP = 2;
    private static final int VISIBLE_LINKS = 3;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 25;
    private final ResourceLocation ignivorusEggTexture;
    private final List<CodexPageLink> ecologyPageLinks = new ArrayList<>();
    private static final List<ResourceLocation> IGNIVORUS_FAVORITE_FOODS = List.of(
            new ResourceLocation("minecraft", "salmon"),
            new ResourceLocation("minecraft", "cod"),
            new ResourceLocation("minecraft", "beef"),
            new ResourceLocation("saintsdragons", "hearty_dragon_meal")
    );
    private static final List<ResourceLocation> IGNIVORUS_DROPS = List.of(
            new ResourceLocation("saintsdragons", "ignivorus_scale"),
            new ResourceLocation("saintsdragons", "ignivorus_tooth"),
            new ResourceLocation("saintsdragons", "ignivorus_heart"),
            new ResourceLocation("saintsdragons", "ignivorus_egg")
    );
    private static final List<ResourceLocation> RAEVYX_FAVORITE_FOODS = List.of(
            new ResourceLocation("minecraft", "salmon"),
            new ResourceLocation("saintsdragons", "hearty_dragon_meal")
    );
    private static final List<ResourceLocation> RAEVYX_DROPS = List.of(
            new ResourceLocation("saintsdragons", "raevyx_scale"),
            new ResourceLocation("saintsdragons", "raevyx_egg")
    );
    private static final List<ResourceLocation> NULLJAW_FAVORITE_FOODS = List.of(
            new ResourceLocation("minecraft", "salmon"),
            new ResourceLocation("minecraft", "tropical_fish"),
            new ResourceLocation("minecraft", "cod"),
            new ResourceLocation("saintsdragons", "hearty_dragon_meal")
    );
    private static final List<ResourceLocation> NULLJAW_DROPS = List.of(
            new ResourceLocation("saintsdragons", "nulljaw_scale"),
            new ResourceLocation("saintsdragons", "nulljaw_egg")
    );
    private static final List<ResourceLocation> CINDERVANE_FAVORITE_FOODS = List.of(
            new ResourceLocation("saintsdragons", "hearty_dragon_meal"),
            new ResourceLocation("minecraft", "chicken"),
            new ResourceLocation("minecraft", "cod"),
            new ResourceLocation("minecraft", "salmon")
    );
    private static final List<ResourceLocation> CINDERVANE_DROPS = List.of(
            new ResourceLocation("saintsdragons", "cindervane_scale"),
            new ResourceLocation("saintsdragons", "cindervane_egg")
    );
    private static final List<ResourceLocation> STEGONAUT_FAVORITE_FOODS = List.of(
            new ResourceLocation("minecraft", "cod"),
            new ResourceLocation("minecraft", "salmon"),
            new ResourceLocation("saintsdragons", "hearty_dragon_meal")
    );
    private static final List<ResourceLocation> STEGONAUT_DROPS = List.of(
            new ResourceLocation("saintsdragons", "stegonaut_scale"),
            new ResourceLocation("minecraft", "amethyst_shard"),
            new ResourceLocation("saintsdragons", "stegonaut_egg")
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

    public CodexEcologyPanel(ResourceLocation ignivorusEggTexture) {
        this.ignivorusEggTexture = ignivorusEggTexture;
    }

    public void initWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> addWidget,
                            Font font, int leftPos, int topPos, IntSupplier pageSupplier, IntConsumer pageSetter,
                            Supplier<CodexDragonEntry> selectedSupplier, Runnable visibilityUpdater) {
        int contentX = leftPos + 229;
        int contentY = topPos - 3;
        int startY = contentY + 16;
        int navY = startY + 170;
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
        String baseKey = "saintsdragons.gui.draconic_codex.ecology." + selected.dragonType();
        String pageKey = baseKey + ".page" + ecologyPage;

        Component testComponent = Component.translatable(pageKey);
        String ecologyText = testComponent.getString();

        if (ecologyText.equals(pageKey)) {
            ecologyText = Component.translatable(baseKey).getString();
        }

        int maxWidth = 130;
        int startY = contentY + 16;

        int totalPages = getTotalEcologyPages(selected.dragonType());

        List<String> lines = new ArrayList<>();
        String[] words = ecologyText.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawString(font, lines.get(i), contentX, startY + (i * 10), CodexLayout.TEXT_COLOR, false);
        }
        switch (selected.dragonType()) {
            case "ignivorus" -> {
                if (ecologyPage == 7) {
                    drawFavoriteFoods(guiGraphics, font, contentX, startY, IGNIVORUS_FAVORITE_FOODS);
                }
                if (ecologyPage == 8) {
                    drawDrops(guiGraphics, font, contentX, startY, IGNIVORUS_DROPS);
                }
            }
            case "raevyx" -> {
                if (ecologyPage == 5) {
                    drawFavoriteFoods(guiGraphics, font, contentX, startY, RAEVYX_FAVORITE_FOODS);
                }
                if (ecologyPage == 6) {
                    drawDrops(guiGraphics, font, contentX, startY, RAEVYX_DROPS);
                }
            }
            case "nulljaw" -> {
                if (ecologyPage == 7) {
                    drawFavoriteFoods(guiGraphics, font, contentX, startY, NULLJAW_FAVORITE_FOODS);
                }
                if (ecologyPage == 8) {
                    drawDrops(guiGraphics, font, contentX, startY, NULLJAW_DROPS);
                }
            }
            case "cindervane" -> {
                if (ecologyPage == 6) {
                    drawFavoriteFoods(guiGraphics, font, contentX, startY, CINDERVANE_FAVORITE_FOODS);
                }
                if (ecologyPage == 7) {
                    drawDrops(guiGraphics, font, contentX, startY, CINDERVANE_DROPS);
                }
            }
            case "stegonaut" -> {
                if (ecologyPage == 6) {
                    drawFavoriteFoods(guiGraphics, font, contentX, startY, STEGONAUT_FAVORITE_FOODS);
                }
                if (ecologyPage == 7) {
                    drawDrops(guiGraphics, font, contentX, startY, STEGONAUT_DROPS);
                }
            }
            default -> {
            }
        }

        if (totalPages > 1) {
            drawEcologyPageNavigation(guiGraphics, font, contentX, startY, totalPages,
                    selected.dragonType(), ecologyPage, mouseX, mouseY);
        }
    }

    private void drawFavoriteFoods(GuiGraphics guiGraphics, Font font, int contentX, int startY,
                                   List<ResourceLocation> foods) {
        int itemX = contentX + 2;
        int itemY = startY + 10;
        int rowGap = 18;

        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.core.Registry<net.minecraft.world.item.Item> registry =
                net.minecraft.core.registries.BuiltInRegistries.ITEM;

        for (ResourceLocation id : foods) {
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
        String baseKey = "saintsdragons.gui.draconic_codex.ecology." + dragonType;
        int pageCount = 1;

        for (int i = 2; i <= 10; i++) {
            String pageKey = baseKey + ".page" + i;
            Component testComponent = Component.translatable(pageKey);
            String result = testComponent.getString();
            if (result.equals(pageKey)) {
                break;
            }
            pageCount = i;
        }

        return pageCount;
    }

    private void drawEcologyPageNavigation(GuiGraphics guiGraphics, Font font, int contentX, int startY,
                                           int totalPages, String dragonType, int ecologyPage,
                                           int mouseX, int mouseY) {
        int navY = startY + 170;

        String pageText = ecologyPage + "/" + totalPages;
        int pageTextWidth = font.width(pageText);
        int centerX = contentX + 62;
        guiGraphics.drawString(font, pageText, centerX - pageTextWidth / 2, navY, CodexLayout.TEXT_COLOR, false);

        ecologyPageLinks.clear();
        linkAreaValid = false;
        switch (dragonType) {
            case "ignivorus" -> {
                List<SectionLink> sections = List.of(
                        new SectionLink("1. Overview", 1, ecologyPage >= 1 && ecologyPage <= 3),
                        new SectionLink("2. Egg Info", 4, ecologyPage >= 4 && ecologyPage <= 5),
                        new SectionLink("3. Biomes", 6, ecologyPage == 6),
                        new SectionLink("4. Favorite Food", 7, ecologyPage == 7),
                        new SectionLink("5. Drops", 8, ecologyPage == 8)
                );
                drawSectionList(guiGraphics, font, contentX, navY, mouseX, mouseY, sections);
            }
            case "raevyx" -> {
                List<SectionLink> sections = List.of(
                        new SectionLink("1. Overview", 1, ecologyPage == 1),
                        new SectionLink("2. Egg Info", 3, ecologyPage == 3),
                        new SectionLink("3. Biomes", 4, ecologyPage == 4),
                        new SectionLink("4. Favorite Food", 5, ecologyPage == 5),
                        new SectionLink("5. Drops", 6, ecologyPage == 6)
                );
                drawSectionList(guiGraphics, font, contentX, navY, mouseX, mouseY, sections);
            }
            case "nulljaw" -> {
                List<SectionLink> sections = List.of(
                        new SectionLink("1. Overview", 1, ecologyPage == 1),
                        new SectionLink("2. Egg Information", 4, ecologyPage == 4),
                        new SectionLink("3. Biome", 5, ecologyPage == 5),
                        new SectionLink("4. Favorite food", 7, ecologyPage == 7),
                        new SectionLink("5. Drops", 8, ecologyPage == 8)
                );
                drawSectionList(guiGraphics, font, contentX, navY, mouseX, mouseY, sections);
            }
            case "cindervane" -> {
                List<SectionLink> sections = List.of(
                        new SectionLink("1. Overview", 1, ecologyPage >= 1 && ecologyPage <= 2),
                        new SectionLink("2. Egg Info", 3, ecologyPage == 3),
                        new SectionLink("3. Biomes", 4, ecologyPage >= 4 && ecologyPage <= 5),
                        new SectionLink("4. Favorite Food", 6, ecologyPage == 6),
                        new SectionLink("5. Drops", 7, ecologyPage == 7)
                );
                drawSectionList(guiGraphics, font, contentX, navY, mouseX, mouseY, sections);
            }
            case "stegonaut" -> {
                List<SectionLink> sections = List.of(
                        new SectionLink("1. Overview", 1, ecologyPage >= 1 && ecologyPage <= 2),
                        new SectionLink("2. Egg Info", 3, ecologyPage == 3),
                        new SectionLink("3. Biomes", 4, ecologyPage >= 4 && ecologyPage <= 5),
                        new SectionLink("4. Favorite Food", 6, ecologyPage == 6),
                        new SectionLink("5. Drops", 7, ecologyPage == 7)
                );
                drawSectionList(guiGraphics, font, contentX, navY, mouseX, mouseY, sections);
            }
            default -> {
            }
        }
    }

    private record SectionLink(String label, int page, boolean active) {
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
