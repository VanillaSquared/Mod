package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.resources.Identifier;

import java.util.Map;

/** Selects the recipe tag used by each enchanting-recipe distribution source. */
public final class EnchantingRecipeDistribution {
    public static final Identifier DEFAULT_LOOT_TAG = tag("default");
    public static final Identifier DEFAULT_LIBRARIAN_TAG = tag("villager/librarian/default");
    private static final Map<String, Identifier> LOOT_TABLE_TO_TAG = Map.ofEntries(
            Map.entry("chests/jungle_temple", tag("jungle_temple_chest")),
            Map.entry("chests/stronghold_crossing", tag("stronghold_storeroom_chest")),
            Map.entry("chests/stronghold_library", tag("stronghold_library_chest")),
            Map.entry("chests/stronghold_corridor", tag("stronghold_altar_chest")),
            Map.entry("chests/simple_dungeon", tag("monster_room_chest")),
            Map.entry("chests/abandoned_mineshaft", tag("mineshaft_chest")),
            Map.entry("chests/ancient_city", tag("ancient_city_chest")),
            Map.entry("chests/desert_pyramid", tag("desert_pyramid_chest")),
            Map.entry("chests/pillager_outpost", tag("pillager_outpost_chest")),
            Map.entry("chests/underwater_ruin_big", tag("ocean_ruins_big_ruins_chest")),
            Map.entry("chests/woodland_mansion", tag("woodland_mansion_chest")),
            Map.entry("chests/bastion_other", tag("bastion_remnant_generic_chest")),
            Map.entry("chests/bastion_remnant_generic", tag("bastion_remnant_generic_chest")),
            Map.entry("chests/trial_chambers/reward", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_common", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_rare", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_unique", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous_common", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous_rare", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous_unique", tag("trial_chamber_vault")),
            Map.entry("gameplay/fishing/treasure", tag("fishing")),
            Map.entry("gameplay/fishing", tag("fishing")),
            Map.entry("gameplay/piglin_bartering", tag("piglin_bartering")),
            Map.entry("chests/end_city_treasure", tag("end_city_treasure_chest"))
    );

    private EnchantingRecipeDistribution() {
    }

    public static Identifier lootTagForTable(Identifier lootTableId) {
        return "minecraft".equals(lootTableId.getNamespace())
                ? LOOT_TABLE_TO_TAG.getOrDefault(lootTableId.getPath(), DEFAULT_LOOT_TAG)
                : DEFAULT_LOOT_TAG;
    }

    public static Identifier librarianTagForVariant(Identifier villagerVariantId) {
        if (!"minecraft".equals(villagerVariantId.getNamespace())) {
            return DEFAULT_LIBRARIAN_TAG;
        }
        return switch (villagerVariantId.getPath()) {
            case "desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga" -> tag("villager/librarian/" + villagerVariantId.getPath());
            default -> DEFAULT_LIBRARIAN_TAG;
        };
    }

    private static Identifier tag(String path) {
        return Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, path);
    }
}
