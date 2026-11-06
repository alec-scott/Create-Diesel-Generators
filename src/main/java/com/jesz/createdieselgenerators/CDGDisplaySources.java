package com.jesz.createdieselgenerators;

import com.jesz.createdieselgenerators.content.pumpjack.PumpjackOilAmountDisplaySource;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.jesz.createdieselgenerators.port.registrate.util.entry.RegistryEntry;

import static com.jesz.createdieselgenerators.CreateDieselGenerators.REGISTRATE;

public class CDGDisplaySources {
    public static final RegistryEntry<DisplaySource, PumpjackOilAmountDisplaySource> PUMPJACK_OIL_AMOUNT = simple("pumpjack_oil_amount", PumpjackOilAmountDisplaySource::new);

    private static <T extends DisplaySource> RegistryEntry<DisplaySource, T> simple(String name, com.jesz.createdieselgenerators.port.registrate.util.nullness.NonNullSupplier<T> supplier) {
        return REGISTRATE.displaySource(name, supplier).register();
    }

    public static void register() {
    }
}
