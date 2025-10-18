package com.sighs.touhou_little_maid_contact.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MailboxSafetyEvaluator {
    private MailboxSafetyEvaluator() {
    }

    public record MailboxInfo(BlockPos pos, int safetyScore, double distance, boolean accessible) {
        public boolean isUsable() {
            return safetyScore >= 70 && accessible;
        }
    }

    public static Optional<MailboxInfo> getBestUsableMailbox(ServerLevel level, BlockPos center, int searchRadius) {
        List<MailboxInfo> list = evaluateMailboxes(level, center, searchRadius);
        return list.stream().filter(MailboxInfo::isUsable).findFirst();
    }

    public static List<MailboxInfo> evaluateMailboxes(ServerLevel level, BlockPos center, int searchRadius) {
        List<MailboxInfo> mailboxes = new ArrayList<>();
        int r = Math.max(1, Math.min(searchRadius, 16));

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -1, -r),
                center.offset(r, 1, r))) {
            if (isMailbox(level, pos)) {
                BlockPos immutable = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
                int safety = calculateMailboxSafety(level, immutable);
                double dist = center.distSqr(immutable);
                boolean accessible = PathSafetyPlanner.isPositionAccessible(level, center, immutable);

                mailboxes.add(new MailboxInfo(immutable, safety, dist, accessible));
            }
        }

        mailboxes.sort(Comparator
                .comparingInt((MailboxInfo m) -> -m.safetyScore)
                .thenComparingDouble(m -> m.distance));
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

        boolean mailboxSafe = !HazardUtil.isPosHazardous(level, mailboxPos);
        if (!mailboxSafe) {
            return 0;
        }

        boolean hasAccess = hasAccessSpace(level, mailboxPos);
        if (!hasAccess) {
            return 0;
        }

        if (HazardUtil.isCompletelyTrapped(level, mailboxPos)) {
            return 0;
        }

        // 基础分数
        int score = 40;

        // 检查紧邻位置的安全性
        int safeAccessPoints = countSafeAccessPoints(level, mailboxPos);
        int accessBonus = safeAccessPoints * 15; // 提高安全接近点的权重
        score += accessBonus;

        // 检查周围区域的整体安全性
        int areaHazardScore = HazardUtil.calculateHazardScore(level, mailboxPos, 2);
        int areaSafetyBonus = Math.max(0, (100 - areaHazardScore) / 5); // 周围越安全，奖励越多
        score += areaSafetyBonus;

        // 严重惩罚：如果邮筒正上方有危险
        boolean aboveHazard = HazardUtil.isPosHazardous(level, mailboxPos.above());
        if (aboveHazard) {
            score -= 15; // 增加惩罚
        }

        // 检查下方是否安全（防止掉落）
        boolean belowHazard = HazardUtil.isPosHazardous(level, mailboxPos.below());
        if (belowHazard) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private static boolean hasAccessSpace(ServerLevel level, BlockPos mailboxPos) {
        BlockPos[] dirs = {mailboxPos.north(), mailboxPos.south(), mailboxPos.east(), mailboxPos.west()};
        for (BlockPos p : dirs) {
            if (HazardUtil.isSafeStanding(level, p)) {
                return true;
            }
        }
        return false;
    }

    private static int countSafeAccessPoints(ServerLevel level, BlockPos mailboxPos) {
        BlockPos[] dirs = {mailboxPos.north(), mailboxPos.south(), mailboxPos.east(), mailboxPos.west()};
        int count = 0;
        for (BlockPos p : dirs) {
            if (HazardUtil.isSafeStanding(level, p)) {
                count++;
            }
        }
        return count;
    }
}