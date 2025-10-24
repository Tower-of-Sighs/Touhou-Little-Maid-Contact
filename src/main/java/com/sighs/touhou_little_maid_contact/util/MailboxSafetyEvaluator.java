package com.sighs.touhou_little_maid_contact.util;

import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MailboxSafetyEvaluator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MailboxSafetyEvaluator() {
    }

    public record MailboxInfo(BlockPos pos, int safetyScore, double distance, boolean accessible,
                              BlockPathTypes pathType) {
        public boolean isUsable() {
            return safetyScore >= Config.MAILBOX_MIN_SAFETY_SCORE.get() && accessible && !HazardUtil.isPathTypeDangerous(pathType);
        }

        public boolean isHighQuality() {
            return safetyScore >= Config.HIGH_QUALITY_THRESHOLD.get() && accessible && pathType == BlockPathTypes.WALKABLE;
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
                BlockPathTypes pathType = HazardUtil.getBlockPathType(level, immutable);

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
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        if (!"contact".equals(id.getNamespace())) return false;

        String path = id.getPath();
        boolean isPostbox = "red_postbox".equals(path) || "green_postbox".equals(path);
        if (!isPostbox) return false;

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    private static int calculateMailboxSafety(ServerLevel level, BlockPos mailboxPos) {
        BlockPathTypes mailboxPathType = HazardUtil.getBlockPathType(level, mailboxPos);
        if (mailboxPathType == BlockPathTypes.LAVA || mailboxPathType == BlockPathTypes.DAMAGE_FIRE) {
            return 0;
        }

        if (!hasValidAccessPoints(level, mailboxPos)) {
            return 0;
        }

        if (HazardUtil.isCompletelyTrapped(level, mailboxPos)) {
            return 0;
        }

        int score = 50;

        if (mailboxPathType == BlockPathTypes.DANGER_FIRE) {
            score -= 10;
        }

        int safeAccessPoints = countSafeAccessPoints(level, mailboxPos);
        score += safeAccessPoints * 12;

        int areaHazardScore = HazardUtil.calculateHazardScore(level, mailboxPos, 2);
        int areaSafetyBonus = Math.max(0, (100 - areaHazardScore) / 3);
        score += areaSafetyBonus;

        BlockPathTypes aboveType = HazardUtil.getBlockPathType(level, mailboxPos.above());
        if (HazardUtil.isPathTypeDangerous(aboveType)) {
            score -= 20;
        }

        BlockPathTypes belowType = HazardUtil.getBlockPathType(level, mailboxPos.below());
        if (HazardUtil.isPathTypeDangerous(belowType)) {
            score -= 15;
        }

        int goodStandingSpots = countGoodStandingSpots(level, mailboxPos, 2);
        score += goodStandingSpots * 3;

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 检查邮筒是否有有效的接近点（处理双层方块特殊情况）
     */
    private static boolean hasValidAccessPoints(ServerLevel level, BlockPos mailboxPos) {
        BlockPos[] directions = {mailboxPos.north(), mailboxPos.south(), mailboxPos.east(), mailboxPos.west()};
        for (BlockPos pos : directions) {
            if (HazardUtil.isSafeStanding(level, pos)) {
                return true;
            }
        }

        if (HazardUtil.isSafeStanding(level, mailboxPos)) {
            return true;
        }

        var state = level.getBlockState(mailboxPos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            var half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.LOWER) {
                BlockPos upperPos = mailboxPos.above();
                BlockPos[] upperDirections = {upperPos.north(), upperPos.south(), upperPos.east(), upperPos.west()};
                for (BlockPos pos : upperDirections) {
                    if (HazardUtil.isSafeStanding(level, pos)) {
                        return true;
                    }
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
                    BlockPathTypes pathType = HazardUtil.getBlockPathType(level, pos);
                    if (pathType == BlockPathTypes.WALKABLE || pathType == BlockPathTypes.OPEN) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}