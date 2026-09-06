package com.vihaan.ferritymod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import com.vihaan.ferritymod.entity.FerrityEntity;
import com.vihaan.ferritymod.init.FerritymodModEntities;

public class FerrityitemItem extends Item {

    public FerrityitemItem(Item.Properties properties) {
        super(
                properties
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos spawnPos =
                context.getClickedPos()
                        .relative(
                                context.getClickedFace()
                        );

        FerrityEntity ferrity =
                FerritymodModEntities.FERRITY
                        .get()
                        .create(
                                level,
                                EntitySpawnReason.SPAWN_ITEM_USE
                        );

        if (ferrity == null) {
            return InteractionResult.FAIL;
        }

        ferrity.setPos(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D
        );

        level.addFreshEntity(
                ferrity
        );

        if (
                context.getPlayer() == null
                        || !context.getPlayer()
                        .getAbilities()
                        .instabuild
        ) {
            context.getItemInHand()
                    .shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}