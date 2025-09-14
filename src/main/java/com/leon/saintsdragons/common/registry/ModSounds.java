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
    public static final RegistryObject<SoundEvent> DRAGON_GRUMBLE_1 = registerSound("dragon_grumble_1");
    public static final RegistryObject<SoundEvent> DRAGON_GRUMBLE_2 = registerSound("dragon_grumble_2");
    public static final RegistryObject<SoundEvent> DRAGON_GRUMBLE_3 = registerSound("dragon_grumble_3");

    public static final RegistryObject<SoundEvent> DRAGON_PURR = registerSound("dragon_purr");
    public static final RegistryObject<SoundEvent> DRAGON_SNORT = registerSound("dragon_snort");
    public static final RegistryObject<SoundEvent> DRAGON_CHUFF = registerSound("dragon_chuff");

    // ===== EMOTIONAL SOUNDS =====
    public static final RegistryObject<SoundEvent> DRAGON_CONTENT = registerSound("dragon_content");
    public static final RegistryObject<SoundEvent> DRAGON_ANNOYED = registerSound("dragon_annoyed");
    public static final RegistryObject<SoundEvent> DRAGON_EXCITED = registerSound("dragon_excited");

    // ===== COMBAT SOUNDS =====
    public static final RegistryObject<SoundEvent> DRAGON_ROAR = registerSound("dragon_roar");
    public static final RegistryObject<SoundEvent> DRAGON_SUMMON_STORM = registerSound("summon_storm");
    public static final RegistryObject<SoundEvent> DRAGON_GROWL_WARNING = registerSound("dragon_growl_warning");
    public static final RegistryObject<SoundEvent> DRAGON_HURT = registerSound("dragon_hurt");
    public static final RegistryObject<SoundEvent> DRAGON_BITE = registerSound("dragon_bite");
    public static final RegistryObject<SoundEvent> DRAGON_HORNGORE = registerSound("dragon_horngore");
    public static final RegistryObject<SoundEvent> DRAGON_DIE = registerSound("dragon_die");

    // ===== STEP SOUNDS =====
    public static final RegistryObject<SoundEvent> DRAGON_STEP = registerSound("dragon_step") ;

    // ===== KEYFRAMED ANIMATION SOUNDS =====
    public static final RegistryObject<SoundEvent> FLAP1 = registerSound("flap1");
    public static final RegistryObject<SoundEvent> STEP1 = registerSound("step1");
    public static final RegistryObject<SoundEvent> STEP2 = registerSound("step2");
    // Run steps (faster cadence, distinct audio)
    public static final RegistryObject<SoundEvent> RUN_STEP1 = registerSound("run_step1");
    public static final RegistryObject<SoundEvent> RUN_STEP2 = registerSound("run_step2");

    /**
     * Helper method to register sounds without writing the same crap over and over
     */
    private static RegistryObject<SoundEvent> registerSound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SaintsDragons.MOD_ID, name)));
    }
}
