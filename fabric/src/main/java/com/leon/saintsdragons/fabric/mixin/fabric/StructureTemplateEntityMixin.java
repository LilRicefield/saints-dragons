package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateEntityMixin {
    @Inject(method = "createEntityIgnoreException", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$filterDisabledStructureEntity(
            ServerLevelAccessor level,
            CompoundTag entityTag,
            CallbackInfoReturnable<Optional<Entity>> cir) {
        if (saintsdragons$isStructureSpawnDisabled(entityTag.getString("id"))) {
            cir.setReturnValue(Optional.empty());
        }
    }

    private static boolean saintsdragons$isStructureSpawnDisabled(String entityId) {
        return switch (entityId) {
            case "saintsdragons:raevyx" -> !SaintsDragonsConfig.isRaevyxSpawningEnabled();
            case "saintsdragons:stegonaut" -> !SaintsDragonsConfig.isStegonautSpawningEnabled();
            case "saintsdragons:cindervane" -> !SaintsDragonsConfig.isCindervaneSpawningEnabled();
            case "saintsdragons:ignivorus" -> !SaintsDragonsConfig.isIgnivorusSpawningEnabled();
            case "saintsdragons:varasuchus" -> !SaintsDragonsConfig.isVarasuchusSpawningEnabled();
            case "saintsdragons:atroxiia" -> !SaintsDragonsConfig.isAtroxiiaSpawningEnabled();
            case "saintsdragons:volitans" -> !SaintsDragonsConfig.isVolitansSpawningEnabled();
            case "saintsdragons:nulljaw" -> !SaintsDragonsConfig.isNulljawSpawningEnabled();
            case "saintsdragons:moop" -> !SaintsDragonsConfig.isMoopSpawningEnabled();
            case "saintsdragons:mossback" -> !SaintsDragonsConfig.isMossbackSpawningEnabled();
            case "saintsdragons:ivy_oleander" -> !SaintsDragonsConfig.isIvySpawningEnabled();
            default -> false;
        };
    }
}
