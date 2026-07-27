package blob.vanillasquared.main.world.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class VSQDirectionalRedstoneTransmission {
    public static final TagKey<Block> TRANSMITS_UPWARD = tag("redstone_transmits_upward");
    public static final TagKey<Block> TRANSMITS_DOWNWARD = tag("redstone_transmits_downward");

    private VSQDirectionalRedstoneTransmission() {
    }

    public static boolean transmitsUpward(BlockState state, BlockGetter level, BlockPos pos) {
        if (hasDirectionalTransmission(state)) {
            return state.is(TRANSMITS_UPWARD);
        }

        return !state.isRedstoneConductor(level, pos);
    }

    public static boolean transmitsDownward(BlockState state, BlockGetter level, BlockPos pos) {
        if (hasDirectionalTransmission(state)) {
            return state.is(TRANSMITS_DOWNWARD);
        }

        return state.isRedstoneConductor(level, pos);
    }

    public static boolean hasDirectionalTransmission(BlockState state) {
        return state.is(TRANSMITS_UPWARD) || state.is(TRANSMITS_DOWNWARD);
    }

    public static boolean routesUpdateUpward(BlockState state, BlockGetter level, BlockPos pos) {
        return hasDirectionalTransmission(state) ? state.is(TRANSMITS_UPWARD) : state.isRedstoneConductor(level, pos);
    }

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("vsq", path));
    }
}
