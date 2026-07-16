package blob.vanillasquared.util.api.modules.components;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;

public final class RegisterComponents {
    public static final DataComponentType<VSQEnchantmentComponent> enchantmentComponent = ComponentRegistry.registerSynchronizedCached(
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchantment"),
            VSQEnchantmentComponent.CODEC.codec(),
            VSQEnchantmentComponent.STREAM_CODEC
    );

    public static final DataComponentType<HitThroughComponent> hitThroughComponent = ComponentRegistry.registerSynchronizedCached(
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "hit_through"),
            HitThroughComponent.CODEC,
            HitThroughComponent.STREAM_CODEC
    );

    private RegisterComponents() {
    }

    public static void initialize() {
    }
}
