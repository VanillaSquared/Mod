package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.network.payload.EnchantmentBlockCountsPayload;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu implements VSQEnchantmentMenuProperties {
    @Unique
    private static final int VSQ$PROPERTY_PLAYER_LEVEL = 0;
    @Unique
    private static final int VSQ$PROPERTY_BLOCK_COUNT = 1;
    @Unique
    private static final int VSQ$PROPERTY_LEVEL_REQUIREMENT = 2;
    @Unique
    private static final int VSQ$PROPERTY_BLOCK_REQUIREMENT = 3;
    @Unique
    private static final int VSQ$DUMMYLEVELREQUIREMENT = 69;
    @Unique
    private static final int VSQ$DUMMYBLOCKREQUIREMENT = 4;
    @Unique
    private static final Block[] VSQ$DUMMY_DEBUG_BLOCKS = new Block[] {
            Blocks.BOOKSHELF,
            Blocks.CHISELED_BOOKSHELF,
            Blocks.LECTERN
    };

    @Shadow
    @Final
    @Mutable
    private Container enchantSlots;

    @Unique
    private ContainerLevelAccess vsq$access = ContainerLevelAccess.NULL;

    @Unique
    private ServerPlayer vsq$serverPlayer;

    @Unique
    private int vsq$playerLevel;
    @Unique
    private int vsq$nearbyBlockCount;
    @Unique
    private List<Component> vsq$detectedBlockTooltipLines = List.of();

    @Unique
    private Player vsq$player;

    @Unique
    private final ContainerData vsq$properties = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case VSQ$PROPERTY_PLAYER_LEVEL -> {
                    if (EnchantmentMenuMixin.this.vsq$player != null && !EnchantmentMenuMixin.this.vsq$player.level().isClientSide()) {
                        EnchantmentMenuMixin.this.vsq$playerLevel = EnchantmentMenuMixin.this.vsq$player.experienceLevel;
                    }
                    yield EnchantmentMenuMixin.this.vsq$playerLevel;
                }
                case VSQ$PROPERTY_BLOCK_COUNT -> EnchantmentMenuMixin.this.vsq$nearbyBlockCount;
                case VSQ$PROPERTY_LEVEL_REQUIREMENT -> VSQ$DUMMYLEVELREQUIREMENT;
                case VSQ$PROPERTY_BLOCK_REQUIREMENT -> VSQ$DUMMYBLOCKREQUIREMENT;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case VSQ$PROPERTY_PLAYER_LEVEL -> EnchantmentMenuMixin.this.vsq$playerLevel = value;
                case VSQ$PROPERTY_BLOCK_COUNT -> EnchantmentMenuMixin.this.vsq$nearbyBlockCount = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    protected EnchantmentMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void vsq$rebuildSlotLayoutClient(int containerId, Inventory playerInventory, CallbackInfo ci) {
        this.vsq$player = playerInventory.player;
        this.vsq$rebuildSlotLayout(playerInventory);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void vsq$rebuildSlotLayoutServer(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.vsq$player = playerInventory.player;
        this.vsq$rebuildSlotLayout(playerInventory);
        this.vsq$access = access;
        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            this.vsq$serverPlayer = serverPlayer;
        }
        this.vsq$updateNearbyBlockCount();
    }

    @Inject(method = "slotsChanged(Lnet/minecraft/world/Container;)V", at = @At("TAIL"))
    private void vsq$refreshNearbyBlockCount(Container container, CallbackInfo ci) {
        this.vsq$updateNearbyBlockCount();
    }

    @Unique
    private void vsq$updateNearbyBlockCount() {
        if (this.vsq$serverPlayer == null) {
            return;
        }

        this.vsq$access.execute((Level level, BlockPos tablePos) -> {
            if (level.isClientSide()) {
                return;
            }

            Map<Identifier, Integer> detectedBlocks = this.vsq$collectDetectedBlocks(level, tablePos);
            this.vsq$nearbyBlockCount = detectedBlocks.values().stream().mapToInt(Integer::intValue).sum();
            this.vsq$sendDetectedBlockCounts(detectedBlocks);
        });
    }


    @Unique
    private Map<Identifier, Integer> vsq$collectDetectedBlocks(Level level, BlockPos tablePos) {
        Map<Identifier, Integer> counts = new TreeMap<>(Identifier::compareTo);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos pos = tablePos.offset(dx, dy, dz);
                    if (pos.equals(tablePos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }

                    Block block = state.getBlock();
                    if (!this.vsq$matchesDummyDebugBlock(block)) {
                        continue;
                    }

                    Identifier key = BuiltInRegistries.BLOCK.getKey(block);
                    if (key == null) {
                        continue;
                    }

                    counts.merge(key, 1, Integer::sum);
                }
            }
        }

        return counts;
    }

    @Unique
    private void vsq$sendDetectedBlockCounts(Map<Identifier, Integer> detectedBlocks) {
        if (this.vsq$serverPlayer == null) {
            return;
        }

        List<Identifier> blockIds = new ArrayList<>(detectedBlocks.size());
        List<Integer> counts = new ArrayList<>(detectedBlocks.size());
        for (Map.Entry<Identifier, Integer> entry : detectedBlocks.entrySet()) {
            blockIds.add(entry.getKey());
            counts.add(entry.getValue());
        }

        ServerPlayNetworking.send(this.vsq$serverPlayer, new EnchantmentBlockCountsPayload(this.containerId, blockIds, counts));
    }

    @Unique
    private boolean vsq$matchesDummyDebugBlock(Block block) {
        for (Block candidate : VSQ$DUMMY_DEBUG_BLOCKS) {
            if (candidate == block) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void vsq$rebuildSlotLayout(Inventory playerInventory) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;

        this.enchantSlots = new SimpleContainer(8) {
            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentMenuMixin.this.slotsChanged(this);
            }
        };

        this.slots.clear();
        accessor.vsq$getLastSlots().clear();
        accessor.vsq$getRemoteSlots().clear();

        this.addDataSlots(this.vsq$properties);

        this.addSlot(new Slot(this.enchantSlots, 0, 26, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isEnchantable();
            }
        });

        this.addSlot(new Slot(this.enchantSlots, 1, 80, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }
        });

        this.addSlot(new Slot(this.enchantSlots, 2, 80, 18));
        this.addSlot(new Slot(this.enchantSlots, 3, 62, 36));
        this.addSlot(new Slot(this.enchantSlots, 4, 98, 36));
        this.addSlot(new Slot(this.enchantSlots, 5, 80, 54));

        this.vsq$addPlayerSlots(playerInventory);
    }

    @Unique
    private void vsq$addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
        }
    }

    @Override
    public int vsq$getPlayerLevel() {
        return this.vsq$properties.get(VSQ$PROPERTY_PLAYER_LEVEL);
    }

    @Override
    public int vsq$getBlockAmount() {
        return this.vsq$properties.get(VSQ$PROPERTY_BLOCK_COUNT);
    }

    @Override
    public int vsq$getLevelRequirement() {
        return this.vsq$properties.get(VSQ$PROPERTY_LEVEL_REQUIREMENT);
    }

    @Override
    public int vsq$getBlockRequirement() {
        return this.vsq$properties.get(VSQ$PROPERTY_BLOCK_REQUIREMENT);
    }

    @Override
    public List<Component> vsq$getDetectedBlockTooltipLines() {
        return this.vsq$detectedBlockTooltipLines;
    }

    @Override
    public void vsq$setDetectedBlockCounts(int containerId, List<Identifier> blockIds, List<Integer> counts) {
        if (this.containerId != containerId) {
            return;
        }

        if (blockIds.size() != counts.size()) {
            this.vsq$detectedBlockTooltipLines = List.of();
            return;
        }

        List<Component> tooltipLines = new ArrayList<>(blockIds.size());
        for (int i = 0; i < blockIds.size(); i++) {
            Identifier blockId = blockIds.get(i);
            int count = counts.get(i);
            Block block = BuiltInRegistries.BLOCK.getValue(blockId);
            if (block == null) {
                block = Blocks.AIR;
            }
            MutableComponent line = Component.translatable(
                    "vsq.gui.container.enchantment_table.blocks.tooltip.entry",
                    count,
                    block.getName()
            );
            tooltipLines.add(line);
        }

        this.vsq$detectedBlockTooltipLines = Collections.unmodifiableList(tooltipLines);
    }
}
