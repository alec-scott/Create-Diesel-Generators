#!/usr/bin/env bash
# Writes a 26.1 item model definition (assets/createdieselgenerators/items/<id>.json) for every committed top-level
# models/item/<id>.json. Datagen is stubbed, so this stands in for RegistrateItemModelProvider.run.
# Special cases (1.21.1 "overrides" predicates became data-driven selectors): oil_scanner (select on the
# oil_scanner_state component) and track_layers_bag (condition on the tracks component).
# Run from the repo root.
set -euo pipefail

mod=createdieselgenerators
out=src/generated/resources/assets/$mod/items
mkdir -p "$out"

for dir in src/generated/resources/assets/$mod/models/item src/main/resources/assets/$mod/models/item; do
    [ -d "$dir" ] || continue
    for f in "$dir"/*.json; do
        id=$(basename "$f" .json)
        case "$id" in lighter_open|lighter_ignited|wire_cutters_cut|oil_scanner_none|oil_scanner_medium|oil_scanner_high|track_layers_bag_filled) continue ;; esac
        printf '{\n  "model": {\n    "type": "minecraft:model",\n    "model": "%s:item/%s"\n  }\n}\n' "$mod" "$id" > "$out/$id.json"
    done
done

cat > "$out/oil_scanner.json" <<JSON
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:component",
    "component": "$mod:oil_scanner_state",
    "cases": [
      { "when": 1, "model": { "type": "minecraft:model", "model": "$mod:item/oil_scanner_none" } },
      { "when": 2, "model": { "type": "minecraft:model", "model": "$mod:item/oil_scanner_medium" } },
      { "when": 3, "model": { "type": "minecraft:model", "model": "$mod:item/oil_scanner_high" } }
    ],
    "fallback": { "type": "minecraft:model", "model": "$mod:item/oil_scanner" }
  }
}
JSON

cat > "$out/track_layers_bag.json" <<JSON
{
  "model": {
    "type": "minecraft:condition",
    "property": "minecraft:has_component",
    "component": "$mod:tracks",
    "on_true": { "type": "minecraft:model", "model": "$mod:item/track_layers_bag_filled" },
    "on_false": { "type": "minecraft:model", "model": "$mod:item/track_layers_bag" }
  }
}
JSON

echo "$(ls "$out" | wc -l) item definitions in $out"
