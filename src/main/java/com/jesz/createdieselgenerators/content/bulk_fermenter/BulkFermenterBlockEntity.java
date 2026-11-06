package com.jesz.createdieselgenerators.content.bulk_fermenter;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGRecipes;
import com.jesz.createdieselgenerators.port.CDGFluidUnits;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.content.processing.basin.BasinBlockEntity;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.foundation.recipe.RecipeFinder;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import com.zurrtum.create.infrastructure.items.CombinedInvWrapper;
import com.zurrtum.create.infrastructure.items.ItemStackHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PORT-NOTE(26.1): NeoForge item/fluid handlers became a Create Fly {@link ItemStackHandler} per block (combined over
 * the multiblock with {@link CombinedInvWrapper}) and a {@link BulkFermenterFluidHandler} on the controller, exposed to
 * Fabric's transfer API through the capability shim. Fluid amounts are droplets. NBT goes through ValueInput/Output
 * ("Inventory" is now Create Fly's compact item list; "TankContent" keeps its key).
 */
public class BulkFermenterBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainerFluidItem, IHaveGoggleInformation {

    private static final int MAX_SIZE = 3;
    Container itemHandler;
    final BulkFermenterInventoryWrapper itemCapability = new BulkFermenterInventoryWrapper();
    public ItemStackHandler inventory;
    BulkFermenterFluidHandler fluidCapability;
    BulkFermenterFluidHandler tankInventory;
    BlockPos controller;
    BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    int width = 1;
    int height = 1;

    private static final int SYNC_RATE = 8;
    int syncCooldown;
    boolean queuedSync;

    public int processingTime = -1;
    BulkFermentingRecipe currentRecipe;
    // PORT-NOTE(26.1): synced for the renderer (the client has no recipes)
    public int recipeDuration;

    public boolean packagerMode;

    BlazeBurnerBlock.HeatLevel highestHeatLevel = BlazeBurnerBlock.HeatLevel.NONE;

    public BulkFermenterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tankInventory = createInventory();
        updateConnectivity = false;
        updateCapability = false;
        inventory = new ItemStackHandler(5) {
            @Override
            public void setChanged() {
                onItemsChanged();
            }
        };
        refreshCapability();
    }

    private void onItemsChanged() {
        if (level == null)
            return;
        // PORT-NOTE(26.1): recipes run on the controller; a part only saves itself and forwards
        BulkFermenterBlockEntity controllerBE = getControllerBE();
        if (controllerBE != null && controllerBE != this) {
            setChanged();
            controllerBE.onItemsChanged();
            return;
        }
        if (!level.isClientSide()) {
            List<BulkFermentingRecipe> r = getMatchingRecipes();
            if (!r.contains(currentRecipe))
                processingTime = -1;
            if (processingTime == -1 && !r.isEmpty()) {
                currentRecipe = r.getFirst();
                startProcessing();
            }
            setChanged();
            sendData();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
    }

    protected BulkFermenterFluidHandler createInventory() {
        return new BulkFermenterFluidHandler(6, getCapacityMultiplier(), this::onFluidStackChanged);
    }

    public void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide())
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    private void startProcessing() {
        if (currentRecipe == null)
            return;
        processingTime = currentRecipe.getProcessingDuration();
        sendData();
    }

    @Override
    public void tick() {
        if (isController() && level != null) {
            if (processingTime >= 0) {
                if (!level.isClientSide() && processingTime % 20 == 0 && level.getRandom().nextInt(4) == 0)
                    level.playSound(null, worldPosition.offset(width / 2, height / 2, width / 2), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                            SoundSource.BLOCKS, .15f, .75f);

                if (processingTime == 1)
                    level.playSound(null, worldPosition.offset(width / 2, height / 2, width / 2), SoundEvents.BREWING_STAND_BREW,
                            SoundSource.BLOCKS, .15f, .75f);

                if (currentRecipe == null) {
                    if (level.isClientSide()) {
                        processingTime = Math.max(0, processingTime - 1);
                    } else {
                        List<BulkFermentingRecipe> r = getMatchingRecipes();
                        if (r.isEmpty())
                            processingTime = -1;
                        else
                            currentRecipe = r.getFirst();
                    }
                } else {
                    if (processingTime == 0 && !level.isClientSide()) {
                        for (int i = 0; i < width * width; i++) {
                            if (!currentRecipe.apply(this, true))
                                break;
                            currentRecipe.apply(this, false);
                        }
                        processingTime = -1;
                    } else {
                        processingTime = (int) Math.max(0, processingTime - Math.sqrt(width * height));
                    }
                }
            }
            if (processingTime == -1 && !level.isClientSide()) {
                if (currentRecipe != null) {
                    List<BulkFermentingRecipe> r = getMatchingRecipes();
                    currentRecipe = null;
                    if (!r.isEmpty()) {
                        currentRecipe = r.getFirst();
                        startProcessing();
                    }
                    setChanged();
                    sendData();
                }
            }
        }
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync)
                sendData();
        }

        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if (updateConnectivity)
            updateConnectivity();

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
    }

    // PORT-NOTE(26.1): recipes only exist on the server; the client mirrors processingTime from synced data
    protected List<BulkFermentingRecipe> getMatchingRecipes() {
        if (!(level instanceof ServerLevel serverLevel))
            return List.of();
        List<RecipeHolder<? extends Recipe<?>>> list = RecipeFinder.get(RECIPE_CACHE_KEY, serverLevel, recipe -> recipe.value().getType() == CDGRecipes.BULK_FERMENTING);
        return list.stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof BulkFermentingRecipe)
                .map(r -> (BulkFermentingRecipe) r)
                .sorted((r1, r2) -> r2.getRequiredHeat().ordinal() - r1.getRequiredHeat().ordinal())
                .filter(r -> r.apply(this, true))
                .collect(Collectors.toList());
    }

    static final Object RECIPE_CACHE_KEY = new Object();

    // PORT-NOTE(26.1): was BulkFermenterBlock.onRemove
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        net.minecraft.world.Containers.dropContents(level, pos, inventory);
        level.removeBlockEntity(pos);
        ConnectivityHandler.splitMulti(this);
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onFluidStackChanged() {
        if (!hasLevel() || level == null)
            return;

        if (!level.isClientSide()) {
            List<BulkFermentingRecipe> r = getMatchingRecipes();
            if (!r.contains(currentRecipe))
                processingTime = -1;
            if (processingTime == -1 && !r.isEmpty()) {
                currentRecipe = r.getFirst();
                startProcessing();
            }
        }

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    BulkFermenterBlockEntity tankAt = ConnectivityHandler.partAt(getType(), level, pos);
                    if (tankAt == null)
                        continue;
                    level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState().getBlock());
                }
            }
        }

        if (!level.isClientSide()) {
            setChanged();
            sendData();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public BulkFermenterBlockEntity getControllerBE() {
        if (isController())
            return this;
        if (level == null)
            return null;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof BulkFermenterBlockEntity)
            return (BulkFermenterBlockEntity) blockEntity;
        return null;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (isController())
            return super.createRenderBoundingBox().expandTowards(width - 1, 0, width - 1);
        else
            return super.createRenderBoundingBox();
    }

    public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(blocks * getCapacityMultiplier());
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level == null || level.isClientSide())
            return;
        updateConnectivity = true;
        if (!keepContents)
            applyFluidTankSize(1);
        controller = null;
        width = 1;
        height = 1;

        onFluidStackChanged();
        refreshCapability();
        setChanged();
        sendData();
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    @Override
    public void setController(BlockPos controller) {
        if (level != null && level.isClientSide() && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        refreshCapability();
        setChanged();
        sendData();
    }

    private void refreshCapability() {
        fluidCapability = handlerForCapability();
        // PORT-NOTE(26.1): was invalidateCapabilities(); the Fabric storages are cached in Create Fly behaviours
        var cachedFluids = getBehaviour(com.zurrtum.create.foundation.blockEntity.behaviour.CachedFluidInventoryBehaviour.TYPE);
        if (cachedFluids != null)
            cachedFluids.reset();
        var cachedItems = getBehaviour(com.zurrtum.create.foundation.blockEntity.behaviour.CachedInventoryBehaviour.TYPE);
        if (cachedItems != null)
            cachedItems.reset();
    }

    /** The combined item inventory of the multiblock (rebuilt, the size may have changed). */
    Container getItems() {
        initCapability();
        return itemCapability;
    }

    private void initCapability() {
        if (level == null)
            return;
        if (!isController()) {
            BulkFermenterBlockEntity controllerBE = getControllerBE();
            if (controllerBE == null)
                return;
            controllerBE.initCapability();
            itemHandler = controllerBE.itemHandler;
            itemCapability.setItemHandler(itemHandler, controllerBE);
            return;
        }

        Container[] inventories = new Container[height * width * width];
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos vaultPos = worldPosition.offset(xOffset, yOffset, zOffset);
                    BulkFermenterBlockEntity tankAt =
                            ConnectivityHandler.partAt(CDGBlockEntityTypes.BULK_FERMENTER.get(), level, vaultPos);
                    inventories[yOffset * width * width + xOffset * width + zOffset] =
                            tankAt != null ? tankAt.inventory : new ItemStackHandler(0);
                }
            }
        }

        itemHandler = new CombinedInvWrapper(inventories);
        // Swap the handler behind the stable wrapper (re-registering would invalidate the fluid side too)
        itemCapability.setItemHandler(itemHandler, this);
    }

    private BulkFermenterFluidHandler handlerForCapability() {
        return isController() ?
                tankInventory :
                getControllerBE() != null ? getControllerBE().handlerForCapability() : new BulkFermenterFluidHandler(0, 0, () -> {});
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;

        updateConnectivity = tag.getBooleanOr("Uninitialized", false);
        lastKnownPos = tag.read("LastKnownPos", BlockPos.CODEC).orElse(null);
        controller = tag.read("Controller", BlockPos.CODEC).orElse(null);

        if (isController()) {
            width = tag.getIntOr("Size", 1);
            height = tag.getIntOr("Height", 1);
            highestHeatLevel = BlazeBurnerBlock.HeatLevel.values()[Math.clamp(tag.getIntOr("Heat", 0), 0, BlazeBurnerBlock.HeatLevel.values().length - 1)];
            tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());
            tankInventory.read(tag.childOrEmpty("TankContent"));

            processingTime = tag.getIntOr("ProcessingTime", -1);
            recipeDuration = tag.getIntOr("RecipeDuration", 0);
        }

        inventory.read(tag);

        updateCapability = true;

        if (!clientPacket)
            return;

        boolean changeOfController = !Objects.equals(controllerBefore, controller);

        if (hasLevel() && (changeOfController || prevSize != width || prevHeight != height)) {
            level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());

            if (isController()) {
                tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
                invalidateRenderBoundingBox();
            }
        }
    }

    @Override
    protected void write(ValueOutput tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (updateConnectivity)
            tag.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            tag.store("LastKnownPos", BlockPos.CODEC, lastKnownPos);
        if (!isController())
            tag.store("Controller", BlockPos.CODEC, controller);
        if (isController()) {
            tankInventory.write(tag.child("TankContent"));
            tag.putInt("Size", width);
            tag.putInt("Height", height);
            tag.putInt("ProcessingTime", processingTime);
            tag.putInt("RecipeDuration", currentRecipe != null ? currentRecipe.getProcessingDuration() : recipeDuration);
            tag.putInt("Heat", highestHeatLevel.ordinal());
        }
        inventory.write(tag);

        if (!clientPacket)
            return;

        if (queuedSync)
            tag.putBoolean("LazySync", true);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.BULK_FERMENTER.get(),
                (be, side) -> {
                    if (be.fluidCapability == null)
                        be.refreshCapability();
                    return be.fluidCapability;
                }
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CDGBlockEntityTypes.BULK_FERMENTER.get(),
                (be, side) -> {
                    be.initCapability();
                    return be.itemCapability;
                }
        );
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    public static int getCapacityMultiplier() {
        return CDGFluidUnits.BUCKET;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        onFluidStackChanged();
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return AllConfigs.server().fluids.fluidTankMaxHeight.get();
    }

    @Override
    public int getMaxWidth() {
        return MAX_SIZE;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return getCapacityMultiplier();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    @Override
    public FluidTank getTank(int tank) {
        return tankInventory.tanks.get(tank);
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getStack(tank).copy();
    }

    public BulkFermentingRecipe getRecipe() {
        return currentRecipe;
    }

    public BulkFermenterFluidHandler getTankInventory() {
        return tankInventory;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BulkFermenterBlockEntity controller = getControllerBE();
        if (controller == null)
            return false;

        controller.initCapability();
        Container items = controller.itemCapability;
        BulkFermenterFluidHandler fluids = controller.tankInventory;

        boolean isEmpty = true;

        CreateLang.translate("gui.goggles.basin_contents")
                .forGoggles(tooltip);

        Map<Item, Integer> allItems = new LinkedHashMap<>();
        for (int i = 0; i < items.getContainerSize(); i++) {
            ItemStack stackInSlot = items.getItem(i);
            if (stackInSlot.isEmpty())
                continue;
            allItems.merge(stackInSlot.getItem(), stackInSlot.getCount(), Integer::sum);
            isEmpty = false;
        }

        for (Map.Entry<Item, Integer> e : allItems.entrySet()) {
            CreateLang.text("")
                    .add(Component.translatable(e.getKey().getDescriptionId())
                            .withStyle(ChatFormatting.GRAY))
                    .add(CreateLang.text(" x" + e.getValue())
                            .style(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
        }

        LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluidStack = fluids.getStack(i);
            if (fluidStack.isEmpty())
                continue;
            CreateLang.text("")
                    .add(CreateLang.fluidName(fluidStack)
                            .add(CreateLang.text(" "))
                            .style(ChatFormatting.GRAY)
                            .add(CreateLang.number(CDGFluidUnits.toMb(fluidStack.getAmount()))
                                    .add(mb)
                                    .style(ChatFormatting.BLUE)))
                    .forGoggles(tooltip, 1);
            isEmpty = false;
        }

        if (isEmpty)
            tooltip.removeFirst();

        return true;
    }

    public void updateHeat() {
        if (level == null)
            return;
        BulkFermenterBlockEntity controller = getControllerBE();
        int width;
        if (controller == null)
            width = 1;
        else {
            if (controller != this) {
                controller.updateHeat();
                return;
            }
            width = controller.width;
        }

        BlazeBurnerBlock.HeatLevel highestHeat = BlazeBurnerBlock.HeatLevel.NONE;

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos pos = getController().offset(xOffset, -1, zOffset);
                BlockState blockState = level.getBlockState(pos);
                BlazeBurnerBlock.HeatLevel heat = BasinBlockEntity.getHeatLevelOf(blockState);
                if (!highestHeat.isAtLeast(heat))
                    highestHeat = heat;
            }
        }
        highestHeatLevel = highestHeat;

        if (!level.isClientSide()) {
            List<BulkFermentingRecipe> r = getMatchingRecipes();
            if (!r.contains(currentRecipe))
                processingTime = -1;
            if (processingTime == -1 && !r.isEmpty()) {
                currentRecipe = r.getFirst();
                startProcessing();
            }
            setChanged();
            sendData();
        }
    }
}
