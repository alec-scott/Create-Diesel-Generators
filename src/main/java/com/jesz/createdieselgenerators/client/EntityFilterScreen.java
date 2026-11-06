package com.jesz.createdieselgenerators.client;

import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.jesz.createdieselgenerators.content.entity_filter.EntityAttribute;
import com.jesz.createdieselgenerators.content.entity_filter.EntityFilterMenu;
import com.jesz.createdieselgenerators.packets.EntityFilterScreenPacket;
import com.zurrtum.create.client.content.logistics.filter.AbstractFilterScreen;
import com.zurrtum.create.client.foundation.gui.AllGuiTextures;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import com.zurrtum.create.client.foundation.gui.widget.IconButton;
import com.zurrtum.create.client.foundation.gui.widget.Label;
import com.zurrtum.create.client.foundation.gui.widget.ScrollInput;
import com.zurrtum.create.client.foundation.gui.widget.SelectionScrollInput;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.foundation.gui.menu.MenuType;
import com.zurrtum.create.infrastructure.component.AttributeFilterWhitelistMode;
import com.zurrtum.create.infrastructure.packet.c2s.FilterScreenPacket.Option;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PORT-NOTE(26.1): rebuilt on Create Fly's AttributeFilterScreen (26.1 GUI extract API); the entity attributes and the
 * CDG lang keys are as upstream. Options go to the server as {@link EntityFilterScreenPacket}.
 */
public class EntityFilterScreen extends AbstractFilterScreen<EntityFilterMenu> {
    private IconButton whitelistDis, whitelistCon, blacklist;
    private IconButton add;
    private IconButton addInverted;

    private ItemStack lastItemScanned = ItemStack.EMPTY;
    private final List<EntityAttribute> attributesOfItem = new ArrayList<>();
    private final List<Component> selectedAttributes = new ArrayList<>();
    private SelectionScrollInput attributeSelector;
    private Label attributeSelectorLabel;

