package com.vihaan.ferritymod;

import com.vihaan.ferritymod.entity.FerrityEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FerrityWorldContext {

    private static final int HORIZONTAL_RADIUS =
            16;

    private static final int VERTICAL_RADIUS =
            8;

    private static final int MAX_NOTABLE_BLOCKS =
            40;

    private static final int MAX_ENTITIES =
            30;

    public static String capture(
            MinecraftServer server,
            UUID playerId
    ) {
        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(playerId);

        if (player == null) {
            return """
                    CURRENT WORLD AROUND FERRITY
                    The player could not currently be located.
                    """;
        }

        ServerLevel level =
                (ServerLevel) player.level();

        FerrityEntity ferrity =
                findFerrity(
                        level,
                        player
                );

        String playerName =
                player.nameAndId()
                        .name();

        if (ferrity == null) {
            return """
                    CURRENT WORLD AROUND FERRITY
                    Player: %s
                    Ferrity could not currently locate his own entity nearby.
                    """.formatted(
                            playerName
                    );
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                """
                CURRENT WORLD AROUND FERRITY
                This is temporary live perception, not permanent conversation memory.

                """
        );

        appendPlayerInfo(
                result,
                player,
                playerName
        );

        appendInventoryInfo(
                result,
                player
        );

        appendFerrityInfo(
                result,
                ferrity
        );

        appendEnvironmentInfo(
                result,
                level
        );

        appendEntities(
                result,
                level,
                ferrity,
                player
        );

        appendBlocksAndStructures(
                result,
                level,
                ferrity.blockPosition()
        );

        return result.toString();
    }

    private static FerrityEntity findFerrity(
            ServerLevel level,
            ServerPlayer player
    ) {
        AABB searchArea =
                new AABB(
                        player.getX() - 64.0D,
                        player.getY() - 32.0D,
                        player.getZ() - 64.0D,

                        player.getX() + 64.0D,
                        player.getY() + 32.0D,
                        player.getZ() + 64.0D
                );

        List<FerrityEntity> ferrities =
                level.getEntitiesOfClass(
                        FerrityEntity.class,
                        searchArea
                );

        FerrityEntity nearest =
                null;

        double nearestDistance =
                Double.MAX_VALUE;

        for (
                FerrityEntity candidate :
                ferrities
        ) {
            double distance =
                    candidate.distanceToSqr(
                            player
                    );

            if (distance < nearestDistance) {
                nearestDistance =
                        distance;

                nearest =
                        candidate;
            }
        }

        return nearest;
    }

    private static void appendPlayerInfo(
            StringBuilder result,
            ServerPlayer player,
            String playerName
    ) {
        result.append(
                "Player:\n"
        );

        result.append("- Name: ")
                .append(playerName)
                .append("\n");

        result.append("- Position: ")
                .append(
                        formatPosition(
                                player.getX(),
                                player.getY(),
                                player.getZ()
                        )
                )
                .append("\n");

        result.append("- Health: ")
                .append(
                        Math.round(
                                player.getHealth()
                                        * 10.0F
                        ) / 10.0F
                )
                .append("/")
                .append(
                        Math.round(
                                player.getMaxHealth()
                                        * 10.0F
                        ) / 10.0F
                )
                .append("\n\n");
    }

    private static void appendInventoryInfo(
            StringBuilder result,
            ServerPlayer player
    ) {
        result.append(
                "Player inventory:\n"
        );

        appendItemStack(
                result,
                "- Main hand: ",
                player.getMainHandItem()
        );

        appendItemStack(
                result,
                "- Offhand: ",
                player.getOffhandItem()
        );

        result.append(
                "- Inventory contents:\n"
        );

        Map<String, Integer> totals =
                new HashMap<>();

        int containerSize =
                player.getInventory()
                        .getContainerSize();

        for (int slot = 0; slot < containerSize; slot++) {
            ItemStack stack =
                    player.getInventory()
                            .getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            String itemId =
                    BuiltInRegistries.ITEM
                            .getKey(stack.getItem())
                            .toString();

            totals.merge(
                    itemId,
                    stack.getCount(),
                    Integer::sum
            );
        }

        if (totals.isEmpty()) {
            result.append(
                    "  - Empty\n"
            );
        } else {
            totals.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry ->
                            result.append("  - ")
                                    .append(entry.getKey())
                                    .append(" x")
                                    .append(entry.getValue())
                                    .append("\n")
                    );
        }

        result.append("\n");
    }

    private static void appendItemStack(
            StringBuilder result,
            String prefix,
            ItemStack stack
    ) {
        result.append(prefix);

        if (stack.isEmpty()) {
            result.append("empty\n");
            return;
        }

        result.append(
                BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
        );

        result.append(" x")
                .append(stack.getCount())
                .append("\n");
    }

    private static void appendFerrityInfo(
            StringBuilder result,
            FerrityEntity ferrity
    ) {
        result.append(
                "Ferrity:\n"
        );

        result.append("- Position: ")
                .append(
                        formatPosition(
                                ferrity.getX(),
                                ferrity.getY(),
                                ferrity.getZ()
                        )
                )
                .append("\n\n");
    }

    private static void appendEnvironmentInfo(
            StringBuilder result,
            ServerLevel level
    ) {
        result.append(
                "Environment:\n"
        );

        result.append("- Dimension: ")
                .append(
                        level.dimension()
                                .identifier()
                )
                .append("\n");

        long clockTime =
                level.getDefaultClockTime();

        long timeOfDay =
                Math.floorMod(
                        clockTime,
                        24000L
                );

        result.append("- Time: ")
                .append(
                        describeTime(
                                timeOfDay
                        )
                )
                .append(" (")
                .append(timeOfDay)
                .append(" ticks)\n");

        String weather;

        if (level.isThundering()) {
            weather =
                    "thunderstorm";

        } else if (level.isRaining()) {
            weather =
                    "rain";

        } else {
            weather =
                    "clear";
        }

        result.append("- Weather: ")
                .append(weather)
                .append("\n\n");
    }

    private static void appendEntities(
            StringBuilder result,
            ServerLevel level,
            FerrityEntity ferrity,
            ServerPlayer player
    ) {
        AABB area =
                new AABB(
                        ferrity.getX()
                                - HORIZONTAL_RADIUS,

                        ferrity.getY()
                                - VERTICAL_RADIUS,

                        ferrity.getZ()
                                - HORIZONTAL_RADIUS,

                        ferrity.getX()
                                + HORIZONTAL_RADIUS,

                        ferrity.getY()
                                + VERTICAL_RADIUS,

                        ferrity.getZ()
                                + HORIZONTAL_RADIUS
                );

        List<Entity> entities =
                new ArrayList<>(
                        level.getEntities(
                                ferrity,
                                area
                        )
                );

        entities.sort(
                Comparator.comparingDouble(
                        ferrity::distanceToSqr
                )
        );

        result.append(
                "Nearby entities:\n"
        );

        int written =
                0;

        for (
                Entity entity :
                entities
        ) {
            if (entity == ferrity) {
                continue;
            }

            if (entity == player) {
                continue;
            }

            String type =
                    BuiltInRegistries.ENTITY_TYPE
                            .getKey(
                                    entity.getType()
                            )
                            .toString();

            double distance =
                    Math.sqrt(
                            ferrity.distanceToSqr(
                                    entity
                            )
                    );

            result.append("- ")
                    .append(type)
                    .append(" at ")
                    .append(
                            formatPosition(
                                    entity.getX(),
                                    entity.getY(),
                                    entity.getZ()
                            )
                    )
                    .append(", about ")
                    .append(
                            Math.round(
                                    distance
                                            * 10.0D
                            ) / 10.0D
                    )
                    .append(
                            " blocks away\n"
                    );

            written++;

            if (
                    written
                            >= MAX_ENTITIES
            ) {
                break;
            }
        }

        if (written == 0) {
            result.append(
                    "- None detected\n"
            );
        }

        result.append("\n");
    }

    private static void appendBlocksAndStructures(
            StringBuilder result,
            ServerLevel level,
            BlockPos center
    ) {
        Map<String, Integer> blockCounts =
                new HashMap<>();

        Map<String, Integer> buildingMaterials =
                new HashMap<>();

        List<String> notableBlocks =
                new ArrayList<>();

        int doors =
                0;

        int beds =
                0;

        int craftingTables =
                0;

        int furnaces =
                0;

        int chests =
                0;

        int barrels =
                0;

        int glassBlocks =
                0;

        int minX =
                center.getX()
                        - HORIZONTAL_RADIUS;

        int maxX =
                center.getX()
                        + HORIZONTAL_RADIUS;

        int minY =
                Math.max(
                        level.getMinY(),
                        center.getY()
                                - VERTICAL_RADIUS
                );

        int maxY =
                Math.min(
                        level.getMaxY() - 1,
                        center.getY()
                                + VERTICAL_RADIUS
                );

        int minZ =
                center.getZ()
                        - HORIZONTAL_RADIUS;

        int maxZ =
                center.getZ()
                        + HORIZONTAL_RADIUS;

        for (
                int x = minX;
                x <= maxX;
                x++
        ) {
            for (
                    int y = minY;
                    y <= maxY;
                    y++
            ) {
                for (
                        int z = minZ;
                        z <= maxZ;
                        z++
                ) {
                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );

                    BlockState state =
                            level.getBlockState(
                                    pos
                            );

                    if (state.isAir()) {
                        continue;
                    }

                    String block =
                            BuiltInRegistries.BLOCK
                                    .getKey(
                                            state.getBlock()
                                    )
                                    .toString();

                    String path =
                            BuiltInRegistries.BLOCK
                                    .getKey(
                                            state.getBlock()
                                    )
                                    .getPath();

                    blockCounts.merge(
                            block,
                            1,
                            Integer::sum
                    );

                    if (
                            isBuildingMaterial(
                                    path
                            )
                    ) {
                        buildingMaterials.merge(
                                block,
                                1,
                                Integer::sum
                        );
                    }

                    if (path.contains("door")) {
                        doors++;

                        addNotable(
                                notableBlocks,
                                block,
                                pos
                        );
                    }

                    if (path.contains("bed")) {
                        beds++;

                        addNotable(
                                notableBlocks,
                                block,
                                pos
                        );
                    }

                    if (
                            path.equals(
                                    "crafting_table"
                            )
                    ) {
                        craftingTables++;

                        addNotable(
                                notableBlocks,
                                block,
                                pos
                        );
                    }

                    if (
                            path.contains(
                                    "furnace"
                            )
                    ) {
                        furnaces++;

                        addNotable(
                                notableBlocks,
                                block,
                                pos
                        );
                    }

                    if (
                            path.equals("chest")
                                    || path.equals(
                                            "trapped_chest"
                                    )
                    ) {
                        chests++;

                        addNotable(
                                notableBlocks,
                                block,
                                pos
                        );
                    }

                    if (
                            path.equals(
                                    "barrel"
                            )
                    ) {
                        barrels++;

                        addNotable(
                                notableBlocks,
                                block,
                                pos
                        );
                    }

                    if (
                            path.contains(
                                    "glass"
                            )
                    ) {
                        glassBlocks++;
                    }
                }
            }
        }

        result.append(
                "Notable nearby blocks:\n"
        );

        if (
                notableBlocks.isEmpty()
        ) {
            result.append(
                    "- No notable utility/interior blocks detected\n"
            );

        } else {
            for (
                    String block :
                    notableBlocks
            ) {
                result.append("- ")
                        .append(block)
                        .append("\n");
            }
        }

        result.append("\n");

        appendCommonMaterials(
                result,
                blockCounts
        );

        int interiorObjects =
                beds
                        + craftingTables
                        + furnaces
                        + chests
                        + barrels;

        int totalBuildingMaterialBlocks =
                buildingMaterials
                        .values()
                        .stream()
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();

        boolean possibleHouse =
                doors >= 1
                        && interiorObjects >= 1
                        && totalBuildingMaterialBlocks
                        >= 20;

        result.append(
                "Possible structures:\n"
        );

        if (possibleHouse) {
            result.append(
                    "- Possible player-built house detected nearby.\n"
            );

            result.append(
                    "- This is an inference from the nearby blocks, not a guaranteed structure ID.\n"
            );

            result.append(
                    "- House clues: "
            );

            result.append(doors)
                    .append(
                            " door(s), "
                    );

            result.append(beds)
                    .append(
                            " bed(s), "
                    );

            result.append(
                            craftingTables
                    )
                    .append(
                            " crafting table(s), "
                    );

            result.append(furnaces)
                    .append(
                            " furnace(s), "
                    );

            result.append(chests)
                    .append(
                            " chest(s), "
                    );

            result.append(barrels)
                    .append(
                            " barrel(s), "
                    );

            result.append(glassBlocks)
                    .append(
                            " glass block(s).\n"
                    );

            appendHouseMaterials(
                    result,
                    buildingMaterials
            );

        } else {
            result.append(
                    "- No strong evidence of a house was detected nearby.\n"
            );
        }

        result.append("\n");
    }

    private static void appendCommonMaterials(
            StringBuilder result,
            Map<String, Integer> blockCounts
    ) {
        result.append(
                "Most common nearby block types:\n"
        );

        blockCounts.entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, Integer>
                                comparingByValue()
                                .reversed()
                )
                .limit(12)
                .forEach(entry ->
                        result.append("- ")
                                .append(
                                        entry.getKey()
                                )
                                .append(": ")
                                .append(
                                        entry.getValue()
                                )
                                .append("\n")
                );

        result.append("\n");
    }

    private static void appendHouseMaterials(
            StringBuilder result,
            Map<String, Integer> materials
    ) {
        result.append(
                "- Mostly made from: "
        );

        List<Map.Entry<String, Integer>> sorted =
                materials.entrySet()
                        .stream()
                        .sorted(
                                Map.Entry
                                        .<String, Integer>
                                        comparingByValue()
                                        .reversed()
                        )
                        .limit(5)
                        .toList();

        if (sorted.isEmpty()) {
            result.append(
                    "unknown materials"
            );

        } else {
            for (
                    int i = 0;
                    i < sorted.size();
                    i++
            ) {
                if (i > 0) {
                    result.append(", ");
                }

                result.append(
                        sorted.get(i)
                                .getKey()
                );
            }
        }

        result.append("\n");
    }

    private static boolean isBuildingMaterial(
            String path
    ) {
        return path.contains("planks")
                || path.contains("log")
                || path.contains("wood")
                || path.contains("bricks")
                || path.contains("cobblestone")
                || path.contains("stone_bricks")
                || path.contains("sandstone")
                || path.contains("concrete")
                || path.contains("terracotta")
                || path.contains("deepslate")
                || path.contains("glass")
                || path.contains("quartz")
                || path.contains("prismarine");
    }

    private static void addNotable(
            List<String> blocks,
            String block,
            BlockPos pos
    ) {
        if (
                blocks.size()
                        >= MAX_NOTABLE_BLOCKS
        ) {
            return;
        }

        blocks.add(
                block
                        + " at "
                        + pos.getX()
                        + ", "
                        + pos.getY()
                        + ", "
                        + pos.getZ()
        );
    }

    private static String formatPosition(
            double x,
            double y,
            double z
    ) {
        return Math.round(x)
                + ", "
                + Math.round(y)
                + ", "
                + Math.round(z);
    }

    private static String describeTime(
            long time
    ) {
        if (time < 1000L) {
            return "sunrise";
        }

        if (time < 6000L) {
            return "morning";
        }

        if (time < 12000L) {
            return "day";
        }

        if (time < 13000L) {
            return "sunset";
        }

        if (time < 18000L) {
            return "evening/night";
        }

        if (time < 23000L) {
            return "night";
        }

        return "late night";
    }

    private FerrityWorldContext() {
    }
}