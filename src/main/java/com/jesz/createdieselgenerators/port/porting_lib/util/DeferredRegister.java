package com.jesz.createdieselgenerators.port.porting_lib.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/**
 * PORT-NOTE(26.1): minimal stand-in for NeoForge's {@code DeferredRegister}. Fabric registries are open during mod
 * init, so entries are registered immediately; the returned {@link DeferredHolder} resolves through the registry.
 */
public class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;

    private DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
        return new DeferredRegister<>(registry.key(), namespace);
    }

    @SuppressWarnings("unchecked")
    private Registry<T> registry() {
        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
        if (registry == null) throw new IllegalStateException("Unknown registry " + registryKey);
        return registry;
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, name);
        ResourceKey<T> key = ResourceKey.create(registryKey, id);
        Registry.register(registry(), key, factory.get());
        return DeferredHolder.create(key);
    }
}
