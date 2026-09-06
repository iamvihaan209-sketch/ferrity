package com.vihaan.ferritymod.entity;

import java.util.EnumSet;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import com.vihaan.ferritymod.init.FerritymodModEntities;
import com.vihaan.ferritymod.init.FerritymodModItems;

public class FerrityEntity extends Monster {

    private boolean followingEnabled = true;

    public FerrityEntity(
            EntityType<FerrityEntity> type,
            Level world
    ) {
        super(type, world);
        xpReward = 0;
        setNoAi(false);
    }

    public boolean isFollowingEnabled() {
        return followingEnabled;
    }

    public void setFollowingEnabled(boolean enabled) {
        this.followingEnabled = enabled;

        if (!enabled) {
            this.getNavigation().stop();
        }
    }

    /*
     * PICK UP DA ORB™
     *
     * Right-click Ferrity and he becomes the Ferrity item.
     */
    @Override
    public InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack ferrityItem =
                new ItemStack(
                        FerritymodModItems.FERRITYITEM.get()
                );

        boolean added =
                player.getInventory().add(
                        ferrityItem
                );

        /*
         * If the inventory is full, don't delete Ferrity.
         */
        if (!added) {
            return InteractionResult.PASS;
        }

        /*
         * Remove the entity only AFTER the item
         * successfully entered the inventory.
         */
        this.discard();

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                1,
                new FollowPlayerGoal(
                        this,
                        1.15D
                )
        );
    }

    @Override
    public boolean isInvulnerableTo(
            ServerLevel level,
            DamageSource source
    ) {
        return true;
    }

    @Override
    public SoundEvent getHurtSound(
            DamageSource source
    ) {
        return BuiltInRegistries.SOUND_EVENT.getValue(
                Identifier.parse(
                        "entity.generic.hurt"
                )
        );
    }

    @Override
    public SoundEvent getDeathSound() {
        return BuiltInRegistries.SOUND_EVENT.getValue(
                Identifier.parse(
                        "entity.generic.death"
                )
        );
    }

    @Override
    public boolean checkSpawnRules(
            LevelAccessor level,
            EntitySpawnReason reason
    ) {
        return this.level().dimension() == Level.OVERWORLD
                ? super.checkSpawnRules(
                        level,
                        reason
                )
                : true;
    }

    public static void init(
            RegisterSpawnPlacementsEvent event
    ) {
        event.register(
                FerritymodModEntities.FERRITY.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,

                (
                        entityType,
                        world,
                        reason,
                        pos,
                        random
                ) ->
                        world.getDifficulty()
                                != Difficulty.PEACEFUL

                                && (
                                EntitySpawnReason
                                        .ignoresLightRequirements(
                                                reason
                                        )

                                        || Monster
                                        .isDarkEnoughToSpawn(
                                                world,
                                                pos,
                                                random
                                        )
                        )

                                && Mob.checkMobSpawnRules(
                                entityType,
                                world,
                                reason,
                                pos,
                                random
                        ),

                RegisterSpawnPlacementsEvent
                        .Operation.REPLACE
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder =
                Mob.createMobAttributes();

        builder = builder.add(
                Attributes.MOVEMENT_SPEED,
                0.30D
        );

        builder = builder.add(
                Attributes.MAX_HEALTH,
                10.0D
        );

        builder = builder.add(
                Attributes.ARMOR,
                0.0D
        );

        builder = builder.add(
                Attributes.ATTACK_DAMAGE,
                0.0D
        );

        builder = builder.add(
                Attributes.FOLLOW_RANGE,
                32.0D
        );

        builder = builder.add(
                Attributes.STEP_HEIGHT,
                1.0D
        );

        return builder;
    }

    private static class FollowPlayerGoal
            extends Goal {

        private final FerrityEntity ferrity;
        private final double speed;

        private Player player;
        private int repathTicks;

        private static final double START_DISTANCE =
                2.0D;

        private static final double STOP_DISTANCE =
                1.5D;

        private static final double SEARCH_DISTANCE =
                32.0D;

        private static final int REPATH_INTERVAL =
                5;

        public FollowPlayerGoal(
                FerrityEntity ferrity,
                double speed
        ) {
            this.ferrity = ferrity;
            this.speed = speed;

            this.setFlags(
                    EnumSet.of(
                            Goal.Flag.MOVE,
                            Goal.Flag.LOOK
                    )
            );
        }

        @Override
        public boolean canUse() {
            if (!ferrity.isFollowingEnabled()) {
                return false;
            }

            this.player =
                    ferrity.level()
                            .getNearestPlayer(
                                    ferrity,
                                    SEARCH_DISTANCE
                            );

            if (this.player == null) {
                return false;
            }

            return ferrity.distanceToSqr(
                    this.player
            ) > START_DISTANCE
                    * START_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            if (!ferrity.isFollowingEnabled()) {
                return false;
            }

            if (this.player == null) {
                return false;
            }

            if (!this.player.isAlive()) {
                return false;
            }

            return ferrity.distanceToSqr(
                    this.player
            ) > STOP_DISTANCE
                    * STOP_DISTANCE;
        }

        @Override
        public void start() {
            this.repathTicks = 0;

            moveTowardPlayer();
        }

        @Override
        public void tick() {
            if (!ferrity.isFollowingEnabled()) {
                ferrity.getNavigation().stop();
                return;
            }

            if (this.player == null) {
                return;
            }

            ferrity.getLookControl()
                    .setLookAt(
                            this.player,
                            30.0F,
                            30.0F
                    );

            double distanceSquared =
                    ferrity.distanceToSqr(
                            this.player
                    );

            if (
                    distanceSquared
                            <= STOP_DISTANCE
                            * STOP_DISTANCE
            ) {
                ferrity.getNavigation().stop();
                return;
            }

            this.repathTicks--;

            if (this.repathTicks <= 0) {
                this.repathTicks =
                        REPATH_INTERVAL;

                moveTowardPlayer();
            }

            if (ferrity.horizontalCollision) {
                ferrity.getJumpControl()
                        .jump();
            }
        }

        @Override
        public void stop() {
            ferrity.getNavigation().stop();

            this.player = null;
            this.repathTicks = 0;
        }

        private void moveTowardPlayer() {
            if (
                    this.player == null
                            || !ferrity
                            .isFollowingEnabled()
            ) {
                return;
            }

            ferrity.getNavigation()
                    .moveTo(
                            this.player,
                            this.speed
                    );
        }
    }
}