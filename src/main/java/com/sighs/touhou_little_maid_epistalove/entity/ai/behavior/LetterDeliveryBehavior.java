package com.sighs.touhou_little_maid_epistalove.entity.ai.behavior;

import com.flechazo.contact.common.item.IPackageItem;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.config.ModConfig;
import com.sighs.touhou_little_maid_epistalove.util.HazardUtil;
import com.sighs.touhou_little_maid_epistalove.util.MailboxSafetyEvaluator;
import com.sighs.touhou_little_maid_epistalove.util.PathSafetyPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import org.slf4j.Logger;

public class LetterDeliveryBehavior implements BehaviorControl<EntityMaid> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double WALK_SPEED = 0.5;
    private static final double RUN_SPEED = 0.7;
    private static final int HANDOVER_RADIUS = 2;
    private static final int REPLAN_COOLDOWN = 40;
    private static final int STAY_MIN = 40;
    private static final int STAY_MAX = 80;

    private Behavior.Status status = Behavior.Status.STOPPED;

    private enum TargetType {
        MAILBOX, OWNER
    }

    private TargetType targetType = null;
    private BlockPos targetPos = null;

    private int replanCooldown = 0;
    private int stayTick = 0;

    private BlockPos lastPos = null;
    private int noMovementTicks = 0;

    @Override
    public Behavior.Status getStatus() {
        return status;
    }

    @Override
    public boolean tryStart(ServerLevel level, EntityMaid maid, long gameTime) {
        LetterGenerationService.processMaidLetterGeneration(maid);
        if (!hasLetter(maid)) {
            return false;
        }
        status = Behavior.Status.RUNNING;
        replanCooldown = 0;
        lastPos = maid.blockPosition();
        noMovementTicks = 0;
        targetType = null;
        targetPos = null;
        return true;
    }

    @Override
    public void tickOrStop(ServerLevel level, EntityMaid maid, long gameTime) {
        LetterGenerationService.processMaidLetterGeneration(maid);
        if (!hasLetter(maid)) {
            doStop(level, maid, gameTime);
            return;
        }

        if (stayTick > 0) {
            tickStaying(level, maid);
            return;
        }

        if (replanCooldown > 0) {
            replanCooldown--;
        }

        if (targetPos == null || replanCooldown <= 0) {
            planTarget(level, maid);
            replanCooldown = REPLAN_COOLDOWN;
        }

        if (targetPos == null || targetType == null) {
            return;
        }

        // 到达判定
        double distSqr = maid.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        if (distSqr <= (HANDOVER_RADIUS * HANDOVER_RADIUS)) {
            LetterDeliveryService.tryDeliverLetter(maid);
            if (!hasLetter(maid)) {
                stayTick = STAY_MIN + maid.getRandom().nextInt(STAY_MAX - STAY_MIN);
            } else {
                replanCooldown = REPLAN_COOLDOWN;
            }
            return;
        }

        double dist = Math.sqrt(distSqr);
        double speed = dist > 12 ? RUN_SPEED : WALK_SPEED;

        boolean moveOk = moveToTarget(maid, targetPos, speed);

        handleMovementTracking(maid, moveOk, level);
    }

    @Override
    public void doStop(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getNavigation().stop();
        status = Behavior.Status.STOPPED;
        targetType = null;
        targetPos = null;
        replanCooldown = 0;
        stayTick = 0;
        lastPos = null;
        noMovementTicks = 0;
    }

    @Override
    public String debugString() {
        return "tlm_contact:letter_delivery";
    }

    private boolean hasLetter(EntityMaid maid) {
        return ItemsUtil.isStackIn(maid, stack -> {
            if (!(stack.getItem() instanceof IPackageItem)) return false;
            var tag = stack.getTag();
            return tag != null && tag.getBoolean("MaidMail");
        });
    }

    private void tickStaying(ServerLevel level, EntityMaid maid) {
        if (--stayTick <= 0) {
            doStop(level, maid, level.getGameTime());
        }
    }

    private void planTarget(ServerLevel level, EntityMaid maid) {
        var nav = maid.getNavigation();
        nav.stop();

        boolean homeMode = maid.isHomeModeEnable();
        ServerPlayer owner = (ServerPlayer) maid.getOwner();

        if (!homeMode) {
            // 跟随模式：优先直接交给玩家
            if (owner != null) {
                targetType = TargetType.OWNER;
                BlockPos approach = PathSafetyPlanner.findBestApproachPosition(level, owner.blockPosition(), maid);
                targetPos = approach != null ? approach : owner.blockPosition();
            } else {
                targetType = null;
                targetPos = null;
                LOGGER.warn("[MaidMail][Delivery] Target suspended - no owner in follow mode maidId={}", maid.getId());
            }
            return;
        }

        BlockPos homeCenter = maid.getRestrictCenter();
        int homeRadius = Math.max(4, (int) maid.getRestrictRadius());
        boolean ownerInHome = owner != null && maid.closerThan(owner, homeRadius);

        var bestMailboxOpt = MailboxSafetyEvaluator.getBestUsableMailbox(level, homeCenter, Math.min(ModConfig.get().mailDelivery.mailboxSearchRadius, homeRadius));

        if (ownerInHome) {
            if (bestMailboxOpt.isPresent()) {
                var mailbox = bestMailboxOpt.get();
                if (mailbox.isHighQuality() || mailbox.safetyScore() >= 60) {
                    targetType = TargetType.MAILBOX;
                    targetPos = mailbox.pos();
                    return;
                }
            }

            targetType = TargetType.OWNER;
            BlockPos approach = PathSafetyPlanner.findBestApproachPosition(level, owner.blockPosition(), maid);
            targetPos = approach != null ? approach : owner.blockPosition();
        } else {
            if (bestMailboxOpt.isPresent() && bestMailboxOpt.get().isUsable()) {
                targetType = TargetType.MAILBOX;
                targetPos = bestMailboxOpt.get().pos();
            } else {
                targetType = null;
                targetPos = null;
            }
        }
    }

    private boolean moveToTarget(EntityMaid maid, BlockPos target, double speed) {
        try {
            var nav = maid.getNavigation();
            double dist = maid.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

            if (nav.isInProgress()) {
                var currentPath = nav.getPath();
                if (currentPath != null && !currentPath.isDone()) {
                    ServerLevel level = (ServerLevel) maid.level();
                    if (PathSafetyPlanner.isPositionAccessible(level, maid.blockPosition(), target)) {
                        return true;
                    } else {
                        nav.stop();
                    }
                }
            }

            if (dist <= 9) {
                return handleCloseRangeNavigation(maid, target, speed);
            } else if (dist <= 64) {
                return handleMediumRangeNavigation(maid, target, speed);
            } else {
                return handleLongRangeNavigation(maid, target, speed);
            }

        } catch (Exception e) {
            LOGGER.error("[MaidMail][Delivery] move error maidId={}: {}", maid.getId(), e.getMessage());
            return false;
        }
    }

    private boolean handleCloseRangeNavigation(EntityMaid maid, BlockPos target, double speed) {
        var nav = maid.getNavigation();

        boolean result = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
        if (!result) {
            ServerLevel level = (ServerLevel) maid.level();
            BlockPos[] nearTargets = {
                    target.north(), target.south(), target.east(), target.west(),
                    target.above(), target.below()
            };
            for (BlockPos nearTarget : nearTargets) {
                if (HazardUtil.isSafeForStanding(level, nearTarget, maid)) {
                    boolean nearResult = nav.moveTo(nearTarget.getX() + 0.5, nearTarget.getY(), nearTarget.getZ() + 0.5, speed);
                    if (nearResult) {
                        return true;
                    }
                }
            }
            return AdvancedMovement.forceMoveTo(maid, target, speed);
        }
        return true;
    }

    private boolean handleMediumRangeNavigation(EntityMaid maid, BlockPos target, double speed) {
        var nav = maid.getNavigation();

        // 首先尝试直接导航
        boolean directResult = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
        if (directResult) {
            return true;
        }

        // 尝试目标周围的位置
        BlockPos[] nearTargets = {
                target.north(), target.south(), target.east(), target.west(),
                target.above(), target.below()
        };

        ServerLevel level = (ServerLevel) maid.level();
        for (BlockPos nearTarget : nearTargets) {
            if (HazardUtil.isSafeForStanding(level, nearTarget, maid)) {
                boolean nearResult = nav.moveTo(nearTarget.getX() + 0.5, nearTarget.getY(), nearTarget.getZ() + 0.5, speed);
                if (nearResult) {
                    return true;
                }
            }
        }

        // 最后尝试强制移动
        return AdvancedMovement.forceMoveTo(maid, target, speed);
    }

    /**
     * 远距离导航：使用路径规划器生成安全路径，失败时回退到直接导航和强制移动
     */
    private boolean handleLongRangeNavigation(EntityMaid maid, BlockPos target, double speed) {
        var nav = maid.getNavigation();

        var path = PathSafetyPlanner.planSimpleAvoidancePath(maid, target);
        if (path != null) {
            boolean pathResult = nav.moveTo(path, speed);
            if (pathResult) {
                return true;
            }
        }

        boolean finalResult = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
        if (!finalResult) {
            return AdvancedMovement.forceMoveTo(maid, target, speed);
        }
        return true;
    }

    private void handleMovementTracking(EntityMaid maid, boolean moveOk, ServerLevel level) {
        BlockPos cur = maid.blockPosition();
        if (cur.equals(lastPos)) {
            noMovementTicks++;
        } else {
            noMovementTicks = 0;
            lastPos = cur;
        }

        if (!moveOk || noMovementTicks > 80) {
            planTarget(level, maid);
            replanCooldown = REPLAN_COOLDOWN;
            noMovementTicks = 0;
        }

        var nav = maid.getNavigation();
        boolean navInProgress = nav.isInProgress();
        boolean isStuck = false;

        if (!moveOk) {
            isStuck = true;
        } else if (noMovementTicks > 60) {
            isStuck = true;
        } else if (!navInProgress && noMovementTicks > 20) {
            isStuck = true;
        }

        if (isStuck) {
            nav.stop();
            planTarget(level, maid);
            replanCooldown = REPLAN_COOLDOWN;
            noMovementTicks = 0;
        }
    }
}