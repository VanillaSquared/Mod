package blob.vanillasquared.main.world.loot;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.recipe.RecipeTags;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Selects any loaded recipe type from a recipe tag and stores it in the vanilla recipes component. */
public final class RandomizeRecipesFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomizeRecipesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(
            Identifier.CODEC.fieldOf("tag").forGetter(function -> function.tag)
    ).apply(instance, RandomizeRecipesFunction::new));
    private static final Set<Identifier> WARNED_EMPTY_TAGS = ConcurrentHashMap.newKeySet();

    private final Identifier tag;

    public RandomizeRecipesFunction(List<LootItemCondition> predicates, Identifier tag) {
        super(predicates);
        this.tag = tag;
    }

    @Override
    public MapCodec<RandomizeRecipesFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        return apply(stack, this.tag, context);
    }

    public static ItemStack apply(ItemStack stack, Identifier tag, LootContext context) {
        return RecipeTags.randomRecipe(tag, context.getRandom(), recipe -> vsq$isLoaded(recipe, context))
                .map(recipe -> {
                    stack.set(DataComponents.RECIPES, List.of(recipe));
                    return stack;
                })
                .orElseGet(() -> {
                    vsq$warnEmptyTag(tag);
                    return ItemStack.EMPTY;
                });
    }

    private static boolean vsq$isLoaded(ResourceKey<Recipe<?>> recipe, LootContext context) {
        return context.getLevel().recipeAccess().byKey(recipe).isPresent() || EnchantingRecipeRegistry.contains(recipe);
    }

    public static void clearWarningCache() {
        WARNED_EMPTY_TAGS.clear();
    }

    private static void vsq$warnEmptyTag(Identifier tag) {
        if (WARNED_EMPTY_TAGS.add(tag)) {
            VanillaSquared.LOGGER.warn("Recipe tag {} is missing, empty, or contains no loaded recipes", tag);
        }
    }
}
