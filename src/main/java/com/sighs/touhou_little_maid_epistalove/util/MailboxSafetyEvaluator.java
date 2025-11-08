package com.sighs.touhou_little_maid_epistalove.util;

import com.sighs.touhou_little_maid_epistalove.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.PathType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MailboxSafetyEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxSafetyEvaluator.class);
    private MailboxSafetyEvaluator() {
    }

    public record MailboxInfo(BlockPos pos, int safetyScore, double distance, boolean accessible,
                              PathType pathType) {
        public boolean isUsable() {
            return safetyScore >= Config.MAILBOX_MIN_SAFETY_SCORE.get() && accessible && !HazardUtil.isPathTypeDangerous(pathType);
        }

        public boolean isHighQuality() {
            return safetyScore >= Config.HIGH_QUALITY_THRESHOLD.get() && accessible && pathType == PathType.WALKABLE;
        }
    }

    public static Optional<MailboxInfo> getBestUsableMailbox(ServerLevel level, BlockPos center, int searchRadius) {
        List<MailboxInfo> list = evaluateMailboxes(level, center, searchRadius);
        return list.stream()
                .filter(MailboxInfo::isUsable)
                .max(Comparator.comparingInt(MailboxInfo::safetyScore)
                        .thenComparing(m -> -m.distance)); // 安全度优先，然后距离近的优先
    }

    public static List<MailboxInfo> evaluateMailboxes(ServerLevel level, BlockPos center, int searchRadius) {
        List<MailboxInfo> mailboxes = new ArrayList<>();
        int r = Math.max(1, Math.min(searchRadius, Config.MAILBOX_SEARCH_RADIUS.get()));

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -2, -r),
                center.offset(r, 2, r))) {
            if (isMailbox(level, pos)) {
                BlockPos immutable = pos.immutable();
                int safety = calculateMailboxSafety(level, immutable);
                double dist = center.distSqr(immutable);
                boolean accessible = PathSafetyPlanner.isPositionAccessible(level, center, immutable);
                PathType pathType = HazardUtil.getBlockPathType(level, immutable);

                mailboxes.add(new MailboxInfo(immutable, safety, dist, accessible, pathType));
            }
        }

        mailboxes.sort(Comparator
                .comparingInt((MailboxInfo m) -> -m.safetyScore)
                .thenComparingDouble(MailboxInfo::distance));
        return mailboxes;
    }

    private static boolean isMailbox(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"contact".equals(id.getNamespace())) return false;

        String path = id.getPath();
        boolean isPostbox = "red_postbox".equals(path) || "green_postbox".equals(path);
        if (!isPostbox) return false;

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    /**
     * 计算邮筒的安全评分
     */
    private static int calculateMailboxSafety(ServerLevel level, BlockPos mailboxPos) {
        PathType mailboxPathType = HazardUtil.getBlockPathType(level, mailboxPos);

        // 邮筒位置直接危险
        if (mailboxPathType == PathType.LAVA || mailboxPathType == PathType.DAMAGE_FIRE) {
            return 0;
        }

        if (!hasValidAccessPoints(level, mailboxPos) || HazardUtil.isCompletelyTrapped(level, mailboxPos)) {
            return 0;
        }

        int score = 50;

        // 周围有火源危险则扣分
        if (mailboxPathType == PathType.DANGER_FIRE) {
            score -= 10;
        }

        // 安全接近点加分
        int safeAccessPoints = countSafeAccessPoints(level, mailboxPos);
        score += safeAccessPoints * 12;

        // 区域危险度换算成安全奖励
        int areaHazardScore = HazardUtil.calculateHazardScore(level, mailboxPos, 2);
        int areaSafetyBonus = Math.max(0, (100 - areaHazardScore) / 3);
        score += areaSafetyBonus;

        // 上方/下方危险扣分
        PathType aboveType = HazardUtil.getBlockPathType(level, mailboxPos.above());
        if (HazardUtil.isPathTypeDangerous(aboveType)) {
            score -= 20;
        }

        PathType belowType = HazardUtil.getBlockPathType(level, mailboxPos.below());
        if (HazardUtil.isPathTypeDangerous(belowType)) {
            score -= 15;
        }

        // 周围良好站立点适度加分
        int goodStandingSpots = countGoodStandingSpots(level, mailboxPos, 2);
        score += goodStandingSpots * 3;

        return Math.max(0, Math.min(100, score));
    }

    private static boolean hasValidAccessPoints(ServerLevel level, BlockPos mailboxPos) {
        // 检查四个侧面和邮筒自身是否存在至少一个可安全站立点
        BlockPos[] checkPositions = {
                mailboxPos.north(), mailboxPos.south(),
                mailboxPos.east(), mailboxPos.west(),
                mailboxPos // 邮筒自身也作为候选
        };
        for (BlockPos pos : checkPositions) {
            if (HazardUtil.isSafeStanding(level, pos)) {
                return true;
            }
        }

        // 双层邮筒：检查上半部分周边
        var state = level.getBlockState(mailboxPos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            BlockPos upperPos = mailboxPos.above();
            BlockPos[] upperDirections = {upperPos.north(), upperPos.south(), upperPos.east(), upperPos.west()};
            for (BlockPos pos : upperDirections) {
                if (HazardUtil.isSafeStanding(level, pos)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int countSafeAccessPoints(ServerLevel level, BlockPos mailboxPos) {
        BlockPos[] directions = {mailboxPos.north(), mailboxPos.south(), mailboxPos.east(), mailboxPos.west()};
        int count = 0;
        for (BlockPos pos : directions) {
            if (HazardUtil.isSafeStanding(level, pos)) {
                count++;
            }
        }

        if (HazardUtil.isSafeStanding(level, mailboxPos)) {
            count++;
        }

        var state = level.getBlockState(mailboxPos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            var half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.LOWER) {
                BlockPos upperPos = mailboxPos.above();
                BlockPos[] upperDirections = {upperPos.north(), upperPos.south(), upperPos.east(), upperPos.west()};
                for (BlockPos pos : upperDirections) {
                    if (HazardUtil.isSafeStanding(level, pos)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static int countGoodStandingSpots(ServerLevel level, BlockPos center, int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos pos = center.offset(dx, 0, dz);
                if (HazardUtil.isSafeStanding(level, pos)) {
                    PathType pathType = HazardUtil.getBlockPathType(level, pos);
                    if (pathType == PathType.WALKABLE || pathType == PathType.OPEN) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}