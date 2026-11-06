package com.jesz.createdieselgenerators;

import com.jesz.createdieselgenerators.content.distillation.DistillationControllerItem;
import com.jesz.createdieselgenerators.content.entity_filter.EntityFilterItem;
import com.jesz.createdieselgenerators.content.items.FurnaceBurnItem;
import com.jesz.createdieselgenerators.content.molds.MoldItem;
import com.jesz.createdieselgenerators.content.tools.ChemicalSprayerItem;
import com.jesz.createdieselgenerators.content.tools.OilScannerItem;
import com.jesz.createdieselgenerators.content.tools.hammer.HammerItem;
import com.jesz.createdieselgenerators.content.tools.lighter.LighterItem;
import com.jesz.createdieselgenerators.content.tools.wire_cutters.WireCuttersItem;
import com.jesz.createdieselgenerators.content.track_layers_bag.TrackLayersBagItem;
import com.jesz.createdieselgenerators.port.registrate.util.entry.ItemEntry;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import static com.jesz.createdieselgenerators.CreateDieselGenerators.REGISTRATE;

public class CDGItems {

    public static final ItemEntry<Item> KELP_HANDLE = REGISTRATE.item("kelp_handle", Item::new).register();

    public static final ItemEntry<FurnaceBurnItem> WOOD_CHIPS = REGISTRATE.item("wood_chip", p -> new FurnaceBurnItem(p, 200))
            .register();

    public static final ItemEntry<Item> ENGINE_PISTON = REGISTRATE.item("engine_piston", Item::new).register();

    public static final ItemEntry<Item> ENGINE_SILENCER = REGISTRATE.item("engine_silencer", Item::new).register();

    public static final ItemEntry<Item> ENGINE_TURBO = REGISTRATE.item("engine_turbocharger", Item::new).register();

    public static final ItemEntry<DistillationControllerItem> DISTILLATION_CONTROLLER = REGISTRATE.item("distillation_controller", DistillationControllerItem::new).register();

    public static final ItemEntry<LighterItem> LIGHTER = REGISTRATE.item("lighter", LighterItem::new)
            .properties(p -> p.enchantable(1))
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<ChemicalSprayerItem> CHEMICAL_SPRAYER = REGISTRATE.item("chemical_sprayer", p -> new ChemicalSprayerItem(p, false))
            .properties(p -> p.enchantable(1))
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<ChemicalSprayerItem> CHEMICAL_SPRAYER_LIGHTER = REGISTRATE.item("chemical_sprayer_lighter", p -> new ChemicalSprayerItem(p, true))
            .properties(p -> p.enchantable(1))
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<OilScannerItem> OIL_SCANNER = REGISTRATE.item("oil_scanner", OilScannerItem::new)
            .register();

    public static final ItemEntry<TrackLayersBagItem> TRACK_LAYERS_BAG = REGISTRATE.item("track_layers_bag", TrackLayersBagItem::new)
            .register();

    public static final ItemEntry<MoldItem> MOLD = REGISTRATE.item("mold", MoldItem::new).register();

    public static final ItemEntry<HammerItem> HAMMER = REGISTRATE.item("hammer", HammerItem::new)
            .properties(p -> p.durability(128))
            // PORT-NOTE(26.1): was HammerItem.getDefaultAttributeModifiers (NeoForge); Tiers are gone
            .properties(p -> p.attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 9, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .build()))
            .register();

    public static final ItemEntry<WireCuttersItem> WIRE_CUTTERS = REGISTRATE.item("wire_cutters", WireCuttersItem::new)
            .properties(p -> p.durability(128))
            .register();

    public static final ItemEntry<EntityFilterItem> ENTITY_FILTER = REGISTRATE.item("entity_filter", EntityFilterItem::new).register();

    public static void register() {}
}
