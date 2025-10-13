package com.leon.saintsdragons.server.command;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;

/**
 * Server commands for managing drake allies.
 * Provides /dragonally add/remove/list commands for administrators and drake owners.
 */
public class DragonAllyCommand {
    
    private static final SuggestionProvider<CommandSourceStack> DRAGON_SUGGESTIONS = (context, builder) -> {
        CommandSourceStack source = context.getSource();
        return SharedSuggestionProvider.suggest(
            source.getLevel().getEntitiesOfClass(DragonEntity.class, 
                source.getEntity() != null ? source.getEntity().getBoundingBox().inflate(50) : 
                net.minecraft.world.phys.AABB.ofSize(source.getPosition(), 100, 100, 100))
                .stream()
                .filter(dragon -> dragon.isTame() && source.getEntity() instanceof Player && dragon.isOwnedBy((Player) source.getEntity()))
                .map(dragon -> String.valueOf(dragon.getId())),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dragonally")
            .requires(source -> source.hasPermission(2)) // OP level 2 required
            .then(Commands.literal("add")
                .then(Commands.argument("drake", EntityArgument.entity())
                    .then(Commands.argument("username", StringArgumentType.string())
                        .executes(DragonAllyCommand::addAlly))))
            .then(Commands.literal("remove")
                .then(Commands.argument("drake", EntityArgument.entity())
                    .then(Commands.argument("username", StringArgumentType.string())
                        .executes(DragonAllyCommand::removeAlly))))
            .then(Commands.literal("list")
                .then(Commands.argument("drake", EntityArgument.entity())
                    .executes(DragonAllyCommand::listAllies)))
            .then(Commands.literal("clear")
                .then(Commands.argument("drake", EntityArgument.entity())
                    .executes(DragonAllyCommand::clearAllies))));
    }
    
    private static int addAlly(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "drake");
        String username = StringArgumentType.getString(context, "username");
        
        int successCount = 0;
        for (Entity entity : entities) {
            if (entity instanceof DragonEntity dragon) {
                DragonAllyManager.AllyResult result = dragon.allyManager.addAlly(username);
                Component message = Component.translatable("saintsdragons.command.ally.add.result", 
                    dragon.getDisplayName(), username, result.getMessage());
                source.sendSuccess(() -> message, false);
                
                if (result.isSuccess()) {
                    successCount++;
                }
            } else {
                source.sendFailure(Component.translatable("saintsdragons.command.ally.not_dragon", entity.getDisplayName()));
            }
        }
        
        return successCount;
    }
    
    private static int removeAlly(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "drake");
        String username = StringArgumentType.getString(context, "username");
        
        int successCount = 0;
        for (Entity entity : entities) {
            if (entity instanceof DragonEntity dragon) {
                DragonAllyManager.AllyResult result = dragon.allyManager.removeAlly(username);
                Component message = Component.translatable("saintsdragons.command.ally.remove.result", 
                    dragon.getDisplayName(), username, result.getMessage());
                source.sendSuccess(() -> message, false);
                
                if (result.isSuccess()) {
                    successCount++;
                }
            } else {
                source.sendFailure(Component.translatable("saintsdragons.command.ally.not_dragon", entity.getDisplayName()));
            }
        }
        
        return successCount;
    }
    
    private static int listAllies(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "drake");
        
        for (Entity entity : entities) {
            if (entity instanceof DragonEntity dragon) {
                List<String> allies = dragon.allyManager.getAllyUsernames();
                Component header = Component.translatable("saintsdragons.command.ally.list.header", 
                    dragon.getDisplayName(), allies.size(), dragon.allyManager.getMaxAllies());
                source.sendSuccess(() -> header, false);
                
                if (allies.isEmpty()) {
                    source.sendSuccess(() -> Component.translatable("saintsdragons.command.ally.list.empty"), false);
                } else {
                    for (String ally : allies) {
                        source.sendSuccess(() -> Component.literal("  - " + ally), false);
                    }
                }
            } else {
                source.sendFailure(Component.translatable("saintsdragons.command.ally.not_dragon", entity.getDisplayName()));
            }
        }
        
        return entities.size();
    }
    
    private static int clearAllies(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "drake");
        
        int successCount = 0;
        for (Entity entity : entities) {
            if (entity instanceof DragonEntity dragon) {
                dragon.allyManager.clearAllies();
                Component message = Component.translatable("saintsdragons.command.ally.clear.success", 
                    dragon.getDisplayName());
                source.sendSuccess(() -> message, false);
                successCount++;
            } else {
                source.sendFailure(Component.translatable("saintsdragons.command.ally.not_dragon", entity.getDisplayName()));
            }
        }
        
        return successCount;
    }
}
