package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.block.crucible.DraconicCrucibleFuelTier;
import com.leon.saintsdragons.common.recipe.DraconicCrucibleShapedRecipe;
import com.leon.saintsdragons.common.recipe.DraconicCrucibleSmeltingRecipe;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModRecipes;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.menu.DraconicCrucibleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DraconicCrucibleBlockEntity extends RandomizableContainerBlockEntity {
    public static final int OUTPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int INPUT_SLOT_START = 2;
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int CONTAINER_SIZE = 11;
    public static final int DATA_COUNT = 10;

    private static final long ANIMATION_DURATION_TICKS = 20L;
    private static final int THERMAL_CHARGE_MODEL_VERSION = 2;
    private static final int SHAPED_JOB_SLOT = -1;
    private static final int NO_JOB_SLOT = -2;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int burnTime;
    private int burnTimeTotal;
    private int activeHeatLevel;
    private int processingProgress;
    private int processingTimeTotal;
    private boolean processingLocked;
    private boolean canStartProcessing;
    private boolean continueProcessing;
    private int processingRequiredHeatLevel;
    private int processingFuelCost;
    private int processingFuelSpent;
    private int reservedFuelCharge;
    private int reservedFuelHeatLevel;
    private ItemStack pendingResult = ItemStack.EMPTY;
    @Nullable
    private ResourceLocation activeRecipeId;
    private int activeInputSlot = NO_JOB_SLOT;

    private boolean animationInitialized;
    private boolean open;
    private long animationStartTick;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnTimeTotal;
                case 2 -> processingProgress;
                case 3 -> processingTimeTotal;
                case 4 -> activeHeatLevel;
                case 5 -> processingLocked ? 1 : 0;
                case 6 -> canStartProcessing ? 1 : 0;
                case 7 -> DraconicCrucibleFuelTier.LEVEL_1.chargeCapacity();
                case 8 -> DraconicCrucibleFuelTier.LEVEL_2.chargeCapacity();
                case 9 -> DraconicCrucibleFuelTier.LEVEL_3.chargeCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnTimeTotal = value;
                case 2 -> processingProgress = value;
                case 3 -> processingTimeTotal = value;
                case 4 -> activeHeatLevel = value;
                case 5 -> processingLocked = value != 0;
                case 6 -> canStartProcessing = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DraconicCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRACONIC_CRUCIBLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  DraconicCrucibleBlockEntity crucible) {
        boolean changed = false;
        if (crucible.activateReservedFuel() || crucible.storeInsertedFuel()) {
            changed = true;
        }

        if (crucible.processingLocked
                && crucible.burnTime > 0
                && crucible.activeHeatLevel >= crucible.processingRequiredHeatLevel) {
            int nextProgress = crucible.processingProgress + 1;
            int targetFuelSpent = (int) Math.min(
                    crucible.processingFuelCost,
                    (long) crucible.processingFuelCost * nextProgress
                            / Math.max(1, crucible.processingTimeTotal));
            int fuelSpentThisTick = Math.max(0, targetFuelSpent - crucible.processingFuelSpent);
            if (!crucible.consumeProcessingCharge(fuelSpentThisTick)) {
                return;
            }
            crucible.processingProgress = nextProgress;
            crucible.processingFuelSpent = targetFuelSpent;
            changed = true;
            if (crucible.processingProgress >= crucible.processingTimeTotal
                    && crucible.canAcceptResult(crucible.pendingResult)) {
                crucible.finishProcessing();
                changed = true;
            }
        }

        if (crucible.continueProcessing && !crucible.processingLocked) {
            crucible.continueProcessing = false;
            if (crucible.startProcessing(level)) {
                changed = true;
            }
        }

        boolean canStart = crucible.computeCanStartProcessing(level);
        if (crucible.canStartProcessing != canStart) {
            crucible.canStartProcessing = canStart;
            changed = true;
        }

        crucible.syncProcessingState();

        if (changed) {
            crucible.setChanged();
        }
    }

    public boolean beginProcessing(Level level) {
        return startProcessing(level);
    }

    public boolean isProcessing() {
        return this.processingLocked;
    }

    public int getRemainingProcessingTicks() {
        return Math.max(0, this.processingTimeTotal - this.processingProgress);
    }

    private boolean startProcessing(Level level) {
        if (this.processingLocked) {
            return false;
        }
        if (activateReservedFuel() || storeInsertedFuel()) {
            setChanged();
        }
        CrucibleJob job = findJob(level);
        if (job == null || !canAcceptResult(job.result()) || !ensureFuelForJob(job)) {
            return false;
        }

        int processingCost = DraconicCrucibleFuelTier.fromHeatLevel(job.requiredHeatLevel())
                .processingCost(job.requiredHeatLevel());

        if (!consumeJobInputs(job)) {
            return false;
        }
        this.activeRecipeId = job.recipeId();
        this.activeInputSlot = job.inputSlot();
        this.processingProgress = 0;
        this.processingTimeTotal = job.processingTime();
        this.processingRequiredHeatLevel = job.requiredHeatLevel();
        this.processingFuelCost = processingCost;
        this.processingFuelSpent = 0;
        this.pendingResult = job.result().copy();
        this.processingLocked = true;
        this.canStartProcessing = false;
        if (!level.isClientSide) {
            level.playSound(null, this.worldPosition, ModSounds.DRACONIC_CRUCIBLE_SMELTING.get(),
                    SoundSource.BLOCKS, 0.65F, 1.0F);
        }
        syncProcessingState();
        setChanged();
        return true;
    }

    private boolean computeCanStartProcessing(Level level) {
        if (this.processingLocked) {
            return false;
        }
        CrucibleJob job = findJob(level);
        return canStartJob(job);
    }

    private boolean canStartJob(@Nullable CrucibleJob job) {
        if (job == null || !canAcceptResult(job.result())) {
            return false;
        }
        DraconicCrucibleFuelTier activeTier =
                DraconicCrucibleFuelTier.fromCharge(this.burnTime);
        if (activeTier.canFund(this.burnTime, job.requiredHeatLevel())) {
            return true;
        }

        DraconicCrucibleFuelTier queuedTier =
                DraconicCrucibleFuelTier.resolve(this.items.get(FUEL_SLOT));
        return queuedTier.canFund(queuedTier.chargeCapacity(), job.requiredHeatLevel());
    }

    private boolean ensureFuelForJob(CrucibleJob job) {
        DraconicCrucibleFuelTier activeTier = DraconicCrucibleFuelTier.fromCharge(this.burnTime);
        if (activeTier.canFund(this.burnTime, job.requiredHeatLevel())) {
            return true;
        }

        DraconicCrucibleFuelTier queuedTier =
                DraconicCrucibleFuelTier.resolve(this.items.get(FUEL_SLOT));
        if (!queuedTier.canFund(queuedTier.chargeCapacity(), job.requiredHeatLevel())) {
            return false;
        }

        this.items.get(FUEL_SLOT).shrink(1);
        loadFuel(queuedTier);
        return true;
    }

    private boolean activateReservedFuel() {
        if (this.burnTime > 0 || this.reservedFuelCharge <= 0) {
            return false;
        }
        DraconicCrucibleFuelTier tier = DraconicCrucibleFuelTier.fromHeatLevel(this.reservedFuelHeatLevel);
        this.burnTime = Math.min(this.reservedFuelCharge, tier.chargeCapacity());
        this.burnTimeTotal = tier.chargeCapacity();
        refreshActiveHeatLevel();
        this.reservedFuelCharge = 0;
        this.reservedFuelHeatLevel = 0;
        return true;
    }

    private boolean consumeProcessingCharge(int amount) {
        if (amount <= 0) {
            return true;
        }
        int minimumCharge = DraconicCrucibleFuelTier.minimumRemainingCharge(
                this.processingRequiredHeatLevel);
        if (minimumCharge == Integer.MAX_VALUE || this.burnTime - amount < minimumCharge) {
            return false;
        }

        this.burnTime -= amount;
        refreshActiveHeatLevel();
        return true;
    }

    @Nullable
    private CrucibleJob findJob(Level level) {
        SimpleContainer grid = createGridView();
        DraconicCrucibleShapedRecipe shapedMatch = null;
        for (DraconicCrucibleShapedRecipe recipe :
                level.getRecipeManager().getAllRecipesFor(ModRecipes.DRACONIC_CRUCIBLE_SHAPED_TYPE.get())) {
            if (recipe.matches(grid, level) && isPreferred(recipe, shapedMatch)) {
                shapedMatch = recipe;
            }
        }
        if (shapedMatch != null) {
            return new CrucibleJob(
                    shapedMatch.getId(), SHAPED_JOB_SLOT,
                    shapedMatch.getResultItem(level.registryAccess()).copy(),
                    shapedMatch.requiredHeatLevel(), shapedMatch.processingTime(), shapedMatch);
        }

        for (int gridSlot = 0; gridSlot < INPUT_SLOT_COUNT; gridSlot++) {
            int inventorySlot = INPUT_SLOT_START + gridSlot;
            ItemStack input = this.items.get(inventorySlot);
            if (input.isEmpty()) {
                continue;
            }
            DraconicCrucibleSmeltingRecipe smeltingMatch = null;
            for (DraconicCrucibleSmeltingRecipe recipe :
                    level.getRecipeManager().getAllRecipesFor(ModRecipes.DRACONIC_CRUCIBLE_SMELTING_TYPE.get())) {
                if (recipe.ingredient().test(input) && isPreferred(recipe, smeltingMatch)) {
                    smeltingMatch = recipe;
                }
            }
            if (smeltingMatch != null) {
                return new CrucibleJob(
                        smeltingMatch.getId(), inventorySlot, smeltingMatch.result().copy(),
                        smeltingMatch.requiredHeatLevel(), smeltingMatch.processingTime(), null);
            }

            if (input.is(ModTags.Items.DRACONIC_CRUCIBLE_VANILLA_SMELTING_BLACKLIST)) {
                continue;
            }

            SimpleContainer singleInput = new SimpleContainer(input);
            SmeltingRecipe vanillaRecipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, singleInput, level)
                    .orElse(null);
            if (vanillaRecipe != null) {
                ItemStack result = vanillaRecipe.assemble(singleInput, level.registryAccess());
                return new CrucibleJob(
                        vanillaRecipe.getId(), inventorySlot, result,
                        DraconicCrucibleFuelTier.LEVEL_1.heatLevel(), vanillaRecipe.getCookingTime(), null);
            }
        }
        return null;
    }

    private static boolean isPreferred(DraconicCrucibleShapedRecipe candidate,
                                       @Nullable DraconicCrucibleShapedRecipe current) {
        return current == null
                || candidate.priority() > current.priority()
                || (candidate.priority() == current.priority()
                && candidate.getId().toString().compareTo(current.getId().toString()) < 0);
    }

    private static boolean isPreferred(DraconicCrucibleSmeltingRecipe candidate,
                                       @Nullable DraconicCrucibleSmeltingRecipe current) {
        return current == null
                || candidate.priority() > current.priority()
                || (candidate.priority() == current.priority()
                && candidate.getId().toString().compareTo(current.getId().toString()) < 0);
    }

    private SimpleContainer createGridView() {
        SimpleContainer grid = new SimpleContainer(INPUT_SLOT_COUNT);
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            grid.setItem(slot, this.items.get(INPUT_SLOT_START + slot).copy());
        }
        return grid;
    }

    private boolean canAcceptResult(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean storeInsertedFuel() {
        DraconicCrucibleFuelTier tier = DraconicCrucibleFuelTier.resolve(this.items.get(FUEL_SLOT));
        if (tier == DraconicCrucibleFuelTier.NONE) {
            return false;
        }
        if (this.burnTime > 0) {
            return false;
        }

        ItemStack fuel = this.items.get(FUEL_SLOT);
        fuel.shrink(1);
        loadFuel(tier);
        return true;
    }

    private void loadFuel(DraconicCrucibleFuelTier tier) {
        this.burnTime = tier.chargeCapacity();
        this.burnTimeTotal = tier.chargeCapacity();
        this.activeHeatLevel = tier.heatLevel();
        this.reservedFuelCharge = 0;
        this.reservedFuelHeatLevel = 0;
    }

    private void refreshActiveHeatLevel() {
        this.activeHeatLevel = DraconicCrucibleFuelTier.fromCharge(this.burnTime).heatLevel();
        if (this.burnTime <= 0) {
            this.burnTime = 0;
            this.burnTimeTotal = 0;
        }
    }

    private boolean consumeJobInputs(CrucibleJob job) {
        if (job.shapedRecipe() != null) {
            SimpleContainer consumedGrid = createGridView();
            NonNullList<ItemStack> remainders = job.shapedRecipe().getRemainingItems(consumedGrid);
            if (!job.shapedRecipe().consumeInputs(consumedGrid)) {
                return false;
            }
            for (int gridSlot = 0; gridSlot < INPUT_SLOT_COUNT; gridSlot++) {
                restoreOrDropRemainder(consumedGrid, gridSlot, remainders.get(gridSlot));
            }
            for (int gridSlot = 0; gridSlot < INPUT_SLOT_COUNT; gridSlot++) {
                this.items.set(INPUT_SLOT_START + gridSlot, consumedGrid.getItem(gridSlot));
            }
        } else {
            ItemStack input = this.items.get(job.inputSlot());
            if (input.isEmpty()) {
                return false;
            }
            input.shrink(1);
        }
        return true;
    }

    private void restoreOrDropRemainder(SimpleContainer grid, int slot, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }

        ItemStack existing = grid.getItem(slot);
        if (existing.isEmpty()) {
            grid.setItem(slot, remainder);
            return;
        }
        if (ItemStack.isSameItemSameTags(existing, remainder)) {
            int transfer = Math.min(remainder.getCount(), existing.getMaxStackSize() - existing.getCount());
            if (transfer > 0) {
                existing.grow(transfer);
                remainder.shrink(transfer);
            }
        }
        if (!remainder.isEmpty() && this.level != null) {
            Containers.dropItemStack(
                    this.level,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 1.0D,
                    this.worldPosition.getZ() + 0.5D,
                    remainder);
        }
    }

    private void finishProcessing() {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, this.pendingResult.copy());
        } else {
            output.grow(this.pendingResult.getCount());
        }
        this.activeRecipeId = null;
        this.activeInputSlot = NO_JOB_SLOT;
        this.processingProgress = 0;
        this.processingTimeTotal = 0;
        this.processingRequiredHeatLevel = 0;
        this.processingFuelCost = 0;
        this.processingFuelSpent = 0;
        this.pendingResult = ItemStack.EMPTY;
        this.processingLocked = false;
        this.continueProcessing = true;
    }

    public ContainerData getData() {
        return this.data;
    }

    private void syncProcessingState() {
        if (this.level == null) {
            return;
        }
        BlockState lowerState = this.level.getBlockState(this.worldPosition);
        if (!lowerState.is(getBlockState().getBlock())
                || !lowerState.hasProperty(DraconicCrucibleBlock.LIT)) {
            return;
        }
        if (lowerState.getValue(DraconicCrucibleBlock.LIT) != this.processingLocked) {
            this.level.setBlock(this.worldPosition,
                    lowerState.setValue(DraconicCrucibleBlock.LIT, this.processingLocked),
                    Block.UPDATE_CLIENTS);
            sendVisualState(!this.processingLocked);
        }
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.saintsdragons.draconic_crucible");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory) {
        return new DraconicCrucibleMenu(containerId, inventory, this, this.data);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (this.processingLocked) {
            return false;
        }
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        if (slot == FUEL_SLOT) {
            return DraconicCrucibleFuelTier.resolve(stack) != DraconicCrucibleFuelTier.NONE;
        }
        return slot >= INPUT_SLOT_START && slot < INPUT_SLOT_START + INPUT_SLOT_COUNT;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items);
        }
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("BurnTimeTotal", this.burnTimeTotal);
        tag.putInt("ActiveHeatLevel", this.activeHeatLevel);
        tag.putInt("ProcessingProgress", this.processingProgress);
        tag.putInt("ProcessingTimeTotal", this.processingTimeTotal);
        tag.putBoolean("ProcessingLocked", this.processingLocked);
        tag.putInt("ProcessingRequiredHeatLevel", this.processingRequiredHeatLevel);
        tag.putInt("ProcessingFuelCost", this.processingFuelCost);
        tag.putInt("ProcessingFuelSpent", this.processingFuelSpent);
        tag.putInt("ReservedFuelCharge", this.reservedFuelCharge);
        tag.putInt("ReservedFuelHeatLevel", this.reservedFuelHeatLevel);
        tag.putInt("ThermalChargeModelVersion", THERMAL_CHARGE_MODEL_VERSION);
        if (!this.pendingResult.isEmpty()) {
            tag.put("PendingResult", this.pendingResult.save(new CompoundTag()));
        }
        if (this.activeRecipeId != null) {
            tag.putString("ActiveRecipe", this.activeRecipeId.toString());
            tag.putInt("ActiveInputSlot", this.activeInputSlot);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items);
        }
        this.burnTime = tag.getInt("BurnTime");
        this.burnTimeTotal = tag.getInt("BurnTimeTotal");
        this.activeHeatLevel = tag.getInt("ActiveHeatLevel");
        this.processingProgress = tag.getInt("ProcessingProgress");
        this.processingTimeTotal = tag.getInt("ProcessingTimeTotal");
        this.processingLocked = tag.getBoolean("ProcessingLocked");
        this.processingRequiredHeatLevel = tag.getInt("ProcessingRequiredHeatLevel");
        this.processingFuelCost = tag.getInt("ProcessingFuelCost");
        this.processingFuelSpent = tag.getInt("ProcessingFuelSpent");
        this.reservedFuelCharge = tag.getInt("ReservedFuelCharge");
        this.reservedFuelHeatLevel = tag.getInt("ReservedFuelHeatLevel");
        if (tag.getInt("ThermalChargeModelVersion") < THERMAL_CHARGE_MODEL_VERSION) {
            migrateThermalCharge();
        } else {
            refreshActiveHeatLevel();
            if (this.processingLocked && this.processingFuelCost <= 0) {
                this.processingFuelCost = DraconicCrucibleFuelTier
                        .fromHeatLevel(this.processingRequiredHeatLevel)
                        .processingCost(this.processingRequiredHeatLevel);
                this.processingFuelSpent = (int) Math.min(
                        this.processingFuelCost,
                        (long) this.processingFuelCost * this.processingProgress
                                / Math.max(1, this.processingTimeTotal));
            }
        }
        this.pendingResult = tag.contains("PendingResult")
                ? ItemStack.of(tag.getCompound("PendingResult"))
                : ItemStack.EMPTY;
        this.activeRecipeId = tag.contains("ActiveRecipe")
                ? ResourceLocation.tryParse(tag.getString("ActiveRecipe"))
                : null;
        this.activeInputSlot = this.activeRecipeId == null
                ? NO_JOB_SLOT
                : tag.getInt("ActiveInputSlot");
    }

    private void migrateThermalCharge() {
        DraconicCrucibleFuelTier tier = DraconicCrucibleFuelTier.fromHeatLevel(this.activeHeatLevel);
        int oldCapacity = this.burnTimeTotal;
        int oldChargeBeforeCurrentJob = Math.min(oldCapacity, this.burnTime + this.processingFuelSpent);
        int migratedCharge = oldCapacity <= 0
                ? 0
                : (tier.chargeCapacity() * oldChargeBeforeCurrentJob + oldCapacity - 1) / oldCapacity;

        if (this.processingLocked) {
            this.processingFuelCost = DraconicCrucibleFuelTier
                    .fromHeatLevel(this.processingRequiredHeatLevel)
                    .processingCost(this.processingRequiredHeatLevel);
            this.processingFuelSpent = (int) Math.min(
                    this.processingFuelCost,
                    (long) this.processingFuelCost * this.processingProgress
                            / Math.max(1, this.processingTimeTotal));
            migratedCharge = Math.max(0, migratedCharge - this.processingFuelSpent);
        } else {
            this.processingFuelCost = 0;
            this.processingFuelSpent = 0;
        }

        this.burnTime = migratedCharge;
        this.burnTimeTotal = migratedCharge > 0 ? tier.chargeCapacity() : 0;
        this.activeHeatLevel = DraconicCrucibleFuelTier.fromCharge(migratedCharge).heatLevel();

        if (this.reservedFuelCharge > 0) {
            DraconicCrucibleFuelTier reservedTier =
                    DraconicCrucibleFuelTier.fromHeatLevel(this.reservedFuelHeatLevel);
            int oldReservedCapacity = legacyChargeCapacity(this.reservedFuelHeatLevel);
            this.reservedFuelCharge = oldReservedCapacity <= 0
                    ? 0
                    : Math.min(
                            reservedTier.chargeCapacity(),
                            (reservedTier.chargeCapacity() * this.reservedFuelCharge
                                    + oldReservedCapacity - 1) / oldReservedCapacity
                    );
        }
    }

    private static int legacyChargeCapacity(int heatLevel) {
        return switch (heatLevel) {
            case 1 -> 5;
            case 2 -> 9;
            case 3 -> 18;
            default -> 0;
        };
    }

    public boolean isUsableBy(@NotNull Player player) {
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public void sendVisualState(boolean open) {
        if (this.level != null) {
            this.level.blockEvent(this.worldPosition, getBlockState().getBlock(), 1, open ? 1 : 0);
            if (!this.level.isClientSide) {
                this.level.playSound(null, this.worldPosition,
                        open ? ModSounds.DRACONIC_CRUCIBLE_OPEN.get()
                                : ModSounds.DRACONIC_CRUCIBLE_CLOSE.get(),
                        SoundSource.BLOCKS, 0.5F,
                        0.9F + this.level.random.nextFloat() * 0.1F);
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            boolean nextOpen = type > 0;
            if (!this.animationInitialized || this.open != nextOpen) {
                this.animationInitialized = true;
                this.open = nextOpen;
                this.animationStartTick = this.level == null ? 0L : this.level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public boolean hasAnimationState() {
        return this.animationInitialized;
    }

    public boolean isOpen() {
        return this.open;
    }

    public long getAnimationTimeMillis(float partialTick) {
        if (this.level == null) {
            return 0L;
        }
        double elapsedTicks = this.level.getGameTime() - this.animationStartTick + partialTick;
        double clampedTicks = Math.max(0.0D, Math.min(ANIMATION_DURATION_TICKS, elapsedTicks));
        return (long) (clampedTicks * 50.0D);
    }

    private record CrucibleJob(
            ResourceLocation recipeId,
            int inputSlot,
            ItemStack result,
            int requiredHeatLevel,
            int processingTime,
            @Nullable DraconicCrucibleShapedRecipe shapedRecipe
    ) {
    }
}
