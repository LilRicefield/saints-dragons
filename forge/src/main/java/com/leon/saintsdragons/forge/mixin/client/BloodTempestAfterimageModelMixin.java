package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageRenderContext;
import net.minecraft.client.model.AgeableListModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AgeableListModel.class)
public abstract class BloodTempestAfterimageModelMixin {
    @ModifyVariable(method = "renderToBuffer", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float saintsdragons$tintAfterimageRed(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.red() : original;
    }

    @ModifyVariable(method = "renderToBuffer", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private float saintsdragons$tintAfterimageGreen(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.green() : original;
    }

    @ModifyVariable(method = "renderToBuffer", at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private float saintsdragons$tintAfterimageBlue(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.blue() : original;
    }

    @ModifyVariable(method = "renderToBuffer", at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 0)
    private float saintsdragons$fadeAfterimage(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.alpha() : original;
    }
}
