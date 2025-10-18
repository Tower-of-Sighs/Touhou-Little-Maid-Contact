package com.sighs.touhou_little_maid_contact.util;

import com.sighs.touhou_little_maid_contact.tag.HazardTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class HazardUtil {
    private HazardUtil() {
    }

    public static boolean isPosHazardous(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;

        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isEmpty()) {
            if (fluid.is(HazardTags.HAZARDOUS_FLUIDS) || fluid.is(FluidTags.LAVA)) {
                return true;
            }
        }

        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            if (state.is(HazardTags.HAZARDOUS_BLOCKS) || state.is(BlockTags.CAMPFIRES)) {
                return true;
            }
        }

        BlockState below = level.getBlockState(pos.below());
        return below.is(HazardTags.HAZARDOUS_BLOCKS) || below.is(BlockTags.CAMPFIRES);
    }

    public static int calculateHazardScore(ServerLevel level, BlockPos center, int radius) {
        if (level == null || center == null) return 100;
        int hazardCount = 0;
        int totalChecked = 0;
        int r = Math.max(0, radius);

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos check = center.offset(dx, dy, dz);
                    totalChecked++;
                    if (isPosHazardous(level, check)) {
                        hazardCount++;
                    }
                }
            }
        }
        return totalChecked > 0 ? (hazardCount * 100) / totalChecked : 0;
    }

    public static boolean isSafeStanding(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;

        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());

        if (!feet.isAir() || !head.isAir() || ground.isAir()) {
            return false;
        }

        if (isPosHazardous(level, pos)) {
            return false;
        }

        if (isCompletelyTrapped(level, pos)) {
            return false;
        }

        int hazardScore = calculateHazardScore(level, pos, 1);

        return hazardScore <= 25;
    }

    public static boolean isCompletelyTrapped(ServerLevel level, BlockPos pos) {
        BlockPos[] directions = {
                pos.north(), pos.south(), pos.east(), pos.west()
        };

        int hazardousDirections = 0;
        for (BlockPos dir : directions) {
            if (isPosHazardous(level, dir) || !level.getBlockState(dir).isAir()) {
                hazardousDirections++;
            }
        }

        // 如果四个方向都被阻挡或危险，则认为被完全包围
        return hazardousDirections >= 4;
    }
}