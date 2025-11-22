package com.leon.saintsdragons.server.command;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Command to change dragon gender, which affects texture rendering.
 * Usage: /setgender <dragon_uuid> <male|female>
 */
public final class DragonSetGenderCommand {
    private static final double HIT_RANGE = 64.0D;

    private static final SuggestionProvider<CommandSourceStack> DRAGON_UUID_SUGGESTIONS = (context, builder) -> {
        CommandSourceStack source = context.getSource();
        Set<DragonEntity> ordered = new LinkedHashSet<>();

        // Only suggest the dragon being looked at
        DragonEntity lookedAt = findLookedAtDragon(source);
        if (lookedAt != null) {
            ordered.add(lookedAt);
        }

        for (DragonEntity dragon : ordered) {
            builder.suggest(dragon.getUUID().toString(), dragon.getDisplayName());
        }

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> GENDER_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(new String[]{"male", "female"}, builder);

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DRAGON =
        new DynamicCommandExceptionType(id -> Component.translatable("saintsdragons.command.setgender.not_found", id));

    private static final SimpleCommandExceptionType ERROR_INVALID_GENDER =
        new SimpleCommandExceptionType(Component.translatable("saintsdragons.command.setgender.invalid_gender"));

    private DragonSetGenderCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setgender")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("dragon", uuidArgument())
                .suggests(DRAGON_UUID_SUGGESTIONS)
                .then(Commands.argument("gender", StringArgumentType.word())
                    .suggests(GENDER_SUGGESTIONS)
                    .executes(DragonSetGenderCommand::setGender))));
    }

    private static ArgumentType<UUID> uuidArgument() {
        return UuidArgument.uuid();
    }

    private static int setGender(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID dragonId = UuidArgument.getUuid(context, "dragon");
        String genderStr = StringArgumentType.getString(context, "gender").toLowerCase();
        CommandSourceStack source = context.getSource();

        // Parse gender
        DragonGender gender;
        switch (genderStr) {
            case "male":
                gender = DragonGender.MALE;
                break;
            case "female":
                gender = DragonGender.FEMALE;
                break;
            default:
                throw ERROR_INVALID_GENDER.create();
        }

        // Find dragon
        DragonEntity dragon = findDragon(source, dragonId);
        if (dragon == null) {
            throw ERROR_UNKNOWN_DRAGON.create(dragonId.toString());
        }

        // Set gender
        DragonGender oldGender = dragon.getGender();
        dragon.setGender(gender);

        // Send success message
        Component successMessage = Component.translatable(
            "saintsdragons.command.setgender.success",
            dragon.getDisplayName(),
            Component.translatable("saintsdragons.gender." + genderStr)
        );
        source.sendSuccess(() -> successMessage, false);

        // Info message if gender didn't change
        if (oldGender == gender) {
            Component infoMessage = Component.translatable(
                "saintsdragons.command.setgender.unchanged",
                dragon.getDisplayName()
            );
            source.sendSuccess(() -> infoMessage, false);
        }

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
