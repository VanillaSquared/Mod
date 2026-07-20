package blob.vanillasquared.main.world.recipe;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.loot.LootTableIdResolver;
import blob.vanillasquared.main.world.loot.RandomizeRecipesFunction;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.Recipe;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** Data-pack recipe tags, independent of any particular recipe type. */
public final class RecipeTags {
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "recipe_tag_loader");
    private static final FileToIdConverter TAG_CONVERTER = FileToIdConverter.json("tags/recipe");
    private static final FileToIdConverter RECIPE_CONVERTER = FileToIdConverter.json("recipe");
    private static volatile Map<Identifier, List<ResourceKey<Recipe<?>>>> TAGS = Map.of();

    private RecipeTags() {
    }

    public static void initialize() {
        DataResourceLoader.get().registerReloadListener(RELOAD_LISTENER_ID, registries -> new ReloadListener());
    }

    public static List<ResourceKey<Recipe<?>>> get(Identifier tagId) {
        return TAGS.getOrDefault(tagId, List.of());
    }

    public static Optional<ResourceKey<Recipe<?>>> randomRecipe(Identifier tagId, RandomSource random) {
        return randomRecipe(tagId, random, recipe -> true);
    }

    public static Optional<ResourceKey<Recipe<?>>> randomRecipe(Identifier tagId, RandomSource random, Predicate<ResourceKey<Recipe<?>>> filter) {
        List<ResourceKey<Recipe<?>>> recipes = get(tagId).stream().filter(filter).toList();
        return recipes.isEmpty() ? Optional.empty() : Optional.of(recipes.get(random.nextInt(recipes.size())));
    }

    private record Entry(Identifier id, boolean tag, boolean required) {
    }

    private record PreparedTags(Map<Identifier, List<Entry>> tags, Set<Identifier> recipeIds) {
    }

    private static final class ReloadListener extends SimpleReloadListener<PreparedTags> {
        @Override
        protected PreparedTags prepare(PreparableReloadListener.SharedState store) {
            Map<Identifier, List<Entry>> loaded = new LinkedHashMap<>();
            for (Identifier fileId : TAG_CONVERTER.listMatchingResources(store.resourceManager()).keySet()) {
                Identifier tagId = TAG_CONVERTER.fileToId(fileId);
                List<Resource> resources;
                try {
                    resources = store.resourceManager().getResourceStack(fileId);
                } catch (Exception exception) {
                    VanillaSquared.LOGGER.error("Failed to resolve recipe tag stack for {}", fileId, exception);
                    continue;
                }

                List<Entry> values = new ArrayList<>();
                for (Resource resource : resources) {
                    try (Reader reader = resource.openAsReader()) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        if (json.has("replace") && json.get("replace").isJsonPrimitive() && json.get("replace").getAsBoolean()) {
                            values.clear();
                        }
                        if (json.has("values") && json.get("values").isJsonArray()) {
                            values.addAll(vsq$parseValues(tagId, json.getAsJsonArray("values")));
                        }
                    } catch (Exception exception) {
                        VanillaSquared.LOGGER.error("Failed to load recipe tag {} from {}", tagId, fileId, exception);
                    }
                }
                loaded.put(tagId, List.copyOf(values));
            }
            Set<Identifier> recipeIds = new LinkedHashSet<>();
            for (Identifier fileId : RECIPE_CONVERTER.listMatchingResources(store.resourceManager()).keySet()) {
                recipeIds.add(RECIPE_CONVERTER.fileToId(fileId));
            }
            return new PreparedTags(Map.copyOf(loaded), Set.copyOf(recipeIds));
        }

        private static List<Entry> vsq$parseValues(Identifier tagId, JsonArray values) {
            List<Entry> parsed = new ArrayList<>(values.size());
            for (JsonElement value : values) {
                String rawId = null;
                boolean required = true;
                if (value.isJsonPrimitive()) {
                    rawId = value.getAsString();
                } else if (value.isJsonObject()) {
                    JsonObject object = value.getAsJsonObject();
                    if (object.has("id") && object.get("id").isJsonPrimitive()) {
                        rawId = object.get("id").getAsString();
                    }
                    if (object.has("required") && object.get("required").isJsonPrimitive()) {
                        required = object.get("required").getAsBoolean();
                    }
                }

                boolean isTag = rawId != null && rawId.startsWith("#");
                Identifier id = rawId == null ? null : Identifier.tryParse(isTag ? rawId.substring(1) : rawId);
                if (id == null) {
                    VanillaSquared.LOGGER.warn("Ignoring invalid entry {} in recipe tag {}", rawId, tagId);
                    continue;
                }
                parsed.add(new Entry(id, isTag, required));
            }
            return parsed;
        }

        @Override
        protected void apply(PreparedTags prepared, PreparableReloadListener.SharedState store) {
            LootTableIdResolver.clearCache();
            RandomizeRecipesFunction.clearWarningCache();
            Map<Identifier, List<Entry>> data = prepared.tags();
            Map<Identifier, List<ResourceKey<Recipe<?>>>> resolved = new LinkedHashMap<>();
            for (Identifier tagId : data.keySet()) {
                LinkedHashSet<ResourceKey<Recipe<?>>> recipes = new LinkedHashSet<>();
                vsq$resolve(tagId, data, prepared.recipeIds(), recipes, new LinkedHashSet<>());
                resolved.put(tagId, List.copyOf(recipes));
            }
            TAGS = Map.copyOf(resolved);
            VanillaSquared.LOGGER.info("Loaded {} recipe tags", TAGS.size());
        }

        private static void vsq$resolve(Identifier tagId, Map<Identifier, List<Entry>> data, Set<Identifier> recipeIds, Set<ResourceKey<Recipe<?>>> output, Set<Identifier> visiting) {
            if (!visiting.add(tagId)) {
                VanillaSquared.LOGGER.warn("Ignoring cycle involving recipe tag {}", tagId);
                return;
            }

            for (Entry entry : data.getOrDefault(tagId, List.of())) {
                if (!entry.tag()) {
                    if (recipeIds.contains(entry.id())) {
                        output.add(ResourceKey.create(Registries.RECIPE, entry.id()));
                    } else if (entry.required()) {
                        VanillaSquared.LOGGER.warn("Recipe tag {} references missing required recipe {}", tagId, entry.id());
                    }
                    continue;
                }
                if (!data.containsKey(entry.id())) {
                    if (entry.required()) {
                        VanillaSquared.LOGGER.warn("Recipe tag {} references missing required tag {}", tagId, entry.id());
                    }
                    continue;
                }
                vsq$resolve(entry.id(), data, recipeIds, output, visiting);
            }
            visiting.remove(tagId);
        }
    }
}
