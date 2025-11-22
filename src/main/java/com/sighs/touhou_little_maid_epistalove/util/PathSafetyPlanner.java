package com.sighs.touhou_little_maid_epistalove.util;

import com.sighs.touhou_little_maid_epistalove.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PathSafetyPlanner {
    private PathSafetyPlanner() {
    }

    /**
     * 规划安全的避险路径
     */
    public static Path planSimpleAvoidancePath(Mob entity, BlockPos target) {
        var nav = entity.getNavigation();
        ServerLevel level = (ServerLevel) entity.level();
        BlockPos start = entity.blockPosition();

        // 首先尝试直接路径
        Path direct = nav.createPath(target, 1);
        if (isPathSafeEnough(level, direct, entity)) {
            return direct;
        }

        // 生成智能绕路候选点
        List<BlockPos> smartCandidates = generateSmartDetourCandidates(level, start, target, entity);
        for (BlockPos candidate : smartCandidates) {
            Path path = nav.createPath(candidate, 1);
            if (isPathSafeEnough(level, path, entity)) {
                return path;
            }
        }

        // 尝试附近的安全点
        List<BlockPos> nearCandidates = generateSafeCandidatesNear(level, target, 3, entity);
        for (BlockPos candidate : nearCandidates) {
            Path path = nav.createPath(candidate, 1);
            if (isPathSafeEnough(level, path, entity)) {
                return path;
            }
        }

        // fallback 扩大搜索范围
        Path fallback = nav.createPath(target, 3);
        if (isPathSafeEnough(level, fallback, entity)) {
            return fallback;
        }

        // 如果都不行，返回直接路径让上层处理
        return direct;
    }

    /**
     * 生成智能绕路候选点
     */
    private static List<BlockPos> generateSmartDetourCandidates(ServerLevel level, BlockPos start, BlockPos target, Mob entity) {
        List<BlockPos> candidates = new ArrayList<>();
        PathData pathData = calculatePathData(start, target);

        if (pathData.distance <= 2) {
            return candidates; // 距离太近，不需要绕路
        }

        List<BlockPos> hazardPoints = findHazardsOnPath(level, start, target, pathData);
        if (hazardPoints.isEmpty()) {
            return candidates;
        }

        for (BlockPos hazard : hazardPoints) {
            generateDetourAroundHazard(level, hazard, pathData, candidates, entity);
        }

        candidates.removeIf(pos -> !HazardUtil.isSafeForStanding(level, pos, entity));
        candidates.sort(Comparator.comparingDouble(a -> a.distSqr(target)));

        return candidates;
    }

    /**
     * 为危险点生成绕路候选
     */
    private static void generateDetourAroundHazard(ServerLevel level, BlockPos hazard, PathData pathData,
                                                   List<BlockPos> candidates, Mob entity) {
        if (Math.abs(pathData.dx) > Math.abs(pathData.dz)) {
            addDetourCandidates(candidates, hazard, 0, 0, new int[]{2, 3, 4, -2, -3, -4});
        } else {
            addDetourCandidates(candidates, hazard, new int[]{2, 3, 4, -2, -3, -4}, 0, 0);
        }

        candidates.add(hazard.above());
        candidates.add(hazard.above(2));
        candidates.add(hazard.below());
    }

    private static void addDetourCandidates(List<BlockPos> candidates, BlockPos base, int dx, int dy, int[] offsets) {
        for (int offset : offsets) {
            candidates.add(base.offset(dx, dy, offset));
        }
    }

    private static void addDetourCandidates(List<BlockPos> candidates, BlockPos base, int[] offsets, int dy, int dz) {
        for (int offset : offsets) {
            candidates.add(base.offset(offset, dy, dz));
        }
    }

    /**
     * 在目标附近生成安全候选点（按优先级：水平方向>对角线>远距离>垂直方向）
     */
    public static List<BlockPos> generateSafeCandidatesNear(ServerLevel level, BlockPos target, int radius, Mob entity) {
        List<BlockPos> candidates = new ArrayList<>();

        addIfSafe(candidates, level, target.north(), entity);
        addIfSafe(candidates, level, target.south(), entity);
        addIfSafe(candidates, level, target.east(), entity);
        addIfSafe(candidates, level, target.west(), entity);

        if (radius > 1) {
            addIfSafe(candidates, level, target.north().east(), entity);
            addIfSafe(candidates, level, target.north().west(), entity);
            addIfSafe(candidates, level, target.south().east(), entity);
            addIfSafe(candidates, level, target.south().west(), entity);

            for (int dist = 2; dist <= radius; dist++) {
                addIfSafe(candidates, level, target.north(dist), entity);
                addIfSafe(candidates, level, target.south(dist), entity);
                addIfSafe(candidates, level, target.east(dist), entity);
                addIfSafe(candidates, level, target.west(dist), entity);
            }

            if (candidates.size() < 2) {
                addIfSafe(candidates, level, target.above(), entity);
                addIfSafe(candidates, level, target.below(), entity);
            }
        }

        candidates.sort(Comparator.comparingDouble(a -> a.distSqr(target)));
        return candidates;
    }

    public static BlockPos findBestApproachPosition(ServerLevel level, BlockPos target, Mob entity) {
        List<BlockPos> candidates = generateSafeCandidatesNear(level, target, 3, entity);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static void addIfSafe(List<BlockPos> candidates, ServerLevel level, BlockPos pos, Mob entity) {
        if (HazardUtil.isSafeForStanding(level, pos, entity)) {
            candidates.add(pos);
        }
    }

    private static List<BlockPos> findHazardsOnPath(ServerLevel level, BlockPos start, BlockPos target, PathData pathData) {
        List<BlockPos> hazards = new ArrayList<>();

        if (pathData.distance == 0) return hazards;

        for (int i = 1; i < pathData.distance; i++) {
            int x = start.getX() + (pathData.dx * i) / pathData.distance;
            int z = start.getZ() + (pathData.dz * i) / pathData.distance;
            BlockPos checkPos = new BlockPos(x, start.getY(), z);

            PathType pathType = HazardUtil.getBlockPathType(level, checkPos);
            if (HazardUtil.isPathTypeDangerous(pathType)) {
                hazards.add(checkPos);
            }
        }

        return hazards;
    }

    private static PathData calculatePathData(BlockPos start, BlockPos target) {
        int dx = target.getX() - start.getX();
        int dz = target.getZ() - start.getZ();
        int distance = Math.max(Math.abs(dx), Math.abs(dz));
        return new PathData(dx, dz, distance);
    }

    private record PathData(int dx, int dz, int distance) {
    }

    /**
     * 检查位置是否可达
     */
    public static boolean isPositionAccessible(ServerLevel level, BlockPos from, BlockPos to) {
        double distance = from.distSqr(to);
        if (distance > 1024) return false; // 距离太远

        // 检查目标位置本身
        if (HazardUtil.isSafeStanding(level, to)) {
            return true;
        }

        // 检查周围位置
        BlockPos[] around = {to.north(), to.south(), to.east(), to.west(), to.above(), to.below()};
        for (BlockPos pos : around) {
            if (HazardUtil.isSafeStanding(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查路径是否足够安全
     */
    private static boolean isPathSafeEnough(ServerLevel level, Path path, Mob entity) {
        if (path == null || path.getNodeCount() == 0) return false;

        int nodes = path.getNodeCount();
        int startIdx = Math.max(0, path.getNextNodeIndex());

        int consecutiveSafe = 0;
        int maxConsecutiveSafe = 0;
        int totalSafe = 0;
        int totalChecked = 0;
        int consecutiveDangerous = 0;
        int maxConsecutiveDangerous = 0;

        for (int i = startIdx; i < nodes; i++) {
            var node = path.getNode(i);
            BlockPos pos = new BlockPos(node.x, node.y, node.z);
            totalChecked++;

            PathType pathType = HazardUtil.getBlockPathType(level, pos);
            boolean isDangerous = HazardUtil.isPathTypeDangerous(pathType);

            if (entity != null) {
                float malus = entity.getPathfindingMalus(pathType);
                if (malus < 0.0F) {
                    isDangerous = true;
                }
            }

            if (!isDangerous) {
                totalSafe++;
                consecutiveSafe++;
                maxConsecutiveSafe = Math.max(maxConsecutiveSafe, consecutiveSafe);
                consecutiveDangerous = 0;
            } else {
                consecutiveSafe = 0;
                consecutiveDangerous++;
                maxConsecutiveDangerous = Math.max(maxConsecutiveDangerous, consecutiveDangerous);
            }
        }

        if (totalChecked == 0) return false;

        int safetyPercentage = (totalSafe * 100) / totalChecked;

        if (maxConsecutiveDangerous > Config.MAX_CONSECUTIVE_DANGEROUS.get()) {
            return false;
        }

        if (totalChecked <= 3) {
            return safetyPercentage >= 90;
        }

        if (totalChecked <= 8) {
            return safetyPercentage >= 75;
        }

        boolean hasGoodSafeSegment = maxConsecutiveSafe >= Math.max(3, totalChecked / 5);
        return safetyPercentage >= Config.PATH_SAFETY_PERCENTAGE.get() && hasGoodSafeSegment;
    }
}