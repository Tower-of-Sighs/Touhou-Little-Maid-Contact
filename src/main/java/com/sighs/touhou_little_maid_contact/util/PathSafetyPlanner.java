package com.sighs.touhou_little_maid_contact.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PathSafetyPlanner {
    private PathSafetyPlanner() {
    }

    public static Path planSimpleAvoidancePath(Mob entity, BlockPos target) {
        var nav = entity.getNavigation();
        ServerLevel level = (ServerLevel) entity.level();
        BlockPos start = entity.blockPosition();

        Path direct = nav.createPath(target, 1);
        if (isPathSafeEnough(level, direct)) {
            return direct;
        }

        List<BlockPos> smartCandidates = generateSmartDetourCandidates(level, start, target);
        for (BlockPos candidate : smartCandidates) {
            Path path = nav.createPath(candidate, 1);
            if (isPathSafeEnough(level, path)) {
                return path;
            }
        }

        List<BlockPos> nearCandidates = generateNearCandidates(level, target, 2);
        for (BlockPos candidate : nearCandidates) {
            Path path = nav.createPath(candidate, 1);
            if (isPathSafeEnough(level, path)) {
                return path;
            }
        }

        Path fallback = nav.createPath(target, 3);
        if (isPathSafeEnough(level, fallback)) {
            return fallback;
        }

        return direct;
    }

    private static List<BlockPos> generateSmartDetourCandidates(ServerLevel level, BlockPos start, BlockPos target) {
        List<BlockPos> candidates = new ArrayList<>();

        PathData pathData = calculatePathData(start, target);

        if (pathData.distance <= 2) {
            return candidates;
        }

        List<BlockPos> hazardPoints = findHazardsOnPath(level, start, target, pathData);

        if (hazardPoints.isEmpty()) {
            return candidates;
        }

        // 为每个危险点生成绕路候选
        for (BlockPos hazard : hazardPoints) {
            // 生成垂直于主方向的绕路点
            if (Math.abs(pathData.dx) > Math.abs(pathData.dz)) {
                // 主要是水平移动，尝试垂直绕路
                candidates.add(hazard.offset(0, 0, 3));
                candidates.add(hazard.offset(0, 0, -3));
                candidates.add(hazard.offset(0, 0, 5));
                candidates.add(hazard.offset(0, 0, -5));
            } else {
                // 主要是垂直移动，尝试水平绕路
                candidates.add(hazard.offset(3, 0, 0));
                candidates.add(hazard.offset(-3, 0, 0));
                candidates.add(hazard.offset(5, 0, 0));
                candidates.add(hazard.offset(-5, 0, 0));
            }
        }

        // 过滤掉不安全的候选点
        candidates.removeIf(pos -> !HazardUtil.isSafeStanding(level, pos));

        // 按距离目标的远近排序
        candidates.sort(Comparator.comparingDouble(a -> a.distSqr(target)));

        return candidates;
    }

    private static List<BlockPos> findHazardsOnPath(ServerLevel level, BlockPos start, BlockPos target, PathData pathData) {
        List<BlockPos> hazards = new ArrayList<>();

        if (pathData.distance == 0) return hazards;

        for (int i = 1; i < pathData.distance; i++) {
            int x = start.getX() + (pathData.dx * i) / pathData.distance;
            int z = start.getZ() + (pathData.dz * i) / pathData.distance;
            BlockPos checkPos = new BlockPos(x, start.getY(), z);

            if (HazardUtil.isPosHazardous(level, checkPos)) {
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

    public static boolean isPositionAccessible(ServerLevel level, BlockPos from, BlockPos to) {
        double distance = from.distSqr(to);
        if (distance > 1024) return false;

        if (HazardUtil.isSafeStanding(level, to)) return true;

        BlockPos[] around = {to.north(), to.south(), to.east(), to.west(), to.above(), to.below()};
        for (BlockPos p : around) {
            if (HazardUtil.isSafeStanding(level, p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPathSafeEnough(ServerLevel level, Path path) {
        if (path == null || path.getNodeCount() == 0) return false;
        int nodes = path.getNodeCount();
        int startIdx = Math.max(0, path.getNextNodeIndex());

        int consecutiveSafe = 0;
        int maxConsecutiveSafe = 0;
        int totalSafe = 0;
        int totalChecked = 0;
        int consecutiveHazard = 0;
        int maxConsecutiveHazard = 0;

        for (int i = startIdx; i < nodes; i++) {
            var n = path.getNode(i);
            BlockPos pos = new BlockPos(n.x, n.y, n.z);
            totalChecked++;

            if (!HazardUtil.isPosHazardous(level, pos)) {
                totalSafe++;
                consecutiveSafe++;
                maxConsecutiveSafe = Math.max(maxConsecutiveSafe, consecutiveSafe);
                consecutiveHazard = 0;
            } else {
                consecutiveSafe = 0;
                consecutiveHazard++;
                maxConsecutiveHazard = Math.max(maxConsecutiveHazard, consecutiveHazard);
            }
        }

        if (totalChecked == 0) return false;

        int safetyPercentage = (totalSafe * 100) / totalChecked;

        // 1. 不允许连续3个以上的危险节点
        if (maxConsecutiveHazard >= 3) {
            return false;
        }

        // 2. 短路径（<=3节点）必须100%安全
        if (totalChecked <= 3) {
            return safetyPercentage == 100;
        }

        // 3. 中等路径（4-8节点）需要至少80%安全
        if (totalChecked <= 8) {
            return safetyPercentage >= 80;
        }

        // 4. 长路径需要至少70%安全，且必须有足够长的连续安全段
        boolean hasGoodSafeSegment = maxConsecutiveSafe >= Math.max(3, totalChecked / 4);
        return safetyPercentage >= 70 && hasGoodSafeSegment;
    }

    private static List<BlockPos> generateNearCandidates(ServerLevel level, BlockPos target, int radius) {
        List<BlockPos> candidates = new ArrayList<>();

        // 按优先级生成候选点
        // 1. 首先尝试基本的4个水平方向
        BlockPos[] basicDirs = {target.north(), target.south(), target.east(), target.west()};
        for (BlockPos pos : basicDirs) {
            if (HazardUtil.isSafeStanding(level, pos)) {
                candidates.add(pos);
            }
        }

        if (radius > 1) {
            // 2. 然后尝试对角线方向
            BlockPos[] diagonals = {
                    target.north().east(), target.north().west(),
                    target.south().east(), target.south().west()
            };
            for (BlockPos pos : diagonals) {
                if (HazardUtil.isSafeStanding(level, pos)) {
                    candidates.add(pos);
                }
            }

            // 3. 尝试更远的水平方向
            for (int dist = 2; dist <= radius; dist++) {
                BlockPos[] farDirs = {
                        target.north(dist), target.south(dist),
                        target.east(dist), target.west(dist)
                };
                for (BlockPos pos : farDirs) {
                    if (HazardUtil.isSafeStanding(level, pos)) {
                        candidates.add(pos);
                    }
                }
            }

            // 4. 最后考虑垂直方向（只在必要时）
            if (candidates.isEmpty()) {
                BlockPos[] verticals = {target.above(), target.below()};
                for (BlockPos pos : verticals) {
                    if (HazardUtil.isSafeStanding(level, pos)) {
                        candidates.add(pos);
                    }
                }
            }
        }

        // 按距离目标的远近排序，优先选择更近的候选点
        candidates.sort(Comparator.comparingDouble(a -> a.distSqr(target)));

        return candidates;
    }
}