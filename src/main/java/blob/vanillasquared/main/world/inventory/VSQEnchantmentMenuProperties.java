package blob.vanillasquared.main.world.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public interface VSQEnchantmentMenuProperties {
    int vsq$getPlayerLevel();
    int vsq$getBlockAmount();
    int vsq$getLevelRequirement();
    int vsq$getBlockRequirement();
    List<Component> vsq$getDetectedBlockTooltipLines();
    void vsq$setDetectedBlockCounts(int containerId, List<Identifier> blockIds, List<Integer> counts);
}
