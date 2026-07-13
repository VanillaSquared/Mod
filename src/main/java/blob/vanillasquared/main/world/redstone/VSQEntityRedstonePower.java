package blob.vanillasquared.main.world.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public final class VSQEntityRedstonePower {
    public static final String POWER_REDSTONE_KEY = "vsq:powerRedstone";
    private static final TagKey<Item> POWERS_REDSTONE = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("vsq", "sulfur_cube_archetype/powers_redstone")
    );

    private VSQEntityRedstonePower() {
    }

    public static int getPower(Entity entity) {
        if (entity instanceof VSQEntityRedstonePowerAccess access) {
            return access.vsq$getRedstonePower();
        }
        return 0;
    }

    public static int getContentPower(ItemStack stack) {
        return stack.is(POWERS_REDSTONE) ? 15 : 0;
    }

    public static boolean hasPoweredEntities(ServerLevel level) {
        return level instanceof VSQEntityRedstonePowerLevelAccess access && access.vsq$getPoweredEntityCount() > 0;
    }

    public static void incrementPoweredEntityCount(ServerLevel level) {
        if (level instanceof VSQEntityRedstonePowerLevelAccess access) {
            access.vsq$incrementPoweredEntityCount();
        }
    }

    public static void decrementPoweredEntityCount(ServerLevel level) {
        if (level instanceof VSQEntityRedstonePowerLevelAccess access) {
            access.vsq$decrementPoweredEntityCount();
        }
    }

    public static int getSignal(ServerLevel level, BlockPos pos) {
        if (!hasPoweredEntities(level)) {
            return 0;
        }

        AABB blockBounds = new AABB(pos);
        int signal = 0;
        for (Entity entity : level.getEntities((Entity) null, blockBounds, entity -> !entity.isRemoved())) {
            signal = Math.max(signal, getPower(entity));
            if (signal >= 15) {
                return 15;
            }
        }
        return signal;
    }

    public static void updateNeighbors(ServerLevel level, AABB sourceBounds) {
        int sourceMinX = Mth.floor(sourceBounds.minX);
        int sourceMinY = Mth.floor(sourceBounds.minY);
        int sourceMinZ = Mth.floor(sourceBounds.minZ);
        int sourceMaxX = Mth.ceil(sourceBounds.maxX) - 1;
        int sourceMaxY = Mth.ceil(sourceBounds.maxY) - 1;
        int sourceMaxZ = Mth.ceil(sourceBounds.maxZ) - 1;
        int minX = sourceMinX - 1;
        int minY = sourceMinY - 1;
        int minZ = sourceMinZ - 1;
        int maxX = sourceMaxX + 1;
        int maxY = sourceMaxY + 1;
        int maxZ = sourceMaxZ + 1;
        Set<BlockPos> updatedPositions = new HashSet<>();

        for (int x = sourceMinX; x <= sourceMaxX; x++) {
            for (int y = sourceMinY; y <= sourceMaxY; y++) {
                for (int z = sourceMinZ; z <= sourceMaxZ; z++) {
                    updateNeighbor(level, updatedPositions, x, y, z);
                }
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                updateNeighbor(level, updatedPositions, x, y, minZ);
                updateNeighbor(level, updatedPositions, x, y, maxZ);
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                updateNeighbor(level, updatedPositions, x, minY, z);
                updateNeighbor(level, updatedPositions, x, maxY, z);
            }
        }
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                updateNeighbor(level, updatedPositions, minX, y, z);
                updateNeighbor(level, updatedPositions, maxX, y, z);
            }
        }
    }

    private static void updateNeighbor(ServerLevel level, Set<BlockPos> updatedPositions, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (updatedPositions.add(pos)) {
            level.updateNeighborsAt(pos, Blocks.REDSTONE_WIRE, null);
        }
    }
}
