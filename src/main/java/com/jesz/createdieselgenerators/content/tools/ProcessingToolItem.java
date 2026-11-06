package com.jesz.createdieselgenerators.content.tools;

import com.jesz.createdieselgenerators.CDGDataComponents;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.foundation.recipe.CreateSingleStackRollableRecipe;
import com.zurrtum.create.infrastructure.component.SandPaperItemComponent;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * PORT-NOTE(26.1): shared logic of the hammer and wire cutters (two near-identical upstream classes). The off-hand item
 * is checked against a client-synced RecipePropertySet (the client has no recipe manager in 26.1); the recipe itself
 * is only looked up on the server.
 */
public abstract class ProcessingToolItem<R extends CreateSingleStackRollableRecipe> extends Item {
    private final RecipeType<R> recipeType;
    private final ResourceKey<RecipePropertySet> recipeSet;

    protected ProcessingToolItem(Properties properties, RecipeType<R> recipeType, ResourceKey<RecipePropertySet> recipeSet) {
        super(properties);
        this.recipeType = recipeType;
        this.recipeSet = recipeSet;
    }

    protected abstract SoundEvent useSound();

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null)
            return InteractionResult.PASS;
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack itemInHand = player.getItemInHand(hand);
        ItemStack itemInOtherHand = player.getItemInHand(otherHand);

        if (!itemInOtherHand.isEmpty() && level.recipeAccess().propertySet(recipeSet).test(itemInOtherHand)) {
            ItemStack processingItem = itemInOtherHand.copyWithCount(1);
            itemInOtherHand.shrink(1);
            itemInHand.set(CDGDataComponents.PROCESSING_ITEM, new SandPaperItemComponent(processingItem));
            player.startUsingItem(hand);
            return InteractionResult.SUCCESS.heldItemTransformedTo(itemInHand);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player))
            return stack;
        SandPaperItemComponent component = stack.get(CDGDataComponents.PROCESSING_ITEM);
        if (component == null)
            return stack;
        ItemStack processingItem = component.item();
        stack.remove(CDGDataComponents.PROCESSING_ITEM);
        if (!(level instanceof ServerLevel serverLevel))
            return stack;

        SingleRecipeInput input = new SingleRecipeInput(processingItem);
        Optional<RecipeHolder<R>> recipe = serverLevel.recipeAccess().getRecipeFor(recipeType, input, level);
        if (recipe.isEmpty()) {
            player.getInventory().placeItemBackInInventory(processingItem);
            return stack;
        }
        for (ItemStack result : recipe.get().value().assemble(input, level.getRandom()))
            player.getInventory().placeItemBackInInventory(result.copy());
        stack.hurtAndBreak(1, serverLevel, player, i -> {});
        return stack;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int tick) {
        if (level.getGameTime() % 10 == 0) {
            level.playLocalSound(entity.xo, entity.yo, entity.zo, useSound(), SoundSource.PLAYERS, 0.3f, 1f, true);
            SandPaperItemComponent component = stack.get(CDGDataComponents.PROCESSING_ITEM);
            if (component != null) {
                ItemStack processingItem = component.item();
                for (int i = 0; i < 30; i++) {
                    Vec3 offset = VecHelper.offsetRandomly(entity.position().add(Math.sin(-entity.getYRot() / 180 * Math.PI) / 2, 1.3, Math.cos(-entity.getYRot() / 180 * Math.PI) / 2), level.getRandom(), .3f);
                    Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, level.getRandom(), .1f);
                    level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, processingItem), offset.x(), offset.y(),
                            offset.z(), motion.x(), motion.y(), motion.z());
                }
            }
        }
        super.onUseTick(level, entity, stack, tick);
    }

    // PORT-NOTE(26.1): also covers NeoForge's onStopUsing (vanilla calls releaseUsing when use is interrupted)
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int tick) {
        if (!(entity instanceof Player player))
            return false;
        SandPaperItemComponent component = stack.get(CDGDataComponents.PROCESSING_ITEM);
        if (component == null)
            return false;
        player.getInventory().placeItemBackInInventory(component.item());
        stack.remove(CDGDataComponents.PROCESSING_ITEM);
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 90;
    }
}
