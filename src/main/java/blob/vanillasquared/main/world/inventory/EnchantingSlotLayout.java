package blob.vanillasquared.main.world.inventory;

public final class EnchantingSlotLayout {
    public static final int INPUT_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    public static final int FIRST_CROSS_SLOT = 2;
    public static final int CROSS_SLOT_COUNT = 4;
    public static final int TABLE_SLOT_COUNT = 6;
    public static final int PLAYER_INV_START = TABLE_SLOT_COUNT;
    public static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    public static final int HOTBAR_START = PLAYER_INV_END;

    private EnchantingSlotLayout() {
    }
}
