package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record EnchantmentBlockCountsPayload(
        int containerId,
        List<Identifier> blockIds,
        List<Integer> counts
) implements CustomPacketPayload {
    public static final Type<EnchantmentBlockCountsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchantment_block_counts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantmentBlockCountsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantmentBlockCountsPayload::containerId,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), EnchantmentBlockCountsPayload::blockIds,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), EnchantmentBlockCountsPayload::counts,
            EnchantmentBlockCountsPayload::new
    );

    public EnchantmentBlockCountsPayload {
        blockIds = List.copyOf(blockIds);
        counts = List.copyOf(counts);

        if (blockIds.size() != counts.size()) {
            throw new IllegalArgumentException("blockIds and counts must have the same size");
        }
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
