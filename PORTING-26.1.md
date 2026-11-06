# 26.1.2 Fabric Port — Working Log

Branch `26.1-fabric`, from upstream `1.21.1` (Create Diesel Generators 1.21.1-1.3.15, Create 6.0.10, Minecraft
1.21.1, NeoForge). Goal: CDG on **Fabric 26.1.2**, running on **Create Fly** (our fork at `../Create-Fly`), alongside
the other ports (Steam 'n' Rails, Aeronautics, Big Cannons, D&D, Copycats+, Crafts & Additions, New Age).

Licence: MIT. Push or release only when the user asks.

This file tracks the port's state so work can resume across sessions. Update it as you go.

## Starting point

| | |
|---|---|
| Upstream | `george8188625/Create-Diesel-Generators` (`upstream`); fork `alec-scott/Create-Diesel-Generators` (`origin`) |
| Base branch | `1.21.1` (`79ce136`): one NeoForge module, 200 Java files, 1 mixin config, Registrate |
| Optional compat upstream | JEI, CC:Tweaked, Sable / Sable Companion / Aeronautics / Offroad |

Three ports at once, as for the earlier ports (closest template: `../createaddition/PORTING-26.1.md`, same shape,
including Sable and CC compat):
1. **NeoForge → Fabric.**
2. **Minecraft 1.21.1 → 26.1.2.**
3. **Create → Create Fly** (`com.zurrtum.create`, client under `com.zurrtum.create.client.*`).

## Building

```sh
export JAVA_HOME=/c/Users/akrsc/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon   # Git Bash, JDK 25
./gradlew compileJava --console=plain
```

## Decisions

- **Base**: upstream `1.21.1` (Create 6).
- **Build**: the CC&A template (non-remap Loom 1.17, Java 25, Fabric API 0.155.3, Forge Config API Port 26.1.5).
- **Registrate**: relocated Registrate shim + datagen stubs copied from CC&A into `com.jesz.createdieselgenerators.port.*`.
- **Fluids**: Fabric transfer API, droplets (81 per mB), as Create Fly.
- **Compat**: CC:Tweaked 1.120.0, JEI 29.47 and Sable (our Aeronautics port, `sable_jar` compile-only) handled as in
  CC&A: optional, compile-only, no class loading when absent. Fuels feed CC&A's liquid blaze burner via `c:` fluid tags
  (diesel, gasoline, biodiesel, crude oil, ethanol, plant oil): keep those tags.

## Session log

### 2026-11-06
- Forked to `alec-scott/Create-Diesel-Generators`, cloned, `upstream` remote added, `26.1-fabric` created from
  `upstream/1.21.1`.

## TODO

- [ ] Phase 1: build migration, shims, imports mapped, entrypoints/registration/configs/networking.
- [ ] Fluids and fuel system on Fabric transfer API.
- [ ] Compile clean (block/BE APIs, renderers to extract/submit, mixin retargets).
- [ ] Resources: item definitions, recipe/data conversion (`porting-tools/`), worldgen (oil deposits).
- [ ] Compat: JEI, CC:Tweaked, Sable/Aeronautics.
- [ ] Dedicated server boot (`../../railways-test-server`).
- [ ] Client test (user launches the game); add checks to `../port-testing/`.
