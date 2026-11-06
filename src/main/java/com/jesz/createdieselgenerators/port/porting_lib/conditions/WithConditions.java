package com.jesz.createdieselgenerators.port.porting_lib.conditions;

import java.util.List;

/** Datagen stub; see {@link com.jesz.createdieselgenerators.port.porting_lib.DatagenStubs}. */
public record WithConditions<A>(List<ICondition> conditions, A carrier) {
    public WithConditions(final A carrier, final ICondition... conditions) {
        this(List.of(conditions), carrier);
    }
}
