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

Error triage: `perl porting-tools/errs_summary.pl build.txt` and `perl porting-tools/errs_by_file.pl build.txt`. Mixin
audit: `./gradlew -q --init-script porting-tools/cp.init.gradle printCompileClasspath -DcpOutDir=<dir>`, then
`MIXIN_MODULES=. perl porting-tools/check_mixin_targets.pl . $JAVA_HOME/bin/javap <dir>/cp-createdieselgenerators.txt`.
New tools here: `add_imports.pl`, `prune_imports.pl`, `remove_method.pl`, `fix_hover_text.pl`.

## Decisions

- **Base**: upstream `1.21.1` (Create 6).
- **Build**: the CC&A template (non-remap Loom 1.17, Java 25, Fabric API 0.155.3, Forge Config API Port 26.1.5).
  Excluded in `build.gradle`: `events/datagen`, `compat/kubejs`, `compat/strut_your_stuff`, `compat/EveryCompatCompat`
  (no Fabric 26.1 builds; the sources stay in the tree). `kubejs.plugins.txt` removed. The hooks are gone: KubeJS lighter
  skins, fuel types and oil amount events, Struts entries in the creative tab.
- **Registrate**: relocated Registrate shim + datagen stubs copied from CC&A into `com.jesz.createdieselgenerators.port.*`;
  datagen builder calls stripped (committed `src/generated/resources` are used). The `*Generator` datagen classes are deleted.
- **Fluids**: Fabric transfer API, droplets (81 per mB), as Create Fly. `port/CDGFluidUnits` converts. mB stay mB in
  configs, fuel type burn rates, chunk oil (`OilChunksSavedData`) and goggle/tooltip text. Fuels feed CC&A's liquid blaze
  burner via `c:` fluid tags: keep those tags.
- **Item fluids**: `fluid_contents` component is `port/neoforge/SimpleFluidContent` (immutable record; Create Fly's
  FluidStack has no equals). `FueledToolItem.Storage` is a Fabric `SingleVariantItemStorage` on `FluidStorage.ITEM`.
- **Capabilities**: `port/neoforge/capabilities` shim (items and fluids, no energy) over Create Fly's cached inventory
  behaviours. Providers decide per side as upstream did (engine ports, modular PIPE top, burner not top, hole sides, turret bottom). Multiblocks reset the cache in `refreshCapability` (was
  `invalidateCapabilities`).
- **Recipes**: Create Fly-style records, plain registry fields in `CDGRecipes`, serializers synced when JEI is loaded.
  JSON shapes (resource commit `b64aeaf`): `hammering`, `wire_cutting` = `{ingredient, results}`; `casting` =
  `{fluid_ingredient, mold, results}`; `distillation` = `{fluid_ingredient, fluid_results (1-6), heat_requirement?,
  processing_time}`; `basin_fermenting`, `bulk_fermenting` = `{ingredients, fluid_ingredients, results, fluid_results,
  heat_requirement?, processing_time}`; `compression_molding` = `{ingredients, fluid_ingredients?, mold,
  heat_requirement?, results}` (`SizedIngredient.LIST_CODEC` merges repeats). Compression molding checks the mold in
  `matches`/`apply` (was `BasinRecipeMixin`); the press accepts it through `MechanicalPressBlockEntityMixin`.
- **Hammer / wire cutters**: shared `content/tools/ProcessingToolItem`. The client has no recipe manager, so the off-hand
  check uses `RecipePropertySet`s added to Create Fly's `AllRecipeSets.ALL`; results roll on the server only.
- **Oil**: chunk based (`OilChunksSavedData`, biome tags); there are no biome modifiers, so no `BiomeModifications`.
- **Client split**: goggle tooltips, value boxes, filter slots and sounds are client behaviours
  (`client/CDGBlockEntityBehaviours`, `CDGSoundBehaviours`). Block entities keep `addToGoggleTooltip` and implement Create
  Fly's client-package `IHaveGoggleInformation` (same as CC&A, which was verified on a dedicated server). Common code
  reaches client code only through static hooks set by `CreateDieselGeneratorsClient` (`clientPourEffect`,
  `clientFluidParticle`, `DieselEngineMovementBehaviour.clientTick`). Recipe durations are synced in the update packets
  for the gauges (`RecipeDuration`).
- **Rendering**: extract/submit through `client/render/PartsRenderer` and `PartsKineticRenderer`. The custom Flywheel
  instances are deleted, their parts are drawn by the block entity renderers; only shafts keep `ShaftVisual` /
  `SingleAxisRotatingVisual`. The pumpjack rope on contraptions is a `MovementRenderBehaviour`.
- **Entity filter**: Create Fly's menu registry and `AllMenuScreens`; the turret uses a `ServerFilteringBehaviour`
  subclass, so Create Fly's filter slot replaces the custom `EntityFilteringRenderer` overlay. Screen rebuilt on Create
  Fly's `AttributeFilterScreen`.
- **Projectile sync**: `EntityDataSerializers.COMPOUND_TAG` is gone; fire, cooling and the fluid id are separate entries
  (fluid components are not synced, they only matter on the server).
- **Explosions / dropped lighter**: `ServerExplosionMixin` (was `ExplosionEvent.Detonate`), `ItemEntityMixin` (was
  `onEntityItemUpdate`).
- **CC:Tweaked**: `compat/computercraft/CCProxy` as Create Fly's `AllComputerPeripherals` / New Age: `ComputerBehaviour`
  per type caches the peripheral for `PeripheralLookup`, only called when `computercraft` is loaded.
