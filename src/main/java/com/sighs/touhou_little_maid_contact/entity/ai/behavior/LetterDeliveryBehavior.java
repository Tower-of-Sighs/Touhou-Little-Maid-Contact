package com.sighs.touhou_little_maid_contact.entity.ai.behavior;

import com.flechazo.contact.common.item.IPackageItem;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.util.MailboxSafetyEvaluator;
import com.sighs.touhou_little_maid_contact.util.PathSafetyPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import org.slf4j.Logger;

import java.util.Optional;

public class LetterDeliveryBehavior implements BehaviorControl<EntityMaid> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double WALK_SPEED = 0.5;
    private static final double RUN_SPEED = 0.7;
    private static final int HANDOVER_RADIUS = 3;
    private static final int REPLAN_COOLDOWN = 40;
    private static final int STAY_MIN = 40;
    private static final int STAY_MAX = 80;

    private Behavior.Status status = Behavior.Status.STOPPED;

    private enum TargetType {MAILBOX, OWNER}

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
                LOGGER.warn("[MaidMail][Delivery] deliver failed near={} maidId={}", targetType, maid.getId());
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
            // 跟随模式：仅给玩家
            if (owner != null) {
                targetType = TargetType.OWNER;
                targetPos = owner.blockPosition();
            } else {
                targetType = null;
                targetPos = null;
            }
            return;
        }

        // Home 模式：主人在家范围内时优先邮筒，否则找不到邮筒就搁置
        BlockPos homeCenter = maid.getRestrictCenter();
        int homeRadius = Math.max(4, (int) maid.getRestrictRadius());
        boolean ownerInHome = owner != null && maid.closerThan(owner, homeRadius);

        var allMailboxes = MailboxSafetyEvaluator.evaluateMailboxes(level, homeCenter, Math.min(16, homeRadius));

        Optional<MailboxSafetyEvaluator.MailboxInfo> mailboxOpt =
                MailboxSafetyEvaluator.getBestUsableMailbox(level, homeCenter, Math.min(16, homeRadius));

        if (ownerInHome) {
            if (mailboxOpt.isPresent()) {
                var mb = mailboxOpt.get();
                if (mb.isUsable()) {
                    targetType = TargetType.MAILBOX;
                    targetPos = mb.pos();
                    return;
                } else {
                    LOGGER.warn("[MaidMail][Delivery] Best mailbox {} not usable: safety={} accessible={} maidId={}",
                            mb.pos(), mb.safetyScore(), mb.accessible(), maid.getId());
                }
            } else {
                LOGGER.warn("[MaidMail][Delivery] No usable mailbox found from {} candidates maidId={}",
                        allMailboxes.size(), maid.getId());
            }
            // 邮筒不存在或不安全则交给主人
            targetType = TargetType.OWNER;
            targetPos = owner.blockPosition();
        } else {
            // 主人不在家范围内：若有邮筒则去邮筒，否则暂缓
            if (mailboxOpt.isPresent() && mailboxOpt.get().isUsable()) {
                targetType = TargetType.MAILBOX;
                targetPos = mailboxOpt.get().pos();
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
                    return true;
                }
            }

            // 极近距离：直接移动
            if (dist <= 9) {
                boolean result = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
                if (!result) {
                    return AdvancedMovement.forceMoveTo(maid, target, speed);
                }
                return true;
            }

            // 中等距离：尝试多种导航方式
            if (dist <= 64) {
                boolean directResult = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
                if (directResult) {
                    return true;
                }

                BlockPos[] nearTargets = {
                        target.north(), target.south(), target.east(), target.west(),
                        target.above(), target.below()
                };

                for (BlockPos nearTarget : nearTargets) {
                    boolean nearResult = nav.moveTo(nearTarget.getX() + 0.5, nearTarget.getY(), nearTarget.getZ() + 0.5, speed);
                    if (nearResult) {
                        return true;
                    }
                }

                return AdvancedMovement.forceMoveTo(maid, target, speed);
            }

            // 远距离：路径规划
            var path = PathSafetyPlanner.planSimpleAvoidancePath(maid, target);
            if (path != null) {
                return nav.moveTo(path, speed);
            }

            LOGGER.warn("[MaidMail][Delivery] All navigation methods failed, trying final direct attempt maidId={}", maid.getId());
            boolean finalResult = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
            if (!finalResult) {
                return AdvancedMovement.forceMoveTo(maid, target, speed);
            }
            return true;

        } catch (Exception e) {
            LOGGER.error("[MaidMail][Delivery] move error maidId={}: {}", maid.getId(), e.getMessage());
            return false;
        }
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