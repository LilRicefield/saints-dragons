package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Vanilla sounds are boring as heck
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SaintsDragons.MOD_ID);

    // ===== AMBIENT SOUNDS =====
    public static final RegistryObject<SoundEvent> RAEVYX_GRUMBLE_1 = registerSound("raevyx_grumble_1");
    public static final RegistryObject<SoundEvent> RAEVYX_GRUMBLE_2 = registerSound("raevyx_grumble_2");
    public static final RegistryObject<SoundEvent> RAEVYX_GRUMBLE_3 = registerSound("raevyx_grumble_3");
    
    // ===== PRIMITIVE DRAKE SOUNDS =====
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_GRUMBLE_1 = registerSound("primitivedrake_grumble1");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_GRUMBLE_2 = registerSound("primitivedrake_grumble2");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_GRUMBLE_3 = registerSound("primitivedrake_grumble3");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_SCARED = registerSound("primitivedrake_scared");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_RELIEVED = registerSound("primitivedrake_relieved");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_HURT = registerSound("primitivedrake_hurt");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_DIE = registerSound("primitivedrake_die");

    public static final RegistryObject<SoundEvent> RAEVYX_PURR = registerSound("raevyx_purr");
    public static final RegistryObject<SoundEvent> RAEVYX_SNORT = registerSound("raevyx_snort");
    public static final RegistryObject<SoundEvent> RAEVYX_CHUFF = registerSound("raevyx_chuff");

    // ===== EMOTIONAL SOUNDS =====
    public static final RegistryObject<SoundEvent> RAEVYX_CONTENT = registerSound("raevyx_content");
    public static final RegistryObject<SoundEvent> RAEVYX_ANNOYED = registerSound("raevyx_annoyed");
    public static final RegistryObject<SoundEvent> RAEVYX_EXCITED = registerSound("raevyx_excited");

    // ===== COMBAT SOUNDS =====
    public static final RegistryObject<SoundEvent> RAEVYX_ROAR = registerSound("raevyx_roar");
    public static final RegistryObject<SoundEvent> RAEVYX_SUMMON_STORM = registerSound("raevyx_summon_storm");
    public static final RegistryObject<SoundEvent> RAEVYX_GROWL_WARNING = registerSound("raevyx_growl_warning");
    public static final RegistryObject<SoundEvent> RAEVYX_HURT = registerSound("raevyx_hurt");
    public static final RegistryObject<SoundEvent> RAEVYX_BITE = registerSound("raevyx_bite");
    public static final RegistryObject<SoundEvent> RAEVYX_HORNGORE = registerSound("raevyx_horngore");
    public static final RegistryObject<SoundEvent> RAEVYX_DIE = registerSound("raevyx_die");


    // ===== AMPHITHERE SOUNDS =====
    public static final RegistryObject<SoundEvent> AMPHITHERE_ROAR = registerSound("amphithere_roar");
    public static final RegistryObject<SoundEvent> AMPHITHERE_HURT = registerSound("amphithere_hurt");
    public static final RegistryObject<SoundEvent> AMPHITHERE_BITE = registerSound("amphithere_bite");
    public static final RegistryObject<SoundEvent> AMPHITHERE_DIE = registerSound("amphithere_die");

    // ===== RIFT DRAKE SOUNDS =====
    public static final RegistryObject<SoundEvent> RIFTDRAKE_PHASE1 = registerSound("riftdrake_phase1");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_PHASE2 = registerSound("riftdrake_phase2");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_ROAR = registerSound("riftdrake_roar");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_ROARCLAW = registerSound("riftdrake_roarclaw");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_STEP = registerSound("riftdrake_step");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_CLAW = registerSound("riftdrake_claw");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_BITE = registerSound("riftdrake_bite");


    // ===== KEYFRAMED ANIMATION SOUNDS =====
    public static final RegistryObject<SoundEvent> FLAP1 = registerSound("raevyx_flap1");
    public static final RegistryObject<SoundEvent> STEP1 = registerSound("raevyx_step1");
    public static final RegistryObject<SoundEvent> STEP2 = registerSound("raevyx_step2");
    // Run steps (faster cadence, distinct audio)
    public static final RegistryObject<SoundEvent> RUN_STEP1 = registerSound("raevyx_run_step1");
    public static final RegistryObject<SoundEvent> RUN_STEP2 = registerSound("raevyx_run_step2");

    /**
     * Helper method to register sounds without writing the same crap over and over
     */
    private static RegistryObject<SoundEvent> registerSound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SaintsDragons.MOD_ID, name)));
    }
}
