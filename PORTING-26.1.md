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
  Excluded in `build.gradle`: `events/datagen`, `compat/kubejs`, `compat/strut_your_stuff`
  (no Fabric 26.1 builds of KubeJS or Strut Your Stuff; the sources stay in the tree). Every Compat and Moonlight Lib do have
  26.1.2 Fabric builds (earlier log entries said otherwise: wrong). `kubejs.plugins.txt` removed. The hooks are gone: KubeJS lighter
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
  `CreateCategory`/`BasinCategory`. Compression molding uses Create Fly's press and basin, heat uses its burner; the other machines are
  picture-in-picture renderers in `client/render/gui` (`CDGGuiMachines`, registered through Fabric's
  `PictureInPictureRendererRegistry` by the client entrypoint, no JEI reference), drawn at upstream's coordinates, with
  upstream's slot, arrow, shadow and heat bar positions and sizes.
- **Restored (2026-11-06)**: toolsmith lighter trade is data (`createdieselgenerators:toolsmith/2/emerald_lighter` in `villager_trade`, added through
  `data/minecraft/tags/villager_trade/toolsmith/level_2.json`, which merges with vanilla's). Distillation tank glow is the
  `level` block state property as Create Fly's tanks (`DistillationTankBlock.LIGHT_LEVEL`, `lightLevel`; variants in the
  blockstate JSON omit it). Turret operator hat: `TurretOperatorHatLayer` through `LivingEntityRenderLayerRegistrationCallback`
  plus the client `LivingEntityRendererMixin` (render state data). Every Compat: `EveryCompatCompat` compiles against
  Moonlight Lib (compileOnly), called when `moonlight` is loaded.
- **Dropped**: the KubeJS lighter skin hooks (`LighterSkinsEventJS`; KubeJS has no 26.1 Fabric build), `ContraptionMixin` (Create Fly's
  `captureMultiblock` already localises `Controller`), batch-placement silencing for barrels' own sound type (now
  `ItemPlacementSoundContext`).

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

### 2026-11-06 (restore dropped features)
- Toolsmith lighter trade (`daf9a83`): upstream was `VillagerTradesEvent`, level 2, 5 emeralds for a lighter, 10 uses, 8 xp,
  multiplier 0.02. Same values as data; checked against 26.1 vanilla `trade_set/toolsmith/level_2` (`#minecraft:toolsmith/level_2`).
- Distillation tank glow (`d314d58`): upstream's luminosity rules kept (fluid light / 1.2, bright below the fill line or
  above it for lighter-than-air fluids, 1 elsewhere). Note that upstream never read the luminosity into a light level (no
  `lightLevel`/`getLightEmission` anywhere), so it had no visible effect; here it drives the block light, and like Create Fly's
  tank it is 0 when the tower has no windows.
- Turret operator hat (`58da0e5`): the earlier claim that `getTurretPos` is not synced was checked against upstream and is
  wrong in effect: the turret block entity ticks on both sides and sets it on the entities it finds in both worlds, so
  nothing needs syncing. Layer placement follows Create Fly's `HatFeatureRenderer`; access widener for `ModelPart.cubes`.
- Every Compat (`e116e0c`): Every Compat 26.1.2-3.0.0 (Fabric, version ZN6rPi46) exists, but upstream's code only uses Moonlight
  Lib's `BlockSetAPI`, so we compile against Moonlight 26.1.2-4.3.0 (HYd3L5kA) and gate on `moonlight`, as upstream did.

### 2026-11-06 (restore item visuals)
- **Mechanism**: custom `ItemModel` types registered through `ItemModels.ID_MAPPER` (`client/model/CDGItemModels`, called from
  the client entrypoint), as Create Fly does for the potato cannon, extendo grip and wands; item definitions in
  `src/generated/resources/assets/createdieselgenerators/items/` name them. Client only: nothing in common code references them
  (state comes from data components, which are synced with the stack). Access widener entries for `ItemStackRenderState`
  layers, `BlockEntityRenderState.blockState` and `displayContext`.
- **Transforms**: upstream drew in a space centred on the item and shifted the partial quads by -0.5 itself; 26.1 layers take
  quads in block space, then display transform, then -0.5, then `localTransform`. So every upstream transform `X` becomes
  `T(0.5) * X * T(-0.5)` (`CDGModelHelper.fromCentred`), and an item drawn inside the renderer (`renderStatic`) becomes
  `T(0.5) * X * (its own display transform pose)` under the host's display transform (`appendHeldItem`). The two
  conventions were derived from Create Fly's layer code and the Create 6 renderer; none of it has been seen on screen.
- Lighter (`0db2050`): `LighterModel`. State is the `LIGHTER_STATE` component, skin is `stack.getHoverName()` lower-cased looked
  up in `lighter_skins.json` names (every namespace, read in `resolveDependencies`, so a resource reload re-reads it), as upstream.
  The three states are baked per skin id; the display transforms come from `item/lighter`, as upstream used the main model.
- Hammer and wire cutters (`ce1f8cc`): `HammerModel`, `WireCuttersModel`, reading `PROCESSING_ITEM` (a synced component, so
  other players animate). Same timing and transforms as upstream for first person (with the left-hand flip), third person and
  the rest.
