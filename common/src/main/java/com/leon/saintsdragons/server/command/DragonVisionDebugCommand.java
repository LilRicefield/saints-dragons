package com.leon.saintsdragons.server.command;

import com.leon.saintsdragons.common.network.MessageDragonVisionDebug;
import com.leon.saintsdragons.common.network.MessageDragonPathDebug;
import com.leon.saintsdragons.common.network.MessageDragonBrainDebug;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.debug.DragonPathDebugTracker;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

public final class DragonVisionDebugCommand {
    private DragonVisionDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("dragonvision").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("select").then(Commands.argument("dragon", EntityArgument.entity())
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    if (!player.canUseGameMasterBlocks()) {
                        context.getSource().sendFailure(Component.literal("Dragon debug requires a creative operator."));
                        return 0;
                    }
                    var entity = EntityArgument.getEntity(context, "dragon");
                    if (!(entity instanceof DragonEntity dragon)) {
                        context.getSource().sendFailure(Component.literal("Select a dragon."));
                        return 0;
                    }
                    DragonPathDebugTracker.toggle(player, dragon);
                    return 1;
                })));
        root.then(Commands.literal("off").executes(context -> {
            var player = context.getSource().getPlayerOrException();
            DragonPathDebugTracker.clear(player);
            NetworkHandler.sendToPlayer(player, MessageDragonPathDebug.clear());
            NetworkHandler.sendToPlayer(player, MessageDragonBrainDebug.clear());
            NetworkHandler.sendToPlayer(player, MessageDragonVisionDebug.clear());
            return 1;
        }));
        String[] layers = {"shape", "rays", "awareness", "projectiles", "all"};
        for (int i = 0; i < layers.length; i++) {
            String name = layers[i];
            int layer = i == 4 ? MessageDragonVisionDebug.ALL : 1 << i;
            root.then(Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        if (!DragonPathDebugTracker.setVisionLayer(context.getSource().getPlayerOrException(), layer, enabled)) {
                            context.getSource().sendFailure(Component.literal("Select a dragon with the debug stick or /dragonvision select first."));
                            return 0;
                        }
                        context.getSource().sendSuccess(() -> Component.literal("Dragon vision " + name + ": " + enabled), false);
                        return 1;
                    })));
        }
        dispatcher.register(root);
    }
}
