package com.leon.saintsdragons.server.command;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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
 * Command to change Ignivorus dragon variant (default/crimson).
 * The variant texture displayed depends on both variant and gender (male/female).
 * Usage: /setvariant <dragon_uuid> <default|crimson>
 */
public final class DragonSetVariantCommand {
    private static final double HIT_RANGE = 64.0D;

    private static final SuggestionProvider<CommandSourceStack> DRAGON_UUID_SUGGESTIONS = (context, builder) -> {
        CommandSourceStack source = context.getSource();
        Set<DragonEntity> ordered = new LinkedHashSet<>();

        // Only suggest the dragon being looked at if it's an Ignivorus
        DragonEntity lookedAt = findLookedAtIgnivorus(source);
        if (lookedAt != null) {
            ordered.add(lookedAt);
        }

        for (DragonEntity dragon : ordered) {
            builder.suggest(dragon.getUUID().toString(), dragon.getDisplayName());
        }

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(new String[]{"default", "crimson"}, builder);

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DRAGON =
        new DynamicCommandExceptionType(id -> Component.translatable("saintsdragons.command.setvariant.not_found", id));

    private static final SimpleCommandExceptionType ERROR_INVALID_VARIANT =
        new SimpleCommandExceptionType(Component.translatable("saintsdragons.command.setvariant.invalid_variant"));

    private static final SimpleCommandExceptionType ERROR_NOT_IGNIVORUS =
        new SimpleCommandExceptionType(Component.translatable("saintsdragons.command.setvariant.not_ignivorus"));

    private DragonSetVariantCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setvariant")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("dragon", uuidArgument())
                .suggests(DRAGON_UUID_SUGGESTIONS)
                .then(Commands.argument("variant", StringArgumentType.word())
                    .suggests(VARIANT_SUGGESTIONS)
                    .executes(DragonSetVariantCommand::setVariant))));
    }

    private static ArgumentType<UUID> uuidArgument() {
        return UuidArgument.uuid();
    }

    private static int setVariant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID dragonId = UuidArgument.getUuid(context, "dragon");
        String variantStr = StringArgumentType.getString(context, "variant").toLowerCase();
        CommandSourceStack source = context.getSource();

        // Parse variant
        int variant;
        switch (variantStr) {
            case "default":
                variant = 0;
                break;
            case "crimson":
                variant = 1;
                break;
            default:
                throw ERROR_INVALID_VARIANT.create();
        }

        // Find dragon
        DragonEntity dragon = findDragon(source, dragonId);
        if (dragon == null) {
            throw ERROR_UNKNOWN_DRAGON.create(dragonId.toString());
        }

        // Check if dragon is an Ignivorus
        if (!(dragon instanceof Ignivorus ignivorus)) {
            throw ERROR_NOT_IGNIVORUS.create();
        }

        // Set variant
        int oldVariant = ignivorus.getTextureVariant();
        ignivorus.setTextureVariant(variant);

        // Send success message
        Component successMessage = Component.translatable(
            "saintsdragons.command.setvariant.success",
            dragon.getDisplayName(),
            Component.translatable("saintsdragons.variant." + variantStr)
        );
        source.sendSuccess(() -> successMessage, false);

        // Info message if variant didn't change
        if (oldVariant == variant) {
            Component infoMessage = Component.translatable(
                "saintsdragons.command.setvariant.unchanged",
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

    private static DragonEntity findLookedAtIgnivorus(CommandSourceStack source) {
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
            target -> target instanceof Ignivorus && target.isPickable()
        );

        if (result != null && result.getEntity() instanceof DragonEntity dragon) {
            return dragon;
        }
        return null;
    }
}
