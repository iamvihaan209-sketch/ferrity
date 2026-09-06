package com.vihaan.ferritymod;

import com.vihaan.ferritymod.entity.FerrityEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class FerrityActions {

    public static void execute(
            MinecraftServer server,
            UUID playerId,
            String actionType,
            String itemId,
            int count
    ) {
        if (server == null || playerId == null) {
            return;
        }

        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(playerId);

        if (player == null) {
            return;
        }

        switch (actionType) {

            case "give_item" ->
                    giveItem(
                            player,
                            itemId,
                            count
                    );

            case "start_following" ->
                    setFollowing(
                            player,
                            true
                    );

            case "stop_following" ->
                    setFollowing(
                            player,
                            false
                    );

            default -> {
                // Unknown actions are ignored.
            }
        }
    }

    private static void giveItem(
            ServerPlayer player,
            String itemId,
            int requestedCount
    ) {
        if (
                itemId == null
                        || itemId.isBlank()
        ) {
            return;
        }

        Identifier id;

        try {
            id = Identifier.parse(
                    itemId.trim()
            );
        } catch (Exception error) {
            return;
        }

        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return;
        }

        Item item =
                BuiltInRegistries.ITEM.getValue(id);

        if (item == null) {
            return;
        }

        int count =
                Math.max(
                        1,
                        Math.min(
                                requestedCount,
                                item.getDefaultMaxStackSize()
                        )
                );

        ItemStack stack =
                new ItemStack(
                        item,
                        count
                );

        boolean inserted =
                player.getInventory()
                        .add(stack);

        if (!inserted || !stack.isEmpty()) {
            player.drop(
                    stack,
                    false
            );
        }
    }

    private static void setFollowing(
            ServerPlayer player,
            boolean enabled
    ) {
        FerrityEntity nearest =
                null;

        double nearestDistance =
                Double.MAX_VALUE;

        for (
                Entity entity :
                player.level().getAllEntities()
        ) {
            if (!(entity instanceof FerrityEntity ferrity)) {
                continue;
            }

            double distance =
                    ferrity.distanceToSqr(
                            player
                    );

            if (distance < nearestDistance) {
                nearestDistance =
                        distance;

                nearest =
                        ferrity;
            }
        }

        if (nearest == null) {
            return;
        }

        nearest.setFollowingEnabled(
                enabled
        );
    }

    private FerrityActions() {
    }
}