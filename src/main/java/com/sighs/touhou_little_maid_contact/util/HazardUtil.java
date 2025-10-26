package com.sighs.touhou_little_maid_contact.util;

import com.sighs.touhou_little_maid_contact.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.PathTypeCache;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

public final class HazardUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void logDebug(String format, Object... params) {
        if (!FMLLoader.isProduction()) {
            LOGGER.debug(format, params);
        }
    }

    private HazardUtil() {
    }

    /**
     * 统检查位置是否危险
     */
    public static boolean isPosHazardous(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;

        PathType pathType = getBlockPathType(level, pos);
        return isPathTypeDangerous(pathType);
    }

    /**
     * 获取指定位置的路径类型（块级，非实体维度）
     */
    public static PathType getBlockPathType(BlockGetter level, BlockPos pos) {
        PathTypeCache cache = new PathTypeCache();
        return cache.getOrCompute(level, pos);
    }

    /**
     * 获取指定位置的路径类型（基于实体大小/门等规则）
     */
    public static PathType getPathTypeForMob(CollisionGetter level, BlockPos pos, Mob mob) {
        return WalkNodeEvaluator.getPathTypeStatic(new PathfindingContext(level, mob), pos.mutable());
    }

    /**
     * 判断路径类型是否危险
     */
    public static boolean isPathTypeDangerous(PathType pathType) {
        return switch (pathType) {
            case LAVA, DAMAGE_FIRE, DAMAGE_OTHER, DAMAGE_CAUTIOUS -> true;
            default -> false;
        };
    }

    /**
     * 检查位置是否适合站立
     */
    public static boolean isSafeForStanding(ServerLevel level, BlockPos pos, Mob mob) {
        if (level == null || pos == null) {
            logDebug("[MaidMail][HazardUtil] isSafeForStanding: null level or pos");
            return false;
        }

        PathType feetType = (mob != null) ? getPathTypeForMob(level, pos, mob) : getBlockPathType(level, pos);
        if (feetType != PathType.OPEN && feetType != PathType.WALKABLE) {
            return false;
        }

        PathType headType = (mob != null) ? getPathTypeForMob(level, pos.above(), mob) : getBlockPathType(level, pos.above());
        if (headType != PathType.OPEN) {
            boolean safeDouble = isMailboxBlock(level, pos.above());
            if (!safeDouble) {
                return false;
            }
        }

        if (feetType == PathType.OPEN) {
            BlockPos belowPos = pos.below();
            PathType groundType = (mob != null) ? getPathTypeForMob(level, belowPos, mob) : getBlockPathType(level, belowPos);

            if (groundType == PathType.OPEN) {
                return false;
            }

            if (groundType == PathType.LAVA || groundType == PathType.DAMAGE_FIRE || 
                groundType == PathType.DAMAGE_OTHER || groundType == PathType.DAMAGE_CAUTIOUS) {
                return false;
            }

        } else {
            logDebug("[MaidMail][HazardUtil] Feet is solid ({}), no ground check needed", feetType);
        }

        if (mob != null) {
            float malus = mob.getPathfindingMalus(feetType);
            if (malus < 0.0F) {
                logDebug("[MaidMail][HazardUtil] Negative pathfinding malus");
                return false;
            }
        }

        boolean areaHazardous = isAreaTooHazardous(level, pos, 1);
        if (areaHazardous) {
            logDebug("[MaidMail][HazardUtil] Area is too hazardous");
            return false;
        }

        return true;
    }

    /**
     * 检查指定位置是否是邮筒方块
     */
    private static boolean isMailboxBlock(ServerLevel level, BlockPos pos) {
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

                    PathType pathType = getBlockPathType(level, check);
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

    public static boolean isSafeStanding(ServerLevel level, BlockPos pos) {
        return isSafeForStanding(level, pos, null);
    }

    public static boolean isAreaTooHazardous(ServerLevel level, BlockPos center, int radius) {
        int hazardScore = calculateHazardScore(level, center, radius);
        return hazardScore > Config.AREA_HAZARD_THRESHOLD.get();
    }

    public static boolean isCompletelyTrapped(ServerLevel level, BlockPos pos) {
        BlockPos[] directions = { pos.north(), pos.south(), pos.east(), pos.west() };

        int blockedDirections = 0;
        for (BlockPos dir : directions) {
            PathType pathType = getBlockPathType(level, dir);
            if (pathType == PathType.BLOCKED || isPathTypeDangerous(pathType)) {
                blockedDirections++;
            }
        }

        // 如果四个方向都被阻挡，则认为被完全包围
        return blockedDirections >= 4;
    }

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