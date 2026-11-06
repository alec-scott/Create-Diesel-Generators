package com.jesz.createdieselgenerators.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.jesz.createdieselgenerators.content.tools.lighter.LighterState;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.zurrtum.create.client.flywheel.lib.model.baked.ItemModelRenderHelper.submitQuads;

/**
 * Was LighterItemRenderer, LighterModel (standalone models) and the lighter_skins.json loading of ModEvents. The skin
 * is picked by the lower-cased display name of the stack, as upstream, and the state is the LIGHTER_STATE component
 * (synced with the stack, so other players' lighters render the same).
 */
public class LighterModel implements ItemModel {
    public static final Identifier ID = CreateDieselGenerators.rl("model/lighter");
    private static final Identifier MAIN_ID = CreateDieselGenerators.rl("item/lighter");
    private static final Identifier OPEN_ID = CreateDieselGenerators.rl("item/lighter_open");
    private static final Identifier IGNITED_ID = CreateDieselGenerators.rl("item/lighter_ignited");

    /** name (lower case) to skin id, as lighter_skins.json of every namespace lists them. */
    private static volatile Map<String, String> skinIds = Map.of();

    /** The three states of one skin: upstream's LighterSkinEntry. */
    public static final class Entry {
        final List<BakedQuad> closed;
        final List<BakedQuad> open;
        final List<BakedQuad> ignited;
        final Supplier<Vector3fc[]> closedExtents;
        final Supplier<Vector3fc[]> openExtents;
        final Supplier<Vector3fc[]> ignitedExtents;

        Entry(List<BakedQuad> closed, List<BakedQuad> open, List<BakedQuad> ignited) {
            this.closed = closed;
            this.open = open;
            this.ignited = ignited;
            closedExtents = CDGModelHelper.extentsOf(closed);
            openExtents = CDGModelHelper.extentsOf(open);
            ignitedExtents = CDGModelHelper.extentsOf(ignited);
        }

        static Entry bake(ModelBaker baker, Identifier closed, Identifier open, Identifier ignited) {
            return new Entry(
                CDGModelHelper.Baked.of(baker, closed).quads(),
                CDGModelHelper.Baked.of(baker, open).quads(),
                CDGModelHelper.Baked.of(baker, ignited).quads()
            );
        }
    }

    private static Identifier skinModel(String id, String suffix) {
        return CreateDieselGenerators.rl("item/lighter/" + id + suffix);
    }

    private final ModelRenderProperties settings;
    private final Entry standard;
    private final Map<String, Entry> skinsByName;

    private LighterModel(ModelRenderProperties settings, Entry standard, Map<String, Entry> skinsByName) {
        this.settings = settings;
        this.standard = standard;
        this.skinsByName = skinsByName;
    }

    @Override
    public void update(
        ItemStackRenderState state,
        @Nullable ItemStack stack,
        ItemModelResolver resolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel world,
        @Nullable ItemOwner user,
        int seed
    ) {
        LighterState lighterState = stack == null ? LighterState.CLOSED : stack.getOrDefault(CDGDataComponents.LIGHTER_STATE, LighterState.CLOSED);
        Entry entry = standard;
        if (stack != null) {
            Entry skin = skinsByName.get(stack.getHoverName().getString().toLowerCase(Locale.ROOT));
            if (skin != null)
                entry = skin;
        }
        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(entry);
        state.appendModelIdentityElement(lighterState);
        List<BakedQuad> quads;
        Supplier<Vector3fc[]> extents;
        if (lighterState == LighterState.CLOSED) {
            quads = entry.closed;
            extents = entry.closedExtents;
        } else if (lighterState == LighterState.OPEN) {
            quads = entry.open;
            extents = entry.openExtents;
        } else {
            quads = entry.ignited;
            extents = entry.ignitedExtents;
        }
        LayerRenderState layer = submitQuads(state, settings, displayContext, quads);
        layer.setExtents(extents);
        CDGModelHelper.foil(state, stack, layer);
    }

    public static class Unbaked implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        /** Reads lighter_skins.json of every namespace (upstream did this when the additional models were registered). */
        private static Map<String, String> loadSkinIds() {
            Map<String, String> ids = new HashMap<>();
            var manager = Minecraft.getInstance().getResourceManager();
            for (String namespace : manager.getNamespaces()) {
                Optional<Resource> resource = manager.getResource(Identifier.fromNamespaceAndPath(namespace, "lighter_skins.json"));
                if (resource.isEmpty())
                    continue;
                try (Reader reader = resource.get().openAsReader()) {
                    JsonElement data = JsonParser.parseReader(reader);
                    data.getAsJsonArray().forEach(element -> ids.put(
                        element.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                        element.getAsJsonObject().getAsJsonPrimitive("id").getAsString()
                    ));
                } catch (IOException | RuntimeException e) {
                    com.mojang.logging.LogUtils.getLogger().warn("Could not read lighter skins of namespace {}", namespace, e);
                }
            }
            return ids;
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            skinIds = loadSkinIds();
            resolver.markDependency(MAIN_ID);
            resolver.markDependency(OPEN_ID);
            resolver.markDependency(IGNITED_ID);
            for (String id : skinIds.values()) {
                resolver.markDependency(skinModel(id, ""));
                resolver.markDependency(skinModel(id, "_open"));
                resolver.markDependency(skinModel(id, "_ignited"));
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ModelRenderProperties settings = CDGModelHelper.Baked.of(baker, MAIN_ID).settings();
            Entry standard = Entry.bake(baker, MAIN_ID, OPEN_ID, IGNITED_ID);
            Map<String, Entry> byId = new HashMap<>();
            Map<String, Entry> byName = new HashMap<>();
            skinIds.forEach((name, id) -> byName.put(name, byId.computeIfAbsent(
                id,
                key -> Entry.bake(baker, skinModel(key, ""), skinModel(key, "_open"), skinModel(key, "_ignited"))
            )));
            return new LighterModel(settings, standard, byName);
        }
    }
}
