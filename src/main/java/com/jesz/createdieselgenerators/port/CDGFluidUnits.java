package com.jesz.createdieselgenerators.port;

/**
 * PORT-NOTE(26.1): Create Fly measures fluids in droplets (81 per mB, Fabric's convention). Upstream amounts in
 * code, configs and fuel types are mB and are converted with these helpers.
 */
public final class CDGFluidUnits {
    public static final int MB = 81;
    public static final int BUCKET = 81000;

    private CDGFluidUnits() {
    }

    public static int mb(int millibuckets) {
        return millibuckets * MB;
    }

    public static int toMb(int droplets) {
        return droplets / MB;
    }
}
