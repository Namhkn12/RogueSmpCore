package com.roguesmp.player.inbox;

import com.roguesmp.codec.Codec;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Inbox {

    public static final Codec<Inbox> CODEC = Codec.composite(
            Codec.UUID.fieldOf("uuid").forGetter(Inbox::getUuid),
            Codec.lenientListOf(InboxEntry.CODEC).optionalFieldOf("entries", List.of()).forGetter(Inbox::getEntries),
            Inbox::new
    );

    private final UUID uuid;
    private final List<InboxEntry> entries = new ArrayList<>();
    private boolean dirty;

    public Inbox(UUID uuid, List<InboxEntry> entries) {
        this.uuid = uuid;
        this.entries.addAll(entries);
    }

    public Inbox(UUID uuid) {
        this.uuid = uuid;
        this.dirty = true;
    }

    public Inbox(Inbox toClone) {
        this(toClone.uuid, toClone.entries);
    }

    public UUID getUuid() {
        return uuid;
    }

    public @Unmodifiable List<InboxEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void add(InboxEntry entry) {
        if (entries.stream().anyMatch(existing -> existing.id().equals(entry.id()))) return;
        entries.add(entry);
        dirty = true;
    }

    public @Nullable InboxEntry remove(UUID entryId) {
        for (Iterator<InboxEntry> iterator = entries.iterator(); iterator.hasNext(); ) {
            InboxEntry entry = iterator.next();
            if (!entry.id().equals(entryId)) continue;
            iterator.remove();
            dirty = true;
            return entry;
        }
        return null;
    }

    public void expire(long now) {
        if (entries.removeIf(entry -> entry.isExpired(now))) dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
