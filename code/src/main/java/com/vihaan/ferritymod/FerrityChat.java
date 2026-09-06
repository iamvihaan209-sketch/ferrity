package com.vihaan.ferritymod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "ferritymod")
public class FerrityChat {

    /*
     * Keep track of players who already have a Ferrity
     * request running so one player cannot accidentally
     * send several AI requests at once.
     */
    private static final Set<UUID> WAITING =
            ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onChat(
            ServerChatEvent event
    ) {
        ServerPlayer player =
                event.getPlayer();

        String message =
                event.getRawText()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        /*
         * In multiplayer, Ferrity only responds when
         * somebody actually mentions him.
         */
        boolean mentionsFerrity =
                message.toLowerCase()
                        .contains("ferrity");

        if (!mentionsFerrity) {
            return;
        }

        UUID playerId =
                player.getUUID();

        if (!WAITING.add(playerId)) {
            player.sendSystemMessage(
                    Component.literal(
                            "<Ferrity> Wait, I'm still thinking!"
                    )
            );

            return;
        }

        MinecraftServer server =
                player.level().getServer();

        if (server == null) {
            WAITING.remove(playerId);
            return;
        }

        FerrityAI.ask(
                        server,
                        playerId,
                        message
                )
                .thenAccept(response -> {
                    /*
                     * Return to the Minecraft server thread
                     * before touching player/game state.
                     */
                    server.execute(() -> {
                        try {
                            ServerPlayer currentPlayer =
                                    server.getPlayerList()
                                            .getPlayer(playerId);

                            if (currentPlayer != null) {
                                currentPlayer.sendSystemMessage(
                                        Component.literal(
                                                "<Ferrity> "
                                                        + response
                                        )
                                );
                            }
                        } finally {
                            WAITING.remove(playerId);
                        }
                    });
                })
                .exceptionally(error -> {
                    server.execute(() -> {
                        try {
                            ServerPlayer currentPlayer =
                                    server.getPlayerList()
                                            .getPlayer(playerId);

                            if (currentPlayer != null) {
                                currentPlayer.sendSystemMessage(
                                        Component.literal(
                                                "<Ferrity> Something went wrong."
                                        )
                                );
                            }
                        } finally {
                            WAITING.remove(playerId);
                        }
                    });

                    return null;
                });
    }

    private FerrityChat() {
    }
}