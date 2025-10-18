package com.sighs.touhou_little_maid_contact.entity.ai;

import com.flechazo.contact.common.item.IPackageItem;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.MaidLetterRuntime;
import com.sighs.touhou_little_maid_contact.util.HazardUtil;
import com.sighs.touhou_little_maid_contact.util.MailboxSafetyEvaluator;
import com.sighs.touhou_little_maid_contact.util.PathSafetyPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Optional;

public class SmartDeliveryBehavior implements BehaviorControl<EntityMaid> {
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
        MaidLetterRuntime.onMaidTick(maid);
        if (!hasLetter(maid)) {
            return false;
        }
        status = Behavior.Status.RUNNING;
        replanCooldown = 0;
        lastPos = maid.blockPosition();
        noMovementTicks = 0;
        targetType = null;
        targetPos = null;
        LOGGER.debug("[MaidMail][Simple] start behavior maidId={}", maid.getId());
        return true;
    }

    @Override
    public void tickOrStop(ServerLevel level, EntityMaid maid, long gameTime) {
        MaidLetterRuntime.onMaidTick(maid);
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
            // 暂缓
            return;
        }

        // 到达判定
        double distSqr = maid.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        if (distSqr <= (HANDOVER_RADIUS * HANDOVER_RADIUS)) {
            MaidLetterRuntime.trySendOrGive(maid);
            if (!hasLetter(maid)) {
                stayTick = STAY_MIN + maid.getRandom().nextInt(STAY_MAX - STAY_MIN);
                LOGGER.info("[MaidMail][Simple] delivered, stay={} maidId={}", stayTick, maid.getId());
            } else {
                LOGGER.warn("[MaidMail][Simple] deliver failed near={} maidId={}", targetType, maid.getId());
                replanCooldown = REPLAN_COOLDOWN;
            }
            return;
        }

        double dist = Math.sqrt(distSqr);
        double speed = dist > 12 ? RUN_SPEED : WALK_SPEED;

        boolean moveOk;
        if (noMovementTicks >= 60 && dist <= 25) {
            // 多次失败且距离较近，使用更激进的移动策略
            LOGGER.warn("[MaidMail][Simple] Multiple navigation failures, using aggressive move maidId={}", maid.getId());
            moveOk = moveAvoidingHazard(maid, targetPos, speed * 1.5);
        } else {
            moveOk = moveAvoidingHazard(maid, targetPos, speed);
        }

        BlockPos cur = maid.blockPosition();
        if (cur.equals(lastPos)) {
            noMovementTicks++;
        } else {
            noMovementTicks = 0;
            lastPos = cur;
        }
        if (!moveOk || noMovementTicks > 80) {
            LOGGER.debug("[MaidMail][Simple] replan due to no movement={} moveOk={} maidId={}", noMovementTicks, moveOk, maid.getId());
            planTarget(level, maid);
            replanCooldown = REPLAN_COOLDOWN;
            noMovementTicks = 0;
        }

        // 多重卡住检测条件
        var nav = maid.getNavigation();
        boolean navInProgress = nav.isInProgress();
        boolean isStuck = false;
        String stuckReason = "";

        if (!moveOk) {
            isStuck = true;
            stuckReason = "moveOk=false";
        } else if (noMovementTicks > 60) { // 3秒没移动
            isStuck = true;
            stuckReason = "noMovement=" + noMovementTicks;
        } else if (!navInProgress && noMovementTicks > 20) { // 导航停止且1秒没移动
            isStuck = true;
            stuckReason = "navStopped+noMovement=" + noMovementTicks;
        }

        if (isStuck) {
            LOGGER.debug("[MaidMail][Simple] replan due to stuck: {} maidId={}", stuckReason, maid.getId());
            nav.stop();
            planTarget(level, maid);
            replanCooldown = REPLAN_COOLDOWN;
            noMovementTicks = 0;
        }
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
        LOGGER.debug("[MaidMail][Simple] stop maidId={}", maid.getId());
    }

    @Override
    public String debugString() {
        return "tlm_contact:simple_delivery";
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
                LOGGER.debug("[MaidMail][Simple] Target set: OWNER (follow mode) pos={} maidId={}", targetPos, maid.getId());
            } else {
                targetType = null;
                targetPos = null;
                LOGGER.warn("[MaidMail][Simple] Target suspended - no owner in follow mode maidId={}", maid.getId());
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
                    LOGGER.info("[MaidMail][Simple] Target set: MAILBOX pos={} safety={} accessible={} maidId={}",
                            targetPos, mb.safetyScore(), mb.accessible(), maid.getId());
                    return;
                } else {
                    LOGGER.warn("[MaidMail][Simple] Best mailbox {} not usable: safety={} accessible={} maidId={}",
                            mb.pos(), mb.safetyScore(), mb.accessible(), maid.getId());
                }
            } else {
                LOGGER.warn("[MaidMail][Simple] No usable mailbox found from {} candidates maidId={}",
                        allMailboxes.size(), maid.getId());
            }
            // 邮筒不存在或不安全则交给主人
            targetType = TargetType.OWNER;
            targetPos = owner.blockPosition();
            LOGGER.info("[MaidMail][Simple] Target set: OWNER (fallback) pos={} maidId={}", targetPos, maid.getId());
        } else {
            // 主人不在家范围内：若有邮筒则去邮筒，否则暂缓
            if (mailboxOpt.isPresent() && mailboxOpt.get().isUsable()) {
                targetType = TargetType.MAILBOX;
                targetPos = mailboxOpt.get().pos();
                LOGGER.info("[MaidMail][Simple] Target set: MAILBOX (owner away) pos={} safety={} maidId={}",
                        targetPos, mailboxOpt.get().safetyScore(), maid.getId());
            } else {
                targetType = null;
                targetPos = null;
            }
        }
    }

    private boolean moveAvoidingHazard(EntityMaid maid, BlockPos target, double speed) {
        try {
            var nav = maid.getNavigation();
            double dist = maid.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

            if (nav.isInProgress()) {
                var currentPath = nav.getPath();
                if (currentPath != null && !currentPath.isDone()) {
                    LOGGER.debug("[MaidMail][Simple] Navigation in progress, continuing maidId={}", maid.getId());
                    return true;
                }
            }

            // 极近距离：直接移动，不使用路径规划
            if (dist <= 9) { // 3格内
                LOGGER.debug("[MaidMail][Simple] Very close distance, direct navigation maidId={}", maid.getId());
                boolean result = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
                if (!result) {
                    // 导航失败，尝试强制移动
                    return forceMoveTo(maid, target, speed);
                }
                LOGGER.debug("[MaidMail][Simple] Direct navigation result: {} maidId={}", result, maid.getId());
                return true;
            }

            // 中等距离：尝试多种导航方式
            if (dist <= 64) { // 8格内
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

                return forceMoveTo(maid, target, speed);
            }

            // 远距离：路径规划
            var path = PathSafetyPlanner.planSimpleAvoidancePath(maid, target);
            if (path != null) {
                return nav.moveTo(path, speed);
            }

            LOGGER.warn("[MaidMail][Simple] All navigation methods failed, trying final direct attempt maidId={}", maid.getId());
            boolean finalResult = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
            if (!finalResult) {
                return forceMoveTo(maid, target, speed);
            }
            return true;

        } catch (Exception e) {
            LOGGER.error("[MaidMail][Simple] move error maidId={}: {}", maid.getId(), e.getMessage());
            return false;
        }
    }

    public boolean forceMoveTo(EntityMaid maid, BlockPos target, double speed) {
        try {
            var nav = maid.getNavigation();

            nav.stop();

            Vec3 maidPos = maid.position();
            Vec3 targetPos = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            Vec3 direction = targetPos.subtract(maidPos);
            double distance = direction.length();

            if (distance < 0.5) {
                maid.setPos(targetPos.x, targetPos.y, targetPos.z);
                LOGGER.debug("[MaidMail] Force teleport to target (very close) maidId={}", maid.getId());
                return true;
            }

            direction = direction.normalize();

            // 检查前方是否安全
            BlockPos nextPos = maid.blockPosition().offset((int) Math.signum(direction.x), 0, (int) Math.signum(direction.z));
            if (!HazardUtil.isSafeStanding((ServerLevel) maid.level(), nextPos)) {
                // 前方不安全，尝试垂直移动
                if (direction.y > 0.1) {
                    // 需要向上移动
                    maid.setDeltaMovement(0, 0.2, 0);
                } else if (direction.y < -0.1) {
                    // 需要向下移动
                    maid.setDeltaMovement(0, -0.1, 0);
                } else {
                    // 水平移动，但前方不安全，尝试跳跃
                    maid.setDeltaMovement(direction.x * 0.1, 0.3, direction.z * 0.1);
                }
            } else {
                // 正常移动
                double moveSpeed = Math.min(speed * 0.15, 0.2); // 限制最大速度
                maid.setDeltaMovement(direction.x * moveSpeed, direction.y * moveSpeed * 0.5, direction.z * moveSpeed);
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("[MaidMail] Force move error maidId={}: {}", maid.getId(), e.getMessage());
            return false;
        }
    }
}