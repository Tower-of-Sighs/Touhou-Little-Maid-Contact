package com.sighs.touhou_little_maid_contact.util;

import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MailboxSafetyEvaluator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void logDebug(String format, Object... params) {
        if (!FMLLoader.isProduction()) {
            LOGGER.debug(format, params);
        }
    }

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
        logDebug("[MaidMail][MailboxEvaluator] Searching for mailbox center={} radius={}", center, searchRadius);
        List<MailboxInfo> list = evaluateMailboxes(level, center, searchRadius);
        logDebug("[MaidMail][MailboxEvaluator] Found {} mailboxes", list.size());
        
        Optional<MailboxInfo> result = list.stream()
                .filter(MailboxInfo::isUsable)
                .max(Comparator.comparingInt(MailboxInfo::safetyScore)
                        .thenComparing(m -> -m.distance)); // 安全度优先，然后距离近的优先
        
        if (result.isPresent()) {
            logDebug("[MaidMail][MailboxEvaluator] Best mailbox found pos={} safety={} usable={}", 
                    result.get().pos(), result.get().safetyScore(), result.get().isUsable());
        } else {
            logDebug("[MaidMail][MailboxEvaluator] No usable mailbox found");
        }
        
        return result;
    }

    public static List<MailboxInfo> evaluateMailboxes(ServerLevel level, BlockPos center, int searchRadius) {
        List<MailboxInfo> mailboxes = new ArrayList<>();
        int r = Math.max(1, Math.min(searchRadius, Config.MAILBOX_SEARCH_RADIUS.get()));

        logDebug("[MaidMail][MailboxEvaluator] Evaluating mailboxes in radius {} around {}", r, center);

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -2, -r),
                center.offset(r, 2, r))) {
            if (isMailbox(level, pos)) {
                BlockPos immutable = pos.immutable();
                int safety = calculateMailboxSafety(level, immutable);
                double dist = center.distSqr(immutable);
                boolean accessible = PathSafetyPlanner.isPositionAccessible(level, center, immutable);
                PathType pathType = HazardUtil.getBlockPathType(level, immutable);

                logDebug("[MaidMail][MailboxEvaluator] Found mailbox pos={} safety={} accessible={} pathType={}", 
                        immutable, safety, accessible, pathType);

                mailboxes.add(new MailboxInfo(immutable, safety, dist, accessible, pathType));
            }
        }

        mailboxes.sort(Comparator
                .comparingInt((MailboxInfo m) -> -m.safetyScore)
                .thenComparingDouble(MailboxInfo::distance));
        
        logDebug("[MaidMail][MailboxEvaluator] Evaluated {} mailboxes, sorted by safety", mailboxes.size());
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

    private static int calculateMailboxSafety(ServerLevel level, BlockPos mailboxPos) {
        
        PathType mailboxPathType = HazardUtil.getBlockPathType(level, mailboxPos);
        if (mailboxPathType == PathType.LAVA || mailboxPathType == PathType.DAMAGE_FIRE) {
            return 0;
        }

        if (!hasValidAccessPoints(level, mailboxPos)) {
            return 0;
        }

        if (HazardUtil.isCompletelyTrapped(level, mailboxPos)) {
            return 0;
        }

        int score = 50;

        if (mailboxPathType == PathType.DANGER_FIRE) {
            score -= 10;
        }

        int safeAccessPoints = countSafeAccessPoints(level, mailboxPos);
        score += safeAccessPoints * 12;
        int areaHazardScore = HazardUtil.calculateHazardScore(level, mailboxPos, 2);
        int areaSafetyBonus = Math.max(0, (100 - areaHazardScore) / 3);
        score += areaSafetyBonus;

        int goodStandingSpots = countGoodStandingSpots(level, mailboxPos, 2);
        score += goodStandingSpots * 3;

        return Math.max(0, score);
    }

    /**
     * 检查邮筒是否有有效的接近点（处理双层方块特殊情况）
     */
    private static boolean hasValidAccessPoints(ServerLevel level, BlockPos mailboxPos) {
        
        BlockPos[] directions = {mailboxPos.north(), mailboxPos.south(), mailboxPos.east(), mailboxPos.west()};
        for (int i = 0; i < directions.length; i++) {
            BlockPos pos = directions[i];
            boolean safe = HazardUtil.isSafeStanding(level, pos);
            logDebug("[MaidMail][MailboxAccess] Direction {} pos={} safe={}", 
                    new String[]{"north", "south", "east", "west"}[i], pos, safe);
            if (safe) {
                logDebug("[MaidMail][MailboxAccess] Found valid access point at {}", pos);
                return true;
            }
        }

        boolean mailboxSafe = HazardUtil.isSafeStanding(level, mailboxPos);
        logDebug("[MaidMail][MailboxAccess] Mailbox position itself safe: {}", mailboxSafe);
        if (mailboxSafe) {
            logDebug("[MaidMail][MailboxAccess] Mailbox position itself is safe");
            return true;
        }

        var state = level.getBlockState(mailboxPos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            var half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            logDebug("[MaidMail][MailboxAccess] Double block half: {}", half);
            if (half == DoubleBlockHalf.LOWER) {
                BlockPos upperPos = mailboxPos.above();
                BlockPos[] upperDirections = {upperPos.north(), upperPos.south(), upperPos.east(), upperPos.west()};
                for (int i = 0; i < upperDirections.length; i++) {
                    BlockPos pos = upperDirections[i];
                    boolean safe = HazardUtil.isSafeStanding(level, pos);
                    logDebug("[MaidMail][MailboxAccess] Upper direction {} pos={} safe={}", 
                            new String[]{"north", "south", "east", "west"}[i], pos, safe);
                    if (safe) {
                        logDebug("[MaidMail][MailboxAccess] Found valid upper access point at {}", pos);
                        return true;
                    }
                }
            }
        }

        logDebug("[MaidMail][MailboxAccess] No valid access points found");
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