- Chemical sprayer (`05f9734`): `ChemicalSprayerModel`, both items (the definition names the body model). Cog about the item
  centre; fast while the holder is using the stack.
- Mold (`6dfdf46`): `MoldModel` over every `MoldType` (list stays open to addons). Molds in a basin: client mixins
  `BasinRendererMixin` (extract and submit heads of Create Fly's `BasinRenderer`) and `BasinRenderStateMixin` (carries the data);
  `client/render/MoldBasinRender`. As upstream, a basin holding a mold draws only the filter and its stacks (no fluid).
  Mixin audit: 16 classes, 0 problems.
- Not done: upstream's `renderItem` hook (a mold as a visualised output item while none is in the inventory) has no Create Fly
  counterpart worth a mixin; the canister was not touched, so its glint is unchanged.

### 2026-11-06 (JEI visuals)
- Upstream `compat/jei`: seven categories; machine elements `BasinFermentingStationElement`, `AnimatedBulkFermenter`, `AnimatedDistillationTower`, `AnimatedSpoutCastingStation`, `AnimatedDieselEngineElement`. The engine element is referenced by nothing upstream (`git grep` finds only its own file), so it is not ported; hammer and wire cutters are drawn as items.
- Pattern copied from Create Fly's `PressBasinRenderer` / `SpoutRenderer`: `PictureInPictureRenderer` with `BlockBakedQuadOutput`, `ModelRenderHelper`, `SinglePosVirtualBlockGetter`. Registration needs no mixin: Fabric API's `PictureInPictureRendererRegistry` (which also gives each state in a frame its own renderer, so several recipes per page do not share a texture). Commits `137aa85`, `b62f927`.
- Geometry: the renderer's frame is Create Fly's (z flip, X -15.5, Y 22.5) with the origin at the bottom centre of upstream's first block, so local coordinates are upstream's `atLocal` (y down). Upstream rotated about the block corner, which puts that bottom centre at (x + 15, y + 1.66) on screen; this matches Create Fly's press (box 91,-5 for upstream 91,34) and was only derived, not seen. Burner: Create Fly's `BasinBlazeBurnerRenderState` at upstream y + 14, as its `BasinCategory`.
- Layouts from upstream: heights 100 (distillation 200, tools 55), fermenting slots at x 17 (bulk 2), y 41, results y 51, heat slots y 81 (distillation 171), arrows, shadows, heat bar and label positions, icons (bulk fermenter + clock, hammer + iron sheet).
- Compression molding is still Create Fly's `BasinCategory` (slots of its packing page) with the height set to upstream's 100.

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
- [ ] `enchantable` is set on lighter, sprayers and canister, but `create:capacity` applicability comes from data: check
      these items are in its supported items tag.
- [x] Item visuals restored as custom item models (compile-checked only, see the session log): lighter states and skins,
      mold types, hammer and wire cutters animations, sprayer cog, molds in a basin. In-game checks are in `../port-testing`.
- [x] Fluid side restrictions restored from upstream (engine ports, modular PIPE top, burner not top, hole sides, turret bottom).
- [ ] Bulk fermenter "no new slot" rule is `canPlaceItem` on the wrapper: check Create Fly's insert honours it. Packager
      unpacking uses `packagerMode`.
- [ ] Check in game: gauges (dial offsets were moved from the pose stack into the buffers), crank rod angles, huge engine
      piston phases without the Flywheel instance, enchanted canister glint over the model, CT sheets (`diesel_engine_big`
      assumed CROSS), `pumpjack_crank/large_rod.json` still has `neoforge:obj`.
- [x] JEI: upstream layouts and machine drawings restored (compile-checked only; checks in `../port-testing`). Not drawn: `AnimatedDieselEngineElement`, which no upstream category uses.
- [ ] Sable: `SableAssemblyMixin` needs Sable present to test.

## Restoration boot check (2026-11-06)
Dedicated server boot and `/reload` clean with all restored features. Fluid side rules checked on the server by reading
Create pipe connection states next to each machine (16 checks, all pass): burner sides and top, normal engine in three
facings, turret, huge engine, modular engine, distillation tank, pumpjack hole. Note for repeating this: an unconnected
pipe defaults to straight east-west, so test along north-south. Everything client-side (item models, hat, JEI drawings)
is compile-checked only until the user runs `../port-testing/diesel-generators.md`.

## Engine bucket filling (2026-11-07)
`content/diesel_engine/EngineItemFilling` replaces `FluidHelper.tryEmptyItemIntoBE` on all three engines, which needed the
whole bucket to fit in a 100 mB tank and so always refused. Upstream's rules apply: gated by the server config "Engines
filled with a bucket" (default false, as upstream), a bucket fills an empty tank and the excess is lost, fluid items give
what fits. Deviation: a bucket on a tank already holding fluid does nothing; upstream's FAIL let the client spill it.

## Jade fluid decimals (2026-11-07)
Not an upstream feature. Jade shows part of a mB as a fraction on Fabric (553 17/27mB) and has no option for it, while Create's
goggles show 553.48mB. `JadeFluidTextHelperMixin` (client, applied only when `jade` is loaded) makes Jade use up to two decimals.
It changes Jade's text for all fluids. Compiled against Jade 26.1.11 (compileOnly).
