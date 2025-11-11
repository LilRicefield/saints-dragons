package com.leon.saintsdragons.server.command;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Debug command to instantly tame dragons by UUID.
 * Usage: /tame <dragon_uuid> [player]
 */
public final class DragonTameCommand {
    private static final double SUGGESTION_RADIUS = 64.0D;
    private static final double HIT_RANGE = 64.0D;

    private static final SuggestionProvider<CommandSourceStack> DRAGON_UUID_SUGGESTIONS = (context, builder) -> {
        CommandSourceStack source = context.getSource();
        Set<DragonEntity> ordered = new LinkedHashSet<>();

        DragonEntity lookedAt = findLookedAtDragon(source);
        if (lookedAt != null) {
            ordered.add(lookedAt);
        }

        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();
        AABB scanBox = new AABB(origin, origin).inflate(SUGGESTION_RADIUS);
        level.getEntitiesOfClass(DragonEntity.class, scanBox)
            .stream()
            .sorted(Comparator.comparingDouble(dragon -> dragon.distanceToSqr(origin.x, origin.y, origin.z)))
            .forEach(ordered::add);

        for (DragonEntity dragon : ordered) {
            builder.suggest(dragon.getUUID().toString(), dragon.getDisplayName());
        }

        return builder.buildFuture();
    };

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DRAGON =
        new DynamicCommandExceptionType(id -> Component.translatable("saintsdragons.command.tame.not_found", id));

    private static final DynamicCommandExceptionType ERROR_ALREADY_TAMED =
        new DynamicCommandExceptionType(name -> Component.translatable("saintsdragons.command.tame.already", name));

    private DragonTameCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tame")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("dragon", uuidArgument())
                .suggests(DRAGON_UUID_SUGGESTIONS)
                .executes(ctx -> tameDragon(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> tameDragon(ctx, EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static ArgumentType<UUID> uuidArgument() {
        return UuidArgument.uuid();
    }

    private static int tameDragon(CommandContext<CommandSourceStack> context, Player owner) throws CommandSyntaxException {
        UUID dragonId = UuidArgument.getUuid(context, "dragon");
        CommandSourceStack source = context.getSource();
        DragonEntity dragon = findDragon(source, dragonId);

        if (dragon == null) {
            throw ERROR_UNKNOWN_DRAGON.create(dragonId.toString());
        }

        if (dragon.isTame() && dragon.getOwner() != null && dragon.isOwnedBy(owner)) {
            throw ERROR_ALREADY_TAMED.create(dragon.getDisplayName().getString());
        }

        dragon.tame(owner);
        dragon.setOrderedToSit(false);
        dragon.setTarget(null);

        Component successMessage = Component.translatable(
            "saintsdragons.command.tame.success",
            dragon.getDisplayName(),
            owner.getDisplayName()
        );
        source.sendSuccess(() -> successMessage, false);
        return 1;
    }

    private static DragonEntity findDragon(CommandSourceStack source, UUID id) {
        Entity entity = source.getLevel().getEntity(id);
        if (entity instanceof DragonEntity dragon) {
            return dragon;
        }
        return null;
    }

    private static DragonEntity findLookedAtDragon(CommandSourceStack source) {
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof LivingEntity living)) {
            return null;
        }

        Vec3 start = living.getEyePosition();
        Vec3 look = living.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(HIT_RANGE));
        AABB box = living.getBoundingBox().expandTowards(look.scale(HIT_RANGE)).inflate(1.0D);

        EntityHitResult result = ProjectileUtil.getEntityHitResult(
            living.level(),
            living,
            start,
            end,
            box,
            target -> target instanceof DragonEntity && target.isPickable()
        );

        if (result != null && result.getEntity() instanceof DragonEntity dragon) {
            return dragon;
        }
        return null;
    }
}
