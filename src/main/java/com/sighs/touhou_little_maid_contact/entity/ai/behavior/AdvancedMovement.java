package com.sighs.touhou_little_maid_contact.entity.ai.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.util.HazardUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

public final class AdvancedMovement {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void logDebug(String format, Object... params) {
        if (!FMLLoader.isProduction()) {
            LOGGER.debug(format, params);
        }
    }

    private AdvancedMovement() {
    }

    /**
     * 强制移动到目标位置（当常规导航失败时使用）
     */
    public static boolean forceMoveTo(EntityMaid maid, BlockPos target, double speed) {
        try {
            logDebug("[MaidMail][AdvancedMovement] Force move start maidId={} target={} speed={}", 
                    maid.getId(), target, speed);
            
            var nav = maid.getNavigation();
            nav.stop();

            Vec3 maidPos = maid.position();
            Vec3 targetPos = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            Vec3 direction = targetPos.subtract(maidPos);
            double distance = direction.length();

            if (distance < 0.5) {
                if (isSafeToTeleport(maid, targetPos)) {
                    logDebug("[MaidMail][AdvancedMovement] Teleporting maidId={} to target={}", 
                            maid.getId(), target);
                    maid.setPos(targetPos.x, targetPos.y, targetPos.z);
                    return true;
                }
            }

            direction = direction.normalize();

            BlockPos nextPos = maid.blockPosition().offset(
                    (int) Math.signum(direction.x), 0, (int) Math.signum(direction.z));

            ServerLevel level = (ServerLevel) maid.level();
            PathType pathType = HazardUtil.getBlockPathType(level, nextPos);

            if (HazardUtil.isPathTypeDangerous(pathType) || !HazardUtil.isSafeForStanding(level, nextPos, maid)) {
                logDebug("[MaidMail][AdvancedMovement] Unsafe path detected maidId={} nextPos={} pathType={}", 
                        maid.getId(), nextPos, pathType);
                return handleUnsafePath(maid, direction, target, speed);
            } else {
                logDebug("[MaidMail][AdvancedMovement] Normal movement maidId={} pathType={}", 
                        maid.getId(), pathType);
                return handleNormalMovement(maid, direction, speed);
            }

        } catch (Exception e) {
            LOGGER.error("[MaidMail] Force move error maidId={}: {}", maid.getId(), e.getMessage());
            return false;
        }
    }

    private static boolean isSafeToTeleport(EntityMaid maid, Vec3 targetPos) {
        ServerLevel level = (ServerLevel) maid.level();
        BlockPos pos = BlockPos.containing(targetPos);
        return HazardUtil.isSafeForStanding(level, pos, maid);
    }

    /**
     * 处理不安全路径的移动
     */
    private static boolean handleUnsafePath(EntityMaid maid, Vec3 direction, BlockPos target, double speed) {
        ServerLevel level = (ServerLevel) maid.level();

        // 尝试寻找安全的替代路径
        BlockPos safePos = HazardUtil.findSafestNearbyPosition(level, maid.blockPosition(), 3);
        if (safePos != null) {
            Vec3 safeDirection = new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5)
                    .subtract(maid.position()).normalize();
            return handleNormalMovement(maid, safeDirection, speed * 0.8); // 稍微慢一点
        }

        if (direction.y > 0.1) {
            maid.setDeltaMovement(0, Math.min(0.3, maid.maxUpStep() + 0.1), 0);
            return true;
        } else if (direction.y < -0.1) {
            BlockPos below = maid.blockPosition().below();
            if (HazardUtil.isSafeForStanding(level, below, maid)) {
                maid.setDeltaMovement(0, -0.1, 0);
                return true;
            }
        } else {
            double jumpHeight = Math.min(0.4, maid.maxUpStep() + 0.2);
            maid.setDeltaMovement(direction.x * 0.1, jumpHeight, direction.z * 0.1);
            return true;
        }

        return false;
    }

    private static boolean handleNormalMovement(EntityMaid maid, Vec3 direction, double speed) {
        double moveSpeed = Math.min(speed * 0.15, 0.25);

        ServerLevel level = (ServerLevel) maid.level();
        BlockPos currentPos = maid.blockPosition();
        PathType currentPathType = HazardUtil.getBlockPathType(level, currentPos);

        double speedMultiplier = getSpeedMultiplierForPathType(currentPathType);
        moveSpeed *= speedMultiplier;

        maid.setDeltaMovement(
                direction.x * moveSpeed,
                direction.y * moveSpeed * 0.6,
                direction.z * moveSpeed
        );

        return true;
    }

    private static double getSpeedMultiplierForPathType(PathType pathType) {
        return switch (pathType) {
            case WATER -> 0.6;
            case STICKY_HONEY -> 0.3;
            case WALKABLE, OPEN -> 1.0;
            case LEAVES -> 0.8;
            default -> 0.9;
        };
    }
}