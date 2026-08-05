package com.leon.saintsdragons.server.command;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
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
import net.minecraft.resources.ResourceLocation;
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
 * Command to change dragon texture variants.
 * Dragons with no variants are treated as default-only.
 * Usage: /setvariant <dragon_uuid> <variant_name>
 */
public final class DragonSetVariantCommand {
    private static final double HIT_RANGE = 64.0D;

    private static final SuggestionProvider<CommandSourceStack> DRAGON_UUID_SUGGESTIONS = (context, builder) -> {
        CommandSourceStack source = context.getSource();
        Set<DragonEntity> ordered = new LinkedHashSet<>();

        // Suggest the dragon being looked at (any dragon type)
        DragonEntity lookedAt = findLookedAtDragon(source);
        if (lookedAt != null) {
            ordered.add(lookedAt);
        }

        for (DragonEntity dragon : ordered) {
            builder.suggest(dragon.getUUID().toString(), dragon.getDisplayName());
        }

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (context, builder) -> {
        try {
            UUID dragonId = UuidArgument.getUuid(context, "dragon");
            DragonEntity dragon = findDragon(context.getSource(), dragonId);
            if (dragon != null) {
                return SharedSuggestionProvider.suggest(dragon.getTextureVariantCommandSuggestions(), builder);
            }
        } catch (IllegalArgumentException ignored) {
            // Ignore and fall back to default suggestion.
        }
        return SharedSuggestionProvider.suggest(new String[]{"default"}, builder);
    };

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DRAGON =
        new DynamicCommandExceptionType(id -> Component.translatable("saintsdragons.command.setvariant.not_found", id));

    private static final SimpleCommandExceptionType ERROR_INVALID_VARIANT =
        new SimpleCommandExceptionType(Component.translatable("saintsdragons.command.setvariant.invalid_variant"));

    private DragonSetVariantCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setvariant")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("dragon", uuidArgument())
                .suggests(DRAGON_UUID_SUGGESTIONS)
                .then(Commands.argument("variant", StringArgumentType.greedyString())
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

        // Find dragon
        DragonEntity dragon = findDragon(source, dragonId);
        if (dragon == null) {
            throw ERROR_UNKNOWN_DRAGON.create(dragonId.toString());
        }

        ResourceLocation variant = dragon.getTextureVariantIdNameMap().get(variantStr);
        if (variant == null) {
            throw ERROR_INVALID_VARIANT.create();
        }

        ResourceLocation oldVariant = dragon.getCodexTextureVariantId();
        dragon.setTextureVariantId(variant);

        // Send success message
        String labelKey = dragon.getTextureVariantTranslationKey(variant);
        Component successMessage = Component.translatable(
            "saintsdragons.command.setvariant.success",
            dragon.getDisplayName(),
            Component.translatable(labelKey)
        );
        source.sendSuccess(() -> successMessage, false);

        // Info message if variant didn't change
        if (oldVariant.equals(variant)) {
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
