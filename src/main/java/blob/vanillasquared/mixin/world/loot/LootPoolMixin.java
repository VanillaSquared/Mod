package blob.vanillasquared.mixin.world.loot;

import blob.vanillasquared.main.world.item.VSQItems;
import blob.vanillasquared.main.world.loot.RandomizeEnchantmentSlotsFunction;
import blob.vanillasquared.main.world.loot.RandomizeRecipesFunction;
import blob.vanillasquared.main.world.loot.LootContextBridge;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeDistribution;
import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(LootPool.class)
public abstract class LootPoolMixin {
    @ModifyVariable(method = "addRandomItems", at = @At("HEAD"), argsOnly = true, name = "result")
    // Mixin requires the captured variable followed by the original method arguments for this target signature.
    private Consumer<ItemStack> vsq$sanitizeLootEnchantments(Consumer<ItemStack> original, Consumer<ItemStack> originalArgument, LootContext context) {
        return stack -> {
            if (stack.is(Items.ENCHANTED_BOOK)) {
                Identifier tagId = vsq$resolveLootTag(context);
                ItemStack recipeStack = RandomizeRecipesFunction.apply(new ItemStack(VSQItems.ENCHANT_RECIPE), tagId, context);
                if (!recipeStack.isEmpty()) {
                    original.accept(recipeStack);
                }
                return;
            }
            stack = RandomizeEnchantmentSlotsFunction.DEFAULT_LOOT_RANDOMIZATION.apply(stack, context);
            VSQEnchantments.restoreVanillaEnchantmentDefaults(stack);
            VSQEnchantments.syncDerivedEnchantments(stack);
            original.accept(stack);
        };
    }

    @Unique
    private static Identifier vsq$resolveLootTag(LootContext context) {
        return ((LootContextBridge) context).vsq$currentLootTableId()
                .map(EnchantingRecipeDistribution::lootTagForTable)
                .orElse(EnchantingRecipeDistribution.DEFAULT_LOOT_TAG);
    }
}
