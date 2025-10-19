package com.sighs.touhou_little_maid_contact.entity.ai.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.util.HazardUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class AdvancedMovement {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AdvancedMovement() {
    }

    public static boolean forceMoveTo(EntityMaid maid, BlockPos target, double speed) {
        try {
            var nav = maid.getNavigation();
            nav.stop();

            Vec3 maidPos = maid.position();
            Vec3 targetPos = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            Vec3 direction = targetPos.subtract(maidPos);
            double distance = direction.length();

            // 极近距离直接传送
            if (distance < 0.5) {
                maid.setPos(targetPos.x, targetPos.y, targetPos.z);
                LOGGER.debug("[MaidMail] Force teleport to target (very close) maidId={}", maid.getId());
                return true;
            }

            direction = direction.normalize();

            // 检查前方是否安全
            BlockPos nextPos = maid.blockPosition().offset(
                    (int) Math.signum(direction.x), 0, (int) Math.signum(direction.z));

            if (!HazardUtil.isSafeStanding((ServerLevel) maid.level(), nextPos)) {
                // 前方不安全，尝试垂直移动或跳跃
                handleUnsafePath(maid, direction);
            } else {
                // 正常移动
                handleNormalMovement(maid, direction, speed);
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("[MaidMail] Force move error maidId={}: {}", maid.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 处理不安全路径的移动
     */
    private static void handleUnsafePath(EntityMaid maid, Vec3 direction) {
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
    }

    private static void handleNormalMovement(EntityMaid maid, Vec3 direction, double speed) {
        double moveSpeed = Math.min(speed * 0.15, 0.2);
        maid.setDeltaMovement(
                direction.x * moveSpeed,
                direction.y * moveSpeed * 0.5,
                direction.z * moveSpeed
        );
    }
}