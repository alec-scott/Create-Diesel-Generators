package com.jesz.createdieselgenerators.content.entity_filter;

import net.minecraft.world.InteractionResult;

import com.zurrtum.create.foundation.gui.menu.MenuBase;
import com.zurrtum.create.foundation.gui.menu.MenuProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.component.TooltipDisplay;

import com.jesz.createdieselgenerators.CDGDataComponents;
import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.infrastructure.component.AttributeFilterWhitelistMode;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** PORT-NOTE(26.1): opens through Create Fly's MenuProvider (its menu registry and OpenScreenPacket). */
public class EntityFilterItem extends Item implements MenuProvider {
    public EntityFilterItem(Properties properties) {
        super(properties);
    }
    static List<EntityAttribute.EntityAttributeEntry> getEntries(ItemStack stack){
        return stack.getOrDefault(CDGDataComponents.ENTITY_FILTER_MATCHED_ATTRIBUTES, Collections.emptyList());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipComponents, tooltipFlag);

        List<Component> makeSummary = makeSummary(stack);
        if (makeSummary.isEmpty())
            return;
        tooltipComponents.accept(CommonComponents.SPACE);
        makeSummary.forEach(tooltipComponents);
    }

    private List<Component> makeSummary(ItemStack stack) {
        List<Component> list = new ArrayList<>();

        AttributeFilterWhitelistMode whitelistMode = stack.get(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE);

        list.add((whitelistMode == AttributeFilterWhitelistMode.WHITELIST_CONJ
                ? CreateLang.translateDirect("gui.attribute_filter.allow_list_conjunctive")
                : whitelistMode == AttributeFilterWhitelistMode.WHITELIST_DISJ
                ? CreateLang.translateDirect("gui.attribute_filter.allow_list_disjunctive")
                : CreateLang.translateDirect("gui.attribute_filter.deny_list")).withStyle(ChatFormatting.GOLD));

        int count = 0;

        for (EntityAttribute.EntityAttributeEntry entry : getEntries(stack)) {


            if (count > 5) {
                list.add(Component.literal("- ...")
                        .withStyle(ChatFormatting.DARK_GRAY));
                break;
            }
            list.add(Component.literal("- ")
                    .append(entry.attribute().format(entry.inverted())));
            count++;
        }

        if (count == 0)
            return Collections.emptyList();


        return list;
    }

    public static boolean test(ItemStack stack, Entity entity) {

        AttributeFilterWhitelistMode whitelistMode = stack.getOrDefault(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE, AttributeFilterWhitelistMode.WHITELIST_DISJ);
        AtomicBoolean passed = new AtomicBoolean(false);
        if (whitelistMode == AttributeFilterWhitelistMode.WHITELIST_CONJ)
            passed.set(true);
        getEntries(stack).forEach((entry) -> {
            boolean currentAttributePassed = entry.attribute().test(entity) ^ entry.inverted();
            if (whitelistMode != AttributeFilterWhitelistMode.WHITELIST_CONJ)
                if (!passed.get())
                    passed.set(currentAttributePassed);
            if (whitelistMode == AttributeFilterWhitelistMode.WHITELIST_CONJ)
                passed.set(currentAttributePassed && passed.get());
        });
        return passed.get()^whitelistMode == AttributeFilterWhitelistMode.BLACKLIST;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer sp)
            openHandledScreen(sp);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable MenuBase<?> createMenu(int id, Inventory inv, Player player, RegistryFriendlyByteBuf extraData) {
        ItemStack heldItem = player.getMainHandItem();
        ItemStack.STREAM_CODEC.encode(extraData, heldItem);
        return new EntityFilterMenu(id, inv, heldItem);
    }

    @Override
    public Component getDisplayName() {
        return getName(getDefaultInstance());
    }

}
