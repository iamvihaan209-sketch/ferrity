package com.vihaan.ferritymod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import com.vihaan.ferritymod.entity.FerrityEntity;
import com.vihaan.ferritymod.init.FerritymodModEntities;

@EventBusSubscriber(modid = "ferritymod")
public class Ferrityspawned {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.level();

        // Only spawn Ferrity in the Overworld.
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        FerritySpawnData data =
                level.getServer()
                        .getDataStorage()
                        .computeIfAbsent(FerritySpawnData.TYPE);

        // This world already got its Ferrity.
        if (data.spawned) {
            return;
        }

        FerrityEntity ferrity =
                FerritymodModEntities.FERRITY.get().create(
                        level,
                        EntitySpawnReason.TRIGGERED
                );

        if (ferrity == null) {
            return;
        }

        // Put Ferrity two blocks beside the player.
        ferrity.setPos(
                player.getX() + 2.0D,
                player.getY(),
                player.getZ()
        );

        if (level.addFreshEntity(ferrity)) {
            data.spawned = true;
            data.setDirty();
        }
    }

    public static class FerritySpawnData extends SavedData {

        private boolean spawned;

        public FerritySpawnData() {
            this(false);
        }

        public FerritySpawnData(boolean spawned) {
            this.spawned = spawned;
        }

        public static final Codec<FerritySpawnData> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                Codec.BOOL
                                        .optionalFieldOf("spawned", false)
                                        .forGetter(data -> data.spawned)
                        ).apply(instance, FerritySpawnData::new)
                );

        public static final SavedDataType<FerritySpawnData> TYPE =
                new SavedDataType<>(
                        Identifier.fromNamespaceAndPath(
                                "ferritymod",
                                "ferrity_spawn"
                        ),
                        FerritySpawnData::new,
                        CODEC,
                        null
                );
    }
}