package com.leon.saintsdragons.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class WeakIdentityCache<K, V> {
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();
    private final Map<Key<K>, V> entries = new HashMap<>();

    public V computeIfAbsent(K key, Function<K, V> factory) {
        for (var expired = queue.poll(); expired != null; expired = queue.poll()) entries.remove(expired);
        Key<K> lookup = new Key<>(key, null);
        V value = entries.get(lookup);
        if (value == null) {
            value = factory.apply(key);
            entries.put(new Key<>(key, queue), value);
        }
        return value;
    }

    private static final class Key<K> extends WeakReference<K> {
        private final int hash;

        Key(K key, ReferenceQueue<K> queue) {
            super(key, queue);
            hash = System.identityHashCode(key);
        }

        @Override public int hashCode() { return hash; }

        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key<?> key && get() != null && get() == key.get();
        }
    }
}