- **Sable**: upstream shipped Sable Companion jar-in-jar; here it comes with Sable, so `port/CDGSable` falls back to the
  plain position without it (`compat/sable/CDGSableCompat`). `SableAssemblyMixin` is gated by `CDGMixinPlugin` (the plugin
  is now actually named in the mixin config). Aeronautics, Simulated and Offroad have no code in CDG.
- **JEI**: `jei_mod_plugin` entrypoint `compat/jei/CDGJEI`; all seven categories in `CDGCategories` on Create Fly's
  `CreateCategory`/`BasinCategory`. Compression molding uses Create Fly's press, heat uses its burner; the other machines
  are scaled items, not upstream's animations.
- **Dropped**: lighter skins and `lighter_skins.json` loading, the custom item renderers (hammer, wire cutters, lighter,
  sprayer, mold, and mold-in-basin via `BasinRendererMixin`), turret operator hat layer, `ContraptionMixin` (Create Fly's
  `captureMultiblock` already localises `Controller`), batch-placement silencing for barrels' own sound type (now
  `ItemPlacementSoundContext`), fluid luminosity in the distillation tank, toolsmith lighter trade (26.1 trades are data).

## Port mechanics (how things map)

| 1.21.1 NeoForge / Create 6 | 26.1.2 Fabric / Create Fly here |
|---|---|
| `IFluidHandler.fill(s, EXECUTE/SIMULATE)` | `FluidInventory.insert(s)` / `countSpace(s)` |
| `drain(s or n, EXECUTE)` | `extract(s[, n])`, `extractAny(n)`; `getFluidInTank(i)` → `getStack(i)` |
| `SmartFluidTank`, `FluidTank` callback | `foundation.fluid.FluidTank`, override `markDirty` |
| `ItemStackHandler`, `CombinedInvWrapper` | `infrastructure.items.*`; `onContentsChanged` → `setChanged` |
| `onRemove` | `BlockEntity.preRemoveSideEffects` |
| `ScrollOptionBehaviour` on the BE | `ServerScrollOptionBehaviour` + client behaviour |
| `renderSafe` | `extractRenderState` + `submit` |
| `BakedModelWrapperWithData`, `ModelData` | `WrapperBlockStateModel.addPartsWithInfo` |
| `ItemTooltipEvent`, `ClientTickEvent` | `ItemTooltipCallback`, `ClientTickEvents` |
| `DataPackRegistryEvent.NewRegistry` | `DynamicRegistries.registerSynced` |
| `getBurnTime` | `FuelValueEvents.BUILD` |
| `new ChunkPos(BlockPos)`, `pos.x` | `ChunkPos.containing(pos)`, `pos.x()` |
| `DyeItem.getDyeColor()` | `DataComponents.DYE` |

## Session log

### 2026-11-06
- Forked to `alec-scott/Create-Diesel-Generators`, cloned, `upstream` remote added, `26.1-fabric` created from
  `upstream/1.21.1`.
- Build migrated, shims copied, imports mapped (`6f7ea81`). Resources converted by a parallel agent (`b64aeaf`).
- Registration, recipes, networking, events, fluids, fermenter and distillation (`efeff38`); engines, barrel, burner,
  canister, pumpjack, turret, filter, CC (`ac6596a`); renderers and models, first clean compile (`f80694b`); client
  entrypoint (`fc5ffda`); JEI, pumpjack rope, capability cache resets.
- `./gradlew build -x test` succeeds; mixin audit: 13 classes, 0 problems. **Nothing has been run**: no server boot, no
  client, as instructed for this session. All behaviour below is compile-checked only.

## TODO

- [x] Phase 1: build migration, shims, imports mapped, entrypoints/registration/configs/networking.
- [x] Fluids and fuel system on Fabric transfer API.
- [x] Compile clean (block/BE APIs, renderers to extract/submit, mixin retargets).
- [x] Resources: item definitions, recipe/data conversion.
- [x] Compat: JEI, CC:Tweaked, Sable.
- [x] Dedicated server boot: clean with Create Fly, Steam 'n' Rails, D&D, Copycats+, CC&A, New Age. 64 blocks placed and
      ticking, `/reload` clean. Fixed at boot (`3095144`): `minecraft:chain` is `iron_chain`; mixed tag+item ingredient
      lists need `fabric:any`; strut loot table removed (block excluded).
- [ ] Client test (user launches the game): checks are in `../port-testing/diesel-generators.md`.
- [ ] Toolsmith lighter trade: 26.1 data files (`villager_trade` plus tag). The code path is removed.
- [ ] `enchantable` is set on lighter, sprayers and canister, but `create:capacity` applicability comes from data: check
      these items are in its supported items tag.
- [ ] Item visuals: lighter open/ignited and skins, mold types, hammer and wire cutters animations (static models now;
      lighter state and mold type could be `minecraft:select` item models). Molds in a basin render as plain items.
- [ ] Turret operator hat layer (`IEntity.getTurretPos` is not synced to clients anyway).
- [x] Fluid side restrictions restored from upstream (engine ports, modular PIPE top, burner not top, hole sides, turret bottom).
- [ ] Bulk fermenter "no new slot" rule is `canPlaceItem` on the wrapper: check Create Fly's insert honours it. Packager
      unpacking uses `packagerMode`.
- [ ] Check in game: gauges (dial offsets were moved from the pose stack into the buffers), crank rod angles, huge engine
      piston phases without the Flywheel instance, enchanted canister glint over the model, CT sheets (`diesel_engine_big`
      assumed CROSS), `pumpjack_crank/large_rod.json` still has `neoforge:obj`.
- [ ] JEI: layouts are estimates; machine animations (distiller, fermenter, engine) would need picture-in-picture renderers.
- [ ] Sable: `SableAssemblyMixin` needs Sable present to test.
