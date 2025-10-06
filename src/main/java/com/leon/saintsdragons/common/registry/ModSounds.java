package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * All the custom sounds for our lightning dragon
 * Because vanilla sounds are boring as heck
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SaintsDragons.MOD_ID);

    // ===== AMBIENT SOUNDS =====
    public static final RegistryObject<SoundEvent> DRAGON_GRUMBLE_1 = registerSound("lightningdragon_grumble_1");
    public static final RegistryObject<SoundEvent> DRAGON_GRUMBLE_2 = registerSound("lightningdragon_grumble_2");
    public static final RegistryObject<SoundEvent> DRAGON_GRUMBLE_3 = registerSound("lightningdragon_grumble_3");
    
    // ===== PRIMITIVE DRAKE SOUNDS =====
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_GRUMBLE_1 = registerSound("primitivedrake_grumble1");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_GRUMBLE_2 = registerSound("primitivedrake_grumble2");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_GRUMBLE_3 = registerSound("primitivedrake_grumble3");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_SCARED = registerSound("primitivedrake_scared");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_RELIEVED = registerSound("primitivedrake_relieved");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_HURT = registerSound("primitivedrake_hurt");
    public static final RegistryObject<SoundEvent> PRIMITIVE_DRAKE_DIE = registerSound("primitivedrake_die");

    public static final RegistryObject<SoundEvent> DRAGON_PURR = registerSound("lightningdragon_purr");
    public static final RegistryObject<SoundEvent> DRAGON_SNORT = registerSound("lightningdragon_snort");
    public static final RegistryObject<SoundEvent> DRAGON_CHUFF = registerSound("lightningdragon_chuff");

    // ===== EMOTIONAL SOUNDS =====
    public static final RegistryObject<SoundEvent> DRAGON_CONTENT = registerSound("lightningdragon_content");
    public static final RegistryObject<SoundEvent> DRAGON_ANNOYED = registerSound("lightningdragon_annoyed");
    public static final RegistryObject<SoundEvent> DRAGON_EXCITED = registerSound("lightningdragon_excited");

    // ===== COMBAT SOUNDS =====
    public static final RegistryObject<SoundEvent> DRAGON_ROAR = registerSound("lightningdragon_roar");
    public static final RegistryObject<SoundEvent> DRAGON_SUMMON_STORM = registerSound("lightningdragon_summon_storm");
    public static final RegistryObject<SoundEvent> DRAGON_GROWL_WARNING = registerSound("lightningdragon_growl_warning");
    public static final RegistryObject<SoundEvent> DRAGON_HURT = registerSound("lightningdragon_hurt");
    public static final RegistryObject<SoundEvent> DRAGON_BITE = registerSound("lightningdragon_bite");
    public static final RegistryObject<SoundEvent> DRAGON_HORNGORE = registerSound("lightningdragon_horngore");
    public static final RegistryObject<SoundEvent> DRAGON_DIE = registerSound("lightningdragon_die");


    // ===== AMPHITHERE SOUNDS =====
    public static final RegistryObject<SoundEvent> AMPHITHERE_ROAR = registerSound("amphithere_roar");
    public static final RegistryObject<SoundEvent> AMPHITHERE_HURT = registerSound("amphithere_hurt");
    public static final RegistryObject<SoundEvent> AMPHITHERE_BITE = registerSound("amphithere_bite");
    public static final RegistryObject<SoundEvent> AMPHITHERE_DIE = registerSound("amphithere_die");

    // ===== RIFT DRAKE SOUNDS =====
    public static final RegistryObject<SoundEvent> RIFTDRAKE_PHASE1 = registerSound("riftdrake_phase1");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_PHASE2 = registerSound("riftdrake_phase2");
    public static final RegistryObject<SoundEvent> RIFTDRAKE_ROAR = registerSound("riftdrake_roar");

    // ===== STEP SOUNDS =====
    public static final RegistryObject<SoundEvent> DRAGON_STEP = registerSound("dragon_step") ;

    // ===== KEYFRAMED ANIMATION SOUNDS =====
    public static final RegistryObject<SoundEvent> FLAP1 = registerSound("lightningdragon_flap1");
    public static final RegistryObject<SoundEvent> STEP1 = registerSound("lightningdragon_step1");
    public static final RegistryObject<SoundEvent> STEP2 = registerSound("lightningdragon_step2");
    // Run steps (faster cadence, distinct audio)
    public static final RegistryObject<SoundEvent> RUN_STEP1 = registerSound("lightningdragon_run_step1");
    public static final RegistryObject<SoundEvent> RUN_STEP2 = registerSound("lightningdragon_run_step2");

    /**
     * Helper method to register sounds without writing the same crap over and over
     */
    private static RegistryObject<SoundEvent> registerSound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SaintsDragons.MOD_ID, name)));
    }
}
