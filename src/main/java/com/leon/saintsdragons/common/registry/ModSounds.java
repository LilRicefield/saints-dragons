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

    
    // STEGONAUT
    public static final RegistryObject<SoundEvent> STEGONAUT_GRUMBLE_1 = registerSound("stegonaut_grumble1");
    public static final RegistryObject<SoundEvent> STEGONAUT_GRUMBLE_2 = registerSound("stegonaut_grumble2");
    public static final RegistryObject<SoundEvent> STEGONAUT_GRUMBLE_3 = registerSound("stegonaut_grumble3");
    public static final RegistryObject<SoundEvent> STEGONAUT_SCARED = registerSound("stegonaut_scared");
    public static final RegistryObject<SoundEvent> STEGONAUT_RELIEVED = registerSound("stegonaut_relieved");
    public static final RegistryObject<SoundEvent> STEGONAUT_HURT = registerSound("stegonaut_hurt");
    public static final RegistryObject<SoundEvent> STEGONAUT_DIE = registerSound("stegonaut_die");


    // RAEVYX
    public static final RegistryObject<SoundEvent> RAEVYX_PURR = registerSound("raevyx_purr");
    public static final RegistryObject<SoundEvent> RAEVYX_SNORT = registerSound("raevyx_snort");
    public static final RegistryObject<SoundEvent> RAEVYX_CHUFF = registerSound("raevyx_chuff");
    public static final RegistryObject<SoundEvent> RAEVYX_CONTENT = registerSound("raevyx_content");
    public static final RegistryObject<SoundEvent> RAEVYX_ANNOYED = registerSound("raevyx_annoyed");
    public static final RegistryObject<SoundEvent> RAEVYX_EXCITED = registerSound("raevyx_excited");
    public static final RegistryObject<SoundEvent> RAEVYX_ROAR = registerSound("raevyx_roar");
    public static final RegistryObject<SoundEvent> RAEVYX_SUMMON_STORM = registerSound("raevyx_summon_storm");
    public static final RegistryObject<SoundEvent> RAEVYX_GROWL_WARNING = registerSound("raevyx_growl_warning");
    public static final RegistryObject<SoundEvent> RAEVYX_HURT = registerSound("raevyx_hurt");
    public static final RegistryObject<SoundEvent> RAEVYX_BITE = registerSound("raevyx_bite");
    public static final RegistryObject<SoundEvent> RAEVYX_HORNGORE = registerSound("raevyx_horngore");
    public static final RegistryObject<SoundEvent> RAEVYX_DIE = registerSound("raevyx_die");
    public static final RegistryObject<SoundEvent> RAEVYX_FLAP1 = registerSound("raevyx_flap1");
    public static final RegistryObject<SoundEvent> RAEVYX_STEP1 = registerSound("raevyx_step1");
    public static final RegistryObject<SoundEvent> RAEVYX_STEP2 = registerSound("raevyx_step2");
    public static final RegistryObject<SoundEvent> RAEVYX_RUN_STEP1 = registerSound("raevyx_run_step1");
    public static final RegistryObject<SoundEvent> RAEVYX_RUN_STEP2 = registerSound("raevyx_run_step2");
    public static final RegistryObject<SoundEvent> RAEVYX_GRUMBLE_1 = registerSound("raevyx_grumble_1");
    public static final RegistryObject<SoundEvent> RAEVYX_GRUMBLE_2 = registerSound("raevyx_grumble_2");
    public static final RegistryObject<SoundEvent> RAEVYX_GRUMBLE_3 = registerSound("raevyx_grumble_3");


    // ===== AMPHITHERE SOUNDS =====
    public static final RegistryObject<SoundEvent> CINDERVANE_ROAR = registerSound("cindervane_roar");
    public static final RegistryObject<SoundEvent> CINDERVANE_HURT = registerSound("cindervane_hurt");
    public static final RegistryObject<SoundEvent> CINDERVANE_BITE = registerSound("cindervane_bite");
    public static final RegistryObject<SoundEvent> CINDERVANE_DIE = registerSound("cindervane_die");

    // NULLJAW
    public static final RegistryObject<SoundEvent> NULLJAW_GRUMBLE_1 = registerSound("nulljaw_grumble1");
    public static final RegistryObject<SoundEvent> NULLJAW_GRUMBLE_2 = registerSound("nulljaw_grumble2");
    public static final RegistryObject<SoundEvent> NULLJAW_GRUMBLE_3 = registerSound("nulljaw_grumble3");
    public static final RegistryObject<SoundEvent> NULLJAW_PHASE1 = registerSound("nulljaw_phase1");
    public static final RegistryObject<SoundEvent> NULLJAW_PHASE2 = registerSound("nulljaw_phase2");
    public static final RegistryObject<SoundEvent> NULLJAW_ROAR = registerSound("nulljaw_roar");
    public static final RegistryObject<SoundEvent> NULLJAW_ROARCLAW = registerSound("nulljaw_roarclaw");
    public static final RegistryObject<SoundEvent> NULLJAW_STEP = registerSound("nulljaw_step");
    public static final RegistryObject<SoundEvent> NULLJAW_CLAW = registerSound("nulljaw_claw");
    public static final RegistryObject<SoundEvent> NULLJAW_BITE = registerSound("nulljaw_bite");


    /**
     * Helper method to register sounds without writing the same crap over and over
     */
    private static RegistryObject<SoundEvent> registerSound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SaintsDragons.MOD_ID, name)));
    }
}