    public EntityFilterScreen(EntityFilterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, AllGuiTextures.ATTRIBUTE_FILTER);
    }

    @Nullable
    public static EntityFilterScreen create(Minecraft mc, MenuType<ItemStack> type, int syncId, Inventory inventory,
                                            Component title, RegistryFriendlyByteBuf extraData) {
        return type.create(EntityFilterScreen::new, syncId, inventory, title, getStack(extraData));
    }

    private static MutableComponent lang(String key) {
        return CreateDieselGenerators.lang("gui.entity_filter." + key).plainCopy();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (getChildAt(mouseX, mouseY).filter(element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)).isPresent())
            return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void init() {
        setWindowOffset(-11, 7);
        super.init();

        whitelistDis = new IconButton(leftPos + 38, topPos + 61, AllIcons.I_WHITELIST_OR);
        whitelistDis.withCallback(() -> {
            menu.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_DISJ;
            sendOptionUpdate(Option.WHITELIST);
        });
        whitelistDis.setToolTip(lang("allow_list_disjunctive"));
        whitelistCon = new IconButton(leftPos + 56, topPos + 61, AllIcons.I_WHITELIST_AND);
        whitelistCon.withCallback(() -> {
            menu.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_CONJ;
            sendOptionUpdate(Option.WHITELIST2);
        });
        whitelistCon.setToolTip(lang("allow_list_conjunctive"));
        blacklist = new IconButton(leftPos + 74, topPos + 61, AllIcons.I_WHITELIST_NOT);
        blacklist.withCallback(() -> {
            menu.whitelistMode = AttributeFilterWhitelistMode.BLACKLIST;
            sendOptionUpdate(Option.BLACKLIST);
        });
        blacklist.setToolTip(lang("deny_list"));

        addRenderableWidgets(blacklist, whitelistCon, whitelistDis);

        addRenderableWidget(add = new IconButton(leftPos + 182, topPos + 26, AllIcons.I_ADD));
        addRenderableWidget(addInverted = new IconButton(leftPos + 200, topPos + 26, AllIcons.I_ADD_INVERTED_ATTRIBUTE));
        add.withCallback(() -> handleAddedAttribute(false));
        add.setToolTip(lang("add_attribute"));
        addInverted.withCallback(() -> handleAddedAttribute(true));
        addInverted.setToolTip(lang("add_inverted_attribute"));

        handleIndicators();

        attributeSelectorLabel = new Label(leftPos + 43, topPos + 31, CommonComponents.EMPTY).colored(0xFFF3EBDE).withShadow();
        attributeSelector = new SelectionScrollInput(leftPos + 39, topPos + 26, 137, 18);
        attributeSelector.forOptions(Arrays.asList(CommonComponents.EMPTY));
        attributeSelector.removeCallback();
        referenceItemChanged(menu.ghostInventory.getItem(0));

        addRenderableWidget(attributeSelector);
        addRenderableWidget(attributeSelectorLabel);

        selectedAttributes.clear();
        selectedAttributes.add((menu.selectedAttributes.isEmpty() ? lang("no_selected_attributes") : lang("selected_attributes"))
                .withStyle(ChatFormatting.YELLOW));
        menu.selectedAttributes.forEach(at -> selectedAttributes.add(Component.literal("- ")
                .append(at.attribute().format(at.inverted()))
                .withStyle(ChatFormatting.GRAY)));
    }

    private void referenceItemChanged(ItemStack stack) {
        lastItemScanned = stack;

        if (stack.isEmpty()) {
            attributeSelector.active = false;
            attributeSelector.visible = false;
            attributeSelectorLabel.text = lang("add_reference_item").withStyle(ChatFormatting.ITALIC);
            add.active = false;
            addInverted.active = false;
            attributeSelector.calling(s -> {
            });
            return;
        }

        add.active = true;
        addInverted.active = true;
        attributeSelector.titled(CreateLang.text(stack.getHoverName().getString() + "...")
                .color(ScrollInput.HEADER_RGB.getRGB()).component());
        attributesOfItem.clear();
        for (EntityAttribute entityAttribute : EntityAttribute.ALL)
            attributesOfItem.addAll(entityAttribute.listAttributesOf(stack));
        List<Component> options = attributesOfItem.stream().map(a -> a.format(false)).collect(Collectors.toList());
        attributeSelector.forOptions(options);
        attributeSelector.active = true;
        attributeSelector.visible = true;
        attributeSelector.setState(0);
        attributeSelector.calling(i -> {
            if (options.isEmpty())
                return;
            attributeSelectorLabel.setTextAndTrim(options.get(i), true, 112);
            EntityAttribute selected = attributesOfItem.get(i);
            for (var existing : menu.selectedAttributes) {
                if (existing.attribute().write().equals(selected.write())) {
                    add.active = false;
                    addInverted.active = false;
                    return;
                }
            }
            add.active = true;
            addInverted.active = true;
        });
        attributeSelector.onChanged();
    }

    @Override
    public void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        ItemStack stack = menu.ghostInventory.getItem(1);
        graphics.itemDecorations(font, stack, leftPos + 16, topPos + 62, String.valueOf(selectedAttributes.size() - 1));
        super.renderForeground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack stackInSlot = menu.ghostInventory.getItem(0);
        if (!ItemStack.matches(stackInSlot, lastItemScanned))
            referenceItemChanged(stackInSlot);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) {
            if (hoveredSlot.index == 37) {
                graphics.setComponentTooltipForNextFrame(font, selectedAttributes, mouseX, mouseY);
                return;
            }
            graphics.setTooltipForNextFrame(font, hoveredSlot.getItem(), mouseX, mouseY);
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected List<IconButton> getTooltipButtons() {
        return Arrays.asList(blacklist, whitelistCon, whitelistDis);
    }

    @Override
    protected List<MutableComponent> getTooltipDescriptions() {
        return Arrays.asList(lang("deny_list.description"), lang("allow_list_conjunctive.description"),
                lang("allow_list_disjunctive.description"));
    }

    protected boolean handleAddedAttribute(boolean inverted) {
        int index = attributeSelector.getState();
        if (index >= attributesOfItem.size())
            return false;
        add.active = false;
        addInverted.active = false;
        EntityAttribute attribute = attributesOfItem.get(index);
        ClientPlayNetworking.send(new EntityFilterScreenPacket(inverted ? Option.ADD_INVERTED_TAG : Option.ADD_TAG, attribute));
        menu.appendSelectedAttribute(attribute, inverted);
        if (menu.selectedAttributes.size() == 1)
            selectedAttributes.set(0, lang("selected_attributes").withStyle(ChatFormatting.YELLOW));
        selectedAttributes.add(Component.literal("- ").append(attribute.format(inverted)).withStyle(ChatFormatting.GRAY));
        return true;
    }

    @Override
    protected void sendOptionUpdate(Option option) {
        ClientPlayNetworking.send(new EntityFilterScreenPacket(option, EntityAttribute.IS_MOB));
    }

    @Override
    protected void contentsCleared() {
        selectedAttributes.clear();
        selectedAttributes.add(lang("no_selected_attributes").withStyle(ChatFormatting.YELLOW));
        if (!lastItemScanned.isEmpty()) {
            add.active = true;
            addInverted.active = true;
        }
    }

    @Override
    protected boolean isButtonEnabled(IconButton button) {
        if (button == blacklist)
            return menu.whitelistMode != AttributeFilterWhitelistMode.BLACKLIST;
        if (button == whitelistCon)
            return menu.whitelistMode != AttributeFilterWhitelistMode.WHITELIST_CONJ;
        if (button == whitelistDis)
            return menu.whitelistMode != AttributeFilterWhitelistMode.WHITELIST_DISJ;
        return true;
    }
}
