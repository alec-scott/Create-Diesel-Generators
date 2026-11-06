package com.jesz.createdieselgenerators.client;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.content.andesite_girder.AndesiteGirderWrenchBehaviour;
import com.jesz.createdieselgenerators.content.diesel_engine.EngineTypes;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.zurrtum.create.AllFluids;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.catnip.lang.Lang;
import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.client.infrastructure.config.AllConfigs;
import com.zurrtum.create.content.equipment.goggles.GogglesItem;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.infrastructure.config.CKinetics;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import java.util.Arrays;
import java.util.List;

import static net.minecraft.ChatFormatting.DARK_GRAY;
import static net.minecraft.ChatFormatting.GRAY;

/**
 * PORT-NOTE(26.1): the client game-bus handlers of GameEvents: ClientTickEvent → ClientTickEvents.END_CLIENT_TICK,
 * ItemTooltipEvent → Fabric's ItemTooltipCallback (fuel stats on buckets, engine capacity on engine items). Milk is
 * Create Fly's milk fluid (was NeoForgeMod.MILK). The entity filter overlay tick is gone (Create Fly's filter slot).
 */
public final class CDGClientEvents {
    private CDGClientEvents() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AndesiteGirderWrenchBehaviour.tick();
            TrackLayersBagPlacementClient.clientTick();
        });
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> addToItemTooltip(stack, lines));
    }

    private static void addToItemTooltip(ItemStack stack, List<Component> tooltip) {
        if (!AllConfigs.client().tooltips.get())
            return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null)
            return;
        if (tooltip.isEmpty())
            return;

        var fuelTypes = mc.level.registryAccess().lookup(CDGRegistries.FUEL_TYPE).orElse(null);
        if (fuelTypes == null)
            return;

        Item item = stack.getItem();
        if ((item instanceof BucketItem || stack.is(Items.MILK_BUCKET)) && CDGConfig.FUEL_TOOLTIPS.get()) {
            Fluid fluid = AllFluids.MILK;
            if (item instanceof BucketItem bi)
                fluid = bi.content;

            FuelType type = FuelType.getTypeFor(fuelTypes, fluid);

            if (mc.hasAltDown() && type.normal().speed() != 0) {
                tooltip.add(1, Component.translatable("createdieselgenerators.tooltip.holdForFuelStats", Component.translatable("createdieselgenerators.tooltip.keyAlt").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(2, Component.empty());

                byte enginesEnabled = (byte) ((EngineTypes.NORMAL.enabled() ? 1 : 0) + (EngineTypes.MODULAR.enabled() ? 1 : 0) + (EngineTypes.HUGE.enabled() ? 1 : 0));
                if (enginesEnabled == 0)
                    return;
                int currentEngineIndex = (AnimationTickHolder.getTicks() % (120)) / 20;
                List<EngineTypes> enabledEngines = Arrays.stream(EngineTypes.values()).filter(EngineTypes::enabled).toList();
                EngineTypes currentEngine = enabledEngines.get(currentEngineIndex % enginesEnabled);
                float currentSpeed = type.getGenerated(currentEngine).speed();
                float currentCapacity = type.getGenerated(currentEngine).strength();
                float currentBurn = type.getGenerated(currentEngine).burn();

                if (enginesEnabled != 1)
                    tooltip.add(3, Component.translatable("block.createdieselgenerators." +
                            (currentEngine == EngineTypes.MODULAR ? "large_" : currentEngine == EngineTypes.HUGE ? "huge_" : "") + "diesel_engine").withStyle(ChatFormatting.GRAY));
                tooltip.add(enginesEnabled != 1 ? 4 : 3, Component.translatable("createdieselgenerators.tooltip.fuelSpeed", CreateLang.number(currentSpeed).component().withStyle(FontHelper.Palette.STANDARD_CREATE.primary())).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(enginesEnabled != 1 ? 5 : 4, Component.translatable("createdieselgenerators.tooltip.fuelStress", CreateLang.number(currentCapacity).component().withStyle(FontHelper.Palette.STANDARD_CREATE.primary())).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(enginesEnabled != 1 ? 6 : 5, Component.translatable("createdieselgenerators.tooltip.fuelBurnRate", CreateLang.number(currentBurn * 20).component().withStyle(FontHelper.Palette.STANDARD_CREATE.primary())).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(enginesEnabled != 1 ? 7 : 6, Component.empty());
                tooltip.add(enginesEnabled != 1 ? 8 : 7, Component.translatable("createdieselgenerators.tooltip.burnerStrength", CreateLang.number(type.burnerStrength() * 100).text(" %").component().withStyle(FontHelper.Palette.STANDARD_CREATE.primary())).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(enginesEnabled != 1 ? 9 : 8, Component.empty());
            } else if (type.normal().speed() != 0) {
                tooltip.add(1, Component.translatable("createdieselgenerators.tooltip.holdForFuelStats", Component.translatable("createdieselgenerators.tooltip.keyAlt").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (!(item instanceof BlockItem bi) ||
                !(CDGBlocks.DIESEL_ENGINE.is(bi) ||
                        CDGBlocks.MODULAR_DIESEL_ENGINE.is(bi) ||
                        CDGBlocks.HUGE_DIESEL_ENGINE.is(bi)))
            return;

        CKinetics config = com.zurrtum.create.infrastructure.config.AllConfigs.server().kinetics;

        tooltip.add(Component.empty());

        int highestRPM = 0;
        int highestCapacity = 0;
        int highestStressCapacity = 0;

        for (var r : fuelTypes.listElements().toList()) {
            FuelType type = r.value();
            FuelType.PerEngineProperties props = CDGBlocks.DIESEL_ENGINE.is(bi) ? type.normal()
                    : CDGBlocks.MODULAR_DIESEL_ENGINE.is(bi) ? type.modular() : type.huge();
            if (props.speed() == 0)
                continue;
            highestRPM = (int) Math.max(highestRPM, props.speed());
            highestCapacity = (int) Math.max(highestCapacity, props.strength() / props.speed());
            highestStressCapacity = (int) Math.max(highestStressCapacity, props.strength());
        }
        boolean hasGoggles = GogglesItem.isWearingGoggles(player);

        LangBuilder rpmUnit = CreateLang.translate("generic.unit.rpm");
        LangBuilder suUnit = CreateLang.translate("generic.unit.stress");

        CreateLang.translate("tooltip.capacityProvided")
                .style(GRAY)
                .addTo(tooltip);

        IRotate.StressImpact impactId = highestCapacity >= config.highCapacity.get() ? IRotate.StressImpact.HIGH
                : (highestCapacity >= config.mediumCapacity.get() ? IRotate.StressImpact.MEDIUM : IRotate.StressImpact.LOW);
        IRotate.StressImpact opposite = IRotate.StressImpact.values()[IRotate.StressImpact.values().length - 2 - impactId.ordinal()];
        LangBuilder builder = CreateLang.builder()
                .add(CreateLang.text(TooltipHelper.makeProgressBar(3, impactId.ordinal() + 1))
                        .style(opposite.getAbsoluteColor()));

        if (hasGoggles) {
            builder.add(CreateLang.number(highestCapacity))
                    .text("x ")
                    .add(rpmUnit)
                    .addTo(tooltip);
            LangBuilder amount = CreateLang.number(highestStressCapacity)
                    .add(suUnit);
            CreateLang.text(" -> ")
                    .add(CreateLang.translate("tooltip.up_to", amount))
                    .style(DARK_GRAY)
                    .addTo(tooltip);
        } else
            builder.translate("tooltip.capacityProvided." + Lang.asId(impactId.name()))
                    .addTo(tooltip);
    }
}
