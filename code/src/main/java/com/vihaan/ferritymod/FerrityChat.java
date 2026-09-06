package com.vihaan.ferritymod;

import java.util.Locale;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;

@EventBusSubscriber(
        modid = "ferritymod",
        value = Dist.CLIENT
)
public class FerrityChat {

    private static boolean waitingForResponse =
            false;

    @SubscribeEvent
    public static void onChat(
            ClientChatEvent event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        String message =
                event.getMessage()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        boolean singleplayer =
                minecraft.hasSingleplayerServer();

        boolean mentionsFerrity =
                message
                        .toLowerCase(Locale.ROOT)
                        .contains("ferrity");

        if (
                !singleplayer
                        && !mentionsFerrity
        ) {
            return;
        }

        MinecraftServer server =
                minecraft.getSingleplayerServer();

        if (server == null) {
            addFerrityMessage(
                    minecraft,
                    "Persistent memory isn't available on this server yet."
            );

            return;
        }

        UUID playerId =
                minecraft.player.getUUID();

        /*
         * Handle obvious movement requests directly.
         *
         * This means Ferrity does NOT have to rely on
         * the AI deciding to emit an action marker.
         */
        handleDirectMovementRequest(
                server,
                playerId,
                message
        );

        if (waitingForResponse) {
            addFerrityMessage(
                    minecraft,
                    "Wait, I'm still thinking!"
            );

            return;
        }

        waitingForResponse =
                true;

        FerrityAI.ask(
                        server,
                        playerId,
                        message
                )
                .thenAccept(response -> {
                    minecraft.execute(() -> {
                        addFerrityMessage(
                                minecraft,
                                response
                        );

                        waitingForResponse =
                                false;
                    });
                })
                .exceptionally(error -> {
                    minecraft.execute(() -> {
                        addFerrityMessage(
                                minecraft,
                                "Something went wrong."
                        );

                        waitingForResponse =
                                false;
                    });

                    return null;
                });
    }

    private static void handleDirectMovementRequest(
            MinecraftServer server,
            UUID playerId,
            String message
    ) {
        String normalized =
                normalizeMessage(
                        message
                );

        /*
         * STOP has priority.
         *
         * This prevents something like:
         * "don't come"
         * from accidentally making Ferrity follow.
         */
        if (shouldStopFollowing(normalized)) {
            server.execute(() ->
                    FerrityActions.execute(
                            server,
                            playerId,
                            "stop_following",
                            "",
                            1
                    )
            );

            return;
        }

        if (shouldStartFollowing(normalized)) {
            server.execute(() ->
                    FerrityActions.execute(
                            server,
                            playerId,
                            "start_following",
                            "",
                            1
                    )
            );
        }
    }

    private static boolean shouldStartFollowing(
            String message
    ) {
        return message.equals("come")
                || message.equals("come here")
                || message.equals("follow")
                || message.equals("follow me")
                || message.equals("come with me")
                || message.equals("come follow me")
                || message.equals("start following")
                || message.equals("start following me")
                || message.equals("ferrity come")
                || message.equals("ferrity come here")
                || message.equals("ferrity follow")
                || message.equals("ferrity follow me")
                || message.equals("ferrity come with me");
    }

    private static boolean shouldStopFollowing(
            String message
    ) {
        return message.equals("stop")
                || message.equals("stay")
                || message.equals("wait")
                || message.equals("stop following")
                || message.equals("stop following me")
                || message.equals("dont follow me")
                || message.equals("don't follow me")
                || message.equals("do not follow me")
                || message.equals("dont come")
                || message.equals("don't come")
                || message.equals("do not come")
                || message.equals("stay here")
                || message.equals("wait here")
                || message.equals("ferrity stop")
                || message.equals("ferrity stay")
                || message.equals("ferrity wait")
                || message.equals("ferrity stop following")
                || message.equals("ferrity stop following me");
    }

    private static String normalizeMessage(
            String message
    ) {
        return message
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[.!?,;:]+$", "")
                .trim();
    }

    private static void addFerrityMessage(
            Minecraft minecraft,
            String message
    ) {
        minecraft.gui
                .getChat()
                .addClientSystemMessage(
                        Component.literal(
                                "<Ferrity> "
                                        + message
                        )
                );
    }

    private FerrityChat() {
    }
}