package com.sehatharian.util;

import com.sehatharian.model.UserSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Holds loaded private pages for one session, never for an implicit current user. */
public final class PrivatePageCache<T> implements AutoCloseable {
    private final UserSession session;
    private final Runnable detach;
    private final Map<String, T> pages = new HashMap<>();

    public PrivatePageCache(UserSession session) {
        this.session = Objects.requireNonNull(session);
        detach = session.onIdentityChanged(this::clear);
    }

    public T get(String key) { session.requireActiveUser(); return pages.get(key); }
    public void put(String key, T page) {
        session.requireActiveUser();
        T old = pages.put(Objects.requireNonNull(key), Objects.requireNonNull(page));
        if (old != page) release(old);
    }
    public void clear() {
        var old = java.util.List.copyOf(pages.values());
        pages.clear();
        for (T page : old) release(page);
    }
    private void release(T page) {
        if (page instanceof AutoCloseable resource) {
            try { resource.close(); }
            catch (Exception e) { System.err.println("Resource halaman gagal dilepas."); }
        }
    }
    @Override public void close() { clear(); detach.run(); }
    public int size() { return pages.size(); }
}
