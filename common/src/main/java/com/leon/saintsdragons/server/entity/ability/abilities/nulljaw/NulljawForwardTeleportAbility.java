package com.leon.saintsdragons.server.entity.ability.abilities.nulljaw;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

public final class NulljawForwardTeleportAbility extends DragonAbility<Nulljaw> {
    private static final int COOLDOWN_TICKS = 40;
    private static final DragonAbilitySection[] TRACK = {
            new DragonAbilitySection.AbilitySectionInstant(ACTIVE)
    };

    private Vec3 destination;

    public NulljawForwardTeleportAbility(DragonAbilityType<Nulljaw, NulljawForwardTeleportAbility> type, Nulljaw user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Nulljaw dragon = getUser();
        if (!(dragon.getControllingPassenger() instanceof ServerPlayer rider)) {
            return false;
        }

        this.destination = dragon.findForwardTeleportDestination(rider);
        return this.destination != null;
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == ACTIVE && this.destination != null) {
            getUser().teleportMountedTo(this.destination);
        }
    }
}
