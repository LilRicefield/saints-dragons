package com.leon.saintsdragons.client.ui.codex;

import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class CodexDragonRenderer {
    private static final int IGNIVORUS_SCALE = 8;
    private static final int RAEVYX_SCALE = 13;
    private static final int VOLITANS_SCALE = 13;
    private static final int VARASUCHUS_SCALE = 15;
    private static final int CINDERVANE_SCALE = 12;
    private static final int STEGONAUT_SCALE = 23;

    private static final int IGNIVORUS_OFFSET_X = 0;
    private static final int IGNIVORUS_OFFSET_Y = 0;
    private static final int RAEVYX_OFFSET_X = 0;
    private static final int RAEVYX_OFFSET_Y = -5;
    private static final int VOLITANS_OFFSET_X = 0;
    private static final int VOLITANS_OFFSET_Y = -5;
    private static final int VARASUCHUS_OFFSET_X = 0;
    private static final int VARASUCHUS_OFFSET_Y = -10;
    private static final int CINDERVANE_OFFSET_X = 0;
    private static final int CINDERVANE_OFFSET_Y = -15;
    private static final int STEGONAUT_OFFSET_X = 0;
    private static final int STEGONAUT_OFFSET_Y = -15;

    public void drawDragonPortrait(GuiGraphics guiGraphics, Minecraft minecraft, CodexDragonEntry selected,
                                   int leftPos, int topPos, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.level == null || selected == null || selected.entityId() == null) {
            return;
        }

        DragonEntity dragon = findDragonEntity(minecraft, selected.entityId());
        if (dragon == null) {
            dragon = createDummyDragon(minecraft, selected);
            if (dragon == null) {
                return;
            }
        }

        int boxX = leftPos + CodexLayout.DRAGON_RENDER_BOX_X;
        int boxY = topPos + CodexLayout.DRAGON_RENDER_BOX_Y;
        int centerX = boxX + CodexLayout.DRAGON_RENDER_BOX_SIZE / 2 + getDragonOffsetX(dragon);
        int centerY = boxY + CodexLayout.DRAGON_RENDER_BOX_SIZE + getDragonOffsetY(dragon);

        int size = getDragonScale(dragon);

        guiGraphics.enableScissor(boxX, boxY,
                boxX + CodexLayout.DRAGON_RENDER_BOX_SIZE,
                boxY + CodexLayout.DRAGON_RENDER_BOX_SIZE);

        DraconicCodexScreen.RENDERING_IN_GUI.set(true);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    centerX,
                    centerY,
                    size,
                    (float) (centerX - mouseX),
                    (float) (centerY - CodexLayout.DRAGON_RENDER_BOX_SIZE - mouseY),
                    dragon
            );
        } finally {
            DraconicCodexScreen.RENDERING_IN_GUI.set(false);
        }

        guiGraphics.disableScissor();
    }

    private int getDragonScale(DragonEntity dragon) {
        if (dragon.getType() == ModEntities.IGNIVORUS.get()) {
            return IGNIVORUS_SCALE;
        } else if (dragon.getType() == ModEntities.RAEVYX.get()) {
            return RAEVYX_SCALE;
        } else if (dragon.getType() == ModEntities.VOLITANS.get()) {
            return VOLITANS_SCALE;
        } else if (dragon.getType() == ModEntities.VARASUCHUS.get()) {
            return VARASUCHUS_SCALE;
        } else if (dragon.getType() == ModEntities.CINDERVANE.get()) {
            return CINDERVANE_SCALE;
        } else if (dragon.getType() == ModEntities.STEGONAUT.get()) {
            return STEGONAUT_SCALE;
        }
        return 30;
    }

    private int getDragonOffsetX(DragonEntity dragon) {
        if (dragon.getType() == ModEntities.IGNIVORUS.get()) {
            return IGNIVORUS_OFFSET_X;
        } else if (dragon.getType() == ModEntities.RAEVYX.get()) {
            return RAEVYX_OFFSET_X;
        } else if (dragon.getType() == ModEntities.VOLITANS.get()) {
            return VOLITANS_OFFSET_X;
        } else if (dragon.getType() == ModEntities.VARASUCHUS.get()) {
            return VARASUCHUS_OFFSET_X;
        } else if (dragon.getType() == ModEntities.CINDERVANE.get()) {
            return CINDERVANE_OFFSET_X;
        } else if (dragon.getType() == ModEntities.STEGONAUT.get()) {
            return STEGONAUT_OFFSET_X;
        }
        return 0;
    }

    private int getDragonOffsetY(DragonEntity dragon) {
        if (dragon.getType() == ModEntities.IGNIVORUS.get()) {
            return IGNIVORUS_OFFSET_Y;
        } else if (dragon.getType() == ModEntities.RAEVYX.get()) {
            return RAEVYX_OFFSET_Y;
        } else if (dragon.getType() == ModEntities.VOLITANS.get()) {
            return VOLITANS_OFFSET_Y;
        } else if (dragon.getType() == ModEntities.VARASUCHUS.get()) {
            return VARASUCHUS_OFFSET_Y;
        } else if (dragon.getType() == ModEntities.CINDERVANE.get()) {
            return CINDERVANE_OFFSET_Y;
        } else if (dragon.getType() == ModEntities.STEGONAUT.get()) {
            return STEGONAUT_OFFSET_Y;
        }
        return 0;
    }

    private DragonEntity findDragonEntity(Minecraft minecraft, java.util.UUID dragonId) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof DragonEntity dragon && dragon.getUUID().equals(dragonId)) {
                return dragon;
            }
        }
        return null;
    }

    private DragonEntity createDummyDragon(Minecraft minecraft, CodexDragonEntry entry) {
        EntityType<? extends DragonEntity> entityType = getDragonEntityType(entry.dragonType());
        if (entityType == null) {
            return null;
        }

        DragonEntity dragon = entityType.create(minecraft.level);
        if (dragon == null) {
            return null;
        }

        if (entry.isBaby()) {
            dragon.setBaby(true);
        }

        dragon.setTextureVariant(entry.variantId());

        dragon.setGender(DragonGender.fromId(entry.genderId()));
        return dragon;
    }

    private EntityType<? extends DragonEntity> getDragonEntityType(String dragonType) {
        return switch (dragonType) {
            case "ignivorus" -> ModEntities.IGNIVORUS.get();
            case "raevyx" -> ModEntities.RAEVYX.get();
            case "volitans" -> ModEntities.VOLITANS.get();
            case "varasuchus" -> ModEntities.VARASUCHUS.get();
            case "cindervane" -> ModEntities.CINDERVANE.get();
            case "stegonaut" -> ModEntities.STEGONAUT.get();
            default -> null;
        };
    }
}
