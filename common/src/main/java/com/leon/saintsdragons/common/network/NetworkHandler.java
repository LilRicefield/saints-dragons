package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class NetworkHandler {
    private static final NetworkHelper NETWORK = Services.PLATFORM.getNetworkHelper();
    private static boolean registered = false;

    private NetworkHandler() {
    }

    private static ResourceLocation id(String path) {
        return SaintsDragonsCommon.rl(path);
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        NETWORK.registerServerbound(
                MessageDragonRideInput.class,
                id("dragon_ride_input"),
                MessageDragonRideInput::encode,
                MessageDragonRideInput::decode,
                MessageDragonRideInput::handle
        );

        NETWORK.registerServerbound(
                MessageDragonBonePositions.class,
                id("dragon_bone_positions"),
                MessageDragonBonePositions::encode,
                MessageDragonBonePositions::decode,
                MessageDragonBonePositions::handle
        );

        NETWORK.registerServerbound(
                MessageDraconicCodexRequest.class,
                id("draconic_codex_request"),
                MessageDraconicCodexRequest::encode,
                MessageDraconicCodexRequest::decode,
                MessageDraconicCodexRequest::handle
        );

        NETWORK.registerServerbound(
                MessageGlobalAllyRequest.class,
                id("global_ally_request"),
                MessageGlobalAllyRequest::encode,
                MessageGlobalAllyRequest::decode,
                MessageGlobalAllyRequest::handle
        );

        NETWORK.registerServerbound(
                MessageGlobalAllyManagement.class,
                id("global_ally_management"),
                MessageGlobalAllyManagement::encode,
                MessageGlobalAllyManagement::decode,
                MessageGlobalAllyManagement::handle
        );

        NETWORK.registerServerbound(
                MessageDragonlordDoubleJump.class,
                id("dragonlord_double_jump"),
                MessageDragonlordDoubleJump::encode,
                MessageDragonlordDoubleJump::decode,
                MessageDragonlordDoubleJump::handle
        );

        NETWORK.registerServerbound(
                MessageDragonlordSwordAbility.class,
                id("dragonlord_sword_ability"),
                MessageDragonlordSwordAbility::encode,
                MessageDragonlordSwordAbility::decode,
                MessageDragonlordSwordAbility::handle
        );

        NETWORK.registerServerbound(
                MessageBloodTempestKatanaAbility.class,
                id("blood_tempest_katana_ability"),
                MessageBloodTempestKatanaAbility::encode,
                MessageBloodTempestKatanaAbility::decode,
                MessageBloodTempestKatanaAbility::handle
        );

        NETWORK.registerServerbound(
                MessageBloodTempestDodge.class,
                id("blood_tempest_dodge"),
                MessageBloodTempestDodge::encode,
                MessageBloodTempestDodge::decode,
                MessageBloodTempestDodge::handle
        );

        NETWORK.registerServerbound(
                MessageDialogueChoice.class,
                id("dialogue_choice"),
                MessageDialogueChoice::encode,
                MessageDialogueChoice::decode,
                MessageDialogueChoice::handle
        );

        NETWORK.registerServerbound(
                MessageDialogueDismiss.class,
                id("dialogue_dismiss"),
                MessageDialogueDismiss::encode,
                MessageDialogueDismiss::decode,
                MessageDialogueDismiss::handle
        );

        NETWORK.registerServerbound(
                MessageDialogueNameInput.class,
                id("dialogue_name_input"),
                MessageDialogueNameInput::encode,
                MessageDialogueNameInput::decode,
                MessageDialogueNameInput::handle
        );

        NETWORK.registerClientbound(
                MessageDraconicCodexList.class,
                id("draconic_codex_list"),
                MessageDraconicCodexList::encode,
                MessageDraconicCodexList::decode,
                MessageDraconicCodexList::handle
        );

        NETWORK.registerClientbound(
                MessageGlobalAllyList.class,
                id("global_ally_list"),
                MessageGlobalAllyList::encode,
                MessageGlobalAllyList::decode,
                MessageGlobalAllyList::handle
        );

        NETWORK.registerClientbound(
                MessageGlobalAllyDelta.class,
                id("global_ally_delta"),
                MessageGlobalAllyDelta::encode,
                MessageGlobalAllyDelta::decode,
                MessageGlobalAllyDelta::handle
        );

        NETWORK.registerClientbound(
                MessageDragonMeleeMode.class,
                id("dragon_melee_mode"),
                MessageDragonMeleeMode::encode,
                MessageDragonMeleeMode::decode,
                MessageDragonMeleeMode::handle
        );

        NETWORK.registerClientbound(
                MessageBloodTempestAfterimage.class,
                id("blood_tempest_afterimage"),
                MessageBloodTempestAfterimage::encode,
                MessageBloodTempestAfterimage::decode,
                MessageBloodTempestAfterimage::handle
        );

        NETWORK.registerClientbound(
                MessageCameraImpulse.class,
                id("camera_impulse"),
                MessageCameraImpulse::encode,
                MessageCameraImpulse::decode,
                MessageCameraImpulse::handle
        );

        NETWORK.registerClientbound(
                MessageDragonMovingSound.class,
                id("dragon_moving_sound"),
                MessageDragonMovingSound::encode,
                MessageDragonMovingSound::decode,
                MessageDragonMovingSound::handle
        );

        NETWORK.registerClientbound(
                MessageSwarmBattleMusic.class,
                id("swarm_battle_music"),
                MessageSwarmBattleMusic::encode,
                MessageSwarmBattleMusic::decode,
                MessageSwarmBattleMusic::handle
        );

        NETWORK.registerClientbound(
                MessageSwarmWaveBar.class,
                id("swarm_wave_bar"),
                MessageSwarmWaveBar::encode,
                MessageSwarmWaveBar::decode,
                MessageSwarmWaveBar::handle
        );

        NETWORK.registerClientbound(
                MessageDragonAbilityDebugBox.class,
                id("dragon_ability_debug_box"),
                MessageDragonAbilityDebugBox::encode,
                MessageDragonAbilityDebugBox::decode,
                MessageDragonAbilityDebugBox::handle
        );

        NETWORK.registerClientbound(
                MessageDialogueOpen.class,
                id("dialogue_open"),
                MessageDialogueOpen::encode,
                MessageDialogueOpen::decode,
                MessageDialogueOpen::handle
        );

        NETWORK.registerClientbound(
                MessageDialogueClose.class,
                id("dialogue_close"),
                MessageDialogueClose::encode,
                MessageDialogueClose::decode,
                MessageDialogueClose::handle
        );
    }

    public static void sendToServer(Object message) {
        NETWORK.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        NETWORK.sendToPlayer(player, message);
    }

    public static void sendToTracking(Entity entity, Object message) {
        NETWORK.sendToTracking(entity, message);
    }

    public static void sendToDimension(Level level, Object message) {
        NETWORK.sendToDimension(level, message);
    }
}
