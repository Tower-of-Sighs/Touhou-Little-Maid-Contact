package com.sighs.touhou_little_maid_epistalove.util;

import com.sighs.touhou_little_maid_epistalove.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public final class HazardUtil {
    private HazardUtil() {
    }

    /**
     * 使用原版路径类型系统检查位置是否危险
     */
    public static boolean isPosHazardous(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;

        BlockPathTypes pathType = getBlockPathType(level, pos);
        return isPathTypeDangerous(pathType);
    }

    /**
     * 获取指定位置的路径类型（基于原版逻辑）
     */
    public static BlockPathTypes getBlockPathType(BlockGetter level, BlockPos pos) {
        return WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable());
    }

    /**
     * 判断路径类型是否危险
     */
    public static boolean isPathTypeDangerous(BlockPathTypes pathType) {
        return switch (pathType) {
            case LAVA, DAMAGE_FIRE, DAMAGE_OTHER, DAMAGE_CAUTIOUS -> true;
            case BLOCKED, FENCE, UNPASSABLE_RAIL -> true; // 不可通行也算危险
            default -> false; // DANGER_* 视为警示，不直接判为危险
        };
    }

    /**
     * 检查位置是否适合站立（更严格的检查）
     */
    public static boolean isSafeForStanding(ServerLevel level, BlockPos pos, Mob mob) {
        if (level == null || pos == null) return false;

        // 检查脚部位置
        BlockPathTypes feetType = getBlockPathType(level, pos);
        if (feetType != BlockPathTypes.OPEN && feetType != BlockPathTypes.WALKABLE) {
            return false;
        }

        // 检查头部位置
        BlockPathTypes headType = getBlockPathType(level, pos.above());
        if (headType != BlockPathTypes.OPEN) {
            // 特殊处理：检查是否是安全的双层方块（如邮筒）
            if (!isSafeDoubleBlock(level, pos.above())) {
                return false;
            }
        }

        // 检查地面支撑
        BlockPathTypes groundType = getBlockPathType(level, pos.below());
        if (groundType == BlockPathTypes.OPEN || isPathTypeDangerous(groundType)) {
            return false;
        }

        // 检查周围是否有危险
        if (mob != null) {
            float malus = mob.getPathfindingMalus(feetType);
            if (malus < 0.0F) {
                return false;
            }
        }

        return !isAreaTooHazardous(level, pos, 1);
    }

    /**
     * 检查是否是安全的双层方块（如邮筒）
     */
    private static boolean isSafeDoubleBlock(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var block = state.getBlock();

        var blockId = BuiltInRegistries.BLOCK.getKey(block);
        if ("contact".equals(blockId.getNamespace())) {
            String path = blockId.getPath();
            return "red_postbox".equals(path) || "green_postbox".equals(path);
        }

        return false;
    }

    /**
     * 计算区域危险评分（0-100，越高越危险）
     * 使用加权系统：直接伤害>可能伤害>邻接危险>通行性差
     */
    public static int calculateHazardScore(ServerLevel level, BlockPos center, int radius) {
        if (level == null || center == null) return 100;
        int hazardCount = 0;
        int totalChecked = 0;
        int r = Math.max(0, Math.min(radius, 3));

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos check = center.offset(dx, dy, dz);
                    totalChecked++;

                    BlockPathTypes pathType = getBlockPathType(level, check);
                    int weight = switch (pathType) {
                        case LAVA, DAMAGE_FIRE -> 3;
                        case DAMAGE_OTHER, DAMAGE_CAUTIOUS -> 2;
                        case DANGER_FIRE, DANGER_OTHER, DANGER_POWDER_SNOW -> 1;
                        case BLOCKED, FENCE, UNPASSABLE_RAIL -> 1;
                        default -> 0;
                    };
                    hazardCount += weight;
                }
            }
        }
        return totalChecked > 0 ? Math.min(100, (hazardCount * 100) / (totalChecked * 3)) : 0;
    }

    /**
     * 检查位置是否适合安全站立
     */
    public static boolean isSafeStanding(ServerLevel level, BlockPos pos) {
        return isSafeForStanding(level, pos, null);
    }

    /**
     * 检查区域是否过于危险
     */
    public static boolean isAreaTooHazardous(ServerLevel level, BlockPos center, int radius) {
        int hazardScore = calculateHazardScore(level, center, radius);
        return hazardScore > ModConfig.get().safetyEvaluation.areaHazardThreshold;
    }

    /**
     * 检查位置是否被完全包围
     */
    public static boolean isCompletelyTrapped(ServerLevel level, BlockPos pos) {
        BlockPos[] directions = {
                pos.north(), pos.south(), pos.east(), pos.west()
        };

        int blockedDirections = 0;
        for (BlockPos dir : directions) {
            BlockPathTypes pathType = getBlockPathType(level, dir);
            if (pathType == BlockPathTypes.BLOCKED || isPathTypeDangerous(pathType)) {
                blockedDirections++;
            }
        }

        // 如果四个方向都被阻挡，则认为被完全包围
        return blockedDirections >= 4;
    }

    /**
     * 获取最安全的相邻位置
     */
    public static BlockPos findSafestNearbyPosition(ServerLevel level, BlockPos center, int searchRadius) {
        BlockPos bestPos = null;
        int bestScore = Integer.MAX_VALUE;

        int r = Math.min(searchRadius, 5);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos candidate = center.offset(dx, dy, dz);
                    if (isSafeStanding(level, candidate)) {
                        int score = calculateHazardScore(level, candidate, 1);
                        if (score < bestScore) {
                            bestScore = score;
                            bestPos = candidate;
                        }
                    }
                }
            }
        }

        return bestPos;
    }
}